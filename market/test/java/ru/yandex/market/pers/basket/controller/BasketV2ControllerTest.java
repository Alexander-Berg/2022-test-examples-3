package ru.yandex.market.pers.basket.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.controller.v2.BasketV2DeleteTestRequest;
import ru.yandex.market.pers.basket.controller.v2.BasketV2GetTestRequest;
import ru.yandex.market.pers.basket.controller.v2.BasketV2PostTestRequest;
import ru.yandex.market.pers.basket.controller.v2.BasketV2TestHelper;
import ru.yandex.market.pers.basket.controller.v2.BasketV2TestRequest;
import ru.yandex.market.pers.basket.controller.v2.ThrowingBiConsumer;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.SecondaryReference;
import ru.yandex.market.pers.list.mock.BasketV2MvcMocks;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.UserIdType;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author ifilippov5
 */
public class BasketV2ControllerTest extends PersBasketTest {

    @Autowired
    protected BasketV2TestHelper helper;

    @Autowired
    private BasketV2MvcMocks basketV2Mvc;

    protected BasketV2GetTestRequest defaultGetItemsRequest = new BasketV2GetTestRequest();
    private BasketV2DeleteTestRequest defaultDeleteItemRequest = new BasketV2DeleteTestRequest();

    protected BasketV2PostTestRequest defaultAddItemRequest = new BasketV2PostTestRequest();

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new ParameterNamesModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testValidateGetItemsRequest() throws Exception {
        ThrowingBiConsumer<BasketV2TestRequest, ResultMatcher> requestConsumer =
            (request, status) -> helper.getItems(request, status);
        validateUserArguments(requestConsumer, defaultGetItemsRequest);
        validateMarketColor(requestConsumer, defaultGetItemsRequest);
    }

    @Test
    public void testValidateAddItemRequest() throws Exception {
        ThrowingBiConsumer<BasketV2TestRequest, ResultMatcher> requestConsumer =
            (request, status) -> helper.addItem(request, status);
        validateUserArguments(requestConsumer, defaultAddItemRequest);
        validateMarketColor();
    }

    @Test
    public void testValidateDeleteItemRequest() throws Exception {
        ThrowingBiConsumer<BasketV2TestRequest, ResultMatcher> requestConsumer =
            (request, status) -> helper.deleteItem(request, status);
        helper.tryToAuthorizeUser(defaultDeleteItemRequest);
        validateUserArguments(requestConsumer, defaultDeleteItemRequest);
        validateMarketColor(requestConsumer, defaultDeleteItemRequest);
    }

    private void validateUserArguments(ThrowingBiConsumer<BasketV2TestRequest, ResultMatcher> requestConsumer,
                                       BasketV2TestRequest defaultRequest) throws Exception {
        ResultMatcher success = ok;
        BasketV2TestRequest request = defaultRequest.clone();
        for (UserIdType userIdType : UserIdType.values()) {
            request.setUserIdType(userIdType.name());
            request.setUserAnyId("");
            requestConsumer.accept(request, badRequest);
            request.setUserAnyId(null);
            requestConsumer.accept(request, badRequest);
        }

        // save regular uid
        String randomStr = UUID.randomUUID().toString();
        request.setUserIdType(UserIdType.UID.name());
        request.setUserAnyId(Long.toString(19283719237L));
        requestConsumer.accept(request, success);

        // save broken uid
        request.setUserAnyId("123a");
        requestConsumer.accept(request, badRequest);

        // save uuid
        request.setUserIdType(UserIdType.UUID.name());
        request.setUserAnyId(randomStr);
        requestConsumer.accept(request, success);

        // save owner_id
        request.setUserIdType(UserIdType.OWNER_ID.name());
        request.setUserAnyId(Long.toString(28347293L));
        requestConsumer.accept(request, badRequest);

        //save broken owner_id
        request.setUserAnyId("123a");
        requestConsumer.accept(request, badRequest);

        // save yandexuid
        request.setUserIdType(UserIdType.YANDEXUID.name());
        request.setUserAnyId("123a");
        requestConsumer.accept(request, success);
    }

