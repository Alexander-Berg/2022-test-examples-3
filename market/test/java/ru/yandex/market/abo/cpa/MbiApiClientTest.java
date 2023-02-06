package ru.yandex.market.abo.cpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.api.cpa.yam.dto.RequestsInfoDTO;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.api.client.config.MbiApiClientConfig;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.util.ShopsFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Рекомендуется запускать при обновлении mbi-api-client.
 *
 * @author kukabara
 */
@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MbiApiClientConfig.class)
public class MbiApiClientTest {
    private static final long SHOP_ID = 774L;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    public void setup() {
        ((RestMbiApiClient) mbiApiClient).setServiceUrl("http://mbi-back.tst.vs.market.yandex.net:34820");
    }

    @Test
    public void testConfig() {
        Shop shop = mbiApiClient.getShop(SHOP_ID);
        assertNotNull(shop);
    }

    @Test
    public void test() {
        // premod
        mbiApiClient.getPremoderationReadyShops();

        // prepay
        mbiApiClient.getPrepayRequestIds(PartnerApplicationStatus.INIT, null);
        RequestsInfoDTO.Data requestId = mbiApiClient.getPrepayRequestIds(null, SHOP_ID).getData().stream().findFirst().orElse(null);
        if (requestId != null) {
            mbiApiClient.getPrepayRequest(requestId.getRequestId(), null);
            mbiApiClient.getPrepayRequestShops(requestId.getRequestId());
        }

        List<Long> shopIds = Collections.singletonList(SHOP_ID);

        mbiApiClient.getCpaShops(shopIds, 1, 1, true);

        mbiApiClient.getShop(SHOP_ID);
        mbiApiClient.getShopsList(ShopsFilter.newBuilder()
                .cpaEnabled(true)
                .checkStatus(ParamType.CPA_REGION_CHECK_STATUS, ParamCheckStatus.SUCCESS)
                .build());
        mbiApiClient.getShopsForFeatureTesting(ParamType.SUBSIDIES_STATUS.getId());
        mbiApiClient.getPrepayRequestShops(1L);
        mbiApiClient.getAboCutoffs(SHOP_ID);
        PagedOutletsDTO outlets = mbiApiClient.getOutletsV2(SHOP_ID, null, "Ленина",
                null /*type*/, null /*isShipper*/, null /* status*/, null/* shopOutletId*/,
                1, 10
        );
        mbiApiClient.getOutlet(
                outlets.getOutlets().stream().findFirst()
                        .map(OutletInfoDTO::getId)
                        .orElseThrow(() -> new RuntimeException("There is no outlets")),
                false
        );

        // params
        List<Integer> featureIds = Arrays.asList(
                ParamType.PROMO_CPC_STATUS.getId(),
                ParamType.SUBSIDIES_STATUS.getId()
        );
        mbiApiClient.getShopCheckedParams(shopIds);
        mbiApiClient.getCheckedParamShops(ParamType.CPA_REGION_CHECK_STATUS.getId(), ParamCheckStatus.FAIL);
        mbiApiClient.getFeatureInfos(shopIds, featureIds);
    }
}


