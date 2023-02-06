package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.model.bo.RequestItemFilter;
import ru.yandex.market.ff.model.bo.TotalItemsSumInfo;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Интеграционные тесты для {@link RequestItemService}.
 *
 * @author avetokhin 17/01/18.
 */
class RequestItemServiceTest extends IntegrationTest {
    private static final Pageable PAGEABLE = new PageRequest(0, 5, Sort.Direction.ASC, "id");

    private static final long REQUEST_ID = 1;

    @Autowired
    private RequestItemService requestItemService;

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void pageByArticle() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(REQUEST_ID);
        filter.setArticle("abc");
        checkPage(filter, Arrays.asList(1L, 2L));
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getTotalItemsInfo() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(1L);
        filter.setArticle("abc");
        final TotalItemsSumInfo totalItemsSumInfo = requestItemService.getTotalItemsInfo(filter, false, false);
        assertThat(totalItemsSumInfo).isNotNull();
        assertThat(totalItemsSumInfo.getTotalCount()).isEqualTo(16L);
        assertThat(totalItemsSumInfo.getTotalSurplusCount()).isNull();
        assertThat(totalItemsSumInfo.getTotalDefectCount()).isEqualTo(4L);
        assertThat(totalItemsSumInfo.getTotalFactCount()).isEqualTo(12L);
        assertThat(totalItemsSumInfo.getTotalSupplyPrice()).isEqualTo(new BigDecimal("1451.50"));
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getTotalItemsInfoWithNotNullSurplus() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(2L);
        filter.setArticle("abc");
        final TotalItemsSumInfo totalItemsSumInfo = requestItemService.getTotalItemsInfo(filter, false, false);
        assertThat(totalItemsSumInfo).isNotNull();
        assertThat(totalItemsSumInfo.getTotalCount()).isEqualTo(3L);
        assertThat(totalItemsSumInfo.getTotalSurplusCount()).isEqualTo(1L);
        assertThat(totalItemsSumInfo.getTotalDefectCount()).isNull();
        assertThat(totalItemsSumInfo.getTotalSupplyPrice()).isEqualTo(new BigDecimal("151.50"));
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getTotalItemsInfoForItemsWithValidationErrorsWhenThereAreNoSuchItems() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(2L);
        final TotalItemsSumInfo totalItemsSumInfo = requestItemService.getTotalItemsInfo(filter, true, false);
        assertThat(totalItemsSumInfo).isNotNull();
        assertThat(totalItemsSumInfo.getTotalCount()).isNull();
        assertThat(totalItemsSumInfo.getTotalSurplusCount()).isNull();
        assertThat(totalItemsSumInfo.getTotalDefectCount()).isNull();
        assertThat(totalItemsSumInfo.getTotalSupplyPrice()).isNull();
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getItemsWithValidationErrors() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(1L);

        Page<RequestItem> items = requestItemService.findItems(filter, true, PAGEABLE, false);

        List<RequestItem> content = items.getContent();
        assertThat(items).isNotNull();
        assertThat(content.size()).isEqualTo(3);
        assertTrue(content.stream().anyMatch(item -> item.getId().equals(1L)));
        assertTrue(content.stream().anyMatch(item -> item.getId().equals(3L)));
        assertTrue(content.stream().anyMatch(item -> item.getId().equals(5L)));
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getItemsWithValidationErrorsAndDefectsOnly() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(1L);
        filter.setHasDefects(true);

        Page<RequestItem> items = requestItemService.findItems(filter, true, PAGEABLE, false);

        List<RequestItem> content = items.getContent();
        assertThat(items).isNotNull();
        assertThat(content.size()).isEqualTo(1);
        assertTrue(content.stream().anyMatch(item -> item.getId().equals(1L)));
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getItemsWithPlanOrFact() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(1L);
        filter.setHasPlanOrFact(true);

        Page<RequestItem> items = requestItemService.findItems(filter, false, PAGEABLE, false);