    private void validateMarketColor(ThrowingBiConsumer<BasketV2TestRequest, ResultMatcher> requestConsumer,
                                    BasketV2TestRequest defaultRequest) throws Exception {
        ResultMatcher success = ok;
        BasketV2TestRequest request = defaultRequest.clone();
        for (MarketplaceColor color : MarketplaceColor.values()) {
            request.setRgb(color.getName());
            requestConsumer.accept(request, success);
        }
        request.setRgb("");
        requestConsumer.accept(request, badRequest);
        request.setRgb(null);
        requestConsumer.accept(request, badRequest);
    }

    private void validateMarketColor() throws Exception {
        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        for (MarketplaceColor color : MarketplaceColor.values()) {
            request.setRgb(color.getName());
            ResultMatcher status = request.getItem().getReferenceType().getSupportedColors().contains(color)
                ? ok
                : badRequest;
            helper.addItem(request, status);
        }
        request.setRgb("");
        helper.addItem(request, badRequest);
        request.setRgb(null);
        helper.addItem(request, badRequest);
        request.setRgb("RED");
        helper.addItem(request, badRequest);
    }

    @Test
    public void testPagination() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items2.json");
        MarketplaceColor color = MarketplaceColor.RED;
        for (BasketV2PostTestRequest.BasketRefItem item : items) {
            BasketV2PostTestRequest request = defaultAddItemRequest.clone();
            request.setUserAnyId(uuid);
            request.setUserIdType(UserIdType.UUID);
            addItem(request, item);
        }
        items = items.stream()
            .sorted(Comparator.comparing(BasketV2PostTestRequest.BasketRefItem::getAddedAt).reversed())
            .collect(Collectors.toList());
        BasketV2GetTestRequest getRequest = defaultGetItemsRequest.clone();
        getRequest.setRgb(color.getName());
        getRequest.setUserAnyId(uuid);
        getRequest.setUserIdType(UserIdType.UUID.name());
        assertEquals(items.size(), helper.getItems(getRequestWithPagination(getRequest, null, null, null, null),
            ok).getTotal());
        assertEquals(items,
            getItemsWithPagination(getRequest, null, null, null, null));
        assertEquals(items,
            getItemsWithPagination(getRequest, "1", null, null, null));
        assertEquals(items.subList(0, 3),
            getItemsWithPagination(getRequest, null, "3", null, null));
        assertEquals(Collections.emptyList(),
            getItemsWithPagination(getRequest, "2", "0", null, null));
        assertEquals(items.subList(2, 4),
            getItemsWithPagination(getRequest, "2", "2", null, null));
        assertEquals(Collections.emptyList(),
            getItemsWithPagination(getRequest, "2", "100500", null, null));
        assertEquals(items,
            getItemsWithPagination(getRequest, "1", "100500", null, null));

