package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestYardDTO;
import ru.yandex.market.ff.client.dto.ShopRequestYardDTOContainer;
import ru.yandex.market.ff.client.enums.RequestDocumentType;
import ru.yandex.market.ff.client.enums.RequestItemAttribute;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.model.bo.AggregatedRequestItemsInfo;
import ru.yandex.market.ff.model.bo.CalendaringRequestItemsAggregatedFields;
import ru.yandex.market.ff.model.bo.ShopRequestAdditionalFieldsConfig;
import ru.yandex.market.ff.model.bo.ShopRequestFilter;
import ru.yandex.market.ff.model.bo.ShopRequestYardFilter;
import ru.yandex.market.ff.model.entity.ShopRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для {@link ShopRequestFetchingService}.
 *
 * @author avetokhin 06/12/17.
 */
class ShopRequestFetchingServiceTest extends IntegrationTest {

    private static final Pageable PAGEABLE = new PageRequest(0, 5);
    private static final ShopRequestAdditionalFieldsConfig ADDITIONAL_FIELDS_CONFIG_EMPTY =
            new ShopRequestAdditionalFieldsConfig();

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByShopIdAndStatus() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setStatuses(Collections.singletonList(RequestStatus.CREATED));
        filter.setShopIds(Arrays.asList(1L, 4L, 20L));

