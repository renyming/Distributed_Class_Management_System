import DCMSApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;

public class ManagerClient
{
    static DCMS DCMSImpl;

    public static void main(String args[])
    {
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
            String name = "MTL";
            DCMSImpl = DCMSHelper.narrow(ncRef.resolve_str(name));

            System.out.println("Obtained a handle on server object: " + DCMSImpl);
            System.out.println(DCMSImpl.sayHello());
            DCMSImpl.createTRecord("remoteInputStr","managerIDStr");
            DCMSImpl.createSRecord("remoteInputStr","managerIDStr");
            DCMSImpl.editRecord("remoteInputStr","managerIDStr");
            System.out.println(DCMSImpl.getRecordCounts("managerIDStr"));
            DCMSImpl.transferRecord("remoteInputStr","managerIDStr");
            DCMSImpl.shutdown();

        } catch (Exception e) {
            System.out.println("ERROR : " + e) ;
            e.printStackTrace(System.out);
        }
    }

}