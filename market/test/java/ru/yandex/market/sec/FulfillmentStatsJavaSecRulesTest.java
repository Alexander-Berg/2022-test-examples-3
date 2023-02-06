package ru.yandex.market.sec;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.campaign.model.PartnerId.datasourceId;
import static ru.yandex.market.core.campaign.model.PartnerId.supplierId;

/**
 * Проверка доступов для action fulfillment-stats
 *
 * @author otedikova
 */
class FulfillmentStatsJavaSecRulesTest extends JavaSecFunctionalTest {
    private static final int PARTNER_READER_UID = 557;

    private static Stream<Arguments> args() {
        return Stream.of(
                //Для белого магазина  для SHOP_ADMIN(558) и PARTNER_READER(557) экшн не доступен
                Arguments.of("/fulfillment-stats", 3, datasourceId(2), PARTNER_READER_UID, false),
                Arguments.of("/fulfillment-stats", 3, datasourceId(2), 558, false),

                //Для виртаульного магазина  для SHOP_ADMIN(558), PARTNER_READER(557)  экшн доступен
                Arguments.of("/fulfillment-stats", 4, datasourceId(3), PARTNER_READER_UID, true),
                Arguments.of("/fulfillment-stats", 4, datasourceId(3), 558, true),
                Arguments.of("/fulfillment-stats", 4, datasourceId(3), 559, true),

                //Для белого магазина c фичей фулфилмент экшн доступен PARTNER_READER(557) и SHOP_ADMIN(558)
                Arguments.of("/fulfillment-stats", 5, datasourceId(4), PARTNER_READER_UID, true),
                Arguments.of("/fulfillment-stats", 5, datasourceId(4), 558, true),
                Arguments.of("/fulfillment-stats", 5, datasourceId(4), 559, false),

                //Поставщик со стоками, фидом и предоплатой - экшн доступен PARTNER_READER(557) и SHOP_ADMIN(558)
                Arguments.of("/fulfillment-stats", 6, supplierId(5), PARTNER_READER_UID, true),
                Arguments.of("/fulfillment-stats", 6, supplierId(5), 558, true),
                Arguments.of("/fulfillment-stats", 6, supplierId(5), 559, false),

                //Поставщик без стоков с фичей PREPAY, фидом и предоплатой - экшн доступен PARTNER_READER(557) и SHOP_ADMIN(558)
                Arguments.of("/fulfillment-stats", 7, supplierId(6), PARTNER_READER_UID, true),
                Arguments.of("/fulfillment-stats", 7, supplierId(6), 558, true),
                Arguments.of("/fulfillment-stats", 7, supplierId(6), 559, false),

                //Поставщик без стоков с фичей PREPAY, фидом, но без предоплаты - экшн не доступен PARTNER_READER(557) и SHOP_ADMIN(558)
                Arguments.of("/fulfillment-stats", 8, supplierId(7), PARTNER_READER_UID, false),
                Arguments.of("/fulfillment-stats", 8, supplierId(7), 558, false)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void rules(String operation, long campaignId, PartnerId partnerId, long uid, boolean expected) {
        MockPartnerRequest partnerRequest = CampaignType.SUPPLIER != partnerId.type() ?
                new MockPartnerRequest(uid, uid, partnerId.getDatasourceId(), campaignId)
                : new MockPartnerRequest(uid, campaignId, partnerId);
        boolean result = secManager.canDo(operation, partnerRequest);
        assertEquals(expected, result);
    }
}
