package ru.yandex.market.checkout.checkouter.controller;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkouter.jooq.tables.records.ParcelBoxRecord;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX;

/**
 * @author jkt on 06/08/2020.
 */
public class ParcelBoxControllerTest extends AbstractWebTestBase {

    private static String urlTemplate = "/orders/{order_id}/delivery/parcels/{parcel_id}/boxes";

    @Autowired
    private DSLContext dsl;

    @Test
    public void shouldReturn400OnEmptyBody() throws Exception {
        mockMvc.perform(
                        put(urlTemplate, 1L, 1L)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content("{}")
                ).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Missing boxes")));
    }

    @Test
    public void shouldReturn400OnNullParcelBoxes() throws Exception {
        mockMvc.perform(
                        put(urlTemplate, 1L, 1L)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content("{\"boxes\": null}")
                ).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Missing boxes")));
    }

    @Test
    public void shouldReturn422WhenHasDuplicateItemsInSameBox() throws Exception {
        mockMvc.perform(
                        put(urlTemplate, 1L, 1L)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content("{\"boxes\":[{\"fulfilmentId\":\"28588080-1\",\"weight\":2030,\"width\":11," +
                                        "\"height\":24,\"depth\":14,\"items\":[{\"itemId\":56544398,\"count\":1}," +
                                        "{\"itemId\":56544398,\"count\":1}]}]}")
                ).andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
                .andExpect(content().string(containsString("Parcel box has duplicate item ids")));

    }

    @Test
    public void shouldNotReturn422WhenHasDuplicateItemsInDifferentBoxes() throws Exception {
        mockMvc.perform(
                        put(urlTemplate, 1L, 1L)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content("{\"boxes\":[{\"fulfilmentId\":\"28588080-1\",\"weight\":2030,\"width\":11," +
                                        "\"height\":24,\"depth\":14,\"items\":[{\"itemId\":56544398,\"count\":1}]}," +
                                        "{\"fulfilmentId\":\"28588080-2\",\"weight\":2030,\"width\":11,\"height\":24," +
                                        "\"depth\":14,\"items\":[{\"itemId\":56544398,\"count\":1}]}]}")
                ).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Order not found")));

    }


    @Test
    public void shouldReturn200() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters(1));

        mockMvc.perform(
                put(urlTemplate, order.getId(), order.getDelivery().getParcels().get(0).getId())
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"boxes\":[{\"fulfilmentId\":\"28588080-1\",\"weight\":2030,\"width\":11," +
                                "\"height\":24,\"depth\":14,\"items\":[{\"itemId\":" +
                                Iterables.firstOf(order.getItems()).getId() + ",\"count\":1}]}]}")
        ).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void shouldReturn200WithId() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters(1));

        mockMvc.perform(
                put(urlTemplate, order.getId(), order.getDelivery().getParcels().get(0).getId())
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"boxes\":[{\"id\":-10, \"fulfilmentId\":\"28588080-1\",\"weight\":2030," +
                                "\"width\":11," +
                                "\"height\":24,\"depth\":14,\"items\":[{\"itemId\":" +
                                Iterables.firstOf(order.getItems()).getId() + ",\"count\":1}]}]}")
        ).andExpect(status().is(HttpStatus.OK.value()));


        Result<ParcelBoxRecord> result =
                dsl.selectFrom(PARCEL_BOX)
                        .where(PARCEL_BOX.PARCEL_ID.eq(order.getDelivery().getParcels().get(0).getId()))
                        .fetch();

        assertEquals(1, result.size());
        assertThat(result.get(0).getId(), greaterThan(0L));
    }
}
