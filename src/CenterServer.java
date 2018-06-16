import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Object;
import java.util.*;
import java.net.InetAddress;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

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
    private Logger logger;
    private int TID;
    private int SID;
    private HashMap<Character, ArrayList<String>> nameRecordIDTable;
    private Hashtable<String, Object> recordIDRecordTable;
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
            fh = new FileHandler(serverName + ".log");
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
        String newID = loc + "TR" + Integer.toString(++TID);
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

        // extract data fileds from regex groups
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
            recordIDsByNameList = new ArrayList<String>();
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
        System.out.println("createSRecord called.");
        return "create student record";
    }

    public String editRecord(String remoteInput, String managerID) {
        System.out.println("editRecord called.");
        return "edit record";
    }

    public String getRecordCounts(String managerID) {
//        logger.info("Received record counts query from "+managerID);
//        String info="";
//
//        int cnt[]={-1,-1,-1};
//
//        if (MTLPort < 0 || LVLPort < 0 || DDOPort < 0) {
//            try {
//                getPortsIP();
//            } catch (UnknownHostException e) {
//                logger.severe("["+managerID+"] Cannot get UDP ports from central repository");
//            }
//        }
//
//        if (serverName.equals("MTL"))
//            MTLCnt = getSize();
//        else if (serverName.equals("LVL"))
//            LVLCnt = getSize();
//        else if (serverName.equals("DDO"))
//            DDOCnt = getSize();
//        DatagramSocket socket = null;
//
//        try {
//            socket = new DatagramSocket();
//            byte[] buf = new byte[256];
//            byte[] request = "getSize".getBytes();
//
//            if (MTLCnt == -1) {
//                DatagramPacket packet = new DatagramPacket(request, request.length, MTLIP, MTLPort);
//                socket.send(packet);
//
//                packet = new DatagramPacket(buf, buf.length);
//                socket.receive(packet);
//
//                MTLCnt = Integer.parseInt((new String(packet.getData(), 0, packet.getLength())));
//            }
//
//            if (LVLCnt == -1) {
//                DatagramPacket packet = new DatagramPacket(request, request.length, LVLIP, LVLPort);
//                socket.send(packet);
//
//                packet = new DatagramPacket(buf, buf.length);
//                socket.receive(packet);
//
//                LVLCnt = Integer.parseInt((new String(packet.getData(), 0, packet.getLength())));
//            }
//
//            if (DDOCnt == -1) {
//                DatagramPacket packet = new DatagramPacket(request, request.length, DDOIP, DDOPort);
//                socket.send(packet);
//
//                packet = new DatagramPacket(buf, buf.length);
//                socket.receive(packet);
//
//                DDOCnt = Integer.parseInt((new String(packet.getData(), 0, packet.getLength())));
//            }
//
//            info= "MTL " + Integer.toString(MTLCnt) + ", LVL " + Integer.toString(LVLCnt) + ", DDO "
//                    + Integer.toString(DDOCnt);
//            logger.info("["+managerID+"] "+info);
//            return info;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (socket != null)
//                socket.close();
//        }
//
//        info= "Error when trying to obtain record counts info";
//        logger.info("["+managerID+"] "+info);
//        return info;
        return "abcd";
    }

    public String transferRecord(String remoteInput, String managerID) {
        System.out.println("transferRecord called.");
        return "transfer record";
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
            for (int i = 0; i < 3; ++i) {
                IP[i] = InetAddress.getByName(configFile.getProperty(serverName[i]));
                InetAddress localhost = InetAddress.getLocalHost();
                if (IP[i].equals(localhost))
                    IP[i] = InetAddress.getLoopbackAddress();
                //TODO:replace with logger
                logger.info(serverName[i] + " host: " + IP[i]);
            }
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
        Boolean valid = false;
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
                System.out.println("Invalid choice, please input an integer between 1-3 to choose.");
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

            System.out.println("Distributed Server ready and waiting ...");
            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            server.logger.severe("ERROR: " + e.toString());
        }
        server.logger.info("Run as " + serverName[server.idx]);
        server.getConf("server.conf");

    }
}
