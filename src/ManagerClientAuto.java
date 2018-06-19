import DCMSApp.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ManagerClientAuto extends Thread{
    private DCMS DCMSImpl;
    private String managerID;
    private int idx;
    private static final String[] serverName = {"MTL", "LVL", "DDO"};
    private FileHandler fh;
    private Logger logger;

    public ManagerClientAuto(String managerID){
        this.managerID=managerID;
        String location=managerID.substring(0,3);
        if (location.equals("MTL"))
            idx=0;
        else if (location.equals("LVL"))
            idx=1;
        else
            idx=2;
        //setup logger
        logger = Logger.getLogger(managerID);
        try {
            fh=new FileHandler(managerID+".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public void run(){
        try{
            //System.out.println ("Thread " + Thread.currentThread().getId() + " is running");
            String [] arguments = new String[2];
            arguments[0] = "-ORBInitialPort";
            arguments[1] = "1050";
            ORB orb = ORB.init(arguments, null);
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            String name = serverName[idx];
            DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));

            //create 3 teacher records
            for(int i=0; i<3; ++i){
                String inputString = "firstName;lastName;addr addr addr;123456789;cs,ece;mtl";
                logger.info(DCMSImpl.createTRecord(inputString, managerID));
            }

        } catch (Exception e) {
            logger.severe(e.toString());
        }
    }
}
