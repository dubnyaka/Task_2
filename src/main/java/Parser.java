import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Offender;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.util.Precision;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Parser {

    public static Map<String, Double> amountForFineMain = new ConcurrentHashMap<>();
    public static Map<UUID, Offender> offendersMain = new ConcurrentHashMap<>();


    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        parse("src/main/traffic_violations/","src/main/output",4);
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime-startTime) + "ms");
    }

    public static void parse(String dir,String outputDir,int threads) throws IOException {
        // Runtime.getRuntime().availableProcessors()
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            List<Path> pathsList = new ArrayList<>();
            pathsList.addAll(paths.filter(Files::isRegularFile).toList());

            for (Path path : pathsList) {
                executorService.submit(new RunnableParsing(path, amountForFineMain, offendersMain));
            }
        }
        executorService.shutdown();
        // Wait for all threads to do their work
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
            // Calculation of the average fine amount for each offender
            offendersMain.forEach((key, value) -> value
                    .setAverage_fine(Precision.round(value.getTotal_fine() / value.getNumberOfViolations(), 2)));
            // Sorting total fine
            LinkedHashMap<String, Double> sortedAmountMap = new LinkedHashMap<>();
            amountForFineMain.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(stringDoubleEntry -> sortedAmountMap.put(stringDoubleEntry.getKey(), stringDoubleEntry.getValue()));
            amountForFineMain = new LinkedHashMap<>(sortedAmountMap);
            // Create output file
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(outputDir+"/amountForFine.json"), amountForFineMain);
            objectMapper.writeValue(new File(outputDir+"/offenders.json"), offendersMain.values());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void streamParsing(Path filePath, HashMap<String, Double> amountForFine, HashMap<UUID, Offender> offenders) throws IOException {
        InputStream inputStream = new FileInputStream(filePath.toString());
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

    private static void streamParsingForThread(Path filePath, Map<String, Double> amountForFineRef, Map<UUID, Offender> offendersRef) throws IOException {
        InputStream inputStream = new FileInputStream(filePath.toString());
        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createParser(inputStream);

        String first_name;
        String last_name;
        UUID personID;
        String type = "";
        double fine_amount;

        // Parse json array
        while (jParser.nextToken() != JsonToken.END_ARRAY) {
            if (jParser.currentToken() == JsonToken.START_OBJECT) {
                Offender newOffender = new Offender();

                // Parse json object
                while (jParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jParser.getCurrentName();
                    if ("date_time".equals(fieldname)) {
                        jParser.nextToken();
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
                        // Put new type violation to Map
                        amountForFineRef.putIfAbsent(type, 0.0);
                    }
                    if ("fine_amount".equals(fieldname)) {
                        jParser.nextToken();
                        fine_amount = jParser.getDoubleValue();
                        newOffender.setTotal_fine(fine_amount);

                        double finalFine_amount = fine_amount;
                        amountForFineRef.computeIfPresent(type, (key, value) -> Precision.round(value + finalFine_amount, 2));
                    }
                }
                newOffender.setNumberOfViolations(1);

                //Get offender from violation to hashmap
                if (offendersRef.containsKey(newOffender.getPersonID())) {
                    // System.out.println(offendersRef);
                    Offender existingOffender = offendersRef.getOrDefault(newOffender.getPersonID(), newOffender);
                    // Incrementing violation count
                    existingOffender.setNumberOfViolations(existingOffender.getNumberOfViolations() + 1);
                    // Add fine_amount to total fine
                    existingOffender.setTotal_fine(Precision.round(existingOffender.getTotal_fine() + newOffender.getTotal_fine(), 2));

                    offendersRef.computeIfPresent(newOffender.getPersonID(), (key, value) -> value = existingOffender);
                } else {
                    offendersRef.putIfAbsent(newOffender.getPersonID(), newOffender);
                }
            }
        }
        jParser.close();
        inputStream.close();
    }

    @AllArgsConstructor
    public static class RunnableParsing implements Runnable {
        final private Path path;
        private Map<String, Double> amountForFineRef;
        private Map<UUID, Offender> offendersRef;

        @Override
        public void run() {
            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                streamParsingForThread(path, amountForFineRef, offendersRef);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
