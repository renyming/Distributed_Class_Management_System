import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Object;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;

//The package containing our stubs
import DCMSApp.*;
//HelloServer will use the naming service
import org.omg.CosNaming.*;
//The package containing special exceptions thrown by the name service
//All CORBA applications need these classes
import org.omg.CORBA.*;
//Classes needed for the Portable Server Inheritance Model
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

public class CenterServer extends DCMSPOA {

    //========  Static Global Variables===========
    //Server name array, to facilitate loop operation on array
    private static final String[] serverName = {"MTL", "LVL", "DDO"};
    //UDP ports array: 0-MTL; 1-LVL; 2-DDO
    //Hardcode, no need to set in config file
    private static final int[] port = {55630, 55640, 55650};

    //==================Data Members===============
    //IP address array: 0-MTL; 1-LVL; 2-DDO
    private InetAddress[] IP;
    //Index to indicate current server
    private int idx;
    private ORB orb;
    private DCMS DCMSImpl;
    private Logger logger;
    private int TID;
    private int SID;
    private HashMap<Character, ArrayList<String>> nameRecordIDTable;
    private Hashtable<String, Object> recordIDRecordTable;
    private Hashtable<String, String> lockTable = new Hashtable<>();    //keep track of records that are under modifying
    private static Object o = new Object();                             //for editRecord operation synchronization
    private FileHandler fh;

