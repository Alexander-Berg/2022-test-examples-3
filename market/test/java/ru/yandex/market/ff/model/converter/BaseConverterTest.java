package ru.yandex.market.ff.model.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import org.assertj.core.util.Maps;

import ru.yandex.market.ff.client.dto.BookedSlotDTO;
import ru.yandex.market.ff.client.dto.LegalInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RequestStatusHistoryDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDocumentDTO;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.TimeSlotStatus;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.bo.ItemsWrongCountInfo;
import ru.yandex.market.ff.model.bo.ShopRequestDetails;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.BookedTimeSlot;
import ru.yandex.market.ff.model.entity.RequestLegalInfo;
import ru.yandex.market.ff.model.entity.RequestStatusHistory;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.ShopRequestDocument;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.util.ContactPersonFakeInfo;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Базовый класс для тестирования конвертеров.
 *
 * @author avetokhin 14/12/17.
 */
@SuppressWarnings("HideUtilityClassConstructor")
class BaseConverterTest {

    private static final BigDecimal ITEMS_VOLUMES_SUM = new BigDecimal(20);

    static void assertRequestLegalInfo(final RequestLegalInfo source, final LegalInfoDTO result) {
        if (source == null && result == null) {
            return;
        }
        if (source == null || result == null) {
            fail("Expected " + source + ", found " + result);
        }
        assertThat(result.getConsignee(), equalTo(source.getConsignee()));
        assertThat(result.getContactPersonName(), equalTo(source.getContactPersonName()));
        assertThat(result.getContactPersonSurname(), equalTo(source.getContactPersonSurname()));
        assertThat(result.getPhoneNumber(), equalTo(source.getPhoneNumber()));
    }

    static void assertRequestIncompleteLegalInfo(final RequestLegalInfo source, final LegalInfoDTO result) {
        if (source == null && result == null) {
            return;
        }
        if (source == null || result == null) {
            fail("Expected " + source + ", found " + result);
        }
        assertThat(result.getConsignee(), equalTo(source.getConsignee()));
        assertThat(result.getContactPersonName(), equalTo(ContactPersonFakeInfo.FAKE_CONTACT_PERSON_NAME));
        assertThat(result.getContactPersonSurname(), equalTo(ContactPersonFakeInfo.FAKE_CONTACT_PERSON_SURNAME));
        assertThat(result.getPhoneNumber(), equalTo(ContactPersonFakeInfo.FAKE_PHONE_NUMBER));
    }

    static void assertRequestDocDTO(final ShopRequestDocument source, final ShopRequestDocumentDTO result) {
        assertThat(result.getId(), equalTo(source.getId()));
        assertThat(result.getCreatedAt(), equalTo(source.getCreatedAt()));
        assertThat(result.getFileUrl(), equalTo(source.getFileUrl()));
        assertThat(result.getRequestId(), equalTo(source.getRequestId()));
        assertThat(result.getType(), equalTo(source.getType()));
    }

    static void assertRequestStatusHistoryDTO(final RequestStatusHistory source,
                                              final RequestStatusHistoryDTO result) {
        assertThat(result.getDate(), equalTo(source.getUpdatedAt()));
        assertThat(result.getStatus(), equalTo(source.getStatus()));
    }

    void assertUnitIdDto(RegistryUnitId unitId, RegistryUnitIdDTO unitIdDTO) {
        for (UnitPartialId part : unitId.getParts()) {
            RegistryUnitPartialIdDTO partDto = new RegistryUnitPartialIdDTO(part.getType(), part.getValue());
            assertTrue(unitIdDTO.getParts().contains(partDto));
        }
    }

    static void assertBookedSlots(BookedTimeSlot source, BookedSlotDTO result) {
        if (null == source) {
            assertThat(result, nullValue());
            return;
        }
        assertThat(result.getFrom(), equalTo(source.getFromTime()));
        assertThat(result.getTo(), equalTo(source.getToTime()));
        assertThat(result.getStatus(), equalTo(source.getStatus()));
        assertThat(result.getGateId(), equalTo(source.getGateId()));
    }

