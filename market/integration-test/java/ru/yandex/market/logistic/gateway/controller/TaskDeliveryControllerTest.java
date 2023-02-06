package ru.yandex.market.logistic.gateway.controller;

import java.util.List;

import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.client.ClientUtilsFactory;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Intake;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderParcelId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.Register;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TaskDeliveryControllerTest extends AbstractIntegrationTest {

    private static final long UPDATE_REQUEST_ID = 111L;
    protected Partner partner = new Partner(145L);

    @MockBean
    private DeliveryClient deliveryClient;

    @Test
    public void orderCreateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/create_order/create_order_task_message_with_parcel_id.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).createOrder(
            orderArgumentCaptor.capture(),
            partnerArgumentCaptor.capture(),
            eq((CreateOrderRestrictedData) null)
        );

        assertCaptor(orderArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void orderWithRestrictedDataCreateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("fixtures/executors/create_order/create_order_task_message_with_restricted_data.json")))
                .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);
        ArgumentCaptor<CreateOrderRestrictedData> restrictedDataArgumentCaptor = ArgumentCaptor.forClass(CreateOrderRestrictedData.class);

        verify(deliveryClient).createOrder(orderArgumentCaptor.capture(), partnerArgumentCaptor.capture(), restrictedDataArgumentCaptor.capture());

        assertCaptor(orderArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
        assertCaptor(restrictedDataArgumentCaptor);
    }

    @Test
    public void orderUpdateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/update_order/update_order_task_message.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).updateOrder(
            orderArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(orderArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void orderCancelValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/cancel_order/cancel_order_task_message.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<ResourceId> orderIdArgumentCaptor = ArgumentCaptor.forClass(ResourceId.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).cancelOrder(
            orderIdArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(orderIdArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void recipientUpdateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/recipient/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/update_order/update_recipient_task_message_lom.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<ResourceId> orderIdArgumentCaptor = ArgumentCaptor.forClass(ResourceId.class);
        ArgumentCaptor<Recipient> recipientArgumentCaptor = ArgumentCaptor.forClass(Recipient.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);
        ArgumentCaptor<Long> updateRequestIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(deliveryClient).updateRecipient(
            orderIdArgumentCaptor.capture(),
            eq(null),
            recipientArgumentCaptor.capture(),
            updateRequestIdCaptor.capture(),
            partnerArgumentCaptor.capture()
        );

        assertCaptor(orderIdArgumentCaptor);
        assertCaptor(recipientArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
        assertCaptor(updateRequestIdCaptor);
    }

    @Test
    public void getLabelsValid() throws Exception {
        mockMvc.perform(post("/task/delivery/labels/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/get_labels/get_labels_task_message_with_parcel_id.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        ArgumentCaptor<List<OrderParcelId>> listCaptor = ArgumentCaptor.forClass((Class) List.class);

        verify(deliveryClient).getLabels(
            listCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(listCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    private void assertCaptor(ArgumentCaptor captor) throws AssertionError {
        Validator validator = ClientUtilsFactory.getValidator();
        Assert.assertTrue(validator.validate(captor.getValue()).isEmpty());
    }

    @Test
    public void orderDeliveryDateUpdateWithEmpyUpdateRequestIdValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order-delivery-date/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/update_order/update_order_delivery_date_with_empty_update_request_id_task_message.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<OrderDeliveryDate> orderDeliveryDateArgumentCaptor = ArgumentCaptor.forClass(OrderDeliveryDate.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).updateOrderDeliveryDate(
            orderDeliveryDateArgumentCaptor.capture(),
            partnerArgumentCaptor.capture(),
            eq(null)
        );

        assertCaptor(orderDeliveryDateArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void orderDeliveryDateUpdateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order-delivery-date/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/update_order/update_order_delivery_date_task_message.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<OrderDeliveryDate> orderDeliveryDateArgumentCaptor = ArgumentCaptor.forClass(OrderDeliveryDate.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).updateOrderDeliveryDate(
            orderDeliveryDateArgumentCaptor.capture(),
            partnerArgumentCaptor.capture(),
            eq(UPDATE_REQUEST_ID)
        );

        assertCaptor(orderDeliveryDateArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void intakeCreateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/intake/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/create_intake/create_intake_with_all_parameters_task.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Intake> intakeArgumentCaptor = ArgumentCaptor.forClass(Intake.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).createIntake(
            intakeArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(intakeArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void badRequestTypeInvalid() throws Exception {
        mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .accept(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/delivery_bad_request_type.xml")))
            .andExpect(status().is4xxClientError())
            .andExpect(content()
                .string("Can't find PartnerMethod by apiType='DELIVERY' and requestName='theMostUselessRequest'"));
    }

    @Test
    public void intakeCreateInvalidValid() throws Exception {
        mockMvc.perform(post("/task/delivery/intake/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")))
            .andExpect(status().is2xxSuccessful());

        verify(deliveryClient).createIntake(
            null,
            partner
        );
    }

    @Test
    public void selfExportCreateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/selfexport/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/create_selfexport/create_selfexport_with_all_parameters_task.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<SelfExport> selfExportArgumentCaptor = ArgumentCaptor.forClass(SelfExport.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).createSelfExport(
            selfExportArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(selfExportArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);

    }

    @Test
    public void registerCreateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/register/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/create_register/create_register_with_all_parameters_task.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<Register> registerArgumentCaptor = ArgumentCaptor.forClass(Register.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).createRegister(
            registerArgumentCaptor.capture(), partnerArgumentCaptor.capture()
        );

        assertCaptor(registerArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test
    public void orderItemsUpdateValid() throws Exception {
        mockMvc.perform(post("/task/delivery/order/items/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/update_order/update_order_items_task_message.json")))
            .andExpect(status().is2xxSuccessful());

        ArgumentCaptor<OrderItems> orderItemsArgumentCaptor = ArgumentCaptor.forClass(OrderItems.class);
        ArgumentCaptor<Partner> partnerArgumentCaptor = ArgumentCaptor.forClass(Partner.class);

        verify(deliveryClient).updateOrderItems(
            orderItemsArgumentCaptor.capture(),
            partnerArgumentCaptor.capture()
        );

        assertCaptor(orderItemsArgumentCaptor);
        assertCaptor(partnerArgumentCaptor);
    }

    @Test(expected = NestedServletException.class)
    public void orderDeliveryDateUpdateInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/inbound/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }

    @Test(expected = NestedServletException.class)
    public void recipientUpdateInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/order/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }

    @Test(expected = NestedServletException.class)
    public void orderUpdateInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/order/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }

    @Test(expected = NestedServletException.class)
    public void orderCreateInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/order/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }

    @Test(expected = NestedServletException.class)
    public void orderCancelInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/order/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }

    @Test(expected = NestedServletException.class)
    public void getLabelsInvalid() throws Exception {
        mockMvc.perform(post("/task/fulfillment/outbound/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/executors/broken_message_with_only_partner.json")));
    }
}
