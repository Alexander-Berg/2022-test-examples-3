package ru.yandex.direct.bshistory;

import java.math.BigInteger;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class HistoryTest {
    @Test
    void parse() {
        History result = History.parse(
                "O32719;G1509031;P15882277195365960786,5587828;im16107455:13379843,2028551;16107453:13379847,2028558");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getPhraseBsIds())
                    .containsExactly(new BigInteger("15882277195365960786"), new BigInteger("5587828"));
            softly.assertThat(result.getOrderId()).isEqualTo(32719L);
            softly.assertThat(result.getAdGroupId()).isEqualTo(1509031L);
            softly.assertThat(result.getBannerIdToBannerBsIds()).isEqualTo(
                    ImmutableMap.of(16107453L, asList(13379847L, 2028558L)));
            softly.assertThat(result.getImageBannerIdToBannerBsIds()).isEqualTo(
                    ImmutableMap.of(16107455L, asList(13379843L, 2028551L)));
        });
    }

    @Test
    void parseWithEmptyPhraseBsIds() {
        History result = History.parse("P");
        assertThat("empty PhraseBsIds (DIRECT-88556)", result.serialize(), equalTo(""));
    }
}
