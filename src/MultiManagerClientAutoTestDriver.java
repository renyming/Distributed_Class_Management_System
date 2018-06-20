import java.util.concurrent.TimeUnit;

public class MultiManagerClientAutoTestDriver {
    public static void main(String[] args) throws InterruptedException {

        //==========================================Scenario 1==========================================
        //15 client managers    (MTL1001~1005, LVL1001~1005, DDO1001~1005)
        int n = 15;
        ManagerClientAuto[] clients = new ManagerClientAuto[n];
        String[] managerIDs = new String[n];
        String[] server = new String[3];
        server[0] = "MTL";
        server[1] = "LVL";
        server[2] = "DDO";

        //init managerID
        for (int i = 0; i < 3; ++i) {
            for (int j = 1; j <= (n / 3); ++j) {
                managerIDs[i * (n / 3) + j - 1] = server[i] + "100" + Integer.toString(j);
            }
        }

        //create 15 manager client threads, and interleaved these threads
        for (int i = 0; i < n; ++i) {
            clients[i] = new ManagerClientAuto(managerIDs[i]);
            clients[i].start();
        }

        for (int i = 0; i < n; ++i) {
            clients[i].join();
        }
        //===============================================================================================






        /*
        //===============================Scenario 2 & Scenario 3=========================================
        //21 client managers    (MTL1001~1021)
        int n = 21;
        ManagerClientAuto[] clients = new ManagerClientAuto[n];
        String[] managerIDs = new String[n];

        //init managerID
        for (int i = 0; i < n; ++i) {
            managerIDs[i] = "MTL10" + String.format("%02d", (i + 1));
        }

        //create 1 manager client thread to create a record first
        clients[0] = new ManagerClientAuto(managerIDs[0]);
        clients[0].start();

        //hold the thread for two seconds, until creating record is done
        TimeUnit.SECONDS.sleep(2);

        //create 20 manager client thread, and interleaved these threads
        for (int i = 1; i < n; ++i) {
            clients[i] = new ManagerClientAuto(managerIDs[i]);
            clients[i].start();
        }

        for (int i = 1; i < n; ++i) {
            clients[i].join();
        }
        //===============================================================================================
        */


    }
}