    //===============Member Methods================
    public CenterServer(int serverIdx) {
        this.idx = serverIdx;
        IP = new InetAddress[3];
        TID = 10000;
        SID = 10000;
        nameRecordIDTable = new HashMap<>();
        recordIDRecordTable = new Hashtable<>();
        //setup logger
        logger = Logger.getLogger(serverName[serverIdx]);
        try {
            fh = new FileHandler(serverName[serverIdx] + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    protected int getIdx() {
        return idx;
    }

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public String sayHello() {
        return "\nHello world !!\n";
    }

    private String getTRecordID() {
        synchronized (o) {
            String newID = serverName[idx] + "TR" + Integer.toString(++TID);
            return newID;
        }
    }

    private String getSRecordID() {
        synchronized (o) {
            String newID = serverName[idx] + "SR" + Integer.toString(++SID);
            return newID;
        }
    }

    public String createTRecord(String remoteInput, String managerID) {
        String info = "[" + managerID + "] is creating new teacher record on [" + serverName[idx] + "]";
        logger.info(info);
        // validate input string from client
        Pattern p = Pattern
                .compile("^([a-zA-Z]+);([a-zA-Z]+);([a-zA-Z0-9.,\\s-]+);([0-9]+);([a-zA-Z,\\s]+);(mtl|lvl|ddo)$");
        Matcher m = p.matcher(remoteInput);
        // return error info to client if invalid
        if (!m.matches()) {
            info = "Input error, operation failed!";
            logger.warning("[" + managerID + "] " + info);
            return info;
        }

        // proceeds if valid
        String firstName, lastName, address, phone, specialization;
        Teacher.Location location;

        // extract data fields from regex groups
        firstName = m.group(1);
        lastName = m.group(2);
        address = m.group(3);
        phone = m.group(4);
        specialization = m.group(5);

        String loc = m.group(6);
        if (loc.equals("mtl"))
            location = Teacher.Location.mtl;
        else if (loc.equals("lvl"))
            location = Teacher.Location.lvl;
        else
            location = Teacher.Location.ddo;

        // <key, value> = (lastName, RecordIDsList)
        String recordID = getTRecordID(); // center repo server assigned
        char keyLastName = lastName.toLowerCase().charAt(0);

        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if (recordIDsByNameList == null) {
            recordIDsByNameList = new ArrayList<>();
        }
        recordIDsByNameList.add(recordID);

        // replace the list in hash map
        nameRecordIDTable.put(keyLastName, recordIDsByNameList);

        // <key, value> = (recordID, TeacherRecordObj)
        Teacher tObj = new Teacher(firstName, lastName, address, phone, specialization, location, recordID);
        recordIDRecordTable.put(recordID, tObj);
        info = "[" + managerID + "] created new teacher record [" + recordID + "] on [" + serverName[idx] + "] successfully";
        logger.info(info);
        return info;
    }

    public String createSRecord(String remoteInput, String managerID) {
        String info = "[" + managerID + "] is creating new student record on [" + serverName[idx] + "]";
        logger.info(info);

        // validate input string from client
        Pattern p = Pattern.compile("^([a-zA-Z]+);([a-zA-Z]+);([a-zA-Z,\\s]+);(active|inactive);(\\d{8})$");
        Matcher m = p.matcher(remoteInput);
        // return error info to client if invalid
        if (!m.matches()) {
            info = "Input error, operation failed!";
            logger.warning("[" + managerID + "] " + info);
            return info;
        }

        // proceeds if valid
        String firstName, lastName, courseRegistered;
        Student.Status status;
        LocalDate statusDate;

        // extract data fields from regex groups
        firstName = m.group(1);
        lastName = m.group(2);
        courseRegistered = m.group(3);
        if (m.group(4).equals("active"))
            status = Student.Status.active;
        else
            status = Student.Status.inactive;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        try {
            statusDate = LocalDate.parse(m.group(5), formatter);
        } catch (DateTimeException e) {
            info = "Date is invalid, operation failed!";
            logger.warning("[" + managerID + "] " + info);
            return info;
        }

        String loc = managerID.substring(0, 3);
        String recordID = getSRecordID(); // center server assigned
        char keyLastName = lastName.toLowerCase().charAt(0);

        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if (recordIDsByNameList == null)
            recordIDsByNameList = new ArrayList<>();
        recordIDsByNameList.add(recordID);

        // replace the list in hash map
        nameRecordIDTable.put(keyLastName, recordIDsByNameList);
        // <key, value> = (recordID, StudentRecordObj)
        Student sObj = new Student(firstName, lastName, courseRegistered, status, statusDate, recordID);
        recordIDRecordTable.put(recordID, sObj);
        info = "[" + managerID + "] created new student record [" + recordID + "] on [" + serverName[idx] + "] successfully";
        logger.info(info);
        return info;
    }

    public String editRecord(String remoteInput, String managerID) {
        String info = "[" + managerID + "] is editing record on [" + serverName[idx] + "]";
        logger.info(info);

        // validate input string from client
        Pattern pTeacher = Pattern.compile("^((MTLTR|LVLTR|DDOTR)[1-9]\\d{4});((address);([a-zA-Z0-9.,\\s-]+)|(phone);([0-9]+)|(location);(mtl|lvl|ddo))$");
        Matcher mTeacher = pTeacher.matcher(remoteInput);

        Pattern pStudent = Pattern.compile("^((MTLSR|LVLSR|DDOSR)[1-9]\\d{4});((courseRegistered);([a-zA-Z0-9,\\s]+)|(status);(active|inactive)|(statusDate);(\\d{8}))$");
        Matcher mStudent = pStudent.matcher(remoteInput);

        //processed recordID
        String recordID = "";
        //processed fieldName, newValue
        String fieldName = null, newValue = null;

        if (mTeacher.matches()) {
            for (int i = 4; i <= 9; i += 2) {
                if (mTeacher.start(i) != -1) {
                    recordID = mTeacher.group(1);
                    fieldName = mTeacher.group(i);
                    newValue = mTeacher.group(i + 1);
                    break;
                }
                logger.info("[" + managerID + "] " + "Input info has been validated as Teacher record. Waiting to update...");
            }
        } else if (mStudent.matches()) {
            for (int i = 4; i <= 9; i += 2) {
                if (mStudent.start(i) != -1) {
                    recordID = mStudent.group(1);
                    fieldName = mStudent.group(i);
                    newValue = mStudent.group(i + 1);
                    break;
                }
                logger.info("[" + managerID + "] " + "Input info has been validated as Student record. Waiting to update...");
            }
        } else {
            info = "[" + managerID + "] " + "Input info is invalid! Please try again.";
            logger.warning(info);
            return info;
        }


        Object record = recordIDRecordTable.get(recordID);
        if (record == null) {
            info = "[" + managerID + "] " + " Editing record [" + recordID + "] Rejected. The record is not found on server [" + serverName[idx] + "]. ";
        } else {
            Object obj = null;
            if (record instanceof Teacher) {
                Teacher tRecord = (Teacher) record;
                tRecord.setField(fieldName, newValue);
                obj = tRecord;
            } else if (record instanceof Student) {
                Student sRecord = (Student) record;
                sRecord.setField(fieldName, newValue);
                obj = sRecord;
            }
            //==========Check LockTable to see if Other Client Manager is making changes to the Record============
            synchronized (o) {
                String lockedID = lockTable.get(recordID);
                if (lockedID != null) {
                    info = "[" + managerID + "] " + "editing record [" + recordID + "] Rejected. Other client manager is editing/transferring the record. Please wait and try again later.";
                    logger.warning(info);
                    return info;
                }
                //lock the recordID
                lockTable.put(recordID, recordID);
            }
            //====================================================================================================

            //edit the record
            recordIDRecordTable.put(recordID, obj);


            //intentionally elapse the edit record method's execution time
            try {
                TimeUnit.MILLISECONDS.sleep(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (o) {
                //release the lock of recordID
                lockTable.remove(recordID);
            }
            info = "[" + managerID + "] " + " updated record [" + recordID + "] successfully.";
        }
        logger.info(info);
        return info;
    }

    protected int getSize() {
        return recordIDRecordTable.size();
    }

    public String getRecordCounts(String managerID) {
        String info = "Received record counts query from [" + managerID + "].";
        logger.info(info);

        int cnt[] = {-1, -1, -1};
//        cnt[idx] = getSize();

        DatagramSocket[] socket = null;

        try {
            socket = new DatagramSocket[3];
            byte[][] buf = new byte[3][256];
            byte[] request = "getSize".getBytes();
            DatagramPacket[] packet = new DatagramPacket[3];

            //construct request packets
            for (int i = 0; i < 3; ++i) {
                socket[i] = new DatagramSocket();
                packet[i] = new DatagramPacket(request, request.length, IP[i], port[i]);
            }

            //send requests to other servers concurrently
            for (int i = 0; i < 3; ++i) {
                socket[i].send(packet[i]);
            }

            //receive replying packets
            for (int i = 0; i < 3; ++i) {
                packet[i] = new DatagramPacket(buf[i], buf[i].length);
                socket[i].receive(packet[i]);
            }

            //extract count info from replaying packets
            for (int i = 0; i < 3; ++i) {
                String reply = new String(packet[i].getData(), 0, packet[i].getLength());
                //identify server index of reply
                int serverIdx = Integer.parseInt(reply.substring(0, 1));
                cnt[serverIdx] = Integer.parseInt(reply.substring(1));
            }

            info = "MTL " + Integer.toString(cnt[0]) + ", LVL " + Integer.toString(cnt[1]) + ", DDO "
                    + Integer.toString(cnt[2]);
            logger.info("[" + managerID + "] " + info);
            return info;

        } catch (IOException e) {
            logger.severe(e.toString());
        } finally {
            for (int i = 0; i < 3; ++i) {
                if (socket[i] != null)
                    socket[i].close();
            }
        }

        info = "Error when trying to obtain record counts info";
        logger.severe("[" + managerID + "] " + info);
        return info;
    }

    public String transferRecord(String remoteInput, String managerID) {
        String info = "[" + managerID + "] started to transfer record";
        logger.info(info);

        // validate input string from client
        Pattern p = Pattern.compile("^((MTLTR|LVLTR|DDOTR|MTLSR|LVLSR|DDOSR)[1-9]\\d{4});(mtl|lvl|ddo)$");    //MTL12345;ddo
        Matcher m = p.matcher(remoteInput);

        // return error info to client if invalid
        if (!m.matches()) {
            info = "Input error, operation failed!";
            logger.warning("[" + managerID + "] " + info);
            return info;
        }

        // proceeds if valid, extract data fields from regex groups
        String recordID = m.group(1);
        String loc = m.group(3);

        //get record by record ID
        Object record = recordIDRecordTable.get(recordID);
        if (record == null) {
            info = "[" + managerID + "] Transferring record [" + recordID + "] Rejected. The record is not found on server [" + serverName[idx] + "].";
            logger.warning(info);
            return info;
        }


        String lastName = "";
        String transferRecordString = "";
        if (record instanceof Teacher) {
            Teacher tRecord = (Teacher) record;
            lastName = tRecord.getLastName();
            String firstName = tRecord.getFirstName();
            String address = tRecord.getAddress();
            String phone = tRecord.getPhone();
            String specialization = tRecord.getSpecialization();
            String location = tRecord.getLocation();
            transferRecordString = "TR;" + recordID + ";" + firstName + ";" + lastName + ";" + address + ";" + phone + ";" + specialization + ";" + location;

        } else if (record instanceof Student) {
            Student sRecord = (Student) record;
            lastName = sRecord.getLastName();
            String firstName = sRecord.getFirstName();
            String courses = sRecord.getCourses();
            String status = sRecord.getStatus();
            String statusDate = sRecord.getStatusDate();
            transferRecordString = "SR;" + recordID + ";" + firstName + ";" + lastName + ";" + courses + ";" + status + ";" + statusDate;
        }
//        info = "[" + managerID + "] " + " Found " + recordID + " to transfer successfully.";
//        logger.info(info);

        //==========Check LockTable to see if Other Client Manager is making changes to the Record===============
        synchronized (o) {
            String lockedID = lockTable.get(recordID);
            if (lockedID != null) {
                //other client is modifying the record, stop
                info = "[" + managerID + "] Transferring record [" + recordID + "] Rejected. Other client manager is editing/transferring the record. Please wait and try again later.";
                logger.warning(info);
                return info;
            }
            //lock the record ID
            lockTable.put(recordID, recordID);
        }
        //=======================================================================================================


        //remove the record from this current server
        recordIDRecordTable.remove(recordID);
        char keyLastName = lastName.toLowerCase().charAt(0);
        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if (recordIDsByNameList != null) {
            recordIDsByNameList.remove(recordID);
            // replace the list in hash map
            nameRecordIDTable.put(keyLastName, recordIDsByNameList);
        }

//        info = "Record [" + recordID + "] Removed from [" + serverName[idx] + "] successfully.";
//        logger.info("[" + managerID + "] " + info);

        //process the record to be transferred to another server
        try {
            // create and initialize the ORB
            //ORB orb = ORB.init();

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            // Use NamingContextExt instead of NamingContext. This is
            // part of the Interoperable naming Service.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // resolve the Object Reference in Naming
            int index;
            if (loc.equals("mtl")) {
                index = 0;
            } else if (loc.equals("lvl")) {
                index = 1;
            } else {
                index = 2;
            }
            String name = serverName[index];
            DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));
            info = DCMSImpl.acceptTransferredRecord(transferRecordString, managerID);
            logger.info(info);
        } catch (Exception e) {
            logger.severe(e.toString());
        }

        //======================================Unlock the Record==============================================
        synchronized (o) {
            //unlock the recordID
            lockTable.remove(recordID);
        }
        //=====================================================================================================

        return info;
    }

    public String acceptTransferredRecord(String remoteInput, String managerID) {
        String[] fieldsArray = remoteInput.split(";");
        logger.info("Server [" + serverName[idx] + "] is accepting transferred record [" + fieldsArray[1] + "] sent by Manager [" + managerID + "]");

        String info;
        String type = fieldsArray[0];
        String recordID = fieldsArray[1];
        String lastName = fieldsArray[3];
        Object recordObj = null;
        if (type.equals("TR")) {
            //re-construct the Teacher Object
            String firstName = fieldsArray[2];
            String address = fieldsArray[4];
            String phone = fieldsArray[5];
            String specialization = fieldsArray[6];
            Teacher.Location location;
            if (fieldsArray[7].equals("mtl"))
                location = Teacher.Location.mtl;
            else if (fieldsArray[7].equals("mtl"))
                location = Teacher.Location.lvl;
            else
                location = Teacher.Location.ddo;
            Teacher tObj = new Teacher(firstName, lastName, address, phone, specialization, location, recordID);
            recordObj = tObj;
        } else if (type.equals("SR")) {
            //re-construct the Student Object
            String firstName = fieldsArray[2];
            String courseRegistered = fieldsArray[4];
            Student.Status status;
            if (fieldsArray[5].equals("active")) {
                status = Student.Status.active;
            } else {
                status = Student.Status.inactive;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate statusDate = LocalDate.parse(fieldsArray[6], formatter);
            Student sObj = new Student(firstName, lastName, courseRegistered, status, statusDate, recordID);
            recordObj = sObj;
        }

        //put the transferred Record Object into the hash table
        char keyLastName = lastName.toLowerCase().charAt(0);
        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if (recordIDsByNameList == null) {
            recordIDsByNameList = new ArrayList<>();
        }
        recordIDsByNameList.add(recordID);
        // replace the list in hash map
        nameRecordIDTable.put(keyLastName, recordIDsByNameList);
        recordIDRecordTable.put(recordID, recordObj);
        info = "[" + managerID + "] transferred record [" + recordID + "] to server [" + serverName[idx] + "] successfully.";
        logger.info(info);
        return info;
    }

    // implement shutdown() method
    public void shutdown() {
        //false: shutdown immediately, without waiting for processing to complete
        //orb.shutdown(false);
    }

    private void getConf(String confPath) {
        Properties configFile = new Properties();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(confPath);
            configFile.load(fin);
            String info = "Initialize IP info from configuration:" + System.lineSeparator();
            for (int i = 0; i < 3; ++i) {
                IP[i] = InetAddress.getByName(configFile.getProperty(serverName[i]));
                InetAddress localhost = InetAddress.getLocalHost();
                if (IP[i].equals(localhost))
                    IP[i] = InetAddress.getLoopbackAddress();
                info += serverName[i] + " host: " + IP[i] + System.lineSeparator();
            }
            logger.info(info);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    logger.severe(e.toString());
                }
            }
        }
    }

    public static void main(String[] args) {

        //choose server location
        String userInput = null;
        System.out.println("Please choose location of this center server: 1.MTL; 2.LVL; 3.DDO;");
        Scanner input = new Scanner(System.in);
        Boolean validInt = false;
        while (!validInt) {
            Pattern pServerChoice = Pattern.compile("^([1-3])$");
            userInput = input.nextLine();
            Matcher mServerChoice = pServerChoice.matcher(userInput);
            validInt = mServerChoice.matches();
            if (!validInt)
                System.out.println("Invalid choice, please input an integer between 1-3.");
        }

        //CORBA operations
        CenterServer server = null;
        try {
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);
            // get reference to rootpoa and activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();
            // create servant and register it with the ORB
            server = new CenterServer(Integer.parseInt(userInput) - 1);
            server.setORB(orb);
            // get object reference from the servant
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(server);
            DCMS href = DCMSHelper.narrow(ref);
            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            // Use NamingContextExt which is part of the Interoperable
            // Naming Service (INS) specification.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            // bind the Object Reference in Naming
            String name = serverName[server.idx];
            NameComponent path[] = ncRef.to_name(name);
            ncRef.rebind(path, href);

            server.getConf("server.conf");
            new UDPListener(server, port[server.idx]).start();
            server.logger.info("UDP socket listening on port: " + port[server.idx]);
            server.logger.info("Run as " + serverName[server.idx] + ", waiting for client...");
            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            server.logger.severe("ERROR: " + e.toString());
        }

    }
}
