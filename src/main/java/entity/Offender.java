package entity;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Offender {
    private String first_name;
    private String last_name;
    private UUID PersonID;
    int numberOfViolations;
    double total_fine;
    double average_fine;

    synchronized public int getNumberOfViolations() {
        return numberOfViolations;
    }

    synchronized public void setNumberOfViolations(int numberOfViolations) {
        this.numberOfViolations = numberOfViolations;
    }

    synchronized public double getTotal_fine() {
        return total_fine;
    }

    synchronized public void setTotal_fine(double total_fine) {
        this.total_fine = total_fine;
    }
}
