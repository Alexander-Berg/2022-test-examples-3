package ru.yandex.market.delivery.rupostintegrationapp.controller;

import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import steps.ordersteps.OrderSteps;
import steps.ordersteps.ordersubsteps.ItemSteps;

import ru.yandex.market.delivery.entities.common.CustomsTranslation;
import ru.yandex.market.delivery.entities.common.Order;
import ru.yandex.market.delivery.entities.common.RecipientData;
import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.entities.common.TransitData;
import ru.yandex.market.delivery.entities.common.constant.DocumentFormat;
import ru.yandex.market.delivery.entities.request.ds.DsCreateOrderRequest;
import ru.yandex.market.delivery.entities.request.ds.DsGetLabelsRequest;
import ru.yandex.market.delivery.entities.response.ds.DsResponse;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsCreateOrderResponseContent;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsGetLabelsResponseContent;
import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.exception.ServiceProcessingException;

class InternationalDsApiControllerTest extends BaseContextualTest {

    private static final String ENGLISH_LANGUAGE_CODE = "en";

    @Autowired
    private InternationalDsApiController internationalDsApiController;

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    @ExpectedDatabase(value = "/database/expected/created_rm_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCreateRmOrderSuccessful() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        DsResponse response = internationalDsApiController.createOrder(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsCreateOrderResponseContent.class);
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId())
            .as("Asserting that order ID is not null")
            .isNotNull();
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId().getYandexId())
            .as("Asserting that order's Yandex ID is valid")
            .isEqualTo("666");
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId().getDeliveryId())
            .as("Asserting that order's track code is valid")
            .matches("RJ\\d{9}RU");
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    @ExpectedDatabase(value = "/database/expected/created_rm_order_without_receiver_street.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCreateRmOrderWithoutReceiverStreetSuccessful() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");
        request.getRequestContent().getOrder().getLocationTo().setStreet(null);

        DsResponse response = internationalDsApiController.createOrder(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsCreateOrderResponseContent.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateRmOrderTooManyItemsError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        request.getRequestContent().getOrder().setItems(Arrays.asList(
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem()
        ));

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateRmOrderNoEnglishTranslationError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        request.getRequestContent().getOrder().getItems().forEach(item -> item.setTransitData(null));

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateRmOrderInvalidReceiverStreetError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        request.getRequestContent().getOrder().getLocationTo().setStreet("улица Льва Толстого, 16");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateRmOrderInvalidReceiverZipCodeError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        request.getRequestContent().getOrder().getLocationTo().setZipCode("123AB");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateRmOrderInvalidReceiverEmailError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost RM Registered Small Packet");

        request.getRequestContent().getOrder().getRecipient().setEmail("lol@parcel");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    @ExpectedDatabase(value = "/database/expected/created_ems_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCreateEmsOrderSuccessful() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        DsResponse response = internationalDsApiController.createOrder(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsCreateOrderResponseContent.class);
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId())
            .as("Asserting that order ID is not null")
            .isNotNull();
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId().getYandexId())
            .as("Asserting that order's Yandex ID is valid")
            .isEqualTo("666");
        softly.assertThat(((DsCreateOrderResponseContent) response.getResponseContent()).getOrderId().getDeliveryId())
            .as("Asserting that order's track code is valid")
            .matches("EJ\\d{9}RU");
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    @ExpectedDatabase(value = "/database/expected/created_ems_order_without_receiver_street.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCreateEmsOrderWithoutReceiverStreetSuccessful() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");
        request.getRequestContent().getOrder().getLocationTo().setStreet(null);

        DsResponse response = internationalDsApiController.createOrder(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsCreateOrderResponseContent.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateEmsOrderTooManyItemsError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        request.getRequestContent().getOrder().setItems(Arrays.asList(
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem(),
            ItemSteps.getItem()
        ));

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateEmsOrderNoEnglishTranslationError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        request.getRequestContent().getOrder().getItems().forEach(item -> item.setTransitData(null));

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateEmsOrderInvalidReceiverStreetError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        request.getRequestContent().getOrder().getLocationTo().setStreet("улица Льва Толстого, 16");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateEmsOrderInvalidReceiverZipCodeError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        request.getRequestContent().getOrder().getLocationTo().setZipCode("123AB");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/empty_setup.xml")
    void testCreateEmsOrderInvalidReceiverEmailError() {
        DsCreateOrderRequest request = getDsCreateOrderRequest("RuPost EMS");

        request.getRequestContent().getOrder().getRecipient().setEmail("lol@parcel");

        softly.assertThatThrownBy(() -> internationalDsApiController.createOrder(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/created_rm_order.xml")
    void testGetLabelsBothIdsArePresentSuccessful() {
        DsGetLabelsRequest request = getDsGetLabelsRequest("666", "RJ019744323RU");

        DsResponse response = internationalDsApiController.getLabels(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsGetLabelsResponseContent.class);
        softly.assertThat(((DsGetLabelsResponseContent) response.getResponseContent()).getFormat())
            .as("Asserting that the label's format is PDF")
            .isEqualTo(DocumentFormat.PDF);
        softly.assertThat(((DsGetLabelsResponseContent) response.getResponseContent()).getPdf())
            .as("Asserting that the label's PDF base64 code is not blank")
            .isNotBlank();
    }

    @Test
    @DatabaseSetup("/database/setup/created_rm_order.xml")
    void testGetLabelsOnlyYandexIdIsPresentSuccessful() {
        DsGetLabelsRequest request = getDsGetLabelsRequest("666", null);

        DsResponse response = internationalDsApiController.getLabels(request);

        softly.assertThat(response.getResponseContent())
            .as("Asserting that response content has a valid type")
            .isInstanceOf(DsGetLabelsResponseContent.class);
        softly.assertThat(((DsGetLabelsResponseContent) response.getResponseContent()).getFormat())
            .as("Asserting that the label's format is PDF")
            .isEqualTo(DocumentFormat.PDF);
        softly.assertThat(((DsGetLabelsResponseContent) response.getResponseContent()).getPdf())
            .as("Asserting that the label's PDF base64 code is not blank")
            .isNotBlank();
    }

    @Test
    @DatabaseSetup("/database/setup/created_rm_order.xml")
    void testGetLabelsWrongYandexIdError() {
        DsGetLabelsRequest request = getDsGetLabelsRequest("123", null);

        softly.assertThatThrownBy(() -> internationalDsApiController.getLabels(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    @Test
    @DatabaseSetup("/database/setup/created_rm_order.xml")
    void testGetLabelsBothIdsAreNullError() {
        DsGetLabelsRequest request = getDsGetLabelsRequest(null, null);

        softly.assertThatThrownBy(() -> internationalDsApiController.getLabels(request))
            .isInstanceOf(ServiceProcessingException.class);
    }

    private DsCreateOrderRequest getDsCreateOrderRequest(String tariff) {
        DsCreateOrderRequest request = new DsCreateOrderRequest();
        Order order = OrderSteps.getOrder();

        order.setTariff(tariff);
        order.getRecipient().setRecipientData(
            new RecipientData().setRecipientOrderId(new ResourceId().setYandexId("777")));
        order.getItems().forEach(item -> item.setTransitData(
            new TransitData(Collections.singletonList(new CustomsTranslation(ENGLISH_LANGUAGE_CODE,
                "Item name", "Item category name")))));

        request.setToken("");
        request.setRequestContent(request.new RequestContent());
        request.getRequestContent().setOrder(order);
        return request;
    }

    private DsGetLabelsRequest getDsGetLabelsRequest(String yandexId, String trackCode) {
        DsGetLabelsRequest request = new DsGetLabelsRequest();

        request.setToken("");
        request.setRequestContent(request.new RequestContent());
        request.getRequestContent().setOrdersId(Collections.singletonList(new ResourceId()
            .setYandexId(yandexId)
            .setDeliveryId(trackCode)));

        return request;
    }
}
