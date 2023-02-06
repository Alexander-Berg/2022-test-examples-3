package ru.yandex.market.pers.comparison.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.comparison.PersComparisonTest;
import ru.yandex.market.pers.comparison.dto.ComparisonCategoryResponseDto;
import ru.yandex.market.pers.comparison.dto.ComparisonItemRequestDto;
import ru.yandex.market.pers.comparison.dto.ComparisonItemResponseDto;
import ru.yandex.market.pers.comparison.dto.ComparisonResponseDto;
import ru.yandex.market.pers.comparison.logging.TransactionalLogEvent;
import ru.yandex.market.pers.comparison.model.ComparisonItem;
import ru.yandex.market.pers.comparison.model.ComparisonItemsLogEntity;
import ru.yandex.market.pers.comparison.model.UserType;
import ru.yandex.market.pers.comparison.service.ComparisonServiceNew;
import ru.yandex.market.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 22.11.16
 */
public class ComparisonControllerTest extends PersComparisonTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComparisonServiceNew comparisonServicePg;
    @Autowired
    @Qualifier("transactionalLogEventConsumer")
    private TransactionalLogEventTestConsumer<ComparisonItemsLogEntity> logEventTestConsumer;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        logEventTestConsumer.clear();
    }

    @Test
    public void testSaveItemsOrder() throws Exception {
        long categoryIdLong = 5461L;
        String categoryId = "5461";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(categoryId, "5461"),
            new ComparisonItemRequestDto(categoryId, "9810187"),
            new ComparisonItemRequestDto(categoryId, "43588834"),
            new ComparisonItemRequestDto(categoryId, "2342390")
        );
        // save items
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        logEventTestConsumer.clear();
        Collections.shuffle(items);
        // save items ordered
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .param("regionId", "213")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        checkLogEvents(Arrays.asList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(categoryIdLong, "5461", null, 54L),
                        new ComparisonItem(categoryIdLong, "9810187", null, 54L),
                        new ComparisonItem(categoryIdLong, "43588834", null, 54L),
                        new ComparisonItem(categoryIdLong, "2342390", null, 54L)
                    )
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            ),
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(categoryIdLong, "5461", null, 213L),
                        new ComparisonItem(categoryIdLong, "9810187", null, 213L),
                        new ComparisonItem(categoryIdLong, "43588834", null, 213L),
                        new ComparisonItem(categoryIdLong, "2342390", null, 213L)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));

        List<ComparisonItemRequestDto> actualItems = new ArrayList<>();
        // check items
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> actualItems.addAll(object(result.getResponse().getContentAsString()).getItems().stream()
                .flatMap(c -> c.getItems().stream().map(i -> new ComparisonItemRequestDto(c.getCategoryId(),
                    i.getProductId())))
                .collect(Collectors.toList())))
        ;
        assertEquals(items.size(), comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, categoryIdLong));
        assertEquals(items.size(), actualItems.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i).getCategoryId(), actualItems.get(i).getCategoryId());
            assertEquals(items.get(i).getProductId(), actualItems.get(i).getProductId());
        }
    }

    @Test
    public void testSaveItemsOrderNoItemsInList() throws Exception {
        String categoryId = "9238784";
        List<ComparisonItemRequestDto> items = Collections.singletonList(
            new ComparisonItemRequestDto(categoryId, "3453401")
        );
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(items.size(), comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID,
            Long.parseLong(categoryId)));

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/category/%s/ordered", "YANDEX_UID", YANDEXUID, categoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Collections.emptyList())))
            .andDo(print())
            .andExpect(status().isBadRequest())
        ;
    }

    @Test
    public void testSaveItemsOrderDifferentCategories() throws Exception {
        String categoryId1 = "5461";
        String categoryId2 = "453534";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(categoryId1, "345342"),
            new ComparisonItemRequestDto(categoryId1, "9810187"),
            new ComparisonItemRequestDto(categoryId1, "43588834"),
            new ComparisonItemRequestDto(categoryId2, "2342390")
        );
        // save items
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(items.size() - 1,
            comparisonServicePg.getItemsCnt(UserType.UUID, UUID, Long.parseLong(categoryId1)));
        assertEquals(1,
            comparisonServicePg.getItemsCnt(UserType.UUID, UUID, Long.parseLong(categoryId2)));

        Collections.shuffle(items);
        String response = mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/category/%s/ordered", "UUID", UUID, categoryId1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();

        assertTrue(response.contains("Not all items belongs to specified category"));
    }

    @Test
    public void testSaveItemsOrderNoIdentity() throws Exception {
        String categoryId = "5461";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(categoryId, "345342"),
            new ComparisonItemRequestDto(categoryId, "9810187"),
            new ComparisonItemRequestDto(categoryId, "43588834"),
            new ComparisonItemRequestDto(categoryId, "2342390")
        );
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(4, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, Long.parseLong(categoryId)));
    }

    @Test
    public void testSaveItemsOrderTooManyItems() throws Exception {
        String categoryId = "123213";
        long productId = 100;
        List<ComparisonItemRequestDto> items = new ArrayList<>();
        for (int i = 0; i < ComparisonServiceNew.MAX_ITEMS_IN_CATEGORY_CNT * 2; i++) {
            items.add(new ComparisonItemRequestDto(categoryId, String.valueOf(productId + i)));
        }
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items.subList(0, 1)))) // product with id = 100
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, Long.parseLong(categoryId)));

        Collections.shuffle(items);
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(items.size(), comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, Long.parseLong(categoryId)));

        List<ComparisonItemRequestDto> actualItems = new ArrayList<>();
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> actualItems.addAll(object(result.getResponse().getContentAsString()).getItems().stream()
                .flatMap(c -> c.getItems().stream().map(i -> new ComparisonItemRequestDto(c.getCategoryId(),
                    i.getProductId())))
                .collect(Collectors.toList())))
        ;
        assertEquals(items.size(), actualItems.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i).getCategoryId(), actualItems.get(i).getCategoryId());
            assertEquals(items.get(i).getProductId(), actualItems.get(i).getProductId());
        }
        assertEquals(items.size(), comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, Long.parseLong(categoryId)));
    }

    @Test
    public void getItems() throws Exception {
        long categoryId = 765234L;
        String productId = "1040329";
        Long sku = 12345L;

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        AtomicLong date = new AtomicLong();
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result ->
                date.set(object(result.getResponse().getContentAsString()).getItems().get(0).getLastUpdate().getTime()))
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(
                    new ComparisonCategoryResponseDto(String.valueOf(categoryId), new Date(date.get()),
                    Collections.singletonList(new ComparisonItemResponseDto(productId, sku, new Date(date.get()))))))
            )))
        ;
    }

    @Test
    public void noItemsIfNothingCreated() throws Exception {
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", "98r4kjefwoiu")))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
    }

    @Test
    public void itemsIsEmptyIfNoItems() throws Exception {
        long categoryId = 98089123L;
        String productId = "98720123";

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/product/%s", "UUID", UUID, productId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
    }

    @Test
    public void comparisonCntIsZeroIfNothingCreated() throws Exception {
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s/categories/count", "YANDEX_UID", "87j3e2-3e")))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"count\":0}"))
        ;
    }

    @Test
    public void getComparisonsCnt() throws Exception {
        long categoryId1 = 98534098L;
        String productId1 = "9045822";

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId1), productId1))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        long categoryId2 = 98872L;
        String productId2 = "1096634";

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId2), productId2))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s/categories/count", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"count\":2}"))
        ;
    }

    @Test
    public void saveItem() throws Exception {
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto("342398", "3201801293", 123L))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    new ComparisonItem(342398L, "3201801293", 123L, 54L)),
                    TransactionalLogEvent.Action.ADD,
                ""
            )
        ));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398L));
    }

    @Test
    public void saveItems() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );
        // save items
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "4534")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        // check events
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(342398L, "2344565", null, 4534L),
                        new ComparisonItem(5461L, "345342", null, 4534L),
                        new ComparisonItem(5461L, "2342390", null, 4534L)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));
        // check psql
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
    }

    @Test
    public void saveItemsTwice() throws Exception {
        List<ComparisonItemRequestDto> items1 = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );
        // save items
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "4534")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items1)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        List<ComparisonItemRequestDto> items2 = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("466", "100400")
        );
        // save items
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "4534")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items2)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        // check events
        checkLogEvents(Arrays.asList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(342398L, "2344565", null, 4534L),
                        new ComparisonItem(5461L, "345342", null, 4534L),
                        new ComparisonItem(5461L, "2342390", null, 4534L)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            ),
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(466L, "100400", null, 4534L)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));
        // check psql
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
    }

    @Test
    public void removeItem() throws Exception {
        long categoryId = 98239862;
        String productId = "78782344";
        Long sku1 = 12345L;
        Long sku2 = 67890L;

        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku1),
            new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku2)
        );

        // create items for UUID
        mockMvc.perform(post(String.format("/api/comparison/%s/%s/items", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.YANDEX_UID, YANDEXUID,
                    Arrays.asList(
                        new ComparisonItem(categoryId, productId, sku1),
                        new ComparisonItem(categoryId, productId, sku2)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));

        AtomicLong date = new AtomicLong();
        mockMvc.perform(get(String.format("/api/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result ->
                date.set(object(result.getResponse().getContentAsString()).getItems().get(0).getLastUpdate().getTime()))
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(
                    new ComparisonCategoryResponseDto(
                        String.valueOf(categoryId), new Date(date.get()),
                        Arrays.asList(
                            new ComparisonItemResponseDto(productId, sku1, new Date(date.get())),
                            new ComparisonItemResponseDto(productId, sku2, new Date(date.get()))
                        )
                    )))
            )))
        ;
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, categoryId));

        logEventTestConsumer.clear();
        mockMvc.perform(delete(String.format("/api/comparison/%s/%s/product", "YANDEX_UID", YANDEXUID))
                .param("productId", productId)
                .param("sku", String.valueOf(sku2))
            )
            .andDo(print())
            .andExpect(status().isOk())
        ;
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.YANDEX_UID, YANDEXUID,
                    new ComparisonItem(categoryId, productId, sku2)
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            )
        ));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, categoryId));

        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(
                    new ComparisonCategoryResponseDto(
                        String.valueOf(categoryId), new Date(date.get()),
                        Collections.singletonList(
                            new ComparisonItemResponseDto(productId, sku1, new Date(date.get()))
                        )
                    )))
            )))
        ;
    }

    @Test
    public void removeItemsByProductId() throws Exception {
        long categoryId = 98239862L;
        String productId = "78782344";
        Long sku = 12345L;
        assertEquals(0, logEventTestConsumer.getEvents().size());

        // save item
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        // check events
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.YANDEX_UID, YANDEXUID,
                    new ComparisonItem(categoryId, productId, sku)
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));

        AtomicLong date = new AtomicLong();
        // check comparison list
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result ->
                date.set(object(result.getResponse().getContentAsString()).getItems().get(0).getLastUpdate().getTime()))
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(
                    new ComparisonCategoryResponseDto(String.valueOf(categoryId), new Date(date.get()),
                    Collections.singletonList(new ComparisonItemResponseDto(productId, sku, new Date(date.get()))))))
            )))
        ;
        // check psql
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, categoryId));

        logEventTestConsumer.clear();
        // delete item
        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/product/%s", "YANDEX_UID", YANDEXUID, productId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;
        // check events
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.YANDEX_UID, YANDEXUID,
                    new ComparisonItem(categoryId, productId, sku)
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            )
        ));
        // check psql
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, categoryId));
    }

    @Test
    public void removeItemNotFoundIfNoItem() throws Exception {
        String productId = "45632";
        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/product/%s", "YANDEX_UID", YANDEXUID, productId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;
        checkLogEvents(Collections.emptyList());
    }

    @Test
    public void removeItems() throws Exception {
        long categoryId = 56423487;
        String productId = "1043827";

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/item", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, categoryId));
        logEventTestConsumer.clear();
        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/category/%d", "UID", UID, categoryId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    new ComparisonItem(categoryId, productId)
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            )
        ));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, categoryId));
    }

    @Test
    public void removeUserItems() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/items", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        // because one of them is duplicated
        assertEquals(items.size() - 2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));

        logEventTestConsumer.clear();
        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
        ;
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(342398, "2344565"),
                        new ComparisonItem(5461, "345342"),
                        new ComparisonItem(5461, "2342390")
                    )
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            )
        ));
    }

    @Test
    public void removeItemsNotFoundIfNoItem() throws Exception {
        Long categoryId = 2340234L;
        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/category/%d", "YANDEX_UID", YANDEXUID, categoryId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;
        checkLogEvents(Collections.emptyList());
    }

    @Test
    public void mergeToAuthorized() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );

        // create items for UUID
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "UUID", UUID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // get items for UUID
        AtomicReference<String> itemsString = new AtomicReference<>();
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> itemsString.set(result.getResponse().getContentAsString()))
        ;
        assertNotEquals("{\"items\":[]}", itemsString.get());

        // no items for UID
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
        logEventTestConsumer.clear();
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));
        // merge items from UUID to UID
        mockMvc
            .perform(patch(String.format("/api/comparison/%s/%s", "UUID", UUID))
                .param("userTypeTo", "UID").param("userIdTo", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        // items for UUID is empty
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;

        // items for UID is the same as for UUID
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(itemsString.get()))
        ;
        checkLogEvents(Arrays.asList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UUID, UUID,
                    Arrays.asList(
                        new ComparisonItem(342398L, "2344565", null, 54L),
                        new ComparisonItem(5461L, "345342", null, 54L),
                        new ComparisonItem(5461L, "2342390", null, 54L)
                    )
                ),
                TransactionalLogEvent.Action.REMOVE,
                ""
            ),
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    Arrays.asList(
                        new ComparisonItem(342398L, "2344565", null, 54L),
                        new ComparisonItem(5461L, "345342", null, 54L),
                        new ComparisonItem(5461L, "2342390", null, 54L)
                    )
                ),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));
    }

    @Test
    public void mergeToAuthorizedWithIntersection() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "2344565"),
            new ComparisonItemRequestDto("5461", "345342"),
            new ComparisonItemRequestDto("5461", "2342390"),
            new ComparisonItemRequestDto("5461", "2342390")
        );

        // create items for UUID
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "UUID", UUID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // create items for UID
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items.subList(1,3))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // get items for UUID
        ComparisonResponseDto uuidItems = FormatUtils.fromJson(mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(), ComparisonResponseDto.class);

        assertEquals(3, uuidItems.getItems().stream().mapToInt(x->x.getItems().size()).sum());

        // also items for UID
        ComparisonResponseDto uidItems = FormatUtils.fromJson(mockMvc
            .perform(get(String.format("/api/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(), ComparisonResponseDto.class);
        ;
        assertEquals(2, uidItems.getItems().stream().mapToInt(x->x.getItems().size()).sum());

        logEventTestConsumer.clear();
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));

        // merge items from UUID to UID
        mockMvc
            .perform(patch(String.format("/api/comparison/%s/%s", "UUID", UUID))
                .param("userTypeTo", "UID").param("userIdTo", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().isOk());


        // items for UUID is empty
        ComparisonResponseDto uuidItemsResult = FormatUtils.fromJson(mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(), ComparisonResponseDto.class);

        assertEquals(0, uuidItemsResult.getItems().stream().mapToInt(x->x.getItems().size()).sum());

        // items for UID is the same as for UUID
        ComparisonResponseDto uidItemsResult = FormatUtils.fromJson(mockMvc
            .perform(get(String.format("/api/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(), ComparisonResponseDto.class);

        assertEquals(3, uidItemsResult.getItems().stream().mapToInt(x->x.getItems().size()).sum());

        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 5461));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398));
    }

    @Test
    public void mergeToUnauthorized() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("2348764", "109431"),
            new ComparisonItemRequestDto("432987234", "190932"),
            new ComparisonItemRequestDto("432987234", "1002100"),
            new ComparisonItemRequestDto("9874", "100893")
        );

        // create items for UUID
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%s/items", "UUID", UUID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // get items for UUID, not empty
        AtomicReference<String> itemsString = new AtomicReference<>();
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> itemsString.set(result.getResponse().getContentAsString()))
        ;
        assertNotEquals("{\"items\":[]}", itemsString.get());
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 432987234));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 2348764));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 9874));

        // no items for YANDEXUID
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 432987234));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 2348764));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 9874));

        // merge items from UUID to YANDEX_UID
        mockMvc
            .perform(patch(String.format("/api/comparison/%s/%s", "UUID", UUID))
                .param("userTypeTo", "YANDEX_UID").param("userIdTo", YANDEXUID)
                .param("regionId", "54")
            ).andDo(print())
            .andExpect(status().isOk())
        ;

        // get items for UUID, empty
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 432987234));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 2348764));
        assertEquals(0, comparisonServicePg.getItemsCnt(UserType.UUID, UUID, 9874));

        // get items for YANDEXUID is the same as for UUID, not empty
        mockMvc
            .perform(get(String.format("/api/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(itemsString.get()))
        ;
        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 432987234));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 2348764));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.YANDEX_UID, YANDEXUID, 9874));
    }

    @Test
    public void mergeFromAuthorized() throws Exception {
        mockMvc
            .perform(patch(String.format("/api/comparison/%s/%d", "UID", UID))
                .param("userTypeTo", "UUID").param("userIdTo", UUID))
            .andDo(print())
            .andExpect(status().isBadRequest());
        checkLogEvents(Collections.emptyList());
    }

    @Test
    public void saveItemApi() throws Exception {
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto("342398", "3201801293", 123L))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    UserType.UID, UID_STR,
                    new ComparisonItem(342398L, "3201801293", 123L, 54L)),
                TransactionalLogEvent.Action.ADD,
                ""
            )
        ));
        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398L));
    }

    @Test
    public void checkDeleteNullItems() throws Exception {
        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto("342398", "3201801293", null))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(post(String.format("/api/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto("342398", "3201801293", 12345L))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        assertEquals(2, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398L));

        mockMvc
            .perform(delete(String.format("/api/comparison/%s/%s/product", "UID", UID))
                .param("productId", "3201801293")
                .param("sku", new String[] {null}))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        assertEquals(1, comparisonServicePg.getItemsCnt(UserType.UID, UID_STR, 342398L));
    }


    private ComparisonResponseDto object(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, ComparisonResponseDto.class);
    }

    private void checkLogEvents(List<TransactionalLogEvent<ComparisonItemsLogEntity>> expecteds) {
        assertEquals(expecteds.size(), logEventTestConsumer.getEvents().size());
        for (int i = 0; i < expecteds.size(); ++i) {
            TransactionalLogEvent<ComparisonItemsLogEntity> expected = expecteds.get(i);
            TransactionalLogEvent<ComparisonItemsLogEntity> actual = logEventTestConsumer.getEvents().get(i);
            assertEquals(expected.getAction(), actual.getAction());
            Set<ComparisonItem> expectedItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            expectedItems.addAll(expected.getEntity().getItems());
            Set<ComparisonItem> actualItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            actualItems.addAll(actual.getEntity().getItems());
            assertEquals(expectedItems, actualItems);
            assertTrue(actualItems.stream().allMatch(it -> it.getCreationTime() != null));
        }
    }
}
