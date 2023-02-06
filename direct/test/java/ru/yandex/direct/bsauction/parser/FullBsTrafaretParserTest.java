package ru.yandex.direct.bsauction.parser;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.bsauction.BsAuctionBidItem;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.yabs.server.proto.rank.TTrafaretClickometer;
import ru.yandex.yabs.server.proto.rank.TTrafaretClickometerRow;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class FullBsTrafaretParserTest {

    private FullBsTrafaretParser parserUnderTest;

    @Before
    public void setUp() throws Exception {
        parserUnderTest = new FullBsTrafaretParser(this::convertMicros);
    }

    private Money convertMicros(long micros) {
        return Money.valueOfMicros(micros, CurrencyCode.YND_FIXED);
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
                .build();

        FullBsTrafaretResponsePhrase responsePhrase = parserUnderTest.convertClickometer(clickometer);
        assumeThat(responsePhrase.getBidItems(), hasSize(1));

        BsAuctionBidItem actual = responsePhrase.getBidItems().get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getBid()).describedAs("Bid").isEqualTo(convertMicros(10_000_000L));
            softly.assertThat(actual.getPrice()).describedAs("Price").isEqualTo(convertMicros(9_000_000L));
            softly.assertThat(actual.getPositionCtrCorrection()).describedAs("PositionCtrCorrection")
                    .isEqualTo(1_000_000L);
        });
    }

    @Test
    public void convertClickometer_whenGuaranteeMustBeNormalized() {
        /*
        Логика определения, требуется ли нормализация Гарантии (TrafaretID=-1) такая:
        если у нулевой позиции этого трафарета X=1, то нужно нормализовать, поделив на 10
        */
        TTrafaretClickometer clickometer = TTrafaretClickometer.newBuilder()
                .setTargetID(1L)
                .addClickometer(TTrafaretClickometerRow.newBuilder()
                        .setTrafaretID(-1)
                        .setPosition(0)
                        .setBid(10_000_000L)
                        .setCpc(9_000_000L)
                        .setX(1_000_000L)
                        .build())
                .build();

        FullBsTrafaretResponsePhrase responsePhrase = parserUnderTest.convertClickometer(clickometer);
        assumeThat(responsePhrase.getBidItems(), hasSize(1));

        BsAuctionBidItem actual = responsePhrase.getBidItems().get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getBid()).describedAs("Bid").isEqualTo(convertMicros(10_000_000L));
            softly.assertThat(actual.getPrice()).describedAs("Price").isEqualTo(convertMicros(9_000_000L));
            softly.assertThat(actual.getPositionCtrCorrection()).describedAs("PositionCtrCorrection")
                    .isEqualTo(100_000L);
        });
    }

    @Test
    public void convertClickometer_whenGuaranteeMustBeNormalizedAndPremiumPresent() {
        // Аналогично соседнему тесту, но проверяем, что не задеваем поправкой Спецразмещение
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
                        .setTrafaretID(-1)
                        .setPosition(0)
                        .setBid(1_000_000L)
                        .setCpc(900_000L)
                        .setX(1_000_000L)
                        .build())
                .build();

        FullBsTrafaretResponsePhrase responsePhrase = parserUnderTest.convertClickometer(clickometer);
        assumeThat(responsePhrase.getBidItems(), hasSize(2));

        BsAuctionBidItem actualPremium = responsePhrase.getBidItems().get(0);
        BsAuctionBidItem actualGuarantee = responsePhrase.getBidItems().get(1);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actualPremium.getBid()).describedAs("Bid")
                    .isEqualTo(convertMicros(10_000_000L));
            softly.assertThat(actualPremium.getPrice()).describedAs("Price")
                    .isEqualTo(convertMicros(9_000_000L));
            softly.assertThat(actualPremium.getPositionCtrCorrection()).describedAs("PositionCtrCorrection")
                    .isEqualTo(1_000_000L);

            softly.assertThat(actualGuarantee.getBid()).describedAs("Bid")
                    .isEqualTo(convertMicros(1_000_000L));
            softly.assertThat(actualGuarantee.getPrice()).describedAs("Price")
                    .isEqualTo(convertMicros(900_000L));
            softly.assertThat(actualGuarantee.getPositionCtrCorrection()).describedAs("PositionCtrCorrection")
                    .isEqualTo(100_000L);
        });
    }
}
