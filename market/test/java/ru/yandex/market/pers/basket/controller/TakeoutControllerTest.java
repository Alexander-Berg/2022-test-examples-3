package ru.yandex.market.pers.basket.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.AliceEntry;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.Nid;
import ru.yandex.market.pers.basket.model.Price;
import ru.yandex.market.pers.basket.service.AliceService;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.basket.service.CategoryService;
import ru.yandex.market.pers.list.mock.BasketOldMvcMocks;
import ru.yandex.market.pers.list.model.BasketItem;
import ru.yandex.market.pers.list.model.BasketItemType;
import ru.yandex.market.pers.list.model.BasketLabel;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;
import ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.BLUE;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.RED;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.FEED_GROUP_ID_HASH;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.SKU;

/**
 * @author vvolokh
 * 05.02.2019
 */
public class TakeoutControllerTest extends PersBasketTest {

    private static final long UID = 23427062L;
    private static final long UNKNOWN_UID = UID + 1;
    private static final BasketOwner OWNER = BasketOwner.fromUid(UID);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BasketService dbService;
    @Autowired
    private AliceService aliceService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BasketOldMvcMocks basketOldMvc;

    private Long ownerId;

    @Before
    public void setup() {
        ownerId = dbService.getOrAddOwnerId(OWNER);
        dbService.deleteAllForOwner(ownerId);
    }

    @Test
    public void testGenerateWhiteTakeoutData() throws Exception {
        // any passed ID should not generate error since labels are not supported any more
        long anyLabelId = 9999999999L;

        List<BasketItem> itemsToSave = Arrays.asList(
            generateModelItem(anyLabelId, 1L, "d_name_1"),
            generateOfferItem(anyLabelId, "2", "d_name_2"),
            generateClusterItem(anyLabelId, 3L, "d_name_3"),
            generateGroupItem(anyLabelId, 4L, "d_name_4"));
        basketOldMvc.addItems(BasketOwner.fromUid(UID), itemsToSave);

        String response = mockMvc
            .perform(get("/takeout?color=white&uid=" + UID).contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(fileToString("/takeout/response_white.json"), response,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data[*].added_at", (o1, o2) -> true)
            ));
    }

    @Test
    public void testGenerateRedTakeoutData() throws Exception {
        dbService.addItem(newItem(RED, "Чудо из Китая", FEED_GROUP_ID_HASH, "87485hj", 45), OWNER);
        dbService.addItem(newItem(RED, "Лукум из Турции", FEED_GROUP_ID_HASH, "11hjh", 45), OWNER);

        String response = mockMvc
            .perform(get("/takeout?color=red&uid=" + UID).contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(fileToString("/takeout/response_red.json"), response,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data[*].added_at", (o1, o2) -> true)
            ));
    }

    @Test
    public void testGenerateBlueTakeoutData() throws Exception {
        dbService.addItem(newItem(BLUE, "Конфеты Rafaello", SKU, "56788", 45), OWNER);
        dbService.addItem(newItem(BLUE, "Apple Airpods", SKU, "12345", 45), OWNER);

        String response = mockMvc
            .perform(get("/takeout?color=blue&uid=" + UID).contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(fileToString("/takeout/response_blue.json"), response,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data[*].added_at", (o1, o2) -> true)
            ));
    }

    @Test
    public void testExceptionOnIncorrectColor() throws Exception {
        mockMvc.perform(get("/takeout?color=foo&uid=" + UID).contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is4xxClientError())
            .andReturn().getResponse().getContentAsString();
    }

    private BasketReferenceItem newItem(MarketplaceColor color, String title, ReferenceType referenceType, String referenceId, Integer regionId) {
        BasketReferenceItem result = newItem(color, title, referenceType, referenceId);
        result.setRegionId(regionId);
        return result;
    }

    private BasketReferenceItem newItem(MarketplaceColor color, String title, ReferenceType referenceType, String referenceId) {
        BasketReferenceItem item = new BasketReferenceItem();
        item.setOwnerId(ownerId);
        item.setId(123L);
        item.setColor(color);
        item.setReferenceType(referenceType);
        item.setReferenceId(referenceId);
        item.setImageBaseUrl("https://t.me/libmustdie");
        item.setTitle(title);
        item.setPrice(new Price("RUB", new BigDecimal("1234")));
        item.setData(Map.of());
        if(color == RED) {
            item.setData(Map.of(SecondaryReferenceType.WARE_MD5.getName(), "123"));
        }
        item.setAddedAtInternal(LocalDateTime.of(2018, 10, 5, 15, 30).toInstant(ZoneOffset.UTC));
        return item;
    }

