package ru.yandex.market.abc;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.CampaignType;

import static ru.yandex.market.abc.AbcRestClient.ABC_DELIVERY_KEY;
import static ru.yandex.market.abc.AbcRestClient.ABC_MARKET_KEY;
import static ru.yandex.market.abc.AbcRestClient.ABC_SUPPLIER_KEY;

class AbcRestClientTest {

    @Test
    void testGetRequest() {
        final AbcRestClient client = new AbcRestClient(StringUtils.EMPTY, 0, Collections.emptyMap(), null, null);
        final int clientId = 100;
        final int abcServiceId = 200;
        final int resourceType = 300;
        final AbcRestClient.AbcResourceRequest request = client.getRequest(clientId, abcServiceId, resourceType);

        Assertions.assertEquals(clientId, request.objId);
        Assertions.assertEquals(String.valueOf(clientId), request.attributes.clientId);
        Assertions.assertEquals(resourceType, request.resourceType);
        Assertions.assertEquals(abcServiceId, request.service);
    }

    @Test
    void testGetAbcServiceId() {
        final Map<String, Integer> abcBalanceServiceIds = Map.of(
                ABC_MARKET_KEY, 3675,
                ABC_SUPPLIER_KEY, 3674,
                ABC_DELIVERY_KEY, 3543
        );
        final AbcRestClient client = new AbcRestClient(StringUtils.EMPTY, 0, abcBalanceServiceIds, null, null);

        int abcServiceId = client.getAbcServiceId(CampaignType.SUPPLIER);
        Assertions.assertEquals(abcBalanceServiceIds.get(ABC_SUPPLIER_KEY), abcServiceId);

        abcServiceId = client.getAbcServiceId(CampaignType.DELIVERY);
        Assertions.assertEquals(abcBalanceServiceIds.get(ABC_DELIVERY_KEY), abcServiceId);

        abcServiceId = client.getAbcServiceId(CampaignType.SHOP);
        Assertions.assertEquals(abcBalanceServiceIds.get(ABC_MARKET_KEY), abcServiceId);
    }
}
