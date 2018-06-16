//String firstName, String lastName, String address, String phone, String specialization,Location location

public class Teacher {

    public enum Location{mtl,lvl,ddo};

    private String recordID;
    private String firstName;
    private String lastName;
    private String address;
    private String phone;
    private String specialization;
    private Location location;

    public Teacher(String firstName, String lastName, String address, String phone, String specialization,Location location, String recordID) {
        this.firstName=firstName;
        this.lastName=lastName;
        this.address=address;
        this.phone=phone;
        this.specialization=specialization;
        this.location=location;
        this.recordID=recordID;
    }

    public String getID(){
        return this.recordID;
    }
    public String getLastName(){ return this.lastName; }

    public void setField(String fieldName, String newValue) {
        if(fieldName == "address") {
            this.address = newValue;
        }else if(fieldName == "phone") {
            this.phone = newValue;
        }else if(fieldName == "location") {
            switch(newValue) {
                case "mtl":
                    this.location = Location.mtl;
                    break;
                case "lvl":
                    this.location = Location.lvl;
                    break;
                case "ddo":
                    this.location = Location.ddo;
                    break;
            }
        }
    }

}
