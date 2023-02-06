package ru.yandex.market.rg.asyncreport.unitedcatalog.migration.conflict;

import java.util.List;
import java.util.stream.Stream;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.CopyOfferStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.mismatch;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.offerBuilder;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.conflict.ConflictUtils.explanation;

/**
 * @see OfferMatcher
 */
@SuppressWarnings("unused")
class OfferMatcherTest {

    OfferMatcher matcher = new CopyOfferStrategy(true).offerMatcher();

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void test(String desc, DataCampOffer.Offer source, DataCampOffer.Offer target, boolean result) {
        assertEquals(
                result,
                matcher.matches(source, target)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void testDescribe(String desc, DataCampOffer.Offer source,
                      DataCampOffer.Offer target,
                      boolean result,
                      List<DataCampExplanation.Explanation.Param> mismatch) {
        var explanation = explanation();
        matcher.describeMismatch(source, target, explanation);
        var expected = explanation.addAllParams(mismatch).build();
        assertThat(
                explanation.build(),
                equalTo(expected)
        );
    }

    static Stream<Arguments> args() {
        return Stream.of(
                arguments("empty",
                        offerBuilder().build().getBasic(),
                        offerBuilder().build().getBasic(),
                        true,
                        mismatch()),
                arguments("same barcode",
                        offerBuilder().withBarcode("1234").build().getBasic(),
                        offerBuilder().withBarcode("1234").build().getBasic(),
                        true,
                        mismatch()),
                arguments("diff barcode",
                        offerBuilder().withBarcode("1234").build().getBasic(),
                        offerBuilder().withBarcode("4321").build().getBasic(),
                        false,
                        mismatch(
                                "sourceBarcodes", "1234",
                                "targetBarcodes", "4321"
                        )),
                arguments("common barcode",
                        offerBuilder().withBarcode("1234", "4321").build().getBasic(),
                        offerBuilder().withBarcode("4321", "9876").build().getBasic(),
                        true,
                        mismatch()),
                arguments("common vendor code",
                        offerBuilder()
                                .withBarcode()
                                .withVendorCode("gnusmas", "yxalag")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("4321", "9876")
                                .withVendorCode("gnusmas", "yxalag")
                                .build().getBasic(),
                        true,
                        mismatch()),
                arguments("normalized vendor code",
                        offerBuilder()
                                .withBarcode()
                                .withVendorCode("gnusmas", "yxalag  S   ")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("4321", "9876")
                                .withVendorCode("gnusmas", "   YXALAG s")
                                .build().getBasic(),
                        true,
                        mismatch()),
                arguments("diff vendor code",
                        offerBuilder()
                                .withBarcode()
                                .withVendorCode("gnusmas", "yxalag S2")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("4321", "9876")
                                .withVendorCode("gnusmas", "yxalag s")
                                .build().getBasic(),
                        false,
                        mismatch(
                                "sourceVendorCode", "yxalag S2",
                                "targetVendorCode", "yxalag s"
                        )),
                arguments("diff name",
                        offerBuilder()
                                .withBarcode()
                                .withVendorCode("gnusmas", "yxalag S2")
                                .withName("красное")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("4321", "9876")
                                .withName("белое")
                                .build().getBasic(),
                        false,
                        mismatch(
                                "sourceName", "красное",
                                "targetName", "белое"
                        )),
                arguments("normalized name",
                        offerBuilder()
                                .withName("  КРАСНОЕ и   белое")
                                .build().getBasic(),
                        offerBuilder()
                                .withName("беЛОЕ   и КРАсное  ")
                                .build().getBasic(),
                        true,
                        mismatch()),
                arguments("same barcode diff name",
                        offerBuilder()
                                .withBarcode("1234")
                                .withName("asdf")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("1234")
                                .withName("fdsa")
                                .build().getBasic(),
                        true,
                        mismatch("sourceName", "asdf", "targetName", "fdsa")),
                arguments("same vendor code diff name",
                        offerBuilder()
                                .withVendorCode("gnusmas", "yxalag")
                                .withName("asdf")
                                .build().getBasic(),
                        offerBuilder()
                                .withVendorCode("gnusmas", "yxalag")
                                .withName("fdsa")
                                .build().getBasic(),
                        true,
                        mismatch(
                                "sourceName", "asdf",
                                "targetName", "fdsa"
                        )),
                arguments("same barcode diff vendorcode",
                        offerBuilder()
                                .withBarcode("1234", "444")
                                .withVendorCode("gnusmas", "yxalag")
                                .build().getBasic(),
                        offerBuilder().withBarcode("1234", "555")
                                .withVendorCode("samsung", "galaxy")
                                .build().getBasic(),
                        true,
                        mismatch(
                                "sourceVendor", "gnusmas",
                                "targetVendor", "samsung",
                                "sourceVendorCode", "yxalag",
                                "targetVendorCode", "galaxy"
                        )),
                arguments("all diff",
                        offerBuilder()
                                .withBarcode("1234", "444")
                                .withVendorCode("gnusmas", "yxalag")
                                .withName("source name")
                                .build().getBasic(),
                        offerBuilder()
                                .withBarcode("9876", "555")
                                .withVendorCode("samsung", "galaxy")
                                .withName("target name")
                                .build().getBasic(),
                        false,
                        mismatch(
                                "sourceBarcodes", "1234, 444",
                                "targetBarcodes", "9876, 555",
                                "sourceVendor", "gnusmas",
                                "targetVendor", "samsung",
                                "sourceVendorCode", "yxalag",
                                "targetVendorCode", "galaxy",
                                "sourceName", "source name",
                                "targetName", "target name"
                        ))
        );
    }

}
