package ru.yandex.market.ff.model.converter;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.dto.PutSupplyRequestWithInboundRegisterDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.i18n.RequestExportMessages;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.LogisticsPointRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.SupplierService;
import ru.yandex.market.ff.service.registry.converter.ff.FFInboundRegistryEntityToRegistryConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kotovdv 10/08/2017.
 */
class ShopRequestConverterTest extends BaseConverterTest {

    private static final String RUSSIAN_POSTAL_SERVICE = "RUSSIAN_POSTAL_SERVICE";

    private final RequestSubTypeService mockRequestSubTypeService = Mockito.mock(RequestSubTypeService.class);

    private final ShopRequestConverter converter = new ShopRequestConverter(
            new ShopRequestDocumentConverter(),
            new BookedTimeSlotConverter(Mockito.mock(DateTimeService.class)),
            Mockito.mock(SupplierService.class),
            Mockito.mock(LogisticsPointRepository.class),
            Mockito.mock(RequestCourierConverter.class),
            Mockito.mock(ShopRequestRepository.class),
            mockRequestSubTypeService,
            Mockito.mock(RequestExportMessages.class),
            Mockito.mock(DateTimeService.class),
            Mockito.mock(FFInboundRegistryEntityToRegistryConverter.class)

    );

    @Test
    void testNullConversion() {
        ShopRequestDTO dto = converter.convert(null, null, null, Collections.emptyMap(), null, null, null);

        assertThat(dto)
                .as("Asserting that null entity is converted to null dto")
                .isNull();
    }

    @Test
    void testConversion() {
        Mockito.when(mockRequestSubTypeService.getEntityByRequestTypeAndSubtype(Mockito.any()))
                .thenReturn(new RequestSubTypeEntity(1, RequestType.SUPPLY, "DEFAULT"));

        ShopRequest entity = filledRequest(filledSupplier());

        final ImmutableMap<RequestItemErrorType, Long> errors = ImmutableMap.<RequestItemErrorType, Long>builder()
                .put(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND, 1L).build();

        Map<String, Long> mboErrorsByCode = Map.of("1", 2L);
        long mboErrors = mboErrorsByCode.values().stream().mapToLong(i -> i).sum();
        ShopRequestDTO dto = converter.convert(
                entity, Collections.nCopies(3, filledDocument()), errors, mboErrorsByCode, null, null, null);

        SoftAssertions assertions = new SoftAssertions();

        assertions.assertThat(dto.getId())
                .as("Asserting that request id is the same")
                .isEqualTo(entity.getId());

        assertions.assertThat(dto.getServiceId())
                .as("Asserting that FF id is the same")
                .isEqualTo(entity.getServiceId());

        assertions.assertThat(dto.getShopId())
                .as("Asserting that shop id is the same")
                .isEqualTo(entity.getSupplier().getId());

        assertions.assertThat(dto.getShopName())
                .as("Asserting that shop name is the same")
                .isEqualTo(entity.getSupplier().getName());

        assertions.assertThat(dto.getCreatedAt())
                .as("Asserting that create at is the same")
                .isEqualTo(entity.getCreatedAt());

        assertions.assertThat(dto.getUpdatedAt())
                .as("Asserting that updated at is the same")
                .isEqualTo(entity.getUpdatedAt());

        assertions.assertThat(dto.getStatus())
                .as("Asserting that request status is the same")
                .isEqualTo(entity.getStatus());

        assertions.assertThat(dto.getType())
                .as("Asserting that request type is the same")
                .isEqualTo(entity.getType().getId());

        assertions.assertThat(dto.getDocuments())
                .as("Asserting that documents have size of 3")
                .hasSize(3);

        assertions.assertThat(dto.getRequestedDate())
                .as("Asserting that request requested date info is the same")
                .isEqualTo(entity.getRequestedDate());

        assertions.assertThat(dto.getComment())
                .as("Asserting that request comment info is the same")
                .isEqualTo(entity.getComment());

        assertions.assertThat(dto.getItemsTotalCount())
                .as("Asserting that request items total count is the same")
                .isEqualTo(entity.getItemsTotalCount());

        assertions.assertThat(dto.isHasDefects())
                .as("Asserting that request hasDefects flag is the same")
                .isEqualTo(true);

        assertions.assertThat(dto.isHasSurplus())
                .as("Asserting that request hasSurplus flag is the same")
                .isEqualTo(true);

        assertions.assertThat(dto.isHasShortage())
                .as("Asserting that request hasShortage flag is the same")
                .isEqualTo(false);

        assertions.assertThat(dto.getStockType())
                .as("Asserting that request stock type is the same")
                .isEqualTo(entity.getStockType());

        assertions.assertThat(dto.getErrors())
                .as("Asserting that request errors are the same")
                .isEqualTo(errors);

        assertions.assertThat(dto.getMboErrors())
                .as("Asserting that request MBO errors are the same")
                .isEqualTo(mboErrors);

        assertions.assertThat(dto.getCalendaringMode())
                .as("Asserting that calendaring mode is the same")
                .isEqualTo(entity.getCalendaringMode());

        assertions.assertAll();
    }

    @Test
    void shouldConvertRussianPostalCustomerReturnToCustomerReturn() {
        PutSupplyRequestWithInboundRegisterDTO dto = new PutSupplyRequestWithInboundRegisterDTO();
        dto.setType(1007);
        Mockito.when(mockRequestSubTypeService.getRequestTypeAndSubtypeById(1007))
                .thenReturn(new TypeSubtype(RequestType.CUSTOMER_RETURN, RUSSIAN_POSTAL_SERVICE));
        ShopRequest shopRequest = converter.createShopRequest(dto);
        assertThat(shopRequest.getType()).isEqualTo(RequestType.CUSTOMER_RETURN);
        assertThat(shopRequest.getSubtype()).isEqualTo(RUSSIAN_POSTAL_SERVICE);
    }
}