        checkPage(filter, Arrays.asList(1L, 6L, 7L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByShopIdAndStatusAndType() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setStatuses(Collections.singletonList(RequestStatus.SENT_TO_SERVICE));
        filter.setTypes(Collections.singletonList(RequestType.WITHDRAW.getId()));
        filter.setShopIds(Arrays.asList(1L, 2L, 20L));

        checkPage(filter, Collections.singletonList(3L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByStartDate() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setRequestDateFrom(LocalDate.of(2017, 1, 1));

        checkPage(filter, Arrays.asList(1L, 4L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByBetweenDatesAndId() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setRequestDateFrom(LocalDate.of(2016, 1, 1));
        filter.setRequestDateTo(LocalDate.of(2017, 1, 1));

        filter.setRequestIds(Arrays.asList("2", "id1", "id2"));

        checkPage(filter, Arrays.asList(2L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/request_with_xdoc_date.xml")
    void pageByBetweenDates() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setRequestDateFrom(LocalDate.of(2016, 1, 1));
        filter.setRequestDateTo(LocalDate.of(2017, 1, 1));


        checkPage(filter, Arrays.asList(2L, 3L, 5L, 6L, 28L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByHasDefects() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setHasDefects(true);
        checkPage(filter, Arrays.asList(1L, 4L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByHasSurplus() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setHasSurplus(true);
        checkPage(filter, Arrays.asList(1L, 2L, 4L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByHasShortage() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setHasShortage(true);
        checkPage(filter, Collections.singletonList(2L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByHasAnyProblem() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setHasShortage(true);
        filter.setHasDefects(true);
        checkPage(filter, Arrays.asList(1L, 2L, 4L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByArticle() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setArticle("sku1");
        filter.setStatuses(Collections.singletonList(RequestStatus.CREATED));
        checkPage(filter, Arrays.asList(1L, 4L, 7L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void pageByServiceId() {
        final ShopRequestFilter filter = new ShopRequestFilter();
        filter.setServiceIds(Arrays.asList(101L, 102L));
        checkPage(filter, Arrays.asList(4L, 5L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void exists() {
        assertThat(shopRequestFetchingService.requestExists(1L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(2L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(3L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(4L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(5L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(6L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(7L)).isEqualTo(true);
        assertThat(shopRequestFetchingService.requestExists(8L)).isEqualTo(false);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void getRequestsByExternalRequestIdAndType() {
        Optional<ShopRequest> foundRequest =
                shopRequestFetchingService.getRequestByExternalRequestId(4, "abc123", RequestType.SUPPLY);
        assertThat(foundRequest).isPresent();
        ShopRequest request = foundRequest.orElseThrow(IllegalStateException::new);
        assertThat(request.getId()).isEqualTo(6L);
        assertThat(request.getExternalRequestId()).isEqualTo("abc123");
        assertThat(request.getExternalOperationType()).isEqualTo(ExternalOperationType.MOVE);
        assertThat(request.getType()).isEqualTo(RequestType.SUPPLY);

        foundRequest = shopRequestFetchingService.getRequestByExternalRequestId(4, "abc123", RequestType.WITHDRAW);
        assertThat(foundRequest).isPresent();
        request = foundRequest.orElseThrow(IllegalStateException::new);
        assertThat(request.getId()).isEqualTo(5L);
        assertThat(request.getExternalRequestId()).isEqualTo("abc123");
        assertThat(request.getExternalOperationType()).isEqualTo(ExternalOperationType.MOVE);
        assertThat(request.getType()).isEqualTo(RequestType.WITHDRAW);

        foundRequest = shopRequestFetchingService.getRequestByExternalRequestId(4, "xyz456", RequestType.WITHDRAW);
        assertThat(foundRequest).isEmpty();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByExternalRequestId() {
        final ShopRequestFilter filter = new ShopRequestFilter();

        // 2-записи для первого и 0 для второго
        filter.setRequestIds(Arrays.asList("abc123", "abc555"));
        checkPage(filter, List.of(5L, 6L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByExternalRequestIdOrByRequestId() {
        final ShopRequestFilter filter = new ShopRequestFilter();

        // 1 запись для первого и 2 для второго
        filter.setRequestIds(Arrays.asList("1", "abc123"));
        checkPage(filter, List.of(1L, 5L, 6L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByExternalRequestIdOrByRequestIdOrByServiceRequestId() {
        final ShopRequestFilter filter = new ShopRequestFilter();

        // 1 запись для "1", 1 для "id1", 2 для "abc123"
        filter.setRequestIds(Arrays.asList("1", "id1", "abc123"));
        checkPage(filter, List.of(1L, 2L, 5L, 6L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/find_requests_withdraw_with_cises_and_without_act.xml")
    void testFindRequestsWithdrawWithCisesAndWithoutAct() {
        final Collection<ShopRequest> requests =
                shopRequestFetchingService.findRequestsWithdrawWithCisesAndWithoutReport();
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(1).contains(6L);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/find_supply_requests_with_cises_and_without_report.xml")
    void testFindSupplyRequestsWithCisesAndWithoutReport() {
        final Collection<ShopRequest> requests =
                shopRequestFetchingService.findSupplyRequestsWithCisesAndWithoutReport();
        assertThat(requests).isNotNull();
        assertThat(requests.stream().map(ShopRequest::getId)).hasSize(1).contains(6L);
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/before-get-calendaring-fields.xml")
    public void getCalendaringFieldsByRequests() {
        Map<Long, CalendaringRequestItemsAggregatedFields> calendaringFieldsForRequests =
                shopRequestFetchingService.getCalendaringFieldsForRequests(Set.of(1L, 2L), Collections.emptyMap());
        assertions.assertThat(calendaringFieldsForRequests).hasSize(2);
        CalendaringRequestItemsAggregatedFields first = calendaringFieldsForRequests.get(1L);
        CalendaringRequestItemsAggregatedFields second = calendaringFieldsForRequests.get(2L);
        assertCalendaringAggregatedFieldsExpected(first, 1, BigDecimal.valueOf(359.5),
                "3", "Supplier3", 12, 2);
        assertCalendaringAggregatedFieldsExpected(second, 2, BigDecimal.valueOf(653.5),
                null, null, 7, 80);
    }


    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-vetis.xml")
    void findByVetis() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();

        filter.setVetis(true);
        checkYardPage(filter, List.of(1L));

        filter.setVetis(false);
        checkYardPage(filter, List.of(2L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-doc-types.xml")
    void findByDocumentTypes() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();

        filter.setDocumentTypes(List.of(RequestDocumentType.ELECTRONIC, RequestDocumentType.PAPER));
        checkYardPage(filter, List.of(1L,  2L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-utd-documents.xml")
    void findByDocumentTypesAndUTDDocuments() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setDocumentTypes(List.of(RequestDocumentType.ELECTRONIC));
        filter.setHasUTDDocuments(true);
        checkYardPage(filter, List.of(1L));

        filter.setHasUTDDocuments(false);
        checkYardPage(filter, List.of(2L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-stm.xml")
    void findByStm() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setAttributes(Set.of(RequestItemAttribute.CTM));
        checkYardPage(filter, List.of(12L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-ticket-url.xml")
    void findByDocumentTicketUrl() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setHasDocumentTicketUrl(true);
        checkYardPage(filter, List.of(1L));

        filter.setHasDocumentTicketUrl(false);
        checkYardPage(filter, List.of(2L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests-with-ticket-status.xml")
    void findByDocumentTicketStatus() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setDocumentTicketStatusIsOpen(true);
        checkYardPage(filter, List.of(1L));

        filter.setDocumentTicketStatusIsOpen(false);
        checkYardPage(filter, List.of(2L, 3L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByTypeAndStatus() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setStatuses(List.of(RequestStatus.WAITING_FOR_CONFIRMATION));
        filter.setTypes(List.of(RequestType.UTILIZATION_WITHDRAW));
        filter.setRequestDateFrom(LocalDate.of(2010, 11, 11));

        checkYardPage(filter, List.of(9L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByRequestDateAndShopIds() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setRequestDateFrom(LocalDate.of(2010, 11, 11));
        filter.setShopIds(Arrays.asList(1L, 2L, 20L));

        checkYardPage(filter, List.of(1L, 2L, 3L));
    }


    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByRequestDateAndShopIdsAndRealSupplierIds() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setRequestDateFrom(LocalDate.of(2010, 11, 11));
        filter.setShopIds(Arrays.asList(1L, 2L, 20L));
        filter.setRealSupplierIds(List.of("456"));
        checkYardPage(filter, List.of(1L, 2L, 3L, 9L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void testNoShadowFinished() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setTypes(List.of(RequestType.SHADOW_SUPPLY));
        checkYardPage(filter, List.of(7L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByRealSupplierIds() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setRealSupplierIds(List.of("123", "456"));
        filter.setShopIds(List.of(4L));
        checkYardPage(filter, List.of(5L, 6L, 7L, 9L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findByMultiId() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setRequestIds(List.of("abc123"));
        filter.setRequestDateFrom(LocalDate.of(2016, 1, 1));
        filter.setRequestDateTo(LocalDate.of(2017, 1, 1));

        checkYardPage(filter, List.of(5L, 6L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void findOnlyWithExternalId() {
        final ShopRequestYardFilter filter = new ShopRequestYardFilter();
        filter.setRequestIds(List.of("0000123"));
        filter.setRequestDateFrom(LocalDate.of(2001, 1, 1));
        filter.setRequestDateTo(LocalDate.of(2002, 1, 1));

        checkYardPage(filter, List.of(11L));
    }

    /**
     * Метод извлекает заявки с использованием фильтра и проверяет их валидность.
     * Для простоты проверку делает просто по идентификатору заявок.
     *
     * @param filter   фильтр
     * @param expected ожидаемые идентификаторы заявок
     */
    private void checkPage(final ShopRequestFilter filter, List<Long> expected) {
        ShopRequestDTOContainer requests = shopRequestFetchingService
                .findRequests(filter, ADDITIONAL_FIELDS_CONFIG_EMPTY, PAGEABLE);
        final int expectedSize = expected.size();

        assertThat(requests.getRequests())
                .isNotNull()
                .hasSize(expectedSize);

        final List<ShopRequestDTO> content = requests.getRequests();
        assertThat(content)
                .isNotNull()
                .hasSize(expectedSize);
        List<Long> contentIds = content.stream().map(ShopRequestDTO::getId).collect(Collectors.toList());
        assertThat(contentIds).containsExactlyInAnyOrderElementsOf(expected);
    }

    private void checkYardPage(final ShopRequestYardFilter filter, List<Long> expected) {
        ShopRequestYardDTOContainer requests = shopRequestFetchingService.findRequests(filter, PAGEABLE);

        List<Long> contentIds = requests.getRequests()
                .stream()
                .map(ShopRequestYardDTO::getId)
                .collect(Collectors.toList());
        assertThat(contentIds).containsExactlyInAnyOrderElementsOf(expected);
    }

    private void assertCalendaringAggregatedFieldsExpected(CalendaringRequestItemsAggregatedFields aggregatedFields,
                                                           long requestId,
                                                           BigDecimal totalSupplyPrice,
                                                           String realSupplierId,
                                                           String realSupplierName,
                                                           long takenItems,
                                                           long takenPallets) {
        AggregatedRequestItemsInfo aggregatedItemsInfo = aggregatedFields.getAggregatedRequestItemsInfo();
        assertions.assertThat(aggregatedItemsInfo.getRequestId()).isEqualTo(requestId);
        assertions.assertThat(aggregatedItemsInfo.getTotalItemsPrice()).isEqualByComparingTo(totalSupplyPrice);
        assertions.assertThat(aggregatedItemsInfo.getAnyRealSupplierId()).isEqualTo(realSupplierId);
        assertions.assertThat(aggregatedItemsInfo.getAnyRealSupplierName()).isEqualTo(realSupplierName);
        assertions.assertThat(aggregatedFields.getTakenItemsByRequest()).isEqualTo(takenItems);
        assertions.assertThat(aggregatedFields.getTakenPalletsByRequest()).isEqualTo(takenPallets);
    }
}
