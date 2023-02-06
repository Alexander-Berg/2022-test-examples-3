package ru.yandex.market.mbi.api.controller.partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerStatesResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DbUnitDataSet(before = "FF4ShopsPartnerControllerTest.before.csv")
public class FF4ShopsPartnerControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение всех партнёров с сервисами доставки указанного типа")
    void getPartnerFulfillments() {
        Set<DeliveryServiceType> serviceTypes = Set.of(DeliveryServiceType.DROPSHIP, DeliveryServiceType.CROSSDOCK,
                DeliveryServiceType.DROPSHIP_BY_SELLER);
        PartnerStatesResponse partnerStatesRsp0 = mbiApiClient.getPartnerStates(serviceTypes, 2, null);
        List<FF4ShopsPartnerState> partnerStates0 = partnerStatesRsp0.getPartnerStates();
        assertNotNull(partnerStates0);
        assertThat(partnerStates0.size(), Matchers.equalTo(2));
        assertThat(partnerStates0, Matchers.equalTo(expectedPartnerStates(true).subList(0, 2)));

        String nextPageToken0 = partnerStatesRsp0.getPaging().getNextPageToken();
        assertNotNull(nextPageToken0);
        PartnerStatesResponse partnerStatesRsp1 = mbiApiClient.getPartnerStates(serviceTypes, 2, nextPageToken0);
        List<FF4ShopsPartnerState> partnerStates1 = partnerStatesRsp1.getPartnerStates();
        assertNotNull(partnerStates1);
        assertThat(partnerStates1.size(), Matchers.equalTo(1));
        assertThat(partnerStates1, Matchers.equalTo(expectedPartnerStates(true).subList(2, 3)));

        String nextPageToken1 = partnerStatesRsp1.getPaging().getNextPageToken();
        assertNull(nextPageToken1);
    }

    @Test
    @DisplayName("Получение  партнёра с сервисами доставки указанного типа")
    void getPartnerState() {
        for (FF4ShopsPartnerState state : expectedPartnerStates(false)) {
            assertEquals(state, mbiApiClient.getPartnerState(state.getPartnerId()));
        }
    }

    private static List<FF4ShopsPartnerState> expectedPartnerStates(boolean forImport) {
        List<FF4ShopsPartnerState> states = new ArrayList<>(statesInImport());
        if (forImport) {
            return states;
        }

        states.addAll(statesStatesOutOfImport());
        return states;
    }

    private static List<FF4ShopsPartnerState> statesInImport() {
        return List.of(
                FF4ShopsPartnerState.newBuilder()
                        .withBusinessId(104L)
                        .withPartnerId(104L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(778L)
                                        .withFeedId(14L)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                        .build()
                        ))
                        .withPushStocksIsEnabled(false)
                        .build(),
                FF4ShopsPartnerState.newBuilder()
                        .withBusinessId(107L)
                        .withPartnerId(107L)
                        .withFeatureType(FeatureType.MARKETPLACE_SELF_DELIVERY)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(780L)
                                        .withFeedId(null)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP_BY_SELLER)
                                        .build()
                        ))
                        .withPushStocksIsEnabled(false)
                        .build(),
                FF4ShopsPartnerState.newBuilder()
                        .withBusinessId(108L)
                        .withPartnerId(108L)
                        .withFeatureType(FeatureType.CROSSDOCK)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(779L)
                                        .withFeedId(null)
                                        .withDeliveryServiceType(DeliveryServiceType.CROSSDOCK)
                                        .build()
                        ))
                        .withPushStocksIsEnabled(false)
                        .build()
        );
    }

    private static List<FF4ShopsPartnerState> statesStatesOutOfImport() {
        return List.of(
                // партнёр с выставленным IGNORE_STOCKS
                FF4ShopsPartnerState.newBuilder()
                        .withBusinessId(110L)
                        .withPartnerId(110L)
                        .withFeatureType(FeatureType.MARKETPLACE_SELF_DELIVERY)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(780L)
                                        .withFeedId(null)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP_BY_SELLER)
                                        .build()
                        ))
                        .withPushStocksIsEnabled(false)
                        .build(),

                // партнёр с фичей доставки не в состоянии NEW или SUCCESS
                FF4ShopsPartnerState.newBuilder()
                        .withBusinessId(111L)
                        .withPartnerId(111L)
                        .withFeatureType(FeatureType.MARKETPLACE_SELF_DELIVERY)
                        .withFeatureStatus(ParamCheckStatus.FAIL)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(List.of(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(780L)
                                        .withFeedId(null)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP_BY_SELLER)
                                        .build()
                        ))
                        .withPushStocksIsEnabled(false)
                        .build()
        );
    }
}
