import DCMSApp.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;

import java.io.IOException;
import java.sql.Time;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.TimeUnit;

public class ManagerClientAuto extends Thread {
    private DCMS DCMSImpl;
    private String managerID;
    private int idx;
    private static final String[] serverName = {"MTL", "LVL", "DDO"};
    private FileHandler fh;
    private Logger logger;

    public ManagerClientAuto(String managerID) {
        this.managerID = managerID;
        String location = managerID.substring(0, 3);
        if (location.equals("MTL"))
            idx = 0;
        else if (location.equals("LVL"))
            idx = 1;
        else
            idx = 2;
        //setup logger
        logger = Logger.getLogger(managerID);
        try {
            fh = new FileHandler(managerID + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public void run() {
        try {
            //System.out.println ("Thread " + Thread.currentThread().getId() + " is running");
            String[] arguments = new String[2];
            arguments[0] = "-ORBInitialPort";
            arguments[1] = "1050";
            ORB orb = ORB.init(arguments, null);
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            String name = serverName[idx];
            DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));


            //==============================================TEST SCENARIO 1=============================================

            //===========create 3 teacher records===============================================
            for(int i=0; i<3; ++i){
                String inputString = "firstName;lastName;addr addr addr;123456789;cs,ece;mtl";
                logger.info(DCMSImpl.createTRecord(inputString, managerID));
            }


            //===========MTL manager creates 1 SRecord, LVL 2, and DDO 3.=======================
            int recordCount = idx + 1;
            for(int i=0; i<recordCount; ++i){
                String inputString = "fakeFirstName;fakeLastName;french,english,japanese;active;20180616";
                logger.info(DCMSImpl.createSRecord(inputString, managerID));
            }

            //===========Get records count on each server=======================================
            TimeUnit.SECONDS.sleep(3);   //wait for 3 seconds, until all the previous records are created.
            logger.info(DCMSImpl.getRecordCounts(managerID));


            //==========================================================================================================





            /*
            //==============================================TEST SCENARIO 2=============================================

            //===========MTL1001 creates 1 teacher records======================================
            if(managerID.equals("MTL1001")){
                String inputString = "firstName;lastName;addr addr addr;123456789;cs,ece;mtl";
                logger.info(DCMSImpl.createTRecord(inputString, managerID));

            }else{
            //===========All the other manager tries to edit the same teacher records===========
                String inputString = "MTLTR10001;address;new address strings";
                logger.info(DCMSImpl.editRecord(inputString, managerID));
            }

            //==========================================================================================================
            */



            /*
            //==============================================TEST SCENARIO 3=============================================

            int digitID = Integer.parseInt(managerID.substring(3,7));
            //===========MTL1001 creates 1 teacher record=======================================
            if (digitID == 1001) {
                String inputString = "firstName;lastName;addr addr addr;123456789;cs,ece;mtl";
                logger.info(DCMSImpl.createTRecord(inputString, managerID));

            //===========MTL1002~MTL1011 request to edit the record=============================
            } else if ((digitID >= 1002) && (digitID < 1012)) {
                String inputString = "MTLTR10001;address;new address strings";
                logger.info(DCMSImpl.editRecord(inputString, managerID));

            //===========MTL1012~MTL1021 request to transfer the record=========================
            } else {
                String inputString = "MTLTR10001;lvl";
                logger.info(DCMSImpl.transferRecord(inputString, managerID));
            }
            //==========================================================================================================
            */

        } catch (Exception e) {
            logger.severe(e.toString());
        }
    }
}
