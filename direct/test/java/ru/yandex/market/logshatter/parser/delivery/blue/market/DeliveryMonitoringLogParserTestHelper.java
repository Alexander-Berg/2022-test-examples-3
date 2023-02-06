package ru.yandex.market.logshatter.parser.delivery.blue.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.yandex.market.logshatter.parser.checkout.events.Event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class DeliveryMonitoringLogParserTestHelper {

    private final Gson gson = new GsonBuilder().setDateFormat("dd-MM-yyyy HH:mm:ss").create();

    public <T> void assertRecords(List<T> monitoringRecords, T... records) {
        assertEquals(monitoringRecords.toString(), records.length, monitoringRecords.size());
        for (int i = 0; i < records.length; i++) {
            assertEquals("Error in " + i + "'th element", records[i], monitoringRecords.get(i));
        }
    }

    public Event readEvent(String file) throws Exception {
        String line = readInput(file);
        return gson.fromJson(line, Event.class);
    }


    public String readInput(String fileName) throws Exception {
        Path path = Paths.get(getClass().getClassLoader()
            .getResource("blueOrderMonitoring/" + fileName).toURI());
        return Files.lines(path).collect(Collectors.joining(""));
    }

    public Gson getGson() {
        return gson;
    }
}