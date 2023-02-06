package ru.yandex.market.promoboss.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.promoboss.utils.PromoFieldUtils.promoFieldsFromApiValues;
import static ru.yandex.market.promoboss.utils.PromoFieldUtils.promoFieldsFromRequest;

public class PromoFieldUtilsTest {

    public static Set<PromoField> createAll() {
        return new HashSet<>(List.of(PromoField.values()));
    }

    @Test
    void promoSegmentsFromRequest_empty() {
        Set<PromoField> promoFields = promoFieldsFromRequest(new PromoRequestV2());

        assertNotNull(promoFields);
        assertEquals(0, promoFields.size());
    }

    @Test
    void promoSegmentsFromGet_empty() {
        Set<PromoField> promoFields = promoFieldsFromApiValues(List.of());

        assertNotNull(promoFields);
        assertEquals(0, promoFields.size());
    }

    @Test
    void promoSegmentsFromRequest_notEmpty() {
        Set<PromoField> promoFields = promoFieldsFromRequest(new PromoRequestV2()
                .main(new PromoMainRequestParams()));

        assertNotNull(promoFields);
        assertEquals(1, promoFields.size());
        assertEquals(PromoField.MAIN, promoFields.stream().findFirst().orElseThrow());
    }
}
