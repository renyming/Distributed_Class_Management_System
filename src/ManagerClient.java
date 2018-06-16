import DCMSApp.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagerClient
{
    private DCMS DCMSImpl;
    private String managerID;
    private int idx;
    private static final String[] serverName = {"MTL", "LVL", "DDO"};
    private FileHandler fh;
    private Logger logger;

    public ManagerClient(String managerID){
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

    public static void main(String args[])
    {
        Scanner input = new Scanner(System.in);
        Boolean valid = false;

        System.out.println("Please enter your manager ID:");
        String tmp_ID="";
        ManagerClient client=null;

        while (!valid) {
            tmp_ID = input.nextLine();
            Pattern p = Pattern.compile("^(MTL|LVL|DDO)\\d{4}");
            Matcher m = p.matcher(tmp_ID);
            if (m.matches()) {
                client=new ManagerClient(tmp_ID);
                valid = true;
            } else {
                System.out.println("Your manager ID is invalid, please try again.");
            }
        }

        client.logger.info(client.managerID+" logged in successfully");

        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get the root naming context
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            // Use NamingContextExt instead of NamingContext. This is
            // part of the Interoperable naming Service.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // resolve the Object Reference in Naming
            String name = serverName[client.idx];
            client.DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));

            while (true) {
                valid = false;
                String userInput = "";
                int option = 0;
                while (!valid) {
                    printMenu();
                    userInput = input.nextLine();
                    Pattern p = Pattern.compile("^[1-5]$");
                    Matcher m = p.matcher(userInput);
                    if (m.matches()) {
                        option = Integer.parseInt(userInput);
                        valid = true;
                    } else {
                        client.logger.warning("Your input is invalid, please try again.");
                    }
                }

                if (option == 1) {
                    System.out.println(
                            "Enter the following info: first name;last name;address;phone number;specialization(separated by ,);location(mtl|lvl|ddo)");
                    String inputString = input.nextLine();
                    client.logger.info(client.DCMSImpl.createTRecord(inputString, client.managerID));
                } else if (option == 2) {
                    System.out.println(
                            "Enter the following info: first name;last name;registered courses(separated by ,);status(active|inactive);status date(yyyyMMdd)");
                    String inputString = input.nextLine();
                    client.logger.info(client.DCMSImpl.createSRecord(inputString,client.managerID));
                } else if (option == 3) {
                    client.logger.info("Obtaining record counts info from server...");
                    client.logger.info(client.DCMSImpl.getRecordCounts(client.managerID));
                } else if (option == 4) {
                    System.out.println("Enter the following info: record ID;field name;new value");
                    String inputString=input.nextLine();
                    client.logger.info(client.DCMSImpl.editRecord(inputString, client.managerID));
                } else {
                    client.logger.info("Exited system");
                    return;
                }
                Thread.sleep(500);  //prevent print-out info overlap
            }


        } catch (Exception e) {
            client.logger.severe(e.toString());
        }
    }

    private static void printMenu() {
        String info = "=======================================================================\n";
        info += "Please select your option by enter the number before each option: \n";
        info += "1: Create Teacher Record\n2: Create Student Record\n3: Get Record Counts\n4: Edit Record\n5: Exit";
        System.out.println(info);
    }

}