package entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Offender {
    private String first_name;
    private String last_name;
    int numberOfViolations;
    double total_fine;
    double average_fine;
}
