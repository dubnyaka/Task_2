import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.TrafficViolation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {


    public static void main(String[] args) throws IOException {
        // .registerModule(new JavaTimeModule()); для роботы с LocalDataTime
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Getting files path to list
        List<Path> pathsList = Files.walk(Paths.get("src/main/traffic_violations/")).filter(Files::isRegularFile).toList();
        // Parse json and get v traffic violations
        List<TrafficViolation> trafficViolations = new ArrayList<>();
        for (Path path : pathsList) {
            trafficViolations.addAll(objectMapper.readValue(path.toFile(), new TypeReference<List<TrafficViolation>>() {
            }));
        }
        System.out.println(violationsSort(trafficViolations));
    }

    public static LinkedHashMap<String, Double> violationsSort(List<TrafficViolation> trafficViolationList) {
        HashMap<String, Double> violations = new HashMap<>();
        for (TrafficViolation trafficViolation : trafficViolationList) {
            if (violations.containsKey(trafficViolation.getType())) {
                violations.put(trafficViolation.getType(), violations.get(trafficViolation.getType()) + trafficViolation.getFine_amount());
            }else{
                violations.put(trafficViolation.getType(),trafficViolation.getFine_amount());
            }
        }

        LinkedHashMap<String,Double> sortedViolations = new LinkedHashMap<>();
        violations.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedViolations.put(entry.getKey(),entry.getValue()));


        return sortedViolations;
    }
}
