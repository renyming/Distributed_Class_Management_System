
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Student {
    public enum Status{active, inactive};

    private String recordID;
    private String firstName;
    private String lastName;
    private String courseRegistered;
    private Status status;
    private LocalDate statusDate;

    public Student(String firstName, String lastName, String courseRegistered, Status status2, LocalDate statusDate, String recordID) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.courseRegistered = courseRegistered;
        this.status = status2;
        this.statusDate = statusDate;
        this.recordID = recordID;
    }

    public String getID() {
        return this.recordID;
    }
    public String getLastName(){ return this.lastName; }

    public void setField(String fieldName, String newValue) {
        if(fieldName == "courseRegistered") {
            this.courseRegistered = newValue;
        }else if(fieldName == "status") {
            if(newValue == "active") {
                this.status = Status.active;
                this.statusDate = LocalDate.now();
            }else if(newValue == "inactive") {
                this.status = Status.inactive;
                this.statusDate = LocalDate.now();
            }
        }else if(fieldName=="statusDate"){
            LocalDate statusDate;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            statusDate = LocalDate.parse(newValue, formatter);
            this.statusDate=statusDate;
        }
    }
}
