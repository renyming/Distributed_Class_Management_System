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
        String newID = loc + "TR" + Integer.toString(++TID);
        return newID;
    }

    public String createTRecord(String remoteInput, String managerID) {
        System.out.println("createTRecord called.");
        return "create teacher record";
    }

    public String createSRecord(String remoteInput, String managerID) {
        System.out.println("createSRecord called.");
        return "create student record";
    }

    public String editRecord(String remoteInput, String managerID) {
        System.out.println("editRecord called.");
        return "edit record";
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