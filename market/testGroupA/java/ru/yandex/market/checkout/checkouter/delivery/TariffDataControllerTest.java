package ru.yandex.market.checkout.checkouter.delivery;

import java.util.EnumSet;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterDeliveryAPI;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.TariffDataHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.TariffDataProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TariffDataControllerTest extends AbstractWebTestBase {

    @Autowired
    private TariffDataHelper tariffDataHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private CheckouterDeliveryAPI checkouterDeliveryAPI;

    @Test
    public void putTariffData() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());

        Long orderId = order.getId();
        tariffDataHelper.putTariffData(orderId, TariffDataProvider.getTariffData());

        order = orderService.getOrder(orderId);

        checkTariffData(order.getDelivery().getTariffData());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(orderId);

        OrderHistoryEvent event = Iterables.get(events.getItems(), 0);
        assertThat(event.getType(), is(HistoryEventType.ORDER_DELIVERY_UPDATED));
        assertThat(event.getOrderBefore().getDelivery().getTariffData(), nullValue());

        checkTariffData(event.getOrderAfter().getDelivery().getTariffData());
    }

    @Test
    public void putTariffDataViaClient() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());

        Long orderId = order.getId();
        checkouterDeliveryAPI.putTariffData(orderId, ClientInfo.SYSTEM, TariffDataProvider.getTariffData());

        order = orderService.getOrder(orderId);

        checkTariffData(order.getDelivery().getTariffData());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(orderId);

        OrderHistoryEvent event = Iterables.get(events.getItems(), 0);
        assertThat(event.getType(), is(HistoryEventType.ORDER_DELIVERY_UPDATED));
        assertThat(event.getOrderBefore().getDelivery().getTariffData(), nullValue());

        checkTariffData(event.getOrderAfter().getDelivery().getTariffData());
    }

    @Test
    public void shouldNotAllowToPutUnknownLanguage() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());

        Long orderId = order.getId();

        TariffData tariffData = TariffDataProvider.getTariffData();
        tariffData.setCustomsLanguage(LanguageCode.UNKNOWN);

        tariffDataHelper.putTariffDataForActions(orderId, tariffData)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotAllowToPutUnknownLanguages() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());

        Long orderId = order.getId();

        TariffData tariffData = TariffDataProvider.getTariffData();
        tariffData.setCustomsLanguages(EnumSet.of(LanguageCode.UNKNOWN, LanguageCode.EN));

        tariffDataHelper.putTariffDataForActions(orderId, tariffData)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotAllowToPutUnknownLanguageRaw() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());

        Long orderId = order.getId();

        mockMvc.perform(put(TariffDataHelper.PUT_DELIVERY_TARIFF_URL, orderId)
                .content("{\"" + Names.TariffData.CUSTOMS_LANGUAGE + "\": \"NOT_EXISTING_LANGUAGE\" }")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(log())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Language code cannot be unknown"));
    }


    private void checkTariffData(TariffData tariffData) {
        assertThat(tariffData, notNullValue());
        assertThat(tariffData.isNeedPersonalData(), is(true));
        assertThat(tariffData.getCustomsLanguage(), is("EN"));
        assertThat(tariffData.getCustomsLanguageAsEnum(), is(LanguageCode.EN));
        assertThat(tariffData.getCustomsLanguages(), equalTo(EnumSet.of(LanguageCode.EN, LanguageCode.ZH)));
        assertThat(tariffData.getTariffCode(), is("code"));
        assertThat(tariffData.isNeedTranslationForCustom(), is(true));
    }
}
