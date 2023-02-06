package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validator;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse.LogisticsPointResponseBuilder;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseRequestBase;
import ru.yandex.market.logistics.nesu.utils.UuidGenerator;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.scheduleDay;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressMinimalBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseContactMinimalBuilder;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;

@DatabaseSetup({
    "/repository/validation/default_validation_settings.xml",
    "/controller/warehouse/prepare.xml",
})
public abstract class AbstractShopWarehouseControllerTest extends AbstractContextualTest {
    protected static final Duration ONE_DAY = Duration.ofDays(1);

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected Validator validator;

    @Autowired
    private UuidGenerator uuidGenerator;

    @BeforeEach
    void init() {
        when(uuidGenerator.randomUuid()).thenReturn("externalId");
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Nonnull
    protected static Stream<Arguments> bodyValidationSourceBase() {
        return Stream.<Pair<ValidationErrorData.ValidationErrorDataBuilder, Consumer<ShopWarehouseRequestBase>>>of(
            Pair.of(
                fieldErrorBuilder("schedule", ErrorType.NOT_EMPTY),
                rq -> rq.setSchedule(null)
            ),
            Pair.of(
                fieldErrorBuilder("schedule", ErrorType.NOT_EMPTY),
                rq -> rq.setSchedule(Set.of())
            ),
            Pair.of(
                fieldErrorBuilder("schedule", ErrorType.NOT_NULL_ELEMENTS),
                rq -> rq.setSchedule(Collections.singleton(null))
            ),
            Pair.of(
                fieldErrorBuilder("schedule[].day", ErrorType.NOT_NULL),
                rq -> rq.setSchedule(Set.of(scheduleDay().setDay(null)))
            ),
            Pair.of(
                fieldErrorBuilder("schedule[].day", ErrorType.min(1)),
                rq -> rq.setSchedule(Set.of(scheduleDay().setDay(0)))
            ),
            Pair.of(
                fieldErrorBuilder("schedule[].day", ErrorType.max(7)),
                rq -> rq.setSchedule(Set.of(scheduleDay().setDay(8)))
            ),
            Pair.of(
                fieldErrorBuilder("schedule[].timeFrom", ErrorType.NOT_NULL),
                rq -> rq.setSchedule(Set.of(scheduleDay().setTimeFrom(null)))
            ),
            Pair.of(
                fieldErrorBuilder("schedule[].timeTo", ErrorType.NOT_NULL),
                rq -> rq.setSchedule(Set.of(scheduleDay().setTimeTo(null)))
            ),
            Pair.of(
                fieldErrorBuilder("address.geoId", ErrorType.NOT_NULL),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().geoId(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.house", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().house(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.house", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().house(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("address.locality", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().locality(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.locality", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().locality(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("address.postCode", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().postCode(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.postCode", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().postCode(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("address.region", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().region(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.region", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().region(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("address.street", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().street(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.street", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().street(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.firstName", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().firstName(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.firstName", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().firstName(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.lastName", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().lastName(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.lastName", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().lastName(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.phoneNumber", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().phoneNumber(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("contact.phoneNumber", ErrorType.NOT_BLANK),
                rq -> rq.setContact(warehouseContactMinimalBuilder().phoneNumber(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("handlingTimeDays", ErrorType.POSITIVE_OR_ZERO),
                rq -> rq.setHandlingTimeDays(-1L)
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Nonnull
    protected static LogisticsPointFilter.LogisticsPointFilterBuilder createLogisticsPointFilterBuilder(
        Long... businessIdsArray
    ) {
        return LogisticsPointFilter.newBuilder()
            .businessIds(Set.of(businessIdsArray))
            .type(PointType.WAREHOUSE)
            .active(true);
    }

    @Nonnull
    protected LogisticsPointResponse createLogisticsPointResponse() {
        return logisticsPointResponseBuilder()
            .handlingTime(ONE_DAY)
            .build();
    }

    @Nonnull
    protected LogisticsPointResponse createDaasLogisticsPointResponse() {
        return logisticsPointResponseBuilder()
            .partnerId(null)
            .handlingTime(ONE_DAY)
            .build();
    }

    @Nonnull
    protected static LogisticsPointResponseBuilder logisticsPointResponseBuilder() {
        return logisticsPointResponseBuilder(1);
    }

    @Nonnull
    protected static LogisticsPointResponseBuilder logisticsPointResponseBuilder(long id) {
        return LmsFactory.createLogisticsPointResponseBuilder(id,  1000L, "Имя склада", PointType.WAREHOUSE)
            .address(createAddressDto())
            .businessId(42L);
    }

    @Nonnull
    protected LogisticsPointResponse createMinimalLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(1L)
            .externalId("externalId")
            .type(PointType.WAREHOUSE)
            .name("Имя склада")
            .address(Address.newBuilder()
                .settlement("Новосибирск")
                .postCode("649220")
                .street("Николаева")
                .house("11")
                .build())
            .active(true)
            .schedule(Set.of(LmsFactory.createScheduleDayDto(1)))
            .businessId(42L)
            .build();
    }

    @Nonnull
    protected static Address createAddressDto() {
        return addressBuilder()
            .build();
    }

    @Nonnull
    protected Address.AddressBuilder addressWithoutCalculatedFields() {
        return addressBuilder()
            .latitude(null)
            .longitude(null)
            .subRegion(null);
    }

    @Nonnull
    protected static Address.AddressBuilder addressBuilder() {
        return Address.newBuilder()
            .locationId(65)
            .latitude(new BigDecimal("54.858076"))
            .longitude(new BigDecimal("83.110392"))
            .settlement("Новосибирск")
            .postCode("649220")
            .street("Николаева")
            .house("11")
            .housing("")
            .building("")
            .apartment("")
            .comment("как проехать")
            .region("Новосибирская область")
            .subRegion("Новосибирский округ");
    }

    @Nonnull
    protected Set<Phone> createPhoneDto() {
        return Set.of(new Phone(
            "+7 923 243 5555",
            "777",
            null,
            PhoneType.PRIMARY
        ));
    }
}
