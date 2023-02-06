package ru.yandex.market.abo.web.controller.rating.partner.exclusion;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.abo.core.rating.partner.exclusion.model.RatingExclusionType;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.06.2020
 */
class PartnerRatingExclusionCsvHandlerTest {
    private static final long PARTNER_ID = 123L;
    private static final long ORDER_ID = 42342L;
    private static final long REQUEST_ID = 32152L;
    private static final String EXCLUDED_DATE = "2001-07-08";
    private static final String COMMENT = "магазин-плохиш";

    private static final String BY_ORDER_EXCLUSION = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            PARTNER_ID + ";" + PartnerModel.CROSSDOCK + ";" + RatingExclusionType.BY_ORDER + ";" + ORDER_ID + ";;;" + COMMENT + ";false\n";

    private static final String EMPTY_ORDER_ID_EXCLUSION = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            PARTNER_ID + ";" + PartnerModel.CROSSDOCK + ";" + RatingExclusionType.BY_ORDER + ";" + ";;;" + COMMENT + ";false";

    private static final String TYPE_NOT_SPECIFIED = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            PARTNER_ID + ";" + PartnerModel.CROSSDOCK + ";" + ORDER_ID + ";;;" + COMMENT + ";false\n";

    private static final String EMPTY_DELETED_EXCLUSION = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            ";" + PartnerModel.DSBB + ";" + RatingExclusionType.BY_ORDER + ";" + ORDER_ID + ";;;" + COMMENT + ";\n";

    private static final String EMPTY_COMMENT_EXCLUSION = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            ";" + PartnerModel.DSBS + ";" + RatingExclusionType.BY_ORDER + ";" + ORDER_ID + ";;;;false";

    private static final String MORE_THEN_ONE_EXCLUSION_TYPES = PartnerRatingExclusionCsvHandler.HEADER + "\n" +
            PARTNER_ID + ";" + PartnerModel.CROSSDOCK + ";" + RatingExclusionType.BY_REQUEST + ";" + ORDER_ID + ";" + REQUEST_ID + ";" + EXCLUDED_DATE + ";" + COMMENT + ";false";

    @Test
    void extractByOrderExclusionTest() {
        var extracted = PartnerRatingExclusionCsvHandler.extract(new MockMultipartFile("_", BY_ORDER_EXCLUSION.getBytes()));
        assertEquals(1, extracted.size());
        assertEquals(ORDER_ID, extracted.get(0).getOrderId());
        assertEquals(false, extracted.get(0).getDeleted());
        assertEquals(COMMENT, extracted.get(0).getComment());
    }

    @ParameterizedTest(name = "invalid_exclusions_{index}")
    @MethodSource("invalidExclusions")
    void extractInvalidExclusions(String exclusionsCsv) {
        assertThrows(IllegalArgumentException.class,
                () -> PartnerRatingExclusionCsvHandler.extract(new MockMultipartFile("_", exclusionsCsv.getBytes())));
    }

    static Stream<Arguments> invalidExclusions() {
        return Stream.of(
                EMPTY_ORDER_ID_EXCLUSION,
                TYPE_NOT_SPECIFIED,
                EMPTY_DELETED_EXCLUSION,
                EMPTY_COMMENT_EXCLUSION,
                MORE_THEN_ONE_EXCLUSION_TYPES
        ).map(Arguments::of);
    }
}
