package ru.yandex.market.pers.notify.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.notify.comparison.ComparisonService;
import ru.yandex.market.pers.notify.comparison.model.ComparisonCategoryResponseDto;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItem;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItemRequestDto;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItemResponseDto;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItemsLogEntity;
import ru.yandex.market.pers.notify.comparison.model.ComparisonResponseDto;
import ru.yandex.market.pers.notify.external.comparison.PersComparisonClient;
import ru.yandex.market.pers.notify.logging.TransactionalLogEvent;
import ru.yandex.market.pers.notify.logging.TransactionalLogEvent.Action;
import ru.yandex.market.pers.notify.logging.TransactionalLogEventTestConsumer;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.util.db.ConfigurationService;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
public class ComparisonControllerTest extends MarketUtilsMockedDbTest {


    private static final long UID = 234509767L;
    private static final String UUID = "dalkfjoeiwd-309428304sdlkfjd";
    private static final String YANDEXUID = "kjru439r49ujewq";
    /**
     * Компаратор для сравнения элементов списка в тестах
     * в отличии от {@link ComparisonItem#equals(Object)} срвнивает только значимые для тестирования поля
     */
    private static final Comparator<ComparisonItem> COMPARISON_ITEM_COMPARATOR =
        comparing(ComparisonItem::getCategoryId)
            .thenComparing(ComparisonItem::getProductId, nullsFirst(naturalOrder()))
            .thenComparing(ComparisonItem::getSku, nullsFirst(naturalOrder()))
            .thenComparing(ComparisonItem::getRegionId, nullsFirst(naturalOrder()));

    @Autowired
    @Qualifier("comparisonCache")
    private Cache<String, Object> comparisonCache;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionalLogEventTestConsumer<ComparisonItemsLogEntity> logEventTestConsumer;
    @Autowired
    protected ConfigurationService notifyConfigurationService;
    @Autowired
    protected PersComparisonClient persComparisonClient;

    @BeforeEach
    public void setUp() {
        logEventTestConsumer.clear();
        comparisonCache.invalidateAll();
    }

