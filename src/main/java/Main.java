import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Offender;
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


        // Сбор нарушителей по фамилиям (Лучше было бы по id)
        List<Offender> offenders = new ArrayList<>();
        HashMap<String,String> names = new HashMap<>();
        trafficViolations.forEach(trafficViolation -> names.put(trafficViolation.getLast_name(),trafficViolation.getFirst_name()));

        // Проблема с именами и фамилиями (как вариант вижу ввести id)
        for(Map.Entry<String,String> name : names.entrySet()){
            List<TrafficViolation> offenderViolations = trafficViolations.stream()
                    .filter(n->n.getLast_name().equals(name.getKey()) &&
                            n.getFirst_name().equals(name.getValue())).toList();

            int numberOfViolations = 0;
            double total_fine = 0;
            for (TrafficViolation offenderViolation : offenderViolations){
                numberOfViolations++;
                total_fine += offenderViolation.getFine_amount();
            }
            offenders.add(new Offender(name.getValue(),name.getKey(),numberOfViolations,total_fine,total_fine/numberOfViolations));
        }
        offenders.forEach(System.out::println);
    }

    public static LinkedHashMap<String, Double> violationsSort(List<TrafficViolation> trafficViolationList) {

        HashMap<String, Double> violations = new HashMap<>();
        for (TrafficViolation trafficViolation : trafficViolationList) {
            if (violations.containsKey(trafficViolation.getType())) {
                violations.put(trafficViolation.getType(), violations.get(trafficViolation.getType()) + trafficViolation.getFine_amount());
            } else {
                violations.put(trafficViolation.getType(), trafficViolation.getFine_amount());
            }
        }

        LinkedHashMap<String, Double> sortedViolations = new LinkedHashMap<>();
        violations.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedViolations.put(entry.getKey(), entry.getValue()));

        return sortedViolations;
    }

    public static void offendersToFile(List<Offender> offenders) {

    }
    //2. Содержит список нарушителей за все годы:
    // по каждому нарушителю должно быть видно общее количество нарушений,
    // суммарный штраф и среднюю сумму штрафа.


}
