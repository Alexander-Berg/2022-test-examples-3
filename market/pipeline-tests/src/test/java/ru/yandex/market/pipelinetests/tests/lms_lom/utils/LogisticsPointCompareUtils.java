package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

@UtilityClass
@ParametersAreNonnullByDefault
public class LogisticsPointCompareUtils {

    public void comparePointLists(
        SoftAssertions softly,
        List<LogisticsPointResponse> expected,
        List<LogisticsPointResponse> actual
    ) {
        Assertions.assertEquals(expected.size(), actual.size(), "Не совпали количества найденных точек");
        expected.sort(Comparator.comparingLong(LogisticsPointResponse::getId));
        actual.sort(Comparator.comparingLong(LogisticsPointResponse::getId));

        for (int i = 0; i < expected.size(); ++i) {
            comparePoints(softly, expected.get(i), actual.get(i));
        }
    }

    public void comparePoints(SoftAssertions softly, LogisticsPointResponse expected, LogisticsPointResponse actual) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .comparingOnlyFields(
                "id",
                "active",
                "externalId",
                "instruction",
                "name",
                "partnerId",
                "type",
                "contact"
            )
            .as("Не совпали поля точек")
            .isEqualTo(expected);
        compareAddresses(softly, expected.getAddress(), actual.getAddress());
        comparePhones(softly, expected.getPhones(), actual.getPhones());
        compareSchedules(softly, expected.getSchedule(), actual.getSchedule());
    }

    public void compareAddresses(SoftAssertions softly, Address expected, Address actual) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .comparingOnlyFields(
                "apartment",
                "building",
                "comment",
                "country",
                "house",
                "housing",
                "latitude",
                "locationId",
                "longitude",
                "postCode",
                "region",
                "settlement",
                "street"
            )
            .as("Не совпали адреса точек")
            .withComparatorForFields(new BigDecimalComparator(), "latitude", "longitude")
            .isEqualTo(expected);
    }

    public void comparePhones(SoftAssertions softly, Set<Phone> expected, Set<Phone> actual) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .comparingOnlyFields("internalNumber", "number")
            .as("Не совпали телефоны точек")
            .isEqualTo(expected);
    }

    public void compareSchedules(
        SoftAssertions softly,
        Set<ScheduleDayResponse> expected,
        Set<ScheduleDayResponse> actual
    ) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .as("Не совпали расписания точек")
            .isEqualTo(expected);
    }
}
