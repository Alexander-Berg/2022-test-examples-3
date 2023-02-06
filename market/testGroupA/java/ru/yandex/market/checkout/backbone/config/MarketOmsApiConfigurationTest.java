package ru.yandex.market.checkout.backbone.config;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.promo.loyalty.client.DiscountRequestFactoryTest;
import ru.yandex.market.common.report.model.FeedOfferId;

import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.GENERIC_BUNDLE;

public class MarketOmsApiConfigurationTest extends AbstractServicesTestBase {
    @Autowired
    public ObjectMapper marketOmsObjectMapper;

    @Test
    public void mapperTest() throws IOException {
        var multiOrder = MarketOmsTestUtils.generateMultiOrderWithAdditionalInfo();

        // Ошибок нет, значит всё хорошо
        var stringFromObject = marketOmsObjectMapper.writeValueAsString(multiOrder);
        var objectFromString = marketOmsObjectMapper.readValue(stringFromObject, MultiOrder.class);

        var offerDiscount = objectFromString.getAdditionalInfo().getOffersDiscounts().get(new FeedOfferId("123", 123L))
                .iterator().next();
        var payByYaPlus = objectFromString.getAdditionalInfo().getOffersMap().get(new FeedOfferId("123", 123L))
                .getPayByYaPlus();
        Assertions.assertEquals(GENERIC_BUNDLE, offerDiscount.getReportPromoType());
        var promoDetails = offerDiscount.getPromoDetails();
        Assertions.assertEquals(DiscountRequestFactoryTest.PROMO_KEY, promoDetails.getPromoKey());
        Assertions.assertEquals(GENERIC_BUNDLE.getCode(), promoDetails.getPromoType());
        Assertions.assertEquals(0, payByYaPlus.getPrice());
    }
}
