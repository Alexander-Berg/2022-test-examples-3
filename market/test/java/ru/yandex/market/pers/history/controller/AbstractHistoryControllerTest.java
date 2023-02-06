package ru.yandex.market.pers.history.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.history.Item;
import ru.yandex.market.pers.history.MockedDbTest;
import ru.yandex.market.pers.history.RGB;
import ru.yandex.market.pers.history.Type;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.02.2020
 */
public abstract class AbstractHistoryControllerTest extends MockedDbTest {
    protected static final long UID = 12345L;
    protected static final String UUID = "dlskfjsdoif23";
    protected static final String YANDEXUID = "fbn459bn495n3.com";
    protected static final String HISTORY_URL_UID = "/history/UID/" + UID;
    protected static final String HISTORY_URL_YANDEXUID = "/history/YANDEXUID/" + YANDEXUID;
    protected static final long PAST_TIME = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
    protected static final long FUTURE_TIME = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();
    protected static final String HISTORY_URL_ALL = String.format("/history/all/MODEL/%s/%s", PAST_TIME, FUTURE_TIME);
    protected static final long RESOURCE_ID = 78980L;
    protected static final long ANOTHER_RESOURCE_ID = 2345678L;
    protected static final long YET_ANOTHER_RESOURCE_ID = 23456789L;
    protected static final String SKU = "some sku";
    protected static final String ANOTHER_SKU = "another sku";
    protected static final String ANOTHER_MODEL_SKU = "another model sku";

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected void addHistoryForUid(Item item) throws Exception {
        addHistory(item, HISTORY_URL_UID);
    }

    protected void addHistoryForYandexuid(Item item) throws Exception {
        addHistory(item, HISTORY_URL_YANDEXUID);
    }

    protected void addHistory(Item item, String historyUrl) throws Exception {
        mockMvc.perform(post(historyUrl).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(item)))
            .andExpect(status().is2xxSuccessful());
    }

    protected Item createItem() {
        return createItem(RESOURCE_ID);
    }

    protected Item createBlueItem(long resourceId, String sku) {
        return createBlueItem(resourceId, sku, null);
    }

    protected Item createBlueItemYandexUid(long resourceId, String sku) {
        Item result = createBlueItem(resourceId, sku, null);
        result.setUserId(null);
        result.setYandexUid(YANDEXUID);
        return result;
    }


    protected Item createBlueItem(long resourceId, String sku, String name) {
        Item item = createItem(resourceId);
        item.setRgb(RGB.blue);
        item.setMarketSku(sku);
        item.setName(name);
        return item;
    }

    protected Item createItem(long resourceId) {
        return createItem(Type.MODEL, resourceId);
    }

    protected Item createItem(Type type, long resourceId) {
        return createItem(type, resourceId, null);
    }


    protected Item createItem(Type type, long resourceId, String name) {
        Item item = new Item();
        item.setUserInformation(UserIdType.UID, String.valueOf(UID));
        item.setType(type);
        item.setResourceId(resourceId);
        item.setName(name);
        item.setDate(new Date());
        return item;
    }
}
