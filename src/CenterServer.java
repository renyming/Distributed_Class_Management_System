import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Object;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.net.InetAddress;
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
    private static Object o = new Object();		//for editRecord operation synchronization
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

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public String sayHello() {
        return "\nHello world !!\n";
    }

    private String getTRecordID(String loc) {
        String newID = loc.toUpperCase() + "TR" + Integer.toString(++TID);
        return newID;
    }

    private String getSRecordID(String loc) {
        String newID = loc.toUpperCase() + "SR" + Integer.toString(++SID);
        return newID;
    }

    public String createTRecord(String remoteInput, String managerID) {
        logger.info("["+managerID+"] is creating new teacher record");
        String info;
        // validate input string from client
        Pattern p = Pattern
                .compile("^([a-zA-Z]+);([a-zA-Z]+);([a-zA-Z0-9.,\\s-]+);([0-9]+);([a-zA-Z,\\s]+);(mtl|lvl|ddo)$");
        Matcher m = p.matcher(remoteInput);
        // return error info to client if invalid
        if (!m.matches()) {
            info= "Input error, operation failed!";
            logger.warning("["+managerID+"] "+info);
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
        String recordID = getTRecordID(loc); // center repo server assigned
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
        info="Teacher record added successfully. Record ID: " + recordID;
        logger.info("["+managerID+"] "+info);
        return info;
    }

    public String createSRecord(String remoteInput, String managerID) {
        logger.info("["+managerID+"] is creating new student record");
        String info;

        // validate input string from client
        Pattern p = Pattern.compile("^([a-zA-Z]+);([a-zA-Z]+);([a-zA-Z,\\s]+);(active|inactive);(\\d{8})$");
        Matcher m = p.matcher(remoteInput);
        // return error info to client if invalid
        if (!m.matches()) {
            info= "Input error, operation failed!";
            logger.warning("["+managerID+"] "+info);
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
            info="Date is invalid, operation failed!";
            logger.warning("["+managerID+"] "+info);
            return info;
        }

        String loc = managerID.substring(0,3);
        String recordID = getSRecordID(loc); // center server assigned
        char keyLastName = lastName.toLowerCase().charAt(0);

        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if(recordIDsByNameList == null)
            recordIDsByNameList = new ArrayList<>();
        recordIDsByNameList.add(recordID);

        // replace the list in hash map
        nameRecordIDTable.put(keyLastName, recordIDsByNameList);
        // <key, value> = (recordID, StudentRecordObj)
        Student sObj = new Student(firstName, lastName, courseRegistered, status, statusDate, recordID);
        recordIDRecordTable.put(recordID, sObj);
        info="Student Record ["+recordID+"] added to Server["+serverName[idx]+"] successfully.";
        logger.info("["+managerID+"] "+info);
        return info;
    }

    public String editRecord(String remoteInput, String managerID) {
        logger.info("["+managerID+"] is editing record");
        String info = null;

        // validate input string from client
        Pattern pTeacher = Pattern.compile("^((MTLTR|LVLTR|DDOTR)[1-9]\\d{4});((address);([a-zA-Z0-9.,\\s-]+)|(phone);([0-9]+)|(location);(mtl|lvl|ddo))$");
        Matcher mTeacher = pTeacher.matcher(remoteInput);

        Pattern pStudent = Pattern.compile("^((MTLSR|LVLSR|DDOSR)[1-9]\\d{4});((courseRegistered);([a-zA-Z0-9,\\s]+)|(status);(active|inactive)|(statusDate);(\\d{8}))$");
        Matcher mStudent = pStudent.matcher(remoteInput);

        //processed recordID
        String recordID = "";
        //processed fieldName, newValue
        String fieldName=null, newValue=null;

        if (mTeacher.matches()){
            for (int i=4;i<=9;i+=2){
                if (mTeacher.start(i)!=-1){
                    recordID=mTeacher.group(1);
                    fieldName=mTeacher.group(i);
                    newValue=mTeacher.group(i+1);
                    break;
                }
                logger.info("["+managerID+"] "+"Input info has been validated as Teacher record. Waiting to update...");
            }
        } else if (mStudent.matches()){
            for (int i=4;i<=9;i+=2){
                if (mStudent.start(i)!=-1){
                    recordID=mStudent.group(1);
                    fieldName=mStudent.group(i);
                    newValue=mStudent.group(i+1);
                    break;
                }
                logger.info("["+managerID+"] "+"Input info has been validated as Student record. Waiting to update...");
            }
        } else {
            info="["+managerID+"] "+"Input info is invalid! Please try again.";
            logger.warning(info);
            return info;
        }

        synchronized (o){
            Object record = recordIDRecordTable.get(recordID);
            if(record == null){
                info="["+managerID+"] "+" Found no record associated with "+recordID+".";
            }else{
                if(record instanceof Teacher) {
//				logger.info(managerID+" is editing teacher record");
                    Teacher tRecord = (Teacher)record;
                    tRecord.setField(fieldName, newValue);
                    Object obj = tRecord;
                    recordIDRecordTable.put(recordID,obj);
                }else if(record instanceof Student) {
//				logger.info(managerID+" is editing student record");
                    Student sRecord = (Student)record;
                    sRecord.setField(fieldName, newValue);
                    Object obj = sRecord;
                    recordIDRecordTable.put(recordID,obj);
                }
                info="["+managerID+"] "+" Updated "+recordID+" successfully.";
            }
        }

        logger.info(info);
        return info;
    }

    protected int getSize() {
        return recordIDRecordTable.size();
    }

    public String getRecordCounts(String managerID) {
        logger.info("Received record counts query from " + managerID);
        String info = "";

        int cnt[] = {-1, -1, -1};
        cnt[idx] = getSize();

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            byte[] buf = new byte[256];
            byte[] request = "getSize".getBytes();

            for (int i = 0; i < 3; ++i) {
                if (cnt[i] == -1) {
                    DatagramPacket packet = new DatagramPacket(request, request.length, IP[i], port[i]);
                    socket.send(packet);

                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    cnt[i] = Integer.parseInt((new String(packet.getData(), 0, packet.getLength())));
                }
            }

            info = "MTL " + Integer.toString(cnt[0]) + ", LVL " + Integer.toString(cnt[1]) + ", DDO "
                    + Integer.toString(cnt[2]);
            logger.info("[" + managerID + "] " + info);
            return info;

        } catch (IOException e) {
            logger.severe(e.toString());
        } finally {
            if (socket != null)
                socket.close();
        }

        info = "Error when trying to obtain record counts info";
        logger.severe("[" + managerID + "] " + info);
        return info;
    }

    public String transferRecord(String remoteInput, String managerID) {
        logger.info("["+managerID+"] is transferring new student record");
        String info;

        // validate input string from client
        Pattern p = Pattern.compile("^((MTLTR|LVLTR|DDOTR|MTLSR|LVLSR|DDOSR)[1-9]\\d{4});(mtl|lvl|ddo)$");    //MTL12345;ddo
        Matcher m = p.matcher(remoteInput);

        // return error info to client if invalid
        if (!m.matches()) {
            info= "Input error, operation failed!";
            logger.warning("["+managerID+"] "+info);
            return info;
        }

        // proceeds if valid, extract data fields from regex groups
        String recordID = m.group(1);
        String loc = m.group(3);

        //get record by record ID
        Object record = recordIDRecordTable.get(recordID);
        if(record == null){
            info="["+managerID+"] "+" Found no record associated with "+recordID+".";
            return info;
        }

        String lastName = "";
        String transferRecordString = "";
        if(record instanceof Teacher) {
            Teacher tRecord = (Teacher)record;
            lastName = tRecord.getLastName();
            String firstName = tRecord.getFirstName();
            String address = tRecord.getAddress();
            String phone = tRecord.getPhone();
            String specialization = tRecord.getSpecialization();
            String location = tRecord.getLocation();
            transferRecordString = "TR;"+recordID+";"+firstName+";"+lastName+";"+address+";"+phone+";"+specialization+";"+location;

        }else if(record instanceof Student) {
            Student sRecord = (Student)record;
            lastName = sRecord.getLastName();
            String firstName = sRecord.getFirstName();
            String courses = sRecord.getCourses();
            String status = sRecord.getStatus();
            String statusDate = sRecord.getStatusDate();
            transferRecordString = "SR;"+recordID+";"+firstName+";"+lastName+";"+courses+";"+status+";"+statusDate;
        }
        info="["+managerID+"] "+" Found "+recordID+" to transfer successfully.";
        logger.info(info);


        //remove the record from this current server
        char keyLastName = lastName.toLowerCase().charAt(0);
        ArrayList<String> recordIDsByNameList = nameRecordIDTable.get(keyLastName);
        if(recordIDsByNameList != null)
        {
            recordIDsByNameList.remove(recordID);
            // replace the list in hash map
            nameRecordIDTable.put(keyLastName, recordIDsByNameList);
        }
        recordIDRecordTable.remove(recordID);

        info="Record ["+recordID+"] Removed from ["+serverName[idx]+"] successfully.";
        logger.info("["+managerID+"] "+info);

        //process the record to be transferred to another server
        try{
            // create and initialize the ORB
            //ORB orb = ORB.init();

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            // Use NamingContextExt instead of NamingContext. This is
            // part of the Interoperable naming Service.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // resolve the Object Reference in Naming
            int index;
            if(loc.equals("mtl")){
                index = 0;
            }else if(loc.equals("lvl")){
                index = 1;
            }else{
                index = 2;
            }
            String name = serverName[index];
            DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));
            info = DCMSImpl.acceptTransferredRecord(transferRecordString, managerID);
            logger.info(info);
        } catch (Exception e) {
            logger.severe(e.toString());
        }

        return info;
    }

    public String acceptTransferredRecord(String remoteInput, String managerID){
        String [] fieldsArray = remoteInput.split(";");
        logger.info("Server ["+serverName[idx]+"] is accepting transferred record ["+fieldsArray[1]+"] sent by Manager ["+managerID+"]");

        String info;
        String type = fieldsArray[0];
        String recordID = fieldsArray[1];
        String lastName = fieldsArray[3];
        Object recordObj = null;
        if(type.equals("TR")){
            //re-construct the Teacher Object
            String firstName = fieldsArray[2];
            String address = fieldsArray[4];
            String phone = fieldsArray[5];
            String specialization = fieldsArray[6];
            Teacher.Location location;
            if(fieldsArray[7].equals("mtl"))
                location = Teacher.Location.mtl;
            else if(fieldsArray[7].equals("mtl"))
                location = Teacher.Location.lvl;
            else
                location = Teacher.Location.ddo;
            Teacher tObj = new Teacher(firstName, lastName, address, phone, specialization, location, recordID);
            recordObj = tObj;
        }else if(type.equals("SR")){
            //re-construct the Student Object
            String firstName = fieldsArray[2];
            String courseRegistered = fieldsArray[4];
            Student.Status status;
            if(fieldsArray[5].equals("active")){
                status = Student.Status.active;
            }else{
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
        info="Record ["+recordID+"] Transferred to Server ["+serverName[idx]+"]successfully.";
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
            String info = "Initialize IP info from configuration:"+System.lineSeparator();
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