    static ShopRequestDetails filledRequestDetailsNoSupplier() {
        return new ShopRequestDetails(
                filledRequest(null),
                Collections.singletonList(filledStatusHistory()),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "111", RegistryUnitIdType.IMEI, "aaa"),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "222", RegistryUnitIdType.IMEI, "bbb"),
                Maps.newHashMap(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 10L),
                Maps.newHashMap("mboc.msku.error.supply-forbidden.delisted-offer", 4L),
                null,
                new ItemsWrongCountInfo(3, 5, 1, 5),
                Collections.singletonList(filledDocument()),
                Collections.singletonList(filledBookedSlotDTO()),
                Collections.singletonList(filledPreviouslyBookedSlotDTO()),
                null,
                5,
                ITEMS_VOLUMES_SUM,
                false,
                false,
                0,
                null,
                false,
                null);
    }

    static ShopRequestDetails filledRequestDetailsNoPreviouslyBookedTimeSlots() {
        return new ShopRequestDetails(
                filledRequest(null),
                Collections.singletonList(filledStatusHistory()),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "111", RegistryUnitIdType.IMEI, "aaa"),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "222", RegistryUnitIdType.IMEI, "bbb"),
                Maps.newHashMap(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 10L),
                Maps.newHashMap("mboc.msku.error.supply-forbidden.delisted-offer", 4L),
                null,
                new ItemsWrongCountInfo(3, 5, 1, 5),
                Collections.singletonList(filledDocument()),
                Collections.singletonList(filledBookedSlotDTO()),
                null,
                null,
                5,
                ITEMS_VOLUMES_SUM,
                false,
                false,
                0,
                null,
                false,
                null);
    }

    static ShopRequestDetails filledRequestDetailsWithLegalInfo() {
        final Supplier supplier = filledSupplier();
        return new ShopRequestDetails(
                filledRequest(supplier),
                Collections.singletonList(filledStatusHistory()),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "111", RegistryUnitIdType.IMEI, "aaa"),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "222", RegistryUnitIdType.IMEI, "bbb"),
                Maps.newHashMap(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 10L),
                Maps.newHashMap("mboc.msku.error.supply-forbidden.delisted-offer", 4L),
                filledRequestLegalInfo(),
                new ItemsWrongCountInfo(3, 5, 1, 5),
                Collections.singletonList(filledDocument()),
                Collections.singletonList(filledBookedSlotDTO()),
                Collections.singletonList(filledPreviouslyBookedSlotDTO()),
                null,
                5,
                ITEMS_VOLUMES_SUM,
                false,
                false,
                0,
                null,
                false,
                null);
    }

    static ShopRequestDetails filledRequestDetailsWithIncompleteLegalInfo() {
        final Supplier supplier = filledSupplier();

        return new ShopRequestDetails(
                filledRequest(supplier),
                Collections.singletonList(filledStatusHistory()),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "111", RegistryUnitIdType.IMEI, "aaa"),
                RegistryUnitId.of(RegistryUnitIdType.CIS, "222", RegistryUnitIdType.IMEI, "bbb"),
                Maps.newHashMap(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 10L),
                Maps.newHashMap("mboc.msku.error.supply-forbidden.delisted-offer", 4L),
                filledRequestIncompleteLegalInfo(),
                new ItemsWrongCountInfo(3, 5, 1, 5),
                Collections.singletonList(filledDocument()),
                Collections.singletonList(filledBookedSlotDTO()),
                null,
                null,
                5,
                ITEMS_VOLUMES_SUM,
                false,
                false,
                0,
                null,
                false,
                null);
    }


    static ShopRequestDetails filledRequestDetailsNoIdentifiers() {
        final Supplier supplier = filledSupplier();

        return new ShopRequestDetails(
                filledRequest(supplier),
                Collections.singletonList(filledStatusHistory()),
                RegistryUnitId.builder().build(),
                RegistryUnitId.builder().build(),
                Maps.newHashMap(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 10L),
                Maps.newHashMap("mboc.msku.error.supply-forbidden.delisted-offer", 4L),
                null,
                new ItemsWrongCountInfo(3, 5, 1, 5),
                Collections.singletonList(filledDocument()),
                Collections.singletonList(filledBookedSlotDTO()),
                Collections.singletonList(filledPreviouslyBookedSlotDTO()),
                null,
                5,
                ITEMS_VOLUMES_SUM,
                false,
                false,
                0,
                null,
                false,
                null);
    }

    static BookedTimeSlot filledPreviouslyBookedSlots() {
        LocalDateTime fromTime = LocalDateTime.of(2019, 11, 28, 11, 11, 11);
        BookedTimeSlot slot = new BookedTimeSlot();
        slot.setFromTime(fromTime);
        slot.setToTime(fromTime.plusMinutes(30));
        slot.setStatus(TimeSlotStatus.INACTIVE);
        slot.setGateId(11);
        slot.setCreatedAt(fromTime);
        return slot;
    }

    static BookedSlotDTO filledPreviouslyBookedSlotDTO() {
        LocalDateTime fromTime = LocalDateTime.of(2019, 11, 28, 11, 11, 11);
        BookedSlotDTO slot = new BookedSlotDTO();
        slot.setFrom(fromTime);
        slot.setTo(fromTime.plusMinutes(30));
        slot.setStatus(TimeSlotStatus.INACTIVE);
        slot.setGateId(11);
        slot.setCreatedAt(fromTime);
        return slot;
    }

    static BookedTimeSlot filledBookedSlots() {
        LocalDateTime fromTime = LocalDateTime.of(2019, 11, 28, 11, 11, 11);
        BookedTimeSlot slot = new BookedTimeSlot();
        slot.setFromTime(fromTime);
        slot.setToTime(fromTime.plusMinutes(30));
        slot.setStatus(TimeSlotStatus.ACTIVE);
        slot.setGateId(10);
        slot.setCreatedAt(fromTime);
        return slot;
    }

    static BookedSlotDTO filledBookedSlotDTO() {
        LocalDateTime fromTime = LocalDateTime.of(2019, 11, 28, 11, 11, 11);
        BookedSlotDTO slot = new BookedSlotDTO();
        slot.setFrom(fromTime);
        slot.setTo(fromTime.plusMinutes(30));
        slot.setStatus(TimeSlotStatus.ACTIVE);
        slot.setGateId(10);
        slot.setCreatedAt(fromTime);
        return slot;
    }

    static RequestLegalInfo filledRequestLegalInfo() {
        return new RequestLegalInfo(1, "79232435555", "Грузополучатель", "Имя", "Фамилия");
    }

    static RequestLegalInfo filledRequestIncompleteLegalInfo() {
        RequestLegalInfo requestLegalInfo = new RequestLegalInfo();
        requestLegalInfo.setConsignee("Грузополучатель");
        return requestLegalInfo;
    }

    static Supplier filledSupplier() {
        final Supplier supplier = new Supplier();
        supplier.setId(3L);
        supplier.setName("Supplier");
        supplier.setPrepayRequestId(60L);
        return supplier;
    }

    static RequestStatusHistory filledStatusHistory() {
        final RequestStatusHistory requestStatusHistory = new RequestStatusHistory();
        requestStatusHistory.setStatus(RequestStatus.REJECTED_BY_SERVICE);
        requestStatusHistory.setUpdatedAt(LocalDateTime.now());
        return requestStatusHistory;
    }

    static ShopRequestDocument filledDocument() {
        ShopRequestDocument document = new ShopRequestDocument();
        document.setId(1L);
        document.setRequestId(2L);
        document.setType(DocumentType.WITHDRAW);
        document.setCreatedAt(LocalDateTime.of(2019, 11, 28, 11, 11, 11));
        document.setFileUrl("http://localhost/file_url");
        document.setExtension(FileExtension.CSV);
        return document;
    }

    static ShopRequest filledRequest(final Supplier supplier) {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        shopRequest.setServiceId(2L);
        shopRequest.setSupplier(supplier);

        shopRequest.setCreatedAt(LocalDateTime.now());
        shopRequest.setUpdatedAt(LocalDateTime.now());
        shopRequest.setRequestedDate(LocalDateTime.now());

        shopRequest.setExternalOperationType(ExternalOperationType.OUTBOUND);
        shopRequest.setExternalRequestId("ExReqId");

        shopRequest.setType(RequestType.WITHDRAW);
        shopRequest.setStatus(RequestStatus.CREATED);

        shopRequest.setComment("test");
        shopRequest.setItemsTotalCount(10L);
        shopRequest.setItemsTotalFactCount(10L);
        shopRequest.setItemsTotalDefectCount(1L);
        shopRequest.setItemsTotalSurplusCount(1L);
        shopRequest.setStockType(StockType.DEFECT);

        shopRequest.setCalendaringMode(CalendaringMode.AUTO);
        shopRequest.setSupplyRequestId(123L);
        return shopRequest;
    }

}
