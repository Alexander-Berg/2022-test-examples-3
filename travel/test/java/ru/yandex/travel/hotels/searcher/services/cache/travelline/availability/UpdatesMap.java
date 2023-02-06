package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdatesMap {
    private final Map<String, Map<LocalDate, Long>> updatesMap;
    private final Map<String, Long> lastUpdates;
    private int counter;

    private UpdatesMap() {
        counter = 1;
        updatesMap = new HashMap<>();
        lastUpdates = new HashMap<>();
    }

    public static UpdatesMap generate(int numberOfHotels, int numberOfDays) {
        var res = new UpdatesMap();
        for (int hotels = 0; hotels < numberOfHotels; hotels++) {
            for (int day = 0; day < numberOfDays; day++) {
                res.update(String.valueOf(hotels), LocalDate.now().plusDays(day));
            }
        }
        return res;
    }

    public void update(String code, LocalDate date) {
        long newVersion = counter++;
        if (!updatesMap.containsKey(code)) {
            updatesMap.put(code, new HashMap<>());
        }
        updatesMap.get(code).put(date, newVersion);
        lastUpdates.put(code, newVersion);
    }

    public void remove(String code, LocalDate date) {
        if (!updatesMap.containsKey(code)) {
            updatesMap.put(code, new HashMap<>());
        }
        updatesMap.get(code).remove(date);
    }

    Collection<String> getAllCodes() {
        return lastUpdates.keySet();
    }

    public void remove(String code) {
        updatesMap.remove(code);
        lastUpdates.remove(code);
    }

    List<Map.Entry<LocalDate, Long>> getInventory(String code) {
        return updatesMap.getOrDefault(code, Collections.emptyMap()).entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getKey().toEpochDay()))
                .collect(Collectors.toList());
    }

    Long getVersion(String code) {
        return lastUpdates.get(code);
    }
}
