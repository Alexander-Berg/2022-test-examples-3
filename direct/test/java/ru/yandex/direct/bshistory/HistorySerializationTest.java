package ru.yandex.direct.bshistory;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;

class HistorySerializationTest {

    static Collection<Object[]> params() {
        return asList(
                new Object[]{"only OrderId", "O32719"},
                new Object[]{"only AdGroupId", "G1509031"},
                new Object[]{"only PhraseBsIds", "P40981572,5587828"},
                new Object[]{"big PhraseBsIds", "P15882277195365960786"}, // DIRECT-76084
                new Object[]{"only BannerIdToBannerBsIds", "16107453:13379847,2028558"},
                new Object[]{"only ImageBannerIdToBannerBsIds", "im16107453:13379847,2028558"},
                new Object[]{"AdGroupId and PhraseBsIds", "G1509031;P40981572,5587828"},
                new Object[]{"all fields",
                        "O32719;G1509031;P40981572,5587828;im16107455:13379843,2028551;16107453:13379847,2028558"}
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    void test(String description, String serializedHistory) {
        History history = History.parse(serializedHistory);
        assertThat(description, history.serialize(), Matchers.is(serializedHistory));
    }

}
