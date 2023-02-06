package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.MediabillingApi;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.exception.NoProductForDeviceException;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.exception.ProductNotAvailableException;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.exception.UnapprovedScoreException;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.model.ProductResponseDto;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.model.RefundParamsDto;
import ru.yandex.market.checkout.checkouter.pay.mediabilling.model.RefundResultDto;
import ru.yandex.market.checkout.util.mediabilling.MediabillingMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author ugoryntsev
 */
public class CheckoutMediabillingClientTest extends AbstractWebTestBase {

    @Autowired
    private MediabillingMockConfigurer mediabillingMockConfigurer;

    @Autowired
    private MediabillingApi mediabillingApi;

    @BeforeEach
    public void setUp() {
        mediabillingMockConfigurer.mockWholeMediabilling();
    }

    @Test
    public void given_MockedMediabilling_when_getStationProducts_then_ResponseWithOneProduct() {
        ProductResponseDto stationProduct = mediabillingApi.getStationProduct(
                1L,
                225L,
                "mini_red_24",
                "red",
                "24_month");

        assertEquals(
                "ru.yandex.web.plus.native.1month.autorenewable.3years.aggregated.notrial.light" +
                        ".station_lease_plus_kinopoisk_multi.369",
                stationProduct.getProductId()
        );
        assertEquals(stationProduct.getPrice().getAmount(), 369.0);
        assertEquals(stationProduct.getPrice().getCurrency(), "RUB");
        assertEquals(stationProduct.getRealPrice().getAmount(), 4990.0);
        assertEquals(stationProduct.getRealPrice().getCurrency(), "RUB");
        assertEquals(stationProduct.getLegalInfos().size(), 1);
        assertEquals(stationProduct.getLegalInfos().get(0).getMobile().getText(),
                "Нажимая кнопку, вы принимаете Условия использования");
    }

    @Test
    public void given_MockedMediabilling_when_Refund_then_ResponseWithRefundId() {
        RefundParamsDto paramsDto = new RefundParamsDto(
                42,
                "reason"
        );
        RefundResultDto refundResult = mediabillingApi.refundStationOrder(paramsDto);

        assertEquals("some_id", refundResult.getRefundIds().get(0));
    }

    @Test
    public void given_MockedMediabilling_when_ProductNotAvailable_then_ThrowProductNotAvailableException() {
        assertThrows(ProductNotAvailableException.class, () -> mediabillingApi.getStationProduct(
                42L,
                225L,
                "mini_red_24",
                null,
                null
        ));
    }

    @Test
    public void given_MockedMediabilling_when_BannedUser_then_ThrowProductNotAvailableException() {
        assertThrows(UnapprovedScoreException.class, () -> mediabillingApi.getStationProduct(
                43L,
                225L,
                "mini_red_24",
                null,
                null
        ));
    }

    @Test
    public void given_MockedMediabilling_when_ProductNotFound_then_ThrowNoProductForDeviceException() {
        assertThrows(NoProductForDeviceException.class, () -> mediabillingApi.getStationProduct(
                1L,
                225L,
                "not_exist",
                null,
                null
        ));
    }
}
