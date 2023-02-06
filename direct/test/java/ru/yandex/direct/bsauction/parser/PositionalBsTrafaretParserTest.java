package ru.yandex.direct.bsauction.parser;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.bsauction.BsCpcPrice;
import ru.yandex.direct.bsauction.PositionalBsTrafaretResponsePhrase;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.yabs.server.proto.rank.TTrafaretClickometer;
import ru.yandex.yabs.server.proto.rank.TTrafaretClickometerRow;

import static org.assertj.core.api.Assertions.assertThat;

public class PositionalBsTrafaretParserTest {

    private PositionalBsTrafaretParser parserUnderTest;

    @Before
    public void setUp() throws Exception {
        parserUnderTest = new PositionalBsTrafaretParser(this::convertMicros);
    }

    private Money convertMicros(long micros) {
        return Money.valueOfMicros(micros, CurrencyCode.YND_FIXED);
    }

    @Test
    public void convertClickometerItem_success() {
        TTrafaretClickometerRow clickometerRow =
                TTrafaretClickometerRow.newBuilder()
                        .setX(1_000_000L)
                        .setTrafaretID(1L)
                        .setPosition(0L)
                        .setBid(10_000_000L)
                        .setCpc(9_000_000L)
                        .build();
        BsCpcPrice actual = parserUnderTest.convertClickometerItem(clickometerRow);
        BsCpcPrice expected = buildPrice(10_000_000L, 9_000_000L);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertClickometer_success() {
        TTrafaretClickometer clickometer = TTrafaretClickometer.newBuilder()
                .setTargetID(1L)
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(1)
                        .setPosition(0)
                        .setBid(10_000_000L)
                        .setCpc(9_000_000L)
                        .setX(1_000_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(1)
                        .setPosition(1)
                        .setBid(9_000_000L)
                        .setCpc(8_000_000L)
                        .setX(900_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(1)
                        .setPosition(2)
                        .setBid(8_000_000L)
                        .setCpc(7_000_000L)
                        .setX(800_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(1)
                        .setPosition(3)
                        .setBid(7_000_000L)
                        .setCpc(6_000_000L)
                        .setX(700_000L)
                        .build())
                // Второй трафарет со Спецразмещением по более высоким ценам
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(2)
                        .setPosition(0)
                        .setBid(11_000_000L)
                        .setCpc(10_000_000L)
                        .setX(1_100_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(2)
                        .setPosition(1)
                        .setBid(10_500_000L)
                        .setCpc(9_500_000L)
                        .setX(1_050_000L)
                        .build())
                // Гарантия
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(-1)
                        .setPosition(0)
                        .setBid(6_000_000L)
                        .setCpc(5_000_000L)
                        .setX(600_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(-1)
                        .setPosition(1)
                        .setBid(5_000_000L)
                        .setCpc(4_000_000L)
                        .setX(500_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(-1)
                        .setPosition(2)
                        .setBid(4_000_000L)
                        .setCpc(3_000_000L)
                        .setX(400_000L)
                        .build())
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(-1)
                        .setPosition(3)
                        .setBid(3_000_000L)
                        .setCpc(2_000_000L)
                        .setX(300_000L)
                        .build())
                .build();

        PositionalBsTrafaretResponsePhrase actual = parserUnderTest.convertClickometer(clickometer);
        assertThat(actual).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getId())
                    .describedAs("Id")
                    .isEqualTo(1);
            softly.assertThat(actual.getPremium())
                    .describedAs("Premium")
                    .containsExactly(
                            buildPrice(10_000_000L, 9_000_000L),
                            buildPrice(9_000_000L, 8_000_000L),
                            buildPrice(8_000_000L, 7_000_000L),
                            buildPrice(7_000_000L, 6_000_000L));
            softly.assertThat(actual.getGuarantee())
                    .describedAs("Guarantee")
                    .containsExactly(
                            buildPrice(6_000_000L, 5_000_000L),
                            buildPrice(5_000_000L, 4_000_000L),
                            buildPrice(4_000_000L, 3_000_000L),
                            buildPrice(3_000_000L, 2_000_000L));

        });
    }

    private BsCpcPrice buildPrice(long bid, long cpc) {
        return new BsCpcPrice(convertMicros(cpc), convertMicros(bid));
    }
}
