package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.SupplierRequestNotificationInfo;
import ru.yandex.market.replenishment.autoorder.model.SupplierRequestStatus;
import ru.yandex.market.replenishment.autoorder.model.dto.SupplierRequestExcelItemDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SupplierRequest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SupplierRequestRepository;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.collectionToMap;
public class SupplierRequestRepositoryTest extends FunctionalTest {
    @Autowired
    private SupplierRequestRepository supplierRequestRepository;

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_getExistsIds.before.csv")
    public void testGetExistsIds() {
        Collection<Long> foundIds = supplierRequestRepository.getExistsIds(List.of(1L, 101L, 201L));
        assertTrue(foundIds.contains(1L));
        assertTrue(foundIds.contains(101L));
        assertFalse(foundIds.contains(201L));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_getBySupplierId.before.csv")
    public void testGetBySupplierId() {
        Map<Long, SupplierRequest> foundRequests = collectionToMap(
                supplierRequestRepository.getBySupplierId(
                        2L, List.of(SupplierRequestStatus.NEW), 0L, 100L, null),
                SupplierRequest::getId);
        assertThat(foundRequests.keySet(), hasSize(3));

        SupplierRequest request = foundRequests.get(3L);
        assertNotNull(request);
        assertFalse(request.isDraft());
        assertThat(request.getSupplierId(), equalTo(2L));
        assertThat(request.getStatus(), equalTo(SupplierRequestStatus.NEW));
        assertThat(request.getRequestSskus(), equalTo(13L));
        assertThat(request.getRealSskus(), equalTo(9L));

        request = foundRequests.get(4L);
        assertNotNull(request);
        assertFalse(request.isDraft());
        assertThat(request.getSupplierId(), equalTo(2L));
        assertThat(request.getStatus(), equalTo(SupplierRequestStatus.NEW));
        assertThat(request.getRequestSskus(), equalTo(14L));
        assertThat(request.getRealSskus(), equalTo(5L));

        request = foundRequests.get(6L);
        assertNotNull(request);
        assertFalse(request.isDraft());
        assertThat(request.getSupplierId(), equalTo(2L));
        assertThat(request.getStatus(), equalTo(SupplierRequestStatus.NEW));
        assertThat(request.getRequestSskus(), equalTo(16L));
        assertThat(request.getRealSskus(), equalTo(7L));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_getBySupplierId.before.csv")
    public void testCountBySupplierId() {
        Long count = supplierRequestRepository.countBySupplierId(
                2L, List.of(SupplierRequestStatus.NEW), null);
        assertNotNull(count);
        assertEquals(3L, count.longValue());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_getSupplierRequestItems.before.csv")
    public void testGetSupplierRequestItemsForExcelIsEmptyForDraftRequest() {
        Collection<SupplierRequestExcelItemDTO> items =
                supplierRequestRepository.getSupplierRequestItemsForExcel(3L);
        assertThat(items, hasSize(0));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_getSupplierRequestItems.before.csv")
    public void testGetSupplierRequestItemsForExcelIsNotEmptyForNotDraftRequest() {
        Collection<SupplierRequestExcelItemDTO> items =
                supplierRequestRepository.getSupplierRequestItemsForExcel(1L);
        assertThat(items, hasSize(3));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_testGetRequestsNotSentAfter.before.csv")
    public void testGetRequestsNotSentAfter_whitelist() {
        LocalDateTime threshold = LocalDateTime.of(2021, 3, 15, 2, 0, 0);
        Map<Long, SupplierRequestNotificationInfo> notificationInfos =
                collectionToMap(supplierRequestRepository.getRequestsNotSentAfter(threshold, 3, true),
                        SupplierRequestNotificationInfo::getRequestId);

        assertThat(notificationInfos.keySet(), hasSize(1));

        SupplierRequestNotificationInfo info = notificationInfos.get(1L);
        assertNotNull(info);
        assertThat(info.getRequestId(), equalTo(1L));
        assertThat(info.getRequestIdString(), equalTo("RPL-1"));
        assertThat(info.getSupplierName(), equalTo("Supplier 1"));
        assertThat(info.getWarehouseName(), equalTo("Маршрут"));
        assertThat(info.getCampaignId(), equalTo(100500L));
        assertThat(info.getProductNumber(), equalTo(2L));
        assertThat(info.getProductQuantity(), equalTo(9L));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_testGetRequestsNotSentAfter.before.csv")
    public void testGetRequestsNotSentAfter_blacklist() {
        LocalDateTime threshold = LocalDateTime.of(2021, 3, 15, 2, 0, 0);

        Map<Long, SupplierRequestNotificationInfo> notificationInfos =
                collectionToMap(supplierRequestRepository.getRequestsNotSentAfter(threshold, 3, false),
                        SupplierRequestNotificationInfo::getRequestId);

        assertThat(notificationInfos.keySet(), hasSize(2));

        SupplierRequestNotificationInfo info = notificationInfos.get(2L);
        assertNotNull(info);
        assertThat(info.getRequestId(), equalTo(2L));
        assertThat(info.getRequestIdString(), equalTo("RPL-2"));
        assertThat(info.getSupplierName(), equalTo("Supplier 2"));
        assertThat(info.getWarehouseName(), equalTo("Ростов"));
        assertNull(info.getCampaignId());
        assertThat(info.getProductNumber(), equalTo(1L));
        assertThat(info.getProductQuantity(), equalTo(6L));

        info = notificationInfos.get(3L);
        assertNotNull(info);
        assertThat(info.getRequestId(), equalTo(3L));
        assertThat(info.getRequestIdString(), equalTo("RPL-3"));
        assertThat(info.getSupplierName(), equalTo("Supplier 2"));
        assertThat(info.getWarehouseName(), equalTo("Ростов"));
        assertNull(info.getCampaignId());
        assertThat(info.getProductNumber(), equalTo(1L));
        assertThat(info.getProductQuantity(), equalTo(7L));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestRepositoryTest_responseDateNotChanged.before.csv",
            after = "SupplierRequestRepositoryTest_responseDateNotChanged.after.csv")
    public void testResponseDateNotChanged() {
        final LocalDate now = LocalDate.of(2021, 5, 22);
        supplierRequestRepository.setOutdatedStatusForCreatedBefore(now);
    }
}
