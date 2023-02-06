package ru.yandex.direct.web.entity.adgroup.service.cpm;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;

import static org.junit.Assert.assertEquals;

public class CpmAdGroupHelperTest {

    @Test
    public void getGeneralPrice_NoRetargetings_ReturnGeneralPrice() {
        WebCpmAdGroup adGroup = new WebCpmAdGroup();
        adGroup.withGeneralPrice(0.3);
        BigDecimal price = CpmAdGroupHelper.getGeneralPrice(adGroup);
        assertEquals(new BigDecimal("0.3"), price);
    }

    @Test
    public void getGeneralPrice_WithRetargetings_ReturnCorrect() {
        WebCpmAdGroup adGroup = new WebCpmAdGroup();
        adGroup.withGeneralPrice(0.3)
                .withRetargetings(Collections.singletonList(new WebCpmAdGroupRetargeting().withPriceContext(0.4)));
        BigDecimal price = CpmAdGroupHelper.getGeneralPrice(adGroup);
        assertEquals(new BigDecimal("0.4"), price);
    }
}
