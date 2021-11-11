import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Offender;
import entity.TrafficViolation;
import org.apache.commons.math3.util.Precision;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {


    public static void main(String[] args) throws IOException {
        File jsonFile = new File("src/main/java/traffic_violationsBIG.json").getAbsoluteFile();

        InputStream inputStream = new FileInputStream("src/main/traffic_violations/traffic_violationsBIG.json");

        HashMap<String, Double> typeSum = new HashMap<>();
        HashMap<UUID, Offender> offenders = new HashMap<>();

        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("MB: " + usedBytes / 1048576);
        long startTime = System.currentTimeMillis();

        streamParsing(inputStream, typeSum, offenders);
        for (Map.Entry<UUID, Offender> offenderEntry : offenders.entrySet()) {
            Offender offender = offenderEntry.getValue();
            offender.setAverage_fine(Precision.round(offender.getTotal_fine() / offender.getNumberOfViolations(), 2));
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("MB: " + usedBytes / 1048576);

        System.out.println(typeSum);

        int x = 0;
        for (Map.Entry<UUID, Offender> offenderEntry : offenders.entrySet()) {
            System.out.println(offenderEntry.getValue());
            if (x > 10) break;
            x++;
        }


    }

    public static void streamParsing(InputStream inputStream, HashMap<String, Double> amountForFine, HashMap<UUID, Offender> offenders) throws IOException {
        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(inputStream);

        LocalDateTime date_time;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
        String first_name;
        String last_name;
        UUID personID;
        String type = "";
        double fine_amount = 0;

        // Parse json array
        while (jParser.nextToken() != JsonToken.END_ARRAY) {
            if (jParser.currentToken() == JsonToken.START_OBJECT) {
                Offender newOffender = new Offender();

                // Parse json object
                while (jParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jParser.getCurrentName();
                    if ("date_time".equals(fieldname)) {
                        jParser.nextToken();
                        LocalDateTime dateTime = LocalDateTime.parse(jParser.getText(), formatter);
                        date_time = dateTime;
                    }

                    if ("first_name".equals(fieldname)) {
                        jParser.nextToken();
                        first_name = jParser.getText();
                        newOffender.setFirst_name(first_name);
                    }

                    if ("last_name".equals(fieldname)) {
                        jParser.nextToken();
                        last_name = jParser.getText();
                        newOffender.setLast_name(last_name);
                    }

                    if ("personID".equals(fieldname)) {
                        jParser.nextToken();
                        personID = UUID.fromString(jParser.getText());
                        newOffender.setPersonID(personID);
                    }

                    if ("type".equals(fieldname)) {
                        jParser.nextToken();
                        type = jParser.getText();
                    }

                    if ("fine_amount".equals(fieldname)) {
                        jParser.nextToken();
                        fine_amount = jParser.getDoubleValue();
                        newOffender.setAverage_fine(fine_amount);
                        newOffender.setTotal_fine(fine_amount);
                    }
                }
                newOffender.setNumberOfViolations(1);

                // Денежная сумма штрафов по типам.
                if (amountForFine.containsKey(type)) {
                    amountForFine.put(type, amountForFine.get(type) + fine_amount);
                } else {
                    amountForFine.put(type, fine_amount);
                }
                //Get offender from violation to hashmap
                if (offenders.containsKey(newOffender.getPersonID())) {
                    Offender existingOffender = offenders.get(newOffender.getPersonID());
                    // Incrementing violation count
                    existingOffender.setNumberOfViolations(existingOffender.getNumberOfViolations() + 1);
                    // Add fine_amount to total fine
                    existingOffender.setTotal_fine(Precision.round(existingOffender.getTotal_fine() + newOffender.getTotal_fine(), 2));
                } else {
                    offenders.put(newOffender.getPersonID(), newOffender);
                }
            }
        }
        jParser.close();
        inputStream.close();
    }

}
