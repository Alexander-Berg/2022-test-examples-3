package ru.yandex.market.checkout.checkouter.itemservice;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceDto;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_REGISTRATION;

/**
 * @author zagidullinri
 * @date 02.12.2021
 */
public class ItemServiceTimeSlotWithPartnerIdTest extends AbstractWebTestBase {

    private static final String PARTNER_ID = "pirozhok";

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private WireMockServer yaUslugiMock;

    @Autowired
    private ObjectMapper yaUslugiObjectMapper;

    @BeforeEach
    public void setUp() {
        checkouterProperties.setEnableItemServiceTimeslots(true);
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .willReturn(okJson("{}")));
        String expectedJson = readResourceFile("/json/yaUslugiTimeSlotsResponse.json");
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/get_cached_slots"))
                        .willReturn(okJson(expectedJson)));
    }

    @Test
    public void createShouldSendTimeSlotWhenFromTimeToTimeAreNull() throws IOException {
        LocalDateTime dateTime = LocalDateTime.of(2021, 11, 1, 17, 0);
        createOrderWithService(itemService -> {
            itemService.setPartnerId(PARTNER_ID);
            itemService.setFromTime((LocalTime) null);
            itemService.setToTime((LocalTime) null);
            itemService.setDate(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
        });
        YaServiceDto yaServiceDto = getCreateServiceRequestBody();

        assertNull(yaServiceDto.getFromTime());
        assertNull(yaServiceDto.getToTime());
        assertNotNull(yaServiceDto.getTimeslot());
        assertEquals(dateTime, yaServiceDto.getTimeslot().getDate());
        assertEquals(PARTNER_ID, yaServiceDto.getTimeslot().getPartnerId());
    }

    @Test
    public void createShouldSendIntervalWhenFromTimeToTimeAreNotNull() throws IOException {
        createOrderWithService(itemService -> {
        });
        YaServiceDto yaServiceDto = getCreateServiceRequestBody();

        assertNotNull(yaServiceDto.getDate());
        assertNotNull(yaServiceDto.getFromTime());
        assertNotNull(yaServiceDto.getToTime());
        assertNull(yaServiceDto.getTimeslot());
    }

    private void createOrderWithService(Consumer<ItemService> itemServiceConfigurer) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        ItemService itemService = parameters.addItemService();
        itemServiceConfigurer.accept(itemService);
        orderCreateHelper.createOrder(parameters);
        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);
    }

    private YaServiceDto getCreateServiceRequestBody() throws java.io.IOException {
        String createServiceBodyRequest = yaUslugiMock.getAllServeEvents()
                .stream()
                .filter(it -> "/ydo/api/market_partner_orders/services".equals(it.getRequest().getUrl()))
                .findFirst()
                .get()
                .getRequest()
                .getBodyAsString();
        return yaUslugiObjectMapper.readValue(createServiceBodyRequest, YaServiceDto.class);
    }

    private String readResourceFile(String filePath) {
        try {
            return IOUtils.readInputStream(getClass().getResourceAsStream(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Reading resource " + filePath + "failed");
        }
    }
}
