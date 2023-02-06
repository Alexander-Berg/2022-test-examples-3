package ru.yandex.market.adv.promo.mvc.promo.cashback.logger;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;

public class CashbackLoggerTest extends FunctionalTest {

    @Test
    public void testPartnerCustomCashbackController() {
        long partnerId = 55;
        String promoId = partnerId + "_promo55";
        CashbackLogger logger =
                CashbackLogger.fillWithBaseInfo(CashbackLogger.ActionType.UPDATE, partnerId, LocalDateTime.of(2020, 12, 12, 12, 12))
                        .put(CashbackLogger.CashbackLoggerFields.CASHBACK_TYPE, PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK)
                        .put(CashbackLogger.CashbackLoggerFields.PROMO_ID, promoId);

        Assertions.assertEquals(
                "{\"PROMO_ID\":\"" + promoId + "\",\"ACTION_TIME\":\"2020-12-12 12:12:00+0300\",\"CASHBACK_TYPE\":\"PARTNER_CUSTOM_CASHBACK\",\"PARTNER_ID\":" + partnerId + ",\"ACTION_TYPE\":\"UPDATE\"}",
                logger.build());
    }

    @Test
    public void creationCollectionTest() {
        long partnerId = 66;
        String promoId = partnerId + "_promo66";
        CashbackLogger logger =
                CashbackLogger.fillWithBaseInfo(CashbackLogger.ActionType.UPDATE, partnerId, LocalDateTime.of(2020, 12, 12, 12, 12))
                        .put(CashbackLogger.CashbackLoggerFields.CASHBACK_TYPE, PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK)
                        .put(CashbackLogger.CashbackLoggerFields.PROMO_ID, promoId)
                        .put(CashbackLogger.CashbackLoggerFields.CATEGORY_IDS, List.of(15, 16));
        Assertions.assertEquals(
                "{\"PROMO_ID\":\"" + promoId + "\",\"ACTION_TIME\":\"2020-12-12 12:12:00+0300\",\"CASHBACK_TYPE\":\"PARTNER_CUSTOM_CASHBACK\",\"CATEGORY_IDS\":[15,16],\"PARTNER_ID\":" + partnerId + ",\"ACTION_TYPE\":\"UPDATE\"}",
                logger.build());
    }

    @Test
    public void checkNonNestedClassTest() {
        long partnerId = 77;
        Assertions.assertThrows(IllegalStateException.class, () ->
                CashbackLogger.fillWithBaseInfo(CashbackLogger.ActionType.UPDATE, partnerId, LocalDateTime.of(2020, 12, 12, 12, 12))
                        .put(CashbackLogger.CashbackLoggerFields.CATEGORY_IDS, 9));
    }
}
