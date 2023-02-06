package ru.yandex.market.ff.model.converter;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.model.bo.ShopRequestDetails;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.FeatureToggleService;
import ru.yandex.market.ff.service.RequestSubTypeService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link ShopRequestDetailsConverter}.
 *
 * @author avetokhin 14/12/17.
 */
class ShopRequestDetailsConverterTest extends BaseConverterTest {

    private ShopRequestDetailsConverter converter;

    @BeforeEach
    public void init() {
        RequestSubTypeService subTypeService = mock(RequestSubTypeService.class);
        RequestSubTypeEntity entity = new RequestSubTypeEntity();
        entity.setZeroifyDefectOnAnomaly(true);
        entity.setId(1);
        when(subTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(entity);
        converter = new ShopRequestDetailsConverter(
                new RequestStatusHistoryConverter(),
                new ShopRequestDocumentConverter(),
                new RegistryUnitDTOConverter(),
                mock(FeatureToggleService.class),
                subTypeService);
    }

    @Test
    void convert() {
        final ShopRequestDetails source = filledRequestDetailsWithLegalInfo();
        final ShopRequestDetailsDTO details = converter.convert(source);

        final ShopRequest sourceRequest = source.getRequest();
        assertDetails(source, details);
        assertThat(details.getShopId(), equalTo(sourceRequest.getSupplier().getId()));
        assertThat(details.getShopName(), equalTo(sourceRequest.getSupplier().getName()));
        assertThat(details.getSupplierType(), equalTo(sourceRequest.getSupplier().getSupplierType()));
        assertThat(details.getSupplyRequestId(), equalTo(sourceRequest.getSupplyRequestId()));
    }

    @Test
    void convertWithIncompleteLegalIfo() {
        final ShopRequestDetails source = filledRequestDetailsWithIncompleteLegalInfo();
        final ShopRequestDetailsDTO details = converter.convert(source);

        final ShopRequest sourceRequest = source.getRequest();
        assertRequestIncompleteLegalInfo(source.getLegalInfo(), details.getLegalInfo());
        assertThat(details.getShopId(), equalTo(sourceRequest.getSupplier().getId()));
        assertThat(details.getShopName(), equalTo(sourceRequest.getSupplier().getName()));
        assertThat(details.getSupplierType(), equalTo(sourceRequest.getSupplier().getSupplierType()));
    }

    @Test
    void convertNoPreviouslyBookedTimeSlots() {
        final ShopRequestDetails source = filledRequestDetailsNoPreviouslyBookedTimeSlots();
        final ShopRequestDetailsDTO details = converter.convert(source);
        assertDetails(source, details);
    }

    @Test
    void convertNoSupplier() {
        final ShopRequestDetails source = filledRequestDetailsNoSupplier();
        final ShopRequestDetailsDTO details = converter.convert(source);

        assertDetails(source, details);
        assertThat(details.getShopId(), nullValue());
        assertThat(details.getShopName(), nullValue());
        assertThat(details.getSupplierType(), nullValue());
    }

    @Test
    void convertNoExternalData() {
        final ShopRequestDetails source = filledRequestDetailsNoSupplier();
        source.getRequest().setExternalRequestId(null);
        source.getRequest().setExternalOperationType(null);

        final ShopRequestDetailsDTO details = converter.convert(source);

        assertDetails(source, details);
        assertThat(details.getExternalRequestId(), nullValue());
        assertThat(details.getExternalOperationType(), nullValue());
    }

    @Test
    void convertNoIdentifiers() {
        final ShopRequestDetails source = filledRequestDetailsNoIdentifiers();
        final ShopRequestDetailsDTO details = converter.convert(source);

        final ShopRequest sourceRequest = source.getRequest();
        assertDetails(source, details);
        assertThat(details.getShopId(), equalTo(sourceRequest.getSupplier().getId()));
        assertThat(details.getShopName(), equalTo(sourceRequest.getSupplier().getName()));
        assertThat(details.getSupplierType(), equalTo(sourceRequest.getSupplier().getSupplierType()));
    }

    @Test
    void convertNull() {
        assertThat(converter.convert((ShopRequestDetails) null), nullValue());
    }

    private void assertDetails(final ShopRequestDetails source, final ShopRequestDetailsDTO details) {
        final ShopRequest sourceRequest = source.getRequest();

        assertThat(details.getId(), equalTo(sourceRequest.getId()));
        assertThat(details.getComment(), equalTo(sourceRequest.getComment()));
        assertThat(details.getCreatedAt(), equalTo(sourceRequest.getCreatedAt()));
        assertThat(details.getRequestedDate(), equalTo(sourceRequest.getRequestedDate()));
        assertThat(details.getServiceId(), equalTo(sourceRequest.getServiceId()));
        assertThat(details.getServiceRequestId(), equalTo(sourceRequest.getServiceRequestId()));
        assertThat(details.getStatus(), equalTo(sourceRequest.getStatus()));
        assertThat(details.getType(), equalTo(1));
        assertThat(details.getUpdatedAt(), equalTo(sourceRequest.getUpdatedAt()));
        assertThat(details.isHasDefects(), equalTo(true));
        assertThat(details.isHasSurplus(), equalTo(true));
        assertThat(details.isHasShortage(), equalTo(true));
        assertThat(details.getItemsWithDefects(), equalTo(3));
        assertThat(details.getItemsWithShortage(), equalTo(5));
        assertThat(details.getItemsWithSurplus(), equalTo(1));
        assertThat(details.getItemsWithProblems(), equalTo(5));
        assertThat(details.getStockType(), equalTo(sourceRequest.getStockType()));
        assertThat(details.getSkuCount(), equalTo(source.getSkuCount()));
        assertThat(details.getAdditionalSupplyEnabled(), equalTo(false));

        assertThat(details.getStatusHistory(), hasSize(1));
        assertRequestStatusHistoryDTO(source.getRequestStatusHistory().get(0), details.getStatusHistory().get(0));

        assertUnitIdDto(source.getUnitId(), details.getUnitId());
        assertUnitIdDto(source.getFactUnitId(), details.getFactUnitId());

        assertThat(details.getDocuments(), hasSize(1));
        assertRequestDocDTO(filledDocument(), details.getDocuments().iterator().next());

        assertThat(details.getErrors(), equalTo(source.getErrors()));

        assertRequestLegalInfo(source.getLegalInfo(), details.getLegalInfo());
        assertThat(details.getCalendaringMode(), equalTo(sourceRequest.getCalendaringMode()));

        assertBookedSlots(filledBookedSlots(), details.getBookedSlots().iterator().next());
        assertBookedSlots(source.getPreviouslyBookedSlotsForRequest() == null ? null : filledPreviouslyBookedSlots(),
                Optional.ofNullable(details.getPreviouslyBookedSlots())
                        .map(it -> it.iterator().next())
                        .orElse(null));
    }
}