    private BasketItem generateModelItem(Long labelId, long modelId, String displayName) {
        BasketItem item = new BasketItem(BasketItemType.MODEL);
        item.setModelId(modelId);
        fillCommonFields(item, labelId, displayName);
        return item;
    }

    private BasketItem generateOfferItem(Long labelId, String offerId, String displayName) {
        BasketItem item = new BasketItem(BasketItemType.OFFER);
        item.setModelId(Math.abs(RND.nextInt()));
        item.setOfferId(offerId);
        fillCommonFields(item, labelId, displayName);
        return item;
    }

    private BasketItem generateClusterItem(Long labelId, long modelId, String displayName) {
        BasketItem item = new BasketItem(BasketItemType.CLUSTER);
        item.setClusterId(modelId);
        fillCommonFields(item, labelId, displayName);
        return item;
    }

    private BasketItem generateGroupItem(Long labelId, long modelId, String displayName) {
        BasketItem item = new BasketItem(BasketItemType.GROUP);
        item.setGroupId(modelId);
        fillCommonFields(item, labelId, displayName);
        return item;
    }

    private void fillCommonFields(BasketItem item, Long labelId, String displayName) {
        List<Long> labels = new ArrayList<>(singletonList(labelId));
        item.setHid(Math.abs(RND.nextInt()));
        item.setDisplayName(displayName);
        item.setUid(UID);
        item.setOwnerId(UID);
        item.setLabelIds(labels);
    }

    private BasketLabel generateLabel(String displayName) {
        BasketLabel label = new BasketLabel();
        label.setDisplayName(displayName);
        label.setNote("note");
        label.setOwnerId(UID);
        return label;
    }

    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[0]);
        return request;
    }

    @Test
    public void testGetStatusAndDeleteBasketData() throws Exception {
        dbService.addItem(newItem(BLUE, "Конфеты Rafaello", SKU, "56788", 45), OWNER);
        dbService.addItem(newItem(BLUE, "Apple Airpods", SKU, "12345", 45), OWNER);


        hasBasketData();

        deleteBasketType();
        hasNoData();
    }

    @Test
    public void testGetStatusAndDeleteAliceData() throws Exception {
        aliceService.saveEntries(Collections.singletonList(AliceEntry.simple(ownerId, "text", Collections.emptyList())));

        hasAliceData();
        deleteBasketType();
        hasAliceData();
        mockMvc
                .perform(post("/takeout/delete?types=alice-wishlist&uid=" + UID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().string("")));
        hasNoData();
    }

    @Test
    public void testGetStatusAndDeleteCategoryData() throws Exception {
        categoryService.postEntries(ownerId, BLUE, Collections.singletonList(new Nid(123L, "category", Collections.emptyList())));


        hasBasketData();
        deleteBasketType();
        hasNoData();
    }

    @Test
    public void testGetStatusTakeoutWithoutData() throws Exception {
        hasNoData();
        deleteBothTypes();
        hasNoData();
    }

    @Test
    public void testGetStatusTakeoutWithoutUser() throws Exception {
        categoryService.postEntries(ownerId, BLUE, Collections.singletonList(new Nid(123L, "category", Collections.emptyList())));
        hasNoDataForUnknownUser();
        deleteBothTypesForUnknownUser();
        hasNoDataForUnknownUser();
        hasBasketData();
    }

    private void deleteBasketType() throws Exception {
        mockMvc
                .perform(post("/takeout/delete?types=basket&uid=" + UID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().string("")));
    }

    private void hasNoData() throws Exception {
        mockMvc
                .perform(get("/takeout/status?uid=" + UID).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().json(fileToString("/takeout/status_response_without_data.json"), true)));
    }

    private void hasBasketData() throws Exception {
        mockMvc
                .perform(get("/takeout/status?uid=" + UID).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().json(fileToString("/takeout/status_response_with_data.json"), true)));
    }

    private void hasAliceData() throws Exception {
        mockMvc
                .perform(get("/takeout/status?uid=" + UID).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().json("{\n" +
                                "    \"types\": [\"alice-wishlist\"]\n" +
                                "}", true)));
    }

    private void deleteBothTypes() throws Exception {
        mockMvc
                .perform(post("/takeout/delete?types=basket&types=alice-wishlist&uid=" + UID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().string("")));
    }

    private void hasNoDataForUnknownUser() throws Exception {
        mockMvc
                .perform(get("/takeout/status?uid=" + UNKNOWN_UID).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().json(fileToString("/takeout/status_response_without_data.json"), true)));
    }

    private void deleteBothTypesForUnknownUser() throws Exception {
        mockMvc
                .perform(post("/takeout/delete?types=basket&types=alice-wishlist&uid=" + UNKNOWN_UID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(matchAll(status().isOk(),
                        content().string("")));
    }
}
