import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Offender;
import entity.TrafficViolation;
import org.apache.commons.math3.util.Precision;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {


    public static void main(String[] args) {
        List<TrafficViolation> trafficViolationList = parseTrafficViolation("src/main/traffic_violations");

        System.out.println(violationsSort(trafficViolationList));

        // Serialize offen
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new File("src/main/output/offenders.json"), parseOffenders(trafficViolationList));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // One thread parsing json files in dir
    public static List<TrafficViolation> parseTrafficViolation(String dir) {
        // .registerModule(new JavaTimeModule()); для роботы с LocalDataTime
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        List<TrafficViolation> trafficViolations = new ArrayList<>();
        try {
            // Getting files path to list
            List<Path> pathsList = Files.walk(Paths.get(dir)).filter(Files::isRegularFile).toList();
            // Parse json and get traffic violations
            for (Path path : pathsList) {
                trafficViolations.addAll(objectMapper.readValue(path.toFile(), new TypeReference<List<TrafficViolation>>() {
                }));
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return trafficViolations;
    }

    public static LinkedHashMap<String, Double> violationsSort(List<TrafficViolation> trafficViolationList) {

        HashMap<String, Double> violations = new HashMap<>();
        for (TrafficViolation trafficViolation : trafficViolationList) {
            if (violations.containsKey(trafficViolation.getType())) {
                violations.put(trafficViolation.getType(), Precision.round(violations.get(trafficViolation.getType()) + trafficViolation.getFine_amount(), 2));
            } else {
                violations.put(trafficViolation.getType(), Precision.round(trafficViolation.getFine_amount(), 2));
            }
        }

        LinkedHashMap<String, Double> sortedViolations = new LinkedHashMap<>();
        violations.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedViolations.put(entry.getKey(), entry.getValue()));

        return sortedViolations;
    }

    public static List<Offender> parseOffenders(List<TrafficViolation> trafficViolationList) {

        List<Offender> offenderList = new ArrayList<>();
        HashSet<UUID> offendersID = new HashSet<>();
        trafficViolationList.forEach(violation -> offendersID.add(violation.getPersonID()));

        // Parsing violations for offender
        for (UUID offenderID : offendersID) {
            boolean isNameBeenSet = false;
            String first_name = null;
            String last_name = null;
            int numberOfViolations = 0;
            double total_fine = 0;
            double average_fine = 0;
            for (TrafficViolation violation : trafficViolationList) {
                if (violation.getPersonID().equals(offenderID)) {
                    if (!isNameBeenSet) {
                        first_name = violation.getFirst_name();
                        last_name = violation.getLast_name();
                    }
                    total_fine += violation.getFine_amount();
                    numberOfViolations++;
                }
                total_fine = Precision.round(total_fine, 2);
                average_fine = Precision.round(total_fine / numberOfViolations, 2);
            }
            offenderList.add(new Offender(first_name, last_name, offenderID, numberOfViolations, total_fine, average_fine));
        }

        return offenderList;
    }

}