        assertEquals(items.subList(2, items.size()),
            getItemsWithPagination(getRequest, null, null, "2", "8"));
        assertEquals(items,
            getItemsWithPagination(getRequest, null, null, null, "100500"));
        assertEquals(Collections.emptyList(),
            getItemsWithPagination(getRequest, null, null, "100500", null));
        assertEquals(items.subList(0, 1),
            getItemsWithPagination(getRequest, null, null, "0", "1"));
    }

    @Test
    public void testRegionSave() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items.json");

        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setUserAnyId(uuid);
        request.setUserIdType(UserIdType.UUID);
        request.setRegionId(123);
        addItem(request, items.get(0));

        MarketplaceColor color = MarketplaceColor.BLUE;

        BasketV2GetTestRequest getRequest = defaultGetItemsRequest.clone();
        getRequest.setRgb(color.getName());
        getRequest.setUserAnyId(uuid);
        getRequest.setUserIdType(UserIdType.UUID.name());

        BasketV2TestHelper.PersBasketResponse.Result result = helper
            .getItems(getRequestWithPagination(getRequest, null, null, null, null), ok);

        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getTotal());

        assertEquals(123, result.getItems().get(0).getRegionId().intValue());
    }

    private List<BasketV2PostTestRequest.BasketRefItem> getItemsWithPagination(BasketV2GetTestRequest getRequest,
                                                                               String page, String pageSize,
                                                                               String offset, String limit) throws Exception {
        return getItems(getRequestWithPagination(getRequest, page, pageSize, offset, limit));
    }

    private BasketV2GetTestRequest getRequestWithPagination(BasketV2GetTestRequest req, String page, String pageSize,
                                                            String offset, String limit) {
        BasketV2GetTestRequest request = req.clone();
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setOffset(offset);
        request.setLimit(limit);
        return request;
    }

    @Test
    public void testRemoveAndBackIntoPlace() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items.json");
        items = items.stream()
            .sorted(Comparator.comparing(BasketV2PostTestRequest.BasketRefItem::getAddedAt).reversed())
            .collect(Collectors.toList());
        MarketplaceColor color = MarketplaceColor.BLUE;
        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setUserAnyId(uuid);
        request.setUserIdType(UserIdType.UUID);
        for (BasketV2PostTestRequest.BasketRefItem item : items) {
            addItem(request, item);
        }
        BasketV2GetTestRequest getRequest = defaultGetItemsRequest.clone();
        getRequest.setRgb(color.getName());
        getRequest.setUserAnyId(uuid);
        getRequest.setUserIdType(UserIdType.UUID.name());

        List<BasketV2PostTestRequest.BasketRefItem> items2 = getItems(getRequest);
        assertEquals(items, items2);

        int index = 0;
        for (BasketV2PostTestRequest.BasketRefItem item : items2) {
            //удаляем элемент на позиции index
            BasketV2DeleteTestRequest deleteRequest = defaultDeleteItemRequest;
            deleteRequest.setRgb(color.getName());
            deleteRequest.setUserAnyId(uuid);
            deleteRequest.setUserIdType(UserIdType.UUID.name());
            deleteRequest.setItemId(item.getBasketItemId());
            helper.deleteItem(deleteRequest, ok);
            List<BasketV2PostTestRequest.BasketRefItem> actualItems = getItems(getRequest);
            List<BasketV2PostTestRequest.BasketRefItem> itemsWithoutCurrent = new ArrayList<>(items);
            itemsWithoutCurrent.remove(index++);
            assertEquals(itemsWithoutCurrent, actualItems);

            //возвращаем удаленный элемент
            addItem(request, item);
            actualItems = getItems(getRequest);
            assertEquals(items, actualItems);
        }
    }

    @Test
    public void testGetByReferences() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items2.json");
        MarketplaceColor color = MarketplaceColor.RED;
        for (BasketV2PostTestRequest.BasketRefItem item : items) {
            BasketV2PostTestRequest request = defaultAddItemRequest.clone();
            request.setUserAnyId(uuid);
            request.setUserIdType(UserIdType.UUID);
            addItem(request, item);
        }
        items = items.stream()
            .sorted(Comparator.comparing(BasketV2PostTestRequest.BasketRefItem::getAddedAt).reversed())
            .collect(Collectors.toList());

        BasketV2GetTestRequest getRequest = defaultGetItemsRequest.clone();
        getRequest.setRgb(color.getName());
        getRequest.setUserAnyId(uuid);
        getRequest.setUserIdType(UserIdType.UUID.name());
        getRequest.setRefs(items);

        List<BasketV2PostTestRequest.BasketRefItem> items2 = getItems(getRequest);
        assertEquals(items, items2);

        getRequest.setRefs(items.subList(0, 1));
        items2 = getItems(getRequest);
        assertEquals(items.subList(0, 1), items2);

        ReferenceType otherRefType = items.get(0).getReferenceType() == ReferenceType.SKU ? ReferenceType.FEED_GROUP_ID_HASH
            : ReferenceType.SKU;
        getRequest.setRefs(items.stream()
            .peek(item -> item.setReferenceType(otherRefType))
            .collect(Collectors.toList()));
        items2 = getItems(getRequest);
        assertEquals(items.size(), items2.size());
        items.get(0).setReferenceType(ReferenceType.FEED_GROUP_ID_HASH);
        getRequest.setRefs(items);
        items2 = getItems(getRequest);
        assertEquals(items.subList(0, 1), items2);
    }

    @Test
    public void testValidateImage() throws Exception {
        BasketV2PostTestRequest.BasketRefItem item = new BasketV2PostTestRequest.BasketRefItem();
        item.setImageBaseUrl("https://a");
        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setItem(item);
        request.setRgb(MarketplaceColor.BLUE.getName());
        helper.addItem(request, badRequest);
    }

    @Test
    public void testSecondaryReferences() throws Exception {
        String uuid = UUID.randomUUID().toString();
        BasketV2PostTestRequest.BasketRefItem item = new BasketV2PostTestRequest.BasketRefItem();
        item.setReferenceType(ReferenceType.SKU);
        item.setReferenceId("test1234");
        item.setImageBaseUrl("https://avatars.mds.yandex.net/get-mpic/466729/img_id4470756757588870758/orig");
        item.setTitle("Title");
        List<SecondaryReference> refs = new ArrayList<>();
        refs.add(new SecondaryReference("wareMd5", "testware"));
        refs.add(new SecondaryReference("shopId", "123"));
        refs.add(new SecondaryReference("regionId", "123"));
        item.setSecondaryReferences(refs);
        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setUserAnyId(uuid);
        request.setUserIdType(UserIdType.UUID);
        addItem(request, item);

        BasketV2GetTestRequest getRequest = defaultGetItemsRequest.clone();
        getRequest.setRgb(MarketplaceColor.BLUE.getName());
        getRequest.setUserAnyId(uuid);
        getRequest.setUserIdType(UserIdType.UUID.name());
        getRequest.setRefs(Collections.singletonList(item));
        List<BasketV2PostTestRequest.BasketRefItem> result = getItems(getRequest);
        assertEquals(1, result.size());
        assertEquals("test1234", result.get(0).getReferenceId());
        assertThat(result.get(0).getSecondaryReferences(),
            Matchers.containsInAnyOrder(item.getSecondaryReferences().toArray()));
    }

    @Test
    public void testCorrespondenceSecRefAndData() {
        BasketReferenceItem item = new BasketReferenceItem();
        item.setSecondaryReferences(List.of(new SecondaryReference("wareMd5", "testware")));
        assertEquals(Map.of("wareMd5", "testware"), item.getData());

        item.setData(Map.of("shopId", "unknown"));
        assertEquals(List.of(new SecondaryReference("shopId", "unknown")), item.getSecondaryReferences());
    }

    @Test
    public void testAddEmptyData() throws Exception {
        String uuid = UUID.randomUUID().toString();
        BasketV2PostTestRequest.BasketRefItem item = new BasketV2PostTestRequest.BasketRefItem();
        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setUserAnyId(uuid);
        request.setUserIdType(UserIdType.UUID);
        addItem(request, item);

        List<BasketReferenceItem> basketItemsList = pgaasJdbcTemplate.query(
            "select * from basket_items",
            BasketReferenceItem::valueOf
        );

        assertEquals(1, basketItemsList.size());
        BasketReferenceItem basketReferenceItem = basketItemsList.get(0);

        assertTrue(basketReferenceItem.getSecondaryReferences().isEmpty());
        assertTrue(CollectionUtils.isEmpty(basketReferenceItem.getData()));
    }

    @Test
    public void testAddNonEmptyData() throws Exception {
        String uuid = UUID.randomUUID().toString();
        BasketV2PostTestRequest.BasketRefItem item = new BasketV2PostTestRequest.BasketRefItem();
        List<SecondaryReference> refs = new ArrayList<>();
        refs.add(new SecondaryReference("wareMd5", "testware"));
        refs.add(new SecondaryReference("shopId", "123"));
        refs.add(new SecondaryReference("regionId", "123"));
        item.setSecondaryReferences(refs);

        BasketV2PostTestRequest request = defaultAddItemRequest.clone();
        request.setUserAnyId(uuid);
        request.setUserIdType(UserIdType.UUID);
        addItem(request, item);

        List<BasketReferenceItem> basketItemsList = pgaasJdbcTemplate.query(
            "select * from basket_items",
            BasketReferenceItem::valueOf
        );

        assertEquals(1, basketItemsList.size());
        BasketReferenceItem basketReferenceItem = basketItemsList.get(0);

        assertThat(basketReferenceItem.getSecondaryReferences(),
            Matchers.containsInAnyOrder(item.getSecondaryReferences().toArray()));
        assertEquals(
            Map.of("wareMd5", "testware", "shopId", "123", "regionId", "123"),
            basketReferenceItem.getData()
        );
    }

    @Test
    public void testGetCount() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items2.json");
        MarketplaceColor color = MarketplaceColor.RED;
        for (BasketV2PostTestRequest.BasketRefItem item : items) {
            BasketV2PostTestRequest request = defaultAddItemRequest.clone();
            request.setUserAnyId(uuid);
            request.setUserIdType(UserIdType.UUID);
            addItem(request, item);
        }

        int itemsCount = basketV2Mvc.getItemsCount(BasketOwner.fromUuid(uuid), color, ok);
        assertEquals(7, itemsCount);

        // test caching
        pgaasJdbcTemplate.update("delete from basket_items where rgb = ?", color.getId());
        itemsCount = basketV2Mvc.getItemsCount(BasketOwner.fromUuid(uuid), color, ok);
        assertEquals(7, itemsCount);

        // check cache resets well
        resetCache();
        itemsCount = basketV2Mvc.getItemsCount(BasketOwner.fromUuid(uuid), color, ok);
        assertEquals(0, itemsCount);
    }

    @Test
    public void testGetCountOwnerId() throws Exception {
        String uuid = UUID.randomUUID().toString();
        List<BasketV2PostTestRequest.BasketRefItem> items = parseBasketItems("/v2/basket_items2.json");
        MarketplaceColor color = MarketplaceColor.RED;
        long ownerId = 0;
        int counter = 0;
        for (BasketV2PostTestRequest.BasketRefItem item : items) {
            BasketV2PostTestRequest request = defaultAddItemRequest.clone();
            request.setUserAnyId(uuid);
            request.setUserIdType(UserIdType.UUID);
            BasketReferenceItem addedItem = addItem(request, item);
            ownerId = addedItem.getOwnerId();
            item.setBasketItemId(addedItem.getId());

            // check cache works fine on add
            int itemsCount = basketV2Mvc.getItemsCount(BasketOwner.fromUuid(uuid), color, ok);
            assertEquals(++counter, itemsCount);
        }

        BasketOwner owner = BasketOwner.fromOwnerId(ownerId);
        basketV2Mvc.getItemsCountMvc(owner, color, badRequest);
    }

    private List<BasketV2PostTestRequest.BasketRefItem> parseBasketItems(String fileName) throws IOException {
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
        return  OBJECT_MAPPER.readValue(rawInput,
            OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class,
                BasketV2PostTestRequest.BasketRefItem.class));
    }

    public BasketReferenceItem addItem(BasketV2PostTestRequest request, BasketV2PostTestRequest.BasketRefItem item) throws Exception {
        return helper.addItem(request, item);
    }

    public List<BasketV2PostTestRequest.BasketRefItem> getItems(BasketV2GetTestRequest getRequest) throws Exception {
        return helper.getItems(getRequest);
    }


}
