package ru.yandex.market.core.supplier.state;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;

import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.common.transaction.TransactionListener;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.stocks.RetryableFF4ShopsClient;
import ru.yandex.market.core.supplier.state.service.FF4ShopsPartnerStateService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@DbUnitDataSet(before = "FF4ShopsPartnerStateListenerTest.before.csv")
public class Ff4ShopsPartnerStateListenerTest extends FunctionalTest {

    private static final Map<DeliveryServiceType, Long> PARTNERS = Map.of(
            DeliveryServiceType.CROSSDOCK, 4L,
            DeliveryServiceType.DROPSHIP, 1L,
            DeliveryServiceType.DROPSHIP_BY_SELLER, 3L
    );

    @Autowired
    private FF4ShopsPartnerStateService ff4ShopsPartnerStateService;

    @Autowired
    private RetryableFF4ShopsClient retryableFF4ShopsClient;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    private FF4ShopsPartnerStateListener ff4ShopsPartnerStateListener;

    @BeforeEach
    void init() {
        ff4ShopsPartnerStateListener = new FF4ShopsPartnerStateListener(ff4ShopsPartnerStateService, retryableFF4ShopsClient, localTransactionListener());
    }

    @ParameterizedTest
    @DisplayName("Состояние партнёра отправляется в ff4shops, если выставлен IGNORE_STOCKS (CPA через ПИ)")
    @EnumSource(value = DeliveryServiceType.class, names = {"CROSSDOCK", "DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "Ff4ShopsPartnerStateListenerTest.CpaPI.IgnoreStocks.before.csv")
    void testCpaPI_ShouldNotSyncWithFf4ShopsIfIgnoreStocks(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);
        ff4ShopsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        ArgumentCaptor<FF4ShopsPartnerState> stateCaptor = ArgumentCaptor.forClass(FF4ShopsPartnerState.class);
        verify(ff4ShopsClient, times(1)).updatePartnerState(stateCaptor.capture());
        verifyNoMoreInteractions(ff4ShopsClient);

        FF4ShopsPartnerState value = stateCaptor.getValue();
        assertThat(value.getPartnerId(), is(partnerId));
        assertThat(value.getFeatureType(), is(partnerTypeAwareService.getDeliveryFeatureType(partnerId).get()));
        assertThat(value.getFeatureStatus(), is(ParamCheckStatus.SUCCESS));
        assertThat(value.getBusinessId(), is(partnerId)); //в тестовых данных идентификаторы равны
    }

    @ParameterizedTest
    @DisplayName("Состояние партнёра отправляется в ff4shops, если не выставлен IGNORE_STOCKS (CPA через ПИ)")
    @EnumSource(value = DeliveryServiceType.class, names = {"CROSSDOCK", "DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "Ff4ShopsPartnerStateListenerTest.CpaPI.NotIgnoreStocks.before.csv")
    void testCpaPI_ShouldSyncWithFf4ShopsIfNoIgnoreStocks(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);
        ff4ShopsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        ArgumentCaptor<FF4ShopsPartnerState> stateCaptor = ArgumentCaptor.forClass(FF4ShopsPartnerState.class);
        verify(ff4ShopsClient, times(1)).updatePartnerState(stateCaptor.capture());
        verifyNoMoreInteractions(ff4ShopsClient);

        FF4ShopsPartnerState value = stateCaptor.getValue();
        assertThat(value.getPartnerId(), is(partnerId));
        assertThat(value.getFeatureType(), is(partnerTypeAwareService.getDeliveryFeatureType(partnerId).get()));
        assertThat(value.getFeatureStatus(), is(ParamCheckStatus.SUCCESS));
        assertThat(value.getBusinessId(), is(partnerId)); //в тестовых данных идентификаторы равны
    }

    @ParameterizedTest
    @DisplayName("Состояние партнёра отправляется в ff4shops, если выставлен IGNORE_STOCKS (CPA через API)")
    @EnumSource(value = DeliveryServiceType.class, names = {"CROSSDOCK", "DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "Ff4ShopsPartnerStateListenerTest.CpaAPI.IgnoreStocks.before.csv")
    void testCpaAPI_ShouldNotSyncWithFf4ShopsIfIgnoreStocks(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);
        ff4ShopsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        ArgumentCaptor<FF4ShopsPartnerState> stateCaptor = ArgumentCaptor.forClass(FF4ShopsPartnerState.class);
        verify(ff4ShopsClient, times(1)).updatePartnerState(stateCaptor.capture());
        verifyNoMoreInteractions(ff4ShopsClient);

        FF4ShopsPartnerState value = stateCaptor.getValue();
        assertThat(value.getPartnerId(), is(partnerId));
        assertThat(value.getFeatureType(), is(partnerTypeAwareService.getDeliveryFeatureType(partnerId).get()));
        assertThat(value.getFeatureStatus(), is(ParamCheckStatus.SUCCESS));
        assertThat(value.getBusinessId(), is(partnerId)); //в тестовых данных идентификаторы равны
    }

    @ParameterizedTest
    @DisplayName("Состояние партнёра отправляется в ff4shops, если не выставлен IGNORE_STOCKS (CPA через API)")
    @EnumSource(value = DeliveryServiceType.class, names = {"CROSSDOCK", "DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "Ff4ShopsPartnerStateListenerTest.CpaAPI.NotIgnoreStocks.before.csv")
    void testCpaAPI_ShouldSyncWithFf4ShopsIfNoIgnoreStocks(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);
        ff4ShopsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        ArgumentCaptor<FF4ShopsPartnerState> stateCaptor = ArgumentCaptor.forClass(FF4ShopsPartnerState.class);
        verify(ff4ShopsClient, times(1)).updatePartnerState(stateCaptor.capture());
        verifyNoMoreInteractions(ff4ShopsClient);

        FF4ShopsPartnerState value = stateCaptor.getValue();
        assertThat(value.getPartnerId(), is(partnerId));
        assertThat(value.getFeatureType(), is(partnerTypeAwareService.getDeliveryFeatureType(partnerId).get()));
        assertThat(value.getFeatureStatus(), is(ParamCheckStatus.SUCCESS));
        assertThat(value.getBusinessId(), is(partnerId)); //в тестовых данных идентификаторы равны
    }

    private static long getPartner(DeliveryServiceType deliveryServiceType) {
        Long partnerId = PARTNERS.get(deliveryServiceType);
        if (partnerId == null) {
            fail("No partners for service type: " + deliveryServiceType);
        }

        return partnerId;
    }

    private LocalTransactionListener localTransactionListener() {
        LocalTransactionListener localTransactionListener = mock(LocalTransactionListener.class);
        doAnswer(invocation -> {
            TransactionListener listener = invocation.getArgument(0);
            listener.onBeforeCommit(mock(TransactionStatus.class));
            return null;
        }).when(localTransactionListener).addListener(any());
        return localTransactionListener;
    }
}
