package ru.yandex.market.logistic.gateway.controller;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.Validator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.client.ClientUtilsFactory;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Intake;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Outbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Register;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnRegister;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransportationRegister;

import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ParametersAreNonnullByDefault
public class TaskFulfillmentControllerTest extends AbstractIntegrationTest {

    @MockBean
    private FulfillmentClient fulfillmentClient;

    @Captor
    private ArgumentCaptor<Partner> partnerArgumentCaptor;

    @After
    public void tearDown() {
        assertCaptor(partnerArgumentCaptor);
        Assert.assertEquals(partnerArgumentCaptor.getValue(), new Partner(145L));
    }

    @Test
    public void inboundCreateInvalid() throws Exception {
        performCall("inbound/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createInbound(isNull(Inbound.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void inboundCreateValid() throws Exception {
        performCall("inbound/create", "create_inbound/create_inbound_task_message.json");
        ArgumentCaptor<Inbound> inboundArgumentCaptor = ArgumentCaptor.forClass(Inbound.class);
        verify(fulfillmentClient).createInbound(inboundArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(inboundArgumentCaptor);
    }

    @Test
    public void returnInboundCreateInvalid() throws Exception {
        performCall("return-inbound/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createReturnInbound(isNull(ReturnInbound.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void returnInboundCreateValid() throws Exception {
        performCall("return-inbound/create", "create_return_inbound/create_return_inbound_task_message.json");
        ArgumentCaptor<ReturnInbound> returnInboundArgumentCaptor = ArgumentCaptor.forClass(ReturnInbound.class);
        verify(fulfillmentClient).createReturnInbound(returnInboundArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(returnInboundArgumentCaptor);
    }

    @Test
    public void inboundCancelInvalid() throws Exception {
        performCall("inbound/cancel", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).cancelInbound(isNull(ResourceId.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void inboundCancelValid() throws Exception {
        performCall("inbound/cancel", "cancel_inbound/cancel_inbound_task_message.json");
        ArgumentCaptor<ResourceId> resourceIdArgumentCaptor = ArgumentCaptor.forClass(ResourceId.class);
        verify(fulfillmentClient).cancelInbound(resourceIdArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(resourceIdArgumentCaptor);
    }

    @Test
    public void orderCancelInvalid() throws Exception {
        performCall("order/cancel", "cancel_order/cancel_order_task_message_fail.json");

        verify(fulfillmentClient).cancelOrder(
            refEq(ResourceId.builder().build()),
            partnerArgumentCaptor.capture()
        );
    }

    @Test
    public void orderCancelValid() throws Exception {
        performCall("order/cancel", "cancel_order/cancel_order_task_message.json");
        ArgumentCaptor<ResourceId> resourceIdArgumentCaptor = ArgumentCaptor.forClass(ResourceId.class);
        verify(fulfillmentClient).cancelOrder(resourceIdArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(resourceIdArgumentCaptor);
    }

    @Test
    public void orderUpdateInvalid() throws Exception {
        performCall("order/update", "fulfillment_create_order/fulfillment_update_order_task_message_fail.json");
        verify(fulfillmentClient).updateOrder(refEq(getEmptyOrder()), partnerArgumentCaptor.capture());
    }

    @Test
    public void orderUpdateValid() throws Exception {
        performCall("order/update", "fulfillment_create_order/fulfillment_update_order_task_message.json");
        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(fulfillmentClient).updateOrder(orderArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(orderArgumentCaptor);
    }

    @Test
    public void orderCreateInvalid() throws Exception {
        performCall("order/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createOrder(isNull(Order.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void orderCreateValid() throws Exception {
        performCall("order/create", "fulfillment_create_order/fulfillment_create_order_task_message.json");
        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(fulfillmentClient).createOrder(orderArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(orderArgumentCaptor);
    }

    @Test
    public void outboundCreateInvalid() throws Exception {
        performCall("outbound/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createOutbound(isNull(Outbound.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void outboundCreateValid() throws Exception {
        performCall("outbound/create", "create_outbound/create_outbound_task_message.json");
        ArgumentCaptor<Outbound> outboundArgumentCaptor = ArgumentCaptor.forClass(Outbound.class);
        verify(fulfillmentClient).createOutbound(outboundArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(outboundArgumentCaptor);
    }

    @Test
    public void outboundCancelInvalid() throws Exception {
        performCall("outbound/cancel", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).cancelOutbound(isNull(ResourceId.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void outboundCancelValid() throws Exception {
        performCall("outbound/cancel", "cancel_outbound/cancel_outbound_task_message.json");
        ArgumentCaptor<ResourceId> resourceIdArgumentCaptor = ArgumentCaptor.forClass(ResourceId.class);
        verify(fulfillmentClient).cancelOutbound(resourceIdArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(resourceIdArgumentCaptor);
    }

    @Test
    public void createRegisterInvalid() throws Exception {
        performCall("register/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createRegister(isNull(Register.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void createRegisterValid() throws Exception {
        performCall("register/create", "fulfillment_create_order/fulfillment_create_register_with_all_parameters.json");
        ArgumentCaptor<Register> registerArgumentCaptor = ArgumentCaptor.forClass(Register.class);
        verify(fulfillmentClient).createRegister(registerArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(registerArgumentCaptor);
    }

    @Test
    public void putReferenceItemsInvalid() throws Exception {
        performCall("put/reference/items", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).putReferenceItems(isNull(List.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void putReferenceItemsValid() throws Exception {
        performCall("put/reference/items", "put_reference_items/put_reference_items_task_message.json");
        ArgumentCaptor<List> itemsArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(fulfillmentClient).putReferenceItems(itemsArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(itemsArgumentCaptor);
    }

    @Test
    public void createIntakeInvalid() throws Exception {
        performCall("intake/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createIntake(Matchers.isNull(Intake.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void createIntakeValid() throws Exception {
        performCall("intake/create", "fulfillment/create_intake/with_all_parameters.json");
        ArgumentCaptor<Intake> intakeArgumentCaptor = ArgumentCaptor.forClass(Intake.class);
        verify(fulfillmentClient).createIntake(intakeArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(intakeArgumentCaptor);
    }

    @Test
    public void createSelfExportInvalid() throws Exception {
        performCall("self-export/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createSelfExport(Matchers.isNull(SelfExport.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void createSelfExportValid() throws Exception {
        performCall("self-export/create", "fulfillment/create_self_export/with_all_parameters.json");
        ArgumentCaptor<SelfExport> selfExportArgumentCaptor = ArgumentCaptor.forClass(SelfExport.class);
        verify(fulfillmentClient).createSelfExport(selfExportArgumentCaptor.capture(), partnerArgumentCaptor.capture());
        assertCaptor(selfExportArgumentCaptor);
    }

    @Test
    public void createReturnRegisterInvalid() throws Exception {
        performCall("return-register/create", "broken_message_with_only_partner.json");
        verify(fulfillmentClient).createReturnRegister(Matchers.isNull(ReturnRegister.class), partnerArgumentCaptor.capture());
    }

    @Test
    public void createReturnRegisterValid() throws Exception {
        performCall("return-register/create", "fulfillment_create_return_register/with_all_parameters.json");
        ArgumentCaptor<ReturnRegister> returnRegisterArgumentCaptor = ArgumentCaptor.forClass(ReturnRegister.class);
        verify(fulfillmentClient).createReturnRegister(
            returnRegisterArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(returnRegisterArgumentCaptor);
    }

    @Test
    public void putRegisterValid() throws Exception {
        performCall("register/put", "fulfillment_put_register/put_register_request.json");
        ArgumentCaptor<TransportationRegister> returnRegisterArgumentCaptor = ArgumentCaptor.forClass(TransportationRegister.class);
        verify(fulfillmentClient).putRegister(
            returnRegisterArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(returnRegisterArgumentCaptor);
    }

    private void performCall(String httpRequestPathSuffix, String requestBodyFilename) throws Exception {
        String url = String.join("/", "/task/fulfillment", httpRequestPathSuffix);
        String pathToRequestBodyFile = "fixtures/executors/" + requestBodyFilename;

        mockMvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(pathToRequestBodyFile))
        )
            .andExpect(status().isOk());
    }

    private void assertCaptor(ArgumentCaptor captor) throws AssertionError {
        Validator validator = ClientUtilsFactory.getValidator();
        Assert.assertTrue(validator.validate(captor.getValue()).isEmpty());
    }

    private Order getEmptyOrder() {
        return new Order.OrderBuilder(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
            .build();
    }
}
