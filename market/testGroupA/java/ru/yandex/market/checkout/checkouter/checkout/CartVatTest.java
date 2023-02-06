package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.common.report.model.MarketReportPlace.ACTUAL_DELIVERY;
import static ru.yandex.market.common.report.model.MarketReportPlace.OFFER_INFO;

public class CartVatTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @AfterEach
    public void tearDown() {
        super.clean();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, true);
    }

    @Test
    public void shouldReturnPriceWithoutVatWhenShowVat() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().stream().findFirst().get().setCount(2);

        var reportParameters = parameters.getReportParameters();
        reportParameters.getOrder().getItems().stream().findFirst().get()
                .getPrices().setPriceWithoutVat(BigDecimal.valueOf(100));
        reportParameters.getActualDelivery().getResults().get(0).getDelivery().get(0)
                .setPriceWithoutVat(BigDecimal.valueOf(34));

        parameters.setShowVat(true);

        var multiCart = orderCreateHelper.cart(parameters);

        //Проверка наличия параметра в запросах в репорт
        List<LoggedRequest> reportRequests = reportMock.getServeEvents().getServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.queryParameter("place").containsValue(OFFER_INFO.getId())
                        || r.queryParameter("place").containsValue(ACTUAL_DELIVERY.getId()))
                .collect(Collectors.toList());

        List<LoggedRequest> priceNoVatRequests = reportRequests.stream()
                .filter(r -> r.queryParameter("prices-no-vat").containsValue("1"))
                .collect(Collectors.toList());

        assertEquals(reportRequests.size(), priceNoVatRequests.size());

        //Проверка маппинга стоимости товаров и доставки без НДС
        var orderItem = getFirstOrderItem(multiCart);
        assertEquals(BigDecimal.valueOf(100), orderItem.getPrices().getPriceWithoutVat());

        var deliveryOption = getFirstDeliveryWithType(multiCart, DeliveryType.DELIVERY);
        assertEquals(BigDecimal.valueOf(34), deliveryOption.getPrices().getPriceWithoutVat());

        //Проверка подсчета тоталов
        // vatTotal = 250 * 2 - (100 * 2) = 300
        assertEquals(BigDecimal.valueOf(300), multiCart.getCarts().get(0).getVatTotal());
        assertEquals(BigDecimal.valueOf(300), multiCart.getTotals().getVatTotal());
    }

    @Test
    void shouldReturnPricesWithVatIfReportReturnedNullWithShowVat() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().stream().findFirst().get().setCount(2);

        parameters.setShowVat(true);

        var multiCart = orderCreateHelper.cart(parameters);

        //Проверка наличия параметра в запросах в репорт
        List<LoggedRequest> reportRequests = reportMock.getServeEvents().getServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.queryParameter("place").containsValue(OFFER_INFO.getId())
                        || r.queryParameter("place").containsValue(ACTUAL_DELIVERY.getId()))
                .collect(Collectors.toList());

        List<LoggedRequest> priceNoVatRequests = reportRequests.stream()
                .filter(r -> r.queryParameter("prices-no-vat").containsValue("1"))
                .collect(Collectors.toList());

        assertEquals(reportRequests.size(), priceNoVatRequests.size());

        //Проверка подсчета тоталов
        assertEquals(BigDecimal.ZERO, multiCart.getCarts().get(0).getVatTotal());
        assertEquals(BigDecimal.ZERO, multiCart.getTotals().getVatTotal());
    }

    @Test
    void shouldReturnNullForTotalVatWhenNoShowVat() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().stream().findFirst().get().setCount(2);

        var multiCart = orderCreateHelper.cart(parameters);

        //Проверка отсутствия параметра в запросах в репорт
        List<LoggedRequest> reportRequests = reportMock.getServeEvents().getServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.queryParameter("price-no-vat").containsValue("1"))
                .collect(Collectors.toList());

        assertEquals(0, reportRequests.size());

        var orderItem = getFirstOrderItem(multiCart);
        var deliveryOption = getFirstDeliveryWithType(multiCart, DeliveryType.DELIVERY);

        assertNull(orderItem.getPrices().getPriceWithoutVat());
        assertNull(deliveryOption.getPrices().getPriceWithoutVat());

        //Проверка подсчета тоталов
        assertNull(multiCart.getCarts().get(0).getVatTotal());
        assertNull(multiCart.getTotals().getVatTotal());
    }

    @Test
    public void shouldReturnPriceWithoutVatWhenBusinessUserAndFeatureOn() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, true);

        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.getOrder().getItems().stream().findFirst().get().setCount(2);

        var reportParameters = parameters.getReportParameters();
        reportParameters.getOrder().getItems().stream().findFirst().get()
                .getPrices().setPriceWithoutVat(BigDecimal.valueOf(100));
        reportParameters.getActualDelivery().getResults().get(0).getDelivery().get(0)
                .setPriceWithoutVat(BigDecimal.valueOf(34));

        parameters.setShowVat(false);

        var multiCart = orderCreateHelper.cart(parameters);

        //Проверка наличия параметра в запросах в репорт
        List<LoggedRequest> reportRequests = reportMock.getServeEvents().getServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.queryParameter("place").containsValue(OFFER_INFO.getId())
                        || r.queryParameter("place").containsValue(ACTUAL_DELIVERY.getId()))
                .collect(Collectors.toList());

        List<LoggedRequest> priceNoVatRequests = reportRequests.stream()
                .filter(r -> r.queryParameter("prices-no-vat").containsValue("1"))
                .collect(Collectors.toList());

        assertEquals(reportRequests.size(), priceNoVatRequests.size());

        //Проверка маппинга стоимости товаров и доставки без НДС
        var orderItem = getFirstOrderItem(multiCart);
        assertEquals(BigDecimal.valueOf(100), orderItem.getPrices().getPriceWithoutVat());

        var deliveryOption = getFirstDeliveryWithType(multiCart, DeliveryType.DELIVERY);
        assertEquals(BigDecimal.valueOf(34), deliveryOption.getPrices().getPriceWithoutVat());

        //Проверка подсчета тоталов
        // vatTotal = 250 * 2 - (100 * 2) = 300
        assertEquals(BigDecimal.valueOf(300), multiCart.getCarts().get(0).getVatTotal());
        assertEquals(BigDecimal.valueOf(300), multiCart.getTotals().getVatTotal());
    }

    @Test
    void shouldReturnNullForTotalVatWhenUsualUserAndFeatureOn() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SHOW_VAT_ONLY_FOR_BUSINESS, true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().stream().findFirst().get().setCount(2);

        parameters.setShowVat(false);

        var multiCart = orderCreateHelper.cart(parameters);

        //Проверка отсутствия параметра в запросах в репорт
        List<LoggedRequest> reportRequests = reportMock.getServeEvents().getServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.queryParameter("price-no-vat").containsValue("1"))
                .collect(Collectors.toList());

        assertEquals(0, reportRequests.size());

        var orderItem = getFirstOrderItem(multiCart);
        var deliveryOption = getFirstDeliveryWithType(multiCart, DeliveryType.DELIVERY);

        assertNull(orderItem.getPrices().getPriceWithoutVat());
        assertNull(deliveryOption.getPrices().getPriceWithoutVat());

        //Проверка подсчета тоталов
        assertNull(multiCart.getCarts().get(0).getVatTotal());
        assertNull(multiCart.getTotals().getVatTotal());
    }

    private OrderItem getFirstOrderItem(MultiCart cart) {
        return cart.getCarts().get(0).getItems().stream().findFirst().get();
    }

    private Delivery getFirstDeliveryWithType(MultiCart cart, DeliveryType type) {
        return cart.getCarts().get(0).getDeliveryOptions()
                .stream()
                .filter(o -> o.getType() == type)
                .findFirst()
                .get();
    }
}
