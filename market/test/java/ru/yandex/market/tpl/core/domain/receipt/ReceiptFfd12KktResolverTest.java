package ru.yandex.market.tpl.core.domain.receipt;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.FFD_1_2_ENABLED;

@ExtendWith(SpringExtension.class)
class ReceiptFfd12KktResolverTest {

    @Mock
    private ConfigurationProviderAdapter configuration;

    @Mock
    private ReceiptKktResolver baseResolver;

    @Mock
    private SortingCenterPropertyService sortingCenterPropertyService;

    @InjectMocks
    private ReceiptFfd12KktResolver resolver;

    private ReceiptData receiptData;

    @BeforeEach
    void setup() {
        var receiptServiceClient = Mockito.mock(ReceiptServiceClient.class);
        doReturn(ReceiptService.TPL_CLIENT_ID).when(receiptServiceClient).getId();
        receiptData = Mockito.mock(ReceiptData.class);
        doReturn(createPayload(1L)).when(receiptData).getPayload();
        doReturn(receiptServiceClient).when(receiptData).getServiceClient();
    }

    @Test
    void ffd12TestReceiptWhenTestOrder() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        var payload = createPayload(1L);
        // when
        var result = resolver.isFfd12TestReceipt(ReceiptService.TPL_CLIENT_ID, payload);
        // then
        assertTrue(result);
    }

    @Test
    void ffd12TestReceiptWhenNotTestOrder() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        var payload = createPayload(2L);
        // when
        var result = resolver.isFfd12TestReceipt(ReceiptService.TPL_CLIENT_ID, payload);
        // then
        assertFalse(result);
    }

    @Test
    void ffd12TestReceiptWhenNotTpl() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        var payload = createPayload(1L);
        // when
        var result = resolver.isFfd12TestReceipt("Some Other", payload);
        // then
        assertFalse(result);
    }

    @Test
    void determineKttSnWhenConfigIsSet() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        doReturn(Optional.of("123")).when(configuration).getValue(ConfigurationProperties.KKT_SN_FOR_FFD_1_2_TEST);
        // when
        var result = resolver.determineKktSn(receiptData);
        // then
        assertThat(result).isEqualTo("123");
    }

    @Test
    void determineKttSnWhenConfigIsNotSet() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        doReturn(Optional.empty()).when(configuration).getValue(ConfigurationProperties.KKT_SN_FOR_FFD_1_2_TEST);
        doReturn("321").when(baseResolver).determineKktSn(any());
        // when
        var result = resolver.determineKktSn(receiptData);
        // then
        assertThat(result).isEqualTo("321");
    }

    @Test
    void determineKttSnWhenNotTestOrder() {
        // given
        doReturn(Optional.empty()).when(configuration).getValue(ConfigurationProperties.KKT_SN_FOR_FFD_1_2_TEST);
        doReturn("321").when(baseResolver).determineKktSn(any());
        // when
        var result = resolver.determineKktSn(receiptData);
        // then
        assertThat(result).isEqualTo("321");
    }

    @Test
    void isFfd12ReceiptWhenTestReceipt() {
        // given
        doReturn(Set.of(1L)).when(configuration).getValueAsLongs(ConfigurationProperties.ORDERS_FOR_FFD_1_2_TEST_LIST);
        doReturn(false).when(sortingCenterPropertyService)
                .getPropertyValueForDs(eq(FFD_1_2_ENABLED), eq(1L));
        var payload = createPayload(1L);
        // when
        var result = resolver.isFfd12Receipt(ReceiptService.TPL_CLIENT_ID, payload);
        // then
        assertTrue(result);
    }

    @Test
    void isFfd12ReceiptWhenFfd12Ds() {
        // given
        var payload = createPayload(1L);
        doReturn(true).when(sortingCenterPropertyService)
                .getPropertyValueForDs(eq(FFD_1_2_ENABLED), eq(1L));
        // when
        var result = resolver.isFfd12Receipt(ReceiptService.TPL_CLIENT_ID, payload);
        // then
        assertTrue(result);
    }

    private Map<String, Object> createPayload(Long orderId) {
        return Map.of(
                "userId", 1L,
                "orderId", orderId,
                "shiftId", 1L,
                "userShiftId", 1L,
                "orderDeliveryTaskId", 1L,
                "deliveryServiceId", 1L
        );
    }
}
