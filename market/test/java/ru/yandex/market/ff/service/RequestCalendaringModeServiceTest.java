package ru.yandex.market.ff.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.service.implementation.RequestCalendaringModeServiceImpl;
import ru.yandex.market.ff.service.implementation.utils.ShopRequestTypeUtils;

/**
 * Unit-тесты для класса {@link RequestCalendaringModeService}
 */
public class RequestCalendaringModeServiceTest {

    private RequestCalendaringModeService service;
    private SoftAssertions assertions;

    private static final Set<RequestType> APPLICABLE_FIRST_PARTY_REQUEST_TYPES =
            RequestCalendaringModeServiceImpl.APPLICABLE_FOR_CALENDARING_REQUEST_TYPES_BY_SUPPLIER_TYPE.get(
                    SupplierType.FIRST_PARTY);

    private static final EnumSet<RequestType> APPLICABLE_THIRD_PARTY_REQUEST_TYPES = EnumSet.copyOf(
            RequestCalendaringModeServiceImpl.APPLICABLE_FOR_CALENDARING_REQUEST_TYPES_BY_SUPPLIER_TYPE.get(
                    SupplierType.THIRD_PARTY));

    @BeforeEach
    public void init() {
        service = new RequestCalendaringModeServiceImpl();
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void moveWithDifferentCalendaringModes() {
        for (SupplierType supplierType : SupplierType.values()) {
            for (RequestType requestType : RequestType.values()) {
                assertCorrectResultForCorrectData(supplierType, requestType, ExternalOperationType.MOVE,
                        null, CalendaringMode.NOT_REQUIRED);
                for (CalendaringMode calendaringMode : CalendaringMode.values()) {
                    assertCorrectResultForCorrectData(supplierType, requestType, ExternalOperationType.MOVE,
                            calendaringMode, CalendaringMode.NOT_REQUIRED);
                }
            }
        }
    }

    @Test
    public void xDocWithDifferentCalendaringModes() {
        for (SupplierType supplierType : SupplierType.values()) {
            for (RequestType requestType : RequestType.values()) {
                assertCorrectResultForCorrectData(supplierType, requestType, ExternalOperationType.XDOC,
                        null, CalendaringMode.NOT_REQUIRED);
                for (CalendaringMode calendaringMode : CalendaringMode.values()) {
                    assertCorrectResultForCorrectData(supplierType, requestType, ExternalOperationType.XDOC,
                            calendaringMode, CalendaringMode.NOT_REQUIRED);
                }
            }
        }
    }

    @Test
    public void thirdPartyAndCalendaringTypeNotProvided() {
        EnumSet<RequestType> supplies = EnumSet.of(RequestType.SHADOW_SUPPLY, RequestType.SUPPLY);
        var allExceptSupplies = EnumSet.complementOf(supplies);
        for (RequestType requestType : allExceptSupplies) {
            assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, requestType, ExternalOperationType.INBOUND,
                    null, CalendaringMode.NOT_REQUIRED);
        }

        assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, RequestType.SHADOW_SUPPLY,
                ExternalOperationType.INBOUND, null, CalendaringMode.NOT_REQUIRED);

        assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, RequestType.SUPPLY,
                ExternalOperationType.INBOUND, null, CalendaringMode.NOT_REQUIRED);
    }

    @Test
    public void thirdPartyAndCalendaringTypeNotRequired() {
        for (RequestType requestType : RequestType.values()) {
            assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, requestType, ExternalOperationType.INBOUND,
                    CalendaringMode.NOT_REQUIRED, CalendaringMode.NOT_REQUIRED);
        }
    }

    @Test
    public void thirdPartyAndCalendaringTypeRequired() {
        var allExceptSupplies = EnumSet.complementOf(APPLICABLE_THIRD_PARTY_REQUEST_TYPES);
        for (RequestType requestType : allExceptSupplies) {
            assertCorrectResultForIncorrectData(SupplierType.THIRD_PARTY, requestType,
                    ExternalOperationType.INBOUND, CalendaringMode.REQUIRED);
        }
        for (RequestType supplyRequestType : APPLICABLE_THIRD_PARTY_REQUEST_TYPES) {
            assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, supplyRequestType,
                    ExternalOperationType.INBOUND, CalendaringMode.REQUIRED, CalendaringMode.REQUIRED);
        }
    }

    @Test
    public void thirdPartyAndCalendaringTypeAuto() {

        var allExceptSupplies = EnumSet.complementOf(APPLICABLE_THIRD_PARTY_REQUEST_TYPES);
        for (RequestType requestType : allExceptSupplies) {
            assertCorrectResultForIncorrectData(SupplierType.THIRD_PARTY, requestType,
                    ExternalOperationType.INBOUND, CalendaringMode.AUTO);
        }
        for (RequestType supplyRequestType : APPLICABLE_THIRD_PARTY_REQUEST_TYPES) {
            assertCorrectResultForCorrectData(SupplierType.THIRD_PARTY, supplyRequestType,
                    ExternalOperationType.INBOUND, CalendaringMode.AUTO, CalendaringMode.AUTO);
        }
    }

    @Test
    public void firstPartyNotSupplyAndCalendaringTypeNotProvided() {
        for (RequestType requestType : RequestType.values()) {
            if (APPLICABLE_FIRST_PARTY_REQUEST_TYPES.contains(requestType)) {
                continue;
            }
            assertCorrectResultForCorrectData(SupplierType.FIRST_PARTY, requestType, ExternalOperationType.INBOUND,
                    null, CalendaringMode.NOT_REQUIRED);
        }
    }

    @Test
    public void firstPartyCalendaringTypeNotProvided() {
        assertCorrectResultForCorrectData(SupplierType.FIRST_PARTY, RequestType.SUPPLY, ExternalOperationType.INBOUND,
                null, CalendaringMode.REQUIRED);
    }

    @Test
    public void firstPartyNotSupplyAndCalendaringTypeNotRequired() {
        for (RequestType requestType : RequestType.values()) {
            if (APPLICABLE_FIRST_PARTY_REQUEST_TYPES.contains(requestType)) {
                continue;
            }
            assertCorrectResultForCorrectData(SupplierType.FIRST_PARTY, requestType, ExternalOperationType.INBOUND,
                    CalendaringMode.NOT_REQUIRED, CalendaringMode.NOT_REQUIRED);
        }
    }

    @Test
    public void firstPartyNotSupplyAndCalendaringTypeRequired() {
        for (RequestType requestType : RequestType.values()) {
            if (APPLICABLE_FIRST_PARTY_REQUEST_TYPES.contains(requestType)) {
                continue;
            }
            assertCorrectResultForIncorrectData(SupplierType.FIRST_PARTY, requestType,
                    ExternalOperationType.INBOUND, CalendaringMode.REQUIRED);
        }
    }

    @Test
    public void firstPartyNotSupplyAndCalendaringTypeAuto() {
        for (RequestType requestType : RequestType.values()) {
            if (APPLICABLE_FIRST_PARTY_REQUEST_TYPES.contains(requestType)) {
                continue;
            }
            assertCorrectResultForIncorrectData(SupplierType.FIRST_PARTY, requestType,
                    ExternalOperationType.INBOUND, CalendaringMode.AUTO);
        }
    }

    @Test
    public void calendaringNotChosenForRequest() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                null, CalendaringMode.REQUIRED
        );
    }

    @Test
    public void calendaringDisabledForWarehouseAndNotRequired() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.NOT_REQUIRED, CalendaringMode.NOT_REQUIRED
        );
    }

    @Test
    public void calendaringNotRequired() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.NOT_REQUIRED, CalendaringMode.NOT_REQUIRED
        );
    }

    @Test
    public void calendaringDisabledForWarehouseAndAutoSelected() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.AUTO, CalendaringMode.AUTO
        );
    }

    @Test
    public void calendaringAutoSelected() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.AUTO, CalendaringMode.AUTO
        );
    }

    @Test
    public void calendaringDisabledForWarehouseAndAlreadySelected() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.ALREADY_SELECTED, CalendaringMode.ALREADY_SELECTED
        );
    }

    @Test
    public void calendaringAlreadySelected() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES, ExternalOperationType.INBOUND,
                CalendaringMode.ALREADY_SELECTED, CalendaringMode.ALREADY_SELECTED
        );
    }

    @Test
    public void calendaringRequired() {
        assertCorrectResultForCorrectData(
                SupplierType.FIRST_PARTY, APPLICABLE_FIRST_PARTY_REQUEST_TYPES,
                ExternalOperationType.INBOUND,
                CalendaringMode.REQUIRED, CalendaringMode.REQUIRED
        );
    }

    private void assertCorrectResultForCorrectData(@Nonnull SupplierType supplierType,
                                                   @Nonnull Collection<RequestType> requestTypes,
                                                   @Nonnull ExternalOperationType externalOperationType,
                                                   @Nullable CalendaringMode calendaringModeInRequest,
                                                   @Nullable CalendaringMode expectedCalendaringMode) {

        requestTypes.forEach(requestType -> assertCorrectResultForCorrectData(
                supplierType, requestType, externalOperationType,
                calendaringModeInRequest, expectedCalendaringMode)
        );
    }

    private void assertCorrectResultForCorrectData(@Nonnull SupplierType supplierType,
                                                   @Nonnull RequestType requestType,
                                                   @Nonnull ExternalOperationType externalOperationType,
                                                   @Nullable CalendaringMode calendaringModeInRequest,
                                                   @Nullable CalendaringMode expectedCalendaringMode) {
        ShopRequest request = createRequest(supplierType, requestType, externalOperationType, calendaringModeInRequest);
        Optional<CalendaringMode> determinedCalendaringMode = service.determineCalendaringModeChecked(request);
        assertions.assertThat(determinedCalendaringMode).isPresent();
        assertions.assertThat(determinedCalendaringMode.get()).isEqualTo(expectedCalendaringMode);
    }

    private void assertCorrectResultForIncorrectData(@Nonnull SupplierType supplierType,
                                                     @Nonnull RequestType requestType,
                                                     @Nonnull ExternalOperationType externalOperationType,
                                                     @Nullable CalendaringMode calendaringModeInRequest) {
        ShopRequest request = createRequest(supplierType, requestType, externalOperationType, calendaringModeInRequest);
        Optional<CalendaringMode> calendaringMode = service.determineCalendaringModeChecked(request);
        assertions.assertThat(calendaringMode).isEmpty();
    }

    private void assertCorrectResultForIncorrectData(@Nonnull SupplierType supplierType,
                                                     @Nonnull Collection<RequestType> requestTypes,
                                                     @Nonnull ExternalOperationType externalOperationType,
                                                     @Nullable CalendaringMode calendaringModeInRequest) {

        requestTypes.forEach(requestType -> assertCorrectResultForIncorrectData(
                supplierType, requestType, externalOperationType, calendaringModeInRequest)
        );
    }

    @Nonnull
    private ShopRequest createRequest(@Nonnull SupplierType supplierType, @Nonnull RequestType requestType,
                                      @Nonnull ExternalOperationType externalOperationType,
                                      @Nullable CalendaringMode calendaringMode) {
        ShopRequest request = new ShopRequest();
        request.setId(1L);
        long serviceId = 172L;
        Supplier supplier = new Supplier();
        supplier.setId(2);
        supplier.setSupplierType(supplierType);
        request.setSupplier(supplier);
        request.setType(requestType);
        request.setExternalOperationType(externalOperationType);
        request.setCalendaringMode(calendaringMode);
        if (ShopRequestTypeUtils.isNewXDoc(request)) {
            request.setxDocServiceId(serviceId);
        } else {
            request.setServiceId(serviceId);
        }

        return request;
    }
}