        List<RequestItem> content = items.getContent();
        assertThat(content.size()).isEqualTo(4);
    }

    @Test
    @DatabaseSetup("classpath:service/request-item/items.xml")
    void getItemsWithoutPlanOrFact() {
        final RequestItemFilter filter = new RequestItemFilter();
        filter.setRequestId(1L);
        filter.setHasPlanOrFact(false);

        Page<RequestItem> items = requestItemService.findItems(filter, false, PAGEABLE, false);

        List<RequestItem> content = items.getContent();
        assertThat(content.size()).isEqualTo(5);
    }


    /**
     * Метод извлекает строки заявки с использованием фильтра и проверяет их валидность.
     * Для простоты проверку делает просто по идентификатору строки заявки.
     *
     * @param filter   фильтр
     * @param expected ожидаемые идентификаторы строк заявок
     */
    private void checkPage(final RequestItemFilter filter, List<Long> expected) {
        final Page<RequestItem> page = requestItemService.findItems(filter, false, PAGEABLE, false);
        final int expectedSize = expected.size();

        assertThat(page)
            .isNotNull()
            .hasSize(expectedSize);

        final List<RequestItem> content = page.getContent();
        assertThat(content)
            .isNotNull()
            .hasSize(expectedSize);

        for (int i = 0; i < expectedSize; i++) {
            assertThat(content.get(i).getId()).isEqualTo(expected.get(i));
        }
    }


    @Test
    @DatabaseSetup("classpath:repository/request-transfer-item/before-with-errors.xml")
    void findAllByRequestIdWithFetchedInternalErrors() {

        List<RequestItem> items = requestItemService.findAllByRequestIdWithInternalErrorsFetched(7L);
        List<Long> itemIds = items.stream().map(RequestItem::getId).collect(Collectors.toList());

        MatcherAssert.assertThat(items.size(), equalTo(2));
        assertEquals(itemIds, Arrays.asList(1L, 2L));

        Optional<RequestItem> itemOptional = items.stream().filter(z -> z.getId().equals(1L)).findFirst();
        assertTrue(itemOptional.isPresent());
        RequestItem item = itemOptional.get();
        assertEquals(item.getRequestId(), Long.valueOf(7L));
        assertEquals(item.getCount(), 3);
        assertEquals(item.getFactCount(), Integer.valueOf(3));
        assertEquals(item.getSku(), Long.valueOf(190L));

        List<RequestItemError> errors = item.getRequestItemErrorList();
        assertEquals(errors.size(), 1);
        RequestItemError error = errors.get(0);
        assertEquals(error.getErrorType(), RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND);
    }

    @Test
    @DatabaseSetup("classpath:repository/request-transfer-item/before-with-errors.xml")
    void findAllByRequestIdInWithFetchedInternalErrors() {

        List<RequestItem> items =
                requestItemService.findAllByRequestIdInWithInternalErrorsFetched(Arrays.asList(7L, 10L))
                .values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        List<Long> itemIds = items.stream().map(RequestItem::getId).collect(Collectors.toList());

        MatcherAssert.assertThat(items.size(), equalTo(3));
        assertEquals(itemIds, Arrays.asList(1L, 2L, 3L));

        Optional<RequestItem> itemOptional = items.stream().filter(z -> z.getId().equals(1L)).findFirst();
        assertTrue(itemOptional.isPresent());
        RequestItem item = itemOptional.get();
        assertEquals(item.getRequestId(), Long.valueOf(7L));
        assertEquals(item.getCount(), 3);
        assertEquals(item.getFactCount(), Integer.valueOf(3));
        assertEquals(item.getSku(), Long.valueOf(190L));

        List<RequestItemError> errors = item.getRequestItemErrorList();
        assertEquals(errors.size(), 1);
        RequestItemError error = errors.get(0);
        assertEquals(error.getErrorType(), RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND);
    }


}
