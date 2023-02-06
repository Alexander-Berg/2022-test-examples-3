package ru.yandex.market.logistics.lom.service.deadlines;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;

class RegionSDDeadlineServiceTest extends AbstractTest {

    @Test
    void noCollisionsInRegionNames() {
        List<String> allRegionNamesFromDeadlines = RegionSDDeadlineService.REGION_DELIVERY_SERVICE_DEADLINES
            .keySet()
            .stream()
            .map(Pair::getKey)
            .collect(Collectors.toList());

        List<String> manuallyUniqueRegionNames = new ArrayList<>();
        allRegionNamesFromDeadlines.stream()
            .filter(regionNameToAdd -> !manuallyUniqueRegionNames.contains(regionNameToAdd)) // uses String.equals()
            .forEach(manuallyUniqueRegionNames::add);
        Set<Integer> uniqueRegionHashCodes = allRegionNamesFromDeadlines.stream()
            .map(String::hashCode) // use String.hashCode()
            .collect(Collectors.toSet());

        softly.assertThat(manuallyUniqueRegionNames.size()).isEqualTo(uniqueRegionHashCodes.size());
    }
}
