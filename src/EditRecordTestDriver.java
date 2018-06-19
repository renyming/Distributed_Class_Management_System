public class EditRecordTestDriver {
    public static void main(String[] args) throws InterruptedException {
        int n = 15;         //number of manager clients
        ManagerClientAuto [] clients = new ManagerClientAuto[n];
        String [] server = new String[3];
        server[0] = "MTL";
        server[1] = "LVL";
        server[2] = "DDO";
        String [] managerIDs = new String [n];


        for(int i=0; i<3; ++i){
            for(int j=1; j<=(n/3); ++j){
                managerIDs [i*(n/3)+j-1] = server[i] + "100" + Integer.toString(j);
            }
        }

        for(int i=0; i<15; ++i){
            clients[i] = new ManagerClientAuto(managerIDs[i]);
            clients[i].start();
        }

        for(int i=0; i<15; ++i){
            clients[i].join();
        }

    }
}