    @Test
    public void testSaveItemsOrder() throws Exception {
        String categoryId = "5461";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(categoryId, "5461"),
            new ComparisonItemRequestDto(categoryId, "98gfhfgh"),
            new ComparisonItemRequestDto(categoryId, "435ijh34"),
            new ComparisonItemRequestDto(categoryId, "23423dslf")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        logEventTestConsumer.clear();
        Collections.shuffle(items);
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .param("regionId", "213")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        checkLogEvents(Arrays.asList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    new Uid(UID),
                    Arrays.asList(
                        new ComparisonItem(1, 5461, "5461", null, 54L),
                        new ComparisonItem(1, 5461, "98gfhfgh", null, 54L),
                        new ComparisonItem(1, 5461, "435ijh34", null, 54L),
                        new ComparisonItem(1, 5461, "23423dslf", null, 54L)
                    )
                ),
                Action.REMOVE,
                ""
            ),
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    new Uid(UID),
                    Arrays.asList(
                        new ComparisonItem(1, 5461, "5461", null, 213L),
                        new ComparisonItem(1, 5461, "98gfhfgh", null, 213L),
                        new ComparisonItem(1, 5461, "435ijh34", null, 213L),
                        new ComparisonItem(1, 5461, "23423dslf", null, 213L)
                    )
                ),
                Action.ADD,
                ""
            )
        ));

        List<ComparisonItemRequestDto> actualItems = new ArrayList<>();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> actualItems.addAll(object(result.getResponse().getContentAsString()).getItems().stream()
                .flatMap(c -> c.getItems().stream().map(i -> new ComparisonItemRequestDto(c.getCategoryId(), i.getProductId())))
                .collect(Collectors.toList())))
        ;
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
            new ComparisonItemRequestDto(categoryId, "34534kj")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/items", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/category/%s/ordered", "YANDEX_UID", YANDEXUID, categoryId))
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
            new ComparisonItemRequestDto(categoryId1, "34r34t"),
            new ComparisonItemRequestDto(categoryId1, "98gfhfgh"),
            new ComparisonItemRequestDto(categoryId1, "435ijh34"),
            new ComparisonItemRequestDto(categoryId2, "23423dslf")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/items", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        Collections.shuffle(items);
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/category/%s/ordered", "UUID", UUID, categoryId1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().json(
                toJson(new Error("BAD_REQUEST", "Not all items belongs to specified category", 400)))
            )
        ;
    }

    @Test
    public void testSaveItemsOrderNoIdentity() throws Exception {
        String categoryId = "5461";
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto(categoryId, "34r34t"),
            new ComparisonItemRequestDto(categoryId, "98gfhfgh"),
            new ComparisonItemRequestDto(categoryId, "435ijh34"),
            new ComparisonItemRequestDto(categoryId, "23423dslf")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void testSaveItemsOrderTooManyItems() throws Exception {
        String categoryId = "123213";
        List<ComparisonItemRequestDto> items = new ArrayList<>();
        for (int i = 0; i < ComparisonService.MAX_ITEMS_IN_CATEGORY_CNT * 2; i++) {
            items.add(new ComparisonItemRequestDto(categoryId, java.util.UUID.randomUUID().toString()));
        }
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items.subList(0, 1))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        Collections.shuffle(items);
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/category/%s/ordered", "UID", UID, categoryId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        List<ComparisonItemRequestDto> actualItems = new ArrayList<>();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> actualItems.addAll(object(result.getResponse().getContentAsString()).getItems().stream()
                .flatMap(c -> c.getItems().stream().map(i -> new ComparisonItemRequestDto(c.getCategoryId(), i.getProductId())))
                .collect(Collectors.toList())))
        ;
        assertEquals(items.size(), actualItems.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i).getCategoryId(), actualItems.get(i).getCategoryId());
            assertEquals(items.get(i).getProductId(), actualItems.get(i).getProductId());
        }
    }

    @Test
    public void getItems() throws Exception {
        long categoryId = 765234L;
        String productId = "sdlfk32l";
        Long sku = 12345L;

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        AtomicLong date = new AtomicLong();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result ->
                date.set(object(result.getResponse().getContentAsString()).getItems().get(0).getLastUpdate().getTime()))
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(new ComparisonCategoryResponseDto(String.valueOf(categoryId), new Date(date.get()),
                    Collections.singletonList(new ComparisonItemResponseDto(productId, sku, new Date(date.get()))))))
            )))
        ;
    }

    @Test
    public void noItemsIfNothingCreated() throws Exception {
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", "98r4kjefwoiu")))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
    }

    @Test
    public void itemsIsEmptyIfNoItems() throws Exception {
        long categoryId = 98089123L;
        String productId = "98uj2k12w";

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "UUID", UUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/product/%s", "UUID", UUID, productId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
    }

    @Test
    public void comparisonCntIsZeroIfNothingCreated() throws Exception {
        mockMvc
            .perform(get(String.format("/comparison/%s/%s/categories/count", "YANDEX_UID", "87j3e2-3e")))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"count\":0}"))
        ;
    }

    @Test
    public void getComparisonsCnt() throws Exception {
        long categoryId1 = 98534098L;
        String productId1 = "98gdkjn,m";

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId1), productId1))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        long categoryId2 = 98872L;
        String productId2 = "09d.,34";

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId2), productId2))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        mockMvc
            .perform(get(String.format("/comparison/%s/%s/categories/count", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"count\":2}"))
        ;
    }

    @Test
    public void saveItem() throws Exception {
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto("342398", "3201801293", 123L))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(new Uid(UID), new ComparisonItem(1L, 342398L, "3201801293", 123L, 54L)),
                Action.ADD,
                ""
            )
        ));
    }

    @Test
    public void saveItems() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "23ojkdwu"),
            new ComparisonItemRequestDto("5461", "34r34t"),
            new ComparisonItemRequestDto("5461", "23423dslf"),
            new ComparisonItemRequestDto("5461", "23423dslf")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .param("regionId", "4534")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        Identity identity = new Uid(UID);
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    identity,
                    Arrays.asList(
                        new ComparisonItem(1L, 342398, "23ojkdwu", null, 4534L),
                        new ComparisonItem(1L, 5461, "34r34t", null, 4534L),
                        new ComparisonItem(1L, 5461, "23423dslf", null, 4534L)
                    )
                ),
                Action.ADD,
                ""
            )
        ));
    }

    @Test
    public void removeItem() throws Exception {
        long categoryId = 98239862L;
        String productId = "787823jkh";
        Long sku1 = 12345L;
        Long sku2 = 67890L;

        List<ComparisonItemRequestDto> items = Arrays.asList(
                new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku1),
                new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku2)
        );

        // create items for UUID
        mockMvc
                .perform(post(String.format("/comparison/%s/%s/items", "YANDEX_UID", YANDEXUID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(items)))
                .andDo(print())
                .andExpect(status().isCreated())
        ;

        Identity identity = new YandexUid(YANDEXUID);
        checkLogEvents(Collections.singletonList(
                TransactionalLogEvent.create(
                        new ComparisonItemsLogEntity(
                                identity,
                                Arrays.asList(
                                        new ComparisonItem(1L, categoryId, productId, sku1),
                                        new ComparisonItem(1L, categoryId, productId, sku2)
                                )
                        ),
                        Action.ADD,
                        ""
                )
        ));

        AtomicLong date = new AtomicLong();
        mockMvc
                .perform(get(String.format("/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
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

        logEventTestConsumer.clear();
        mockMvc
                .perform(delete(String.format("/comparison/%s/%s/product", "YANDEX_UID", YANDEXUID))
                        .param("productId", productId)
                        .param("sku", String.valueOf(sku2))
                )
                .andDo(print())
                .andExpect(status().isOk())
        ;
        checkLogEvents(Collections.singletonList(
                TransactionalLogEvent.create(
                        new ComparisonItemsLogEntity(
                                identity,
                                new ComparisonItem(1L, categoryId, productId, sku2)
                        ),
                        Action.REMOVE,
                        ""
                )
        ));

        mockMvc
                .perform(get(String.format("/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
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
        String productId = "787823jkh";
        Long sku = 12345L;

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "YANDEX_UID", YANDEXUID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId, sku))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        Identity identity = new YandexUid(YANDEXUID);
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    identity,
                    new ComparisonItem(1L, categoryId, productId, sku)
                ),
                Action.ADD,
                ""
            )
        ));

        AtomicLong date = new AtomicLong();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result ->
                date.set(object(result.getResponse().getContentAsString()).getItems().get(0).getLastUpdate().getTime()))
            .andExpect(content().json(toJson(
                new ComparisonResponseDto(Collections.singletonList(new ComparisonCategoryResponseDto(String.valueOf(categoryId), new Date(date.get()),
                    Collections.singletonList(new ComparisonItemResponseDto(productId, sku, new Date(date.get()))))))
            )))
        ;

        logEventTestConsumer.clear();
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/product/%s", "YANDEX_UID", YANDEXUID, productId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    identity,
                    new ComparisonItem(1L, categoryId, productId, sku)
                ),
                Action.REMOVE,
                ""
            )
        ));
    }

    @Test
    public void removeItemNotFoundIfNoItem() throws Exception {
        String productId = "knjrihu32";
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/product/%s", "YANDEX_UID", YANDEXUID, productId)))
            .andDo(print())
            .andExpect(status().isNotFound())
        ;
        checkLogEvents(Collections.emptyList());
    }

    @Test
    public void removeItems() throws Exception {
        long categoryId = 56423487L;
        String productId = "mnzxcbjreuy43827";

        mockMvc
            .perform(post(String.format("/comparison/%s/%s/item", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ComparisonItemRequestDto(String.valueOf(categoryId), productId))))
            .andDo(print())
            .andExpect(status().isCreated())
        ;
        logEventTestConsumer.clear();
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/category/%d", "UID", UID, categoryId)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    new Uid(UID),
                    new ComparisonItem(1L, categoryId, productId)
                ),
                Action.REMOVE,
                ""
            )
        ));
    }

    @Test
    public void removeUserItems() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "23ojkdwu"),
            new ComparisonItemRequestDto("5461", "34r34t"),
            new ComparisonItemRequestDto("5461", "23423dslf"),
            new ComparisonItemRequestDto("5461", "23423dslf")
        );
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/items", "UID", UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        logEventTestConsumer.clear();
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        Identity identity = new Uid(UID);
        checkLogEvents(Collections.singletonList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    identity,
                    Arrays.asList(
                        new ComparisonItem(1L, 342398, "23ojkdwu"),
                        new ComparisonItem(1L, 5461, "34r34t"),
                        new ComparisonItem(1L, 5461, "23423dslf")
                    )
                ),
                Action.REMOVE,
                ""
            )
        ));
    }

    @Test
    public void removeItemsNotFoundIfNoItem() throws Exception {
        Long categoryId = 2340234L;
        mockMvc
            .perform(delete(String.format("/comparison/%s/%s/category/%d", "YANDEX_UID", YANDEXUID, categoryId)))
            .andDo(print())
            .andExpect(status().isNotFound())
        ;
        checkLogEvents(Collections.emptyList());
    }

    @Test
    public void mergeToAuthorized() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("342398", "23ojkdwu"),
            new ComparisonItemRequestDto("5461", "34r34t"),
            new ComparisonItemRequestDto("5461", "23423dslf"),
            new ComparisonItemRequestDto("5461", "23423dslf")
        );

        // create items for UUID
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/items", "UUID", UUID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // get items for UUID
        AtomicReference<String> itemsString = new AtomicReference<>();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> itemsString.set(result.getResponse().getContentAsString()))
        ;
        assertNotEquals("{\"items\":[]}", itemsString.get());

        // no items for UID
        mockMvc
            .perform(get(String.format("/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;
        logEventTestConsumer.clear();
        // merge items from UUID to UID
        mockMvc
            .perform(patch(String.format("/comparison/%s/%s", "UUID", UUID))
                .param("userTypeTo", "UID").param("userIdTo", String.valueOf(UID)))
            .andDo(print())
            .andExpect(status().isOk())
        ;

        // items for UUID is empty
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;

        // items for UID is the same as for UUID
        mockMvc
            .perform(get(String.format("/comparison/%s/%d", "UID", UID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(itemsString.get()))
        ;
        checkLogEvents(Arrays.asList(
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    new Uuid(UUID),
                    Arrays.asList(
                        new ComparisonItem(1, 342398, "23ojkdwu", null, 54L),
                        new ComparisonItem(1, 5461, "34r34t", null, 54L),
                        new ComparisonItem(1, 5461, "23423dslf", null, 54L)
                    )
                ),
                Action.REMOVE,
                ""
            ),
            TransactionalLogEvent.create(
                new ComparisonItemsLogEntity(
                    new Uid(UID),
                    Arrays.asList(
                        new ComparisonItem(1, 342398, "23ojkdwu", null, 54L),
                        new ComparisonItem(1, 5461, "34r34t", null, 54L),
                        new ComparisonItem(1, 5461, "23423dslf", null, 54L)
                    )
                ),
                Action.ADD,
                ""
            )
        ));
    }

    @Test
    public void mergeToUnauthorized() throws Exception {
        List<ComparisonItemRequestDto> items = Arrays.asList(
            new ComparisonItemRequestDto("2348764", "ergvh934f"),
            new ComparisonItemRequestDto("432987234", "ouc90v9un32"),
            new ComparisonItemRequestDto("432987234", "dkljshf2r"),
            new ComparisonItemRequestDto("9874", "dslfsdfu893")
        );

        // create items for UUID
        mockMvc
            .perform(post(String.format("/comparison/%s/%s/items", "UUID", UUID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(items)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        // get items for UUID
        AtomicReference<String> itemsString = new AtomicReference<>();
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(result -> itemsString.set(result.getResponse().getContentAsString()))
        ;
        assertNotEquals("{\"items\":[]}", itemsString.get());

        // no items for YANDEXUID
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;

        // merge items from UUID to UID
        mockMvc
            .perform(patch(String.format("/comparison/%s/%s", "UUID", UUID))
                .param("userTypeTo", "YANDEX_UID").param("userIdTo", YANDEXUID)
                .param("regionId", "54")
            ).andDo(print())
            .andExpect(status().isOk())
        ;

        // items for UUID is empty
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "UUID", UUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json("{\"items\":[]}"))
        ;

        // items for YANDEXUID is the same as for UUID
        mockMvc
            .perform(get(String.format("/comparison/%s/%s", "YANDEX_UID", YANDEXUID)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(itemsString.get()))
        ;
    }

    @Test
    public void mergeFromAuthorized() throws Exception {
        mockMvc
            .perform(patch(String.format("/comparison/%s/%d", "UID", UID))
                .param("userTypeTo", "UUID").param("userIdTo", UUID))
            .andDo(print())
            .andExpect(status().isBadRequest());
        checkLogEvents(Collections.emptyList());
    }


    @Test
    public void saveUsePersComparisonOk() throws Exception {
        notifyConfigurationService.mergeValue(ComparisonController.USE_NEW_BACKEND_KEY, "true");
        comparisonCache.invalidateAll();
        when(persComparisonClient.saveItem(any(), any(), any(), any()))
            .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        ComparisonItemRequestDto item = new ComparisonItemRequestDto("342398", "3201801293", 123L);
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(item)))
            .andDo(print())
            .andExpect(status().isCreated())
        ;

        ArgumentCaptor<ComparisonItemRequestDto> itemCaptor = ArgumentCaptor.forClass(ComparisonItemRequestDto.class);
        verify(persComparisonClient).saveItem(
            eq(Identity.Type.UID),
            eq(String.valueOf(UID)),
            eq(54L),
            itemCaptor.capture());
        checkItemCapture(item, itemCaptor);
    }

    @Test
    public void saveUsePersComparisonFail() throws Exception {
        notifyConfigurationService.mergeValue(ComparisonController.USE_NEW_BACKEND_KEY, "true");
        comparisonCache.invalidateAll();
        when(persComparisonClient.saveItem(any(), any(), any(), any()))
            .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        ComparisonItemRequestDto item = new ComparisonItemRequestDto("164", "333", 123L);
        mockMvc
            .perform(post(String.format("/comparison/%s/%d/item", "UID", UID))
                .param("regionId", "54")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(item)))
            .andDo(print())
            .andExpect(status().isNotFound())
        ;

        ArgumentCaptor<ComparisonItemRequestDto> itemCaptor = ArgumentCaptor.forClass(ComparisonItemRequestDto.class);
        verify(persComparisonClient).saveItem(
            eq(Identity.Type.UID),
            eq(String.valueOf(UID)),
            eq(54L),
            itemCaptor.capture());
        checkItemCapture(item, itemCaptor);
    }

    private void checkItemCapture(ComparisonItemRequestDto item, ArgumentCaptor<ComparisonItemRequestDto> itemCaptor) {
        ComparisonItemRequestDto actualItem = itemCaptor.getValue();
        assertEquals(actualItem.getCategoryId(), item.getCategoryId());
        assertEquals(actualItem.getProductId(), item.getProductId());
        assertEquals(actualItem.getSku(), item.getSku());
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
            assertEquals(expected.getEntity().getIdentity(), actual.getEntity().getIdentity());
            Set<ComparisonItem> expectedItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            expectedItems.addAll(expected.getEntity().getItems());
            Set<ComparisonItem> actualItems = new TreeSet<>(COMPARISON_ITEM_COMPARATOR);
            actualItems.addAll(actual.getEntity().getItems());
            assertEquals(expectedItems, actualItems);
        }
    }
}
