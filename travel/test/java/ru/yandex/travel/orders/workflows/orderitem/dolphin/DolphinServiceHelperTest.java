package ru.yandex.travel.orders.workflows.orderitem.dolphin;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.dolphin.DefaultDolphinClient;
import ru.yandex.travel.hotels.common.partners.dolphin.model.CreateOrderRequest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.Guest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.GuestName;
import ru.yandex.travel.hotels.common.partners.dolphin.model.Order;
import ru.yandex.travel.hotels.common.partners.dolphin.model.PriceKey;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.testing.misc.TestResources.readResource;

public class DolphinServiceHelperTest {
    public static final ObjectMapper jsonMapper = DefaultDolphinClient.createObjectMapper();
    public static final String CREATE_REQUEST = "partner_responses/dolphin/DolphinSample_CreateOrder_Request.json";
    public static final String ORDERS_RESPONSE = "partner_responses/dolphin/DolphinSample_OrdersInfo_Response.json";
    public static final boolean IS_MALE = true;

    @Test
    public void createRequestsMatch() throws Exception {
        CreateOrderRequest req = jsonMapper.readValue(readResource(CREATE_REQUEST), CreateOrderRequest.class);
        List<Order> rsp = jsonMapper.readValue(readResource(ORDERS_RESPONSE), new TypeReference<List<Order>>() {});

        assertThat(DolphinServiceHelper.createRequestsMatch(req, rsp.get(0).getRequest())).isTrue();
        assertThat(DolphinServiceHelper.createRequestsMatch(rsp.get(0).getRequest(), req)).isTrue();

        // adults + children
        req.setAdults(2);
        assertThat(DolphinServiceHelper.createRequestsMatch(req, rsp.get(0).getRequest())).isFalse();
        req.setAdults(1);
        assertThat(DolphinServiceHelper.createRequestsMatch(req, rsp.get(0).getRequest())).isTrue();

        // email + phone
        req.setEmail("no_such_email@example.com");
        assertThat(DolphinServiceHelper.createRequestsMatch(req, rsp.get(0).getRequest())).isTrue();
    }

    @Test
    public void adultsAndChildrenMatch() {
        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(1, List.of()),
                sampleCreateReq(1, null)
        )).isTrue();

        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(1, List.of()),
                sampleCreateReq(2, List.of())
        )).isFalse();

        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(1, List.of(4)),
                sampleCreateReq(2, List.of())
        )).isTrue();

        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(1, List.of(4)),
                sampleCreateReq(1, List.of(6))
        )).isTrue();

        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(1, List.of(4)),
                sampleCreateReq(2, List.of(4))
        )).isFalse();

        assertThat(DolphinServiceHelper.adultsAndChildrenMatch(
                sampleCreateReq(2, List.of(4)),
                sampleCreateReq(1, List.of(4, 5))
        )).isTrue();
    }

    @Test
    public void priceKeysMatch() {
        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, List.of(1)),
                priceKey(1, List.of(1))
        )).isFalse();

        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, List.of(1)),
                priceKey(2, List.of(1))
        )).isTrue();

        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, List.of(1, 1)),
                priceKey(2, List.of(1, 3))
        )).isTrue();

        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, List.of(1, 1)),
                priceKey(2, List.of(1))
        )).isFalse();

        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, null),
                priceKey(2, List.of(1))
        )).isFalse();

        assertThat(DolphinServiceHelper.priceKeysMatch(
                priceKey(2, null),
                priceKey(2, List.of())
        )).isTrue();
    }

    @Test
    public void guestsListsMatch() {
        Guest guestM = Guest.builder().isMale(IS_MALE).build();
        Guest guestF = Guest.builder().isMale(!IS_MALE).build();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                null,
                null
        )).isTrue();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(),
                null
        )).isTrue();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(guestM),
                List.of()
        )).isFalse();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(guestM),
                List.of(guestM)
        )).isTrue();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(guestM),
                List.of(guestF)
        )).isFalse();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(guestM, guestF),
                List.of(guestM, guestF)
        )).isTrue();

        assertThat(DolphinServiceHelper.guestsListsMatch(
                List.of(guestM, guestF),
                List.of(guestF, guestM)
        )).isFalse();
    }

    @Test
    public void guestsMatch() {
        assertThat(DolphinServiceHelper.guestsMatch(
                guest("First", "Last", IS_MALE),
                guest("First", "Last", IS_MALE)
        )).isTrue();

        assertThat(DolphinServiceHelper.guestsMatch(
                guest("First", "Last", IS_MALE),
                guest("First", "Last", !IS_MALE)
        )).isFalse();

        Guest sameWithExtraFields = guest("First", "Last", IS_MALE);
        sameWithExtraFields.setLatin(GuestName.builder().firstName("X").lastName("Y").build());
        sameWithExtraFields.setCitizenship("RUS");
        sameWithExtraFields.setId(827461264234L);
        assertThat(DolphinServiceHelper.guestsMatch(
                guest("First", "Last", IS_MALE),
                sameWithExtraFields
        )).isTrue();

        sameWithExtraFields.getCyrillic().setPatronymicName("DifferentPatronymic");
        assertThat(DolphinServiceHelper.guestsMatch(
                guest("First", "Last", IS_MALE),
                sameWithExtraFields
        )).isFalse();
    }

    private static CreateOrderRequest sampleCreateReq(int adults, List<Integer> children) {
        return CreateOrderRequest.builder().adults(adults).children(children).build();
    }

    private static PriceKey priceKey(int hotelId, List<Integer> beds) {
        return PriceKey.builder().hotelId(hotelId).beds(beds).build();
    }

    private static Guest guest(String name, String surname, boolean isMale) {
        GuestName guestName = GuestName.builder().firstName(name).lastName(surname).build();
        return Guest.builder().isMale(isMale).cyrillic(guestName).build();
    }
}
