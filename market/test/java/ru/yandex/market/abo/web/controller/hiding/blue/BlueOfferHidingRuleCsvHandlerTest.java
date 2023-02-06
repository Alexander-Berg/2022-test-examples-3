package ru.yandex.market.abo.web.controller.hiding.blue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason;
import ru.yandex.market.abo.web.util.ControllerUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 * @date 03/04/2020.
 */
class BlueOfferHidingRuleCsvHandlerTest {

    private static final Long MSKU_1 = 42342L;
    private static final String SHOP_SKU_3 = "shop_sku_3";
    private static final BlueOfferHidingReason REASON_2 = BlueOfferHidingReason.NO_GUARANTEE;

    /**
     * market_sku; supplier_id; shop_sku; reason; comment; deleted
     */
    private static final String HIDING_RULES = BlueOfferHidingRuleCsvHandler.HEADER + "\n" +
            MSKU_1 + ";534;;LEGAL;foo;public_comment;true\n" +
            "534534;;;" + REASON_2 + ";bar;public_comment;false\n" +
            ";4235435;" + SHOP_SKU_3 + ";FAULTY;baz;public_comment;true";

    private static final String INVALID_RULE_1 = BlueOfferHidingRuleCsvHandler.HEADER + "\n" +
            "bad_msku;534;;LEGAL;foo;true";

    private static final String INVALID_RULE_2 = BlueOfferHidingRuleCsvHandler.HEADER + "\n" +
            "4234;534;;LEGAL;foo;";

    private static final String INVALID_RULE_3 = BlueOfferHidingRuleCsvHandler.HEADER + "\n" +
            "123;4235435;shop_sku;FAULTY;baz;TRUE";

    @Test
    void extractFile() {
        var extracted = BlueOfferHidingRuleCsvHandler
                .extract(new MockMultipartFile("_", HIDING_RULES.getBytes()), ControllerUtil.DEFAULT_ENCODING);
        assertEquals(3, extracted.size());
        assertEquals(MSKU_1, extracted.get(0).getMarketSku());
        assertEquals(true, extracted.get(0).getDeleted());
        assertNull(extracted.get(0).getShopSku());
        assertEquals(REASON_2, extracted.get(1).getHidingReason());
        assertEquals(SHOP_SKU_3, extracted.get(2).getShopSku());
    }

    @ParameterizedTest(name = "invalid_hiding_rules_{index}")
    @MethodSource("badHidingRules")
    void extractInvalid(String rulesCsv) {
        assertThrows(IllegalArgumentException.class,
                () -> BlueOfferHidingRuleCsvHandler.extract(
                        new MockMultipartFile("_", rulesCsv.getBytes()), ControllerUtil.DEFAULT_ENCODING
                )
        );
    }

    static Stream<Arguments> badHidingRules() {
        return Stream.of(INVALID_RULE_1, INVALID_RULE_2, INVALID_RULE_3).map(Arguments::of);
    }
}
