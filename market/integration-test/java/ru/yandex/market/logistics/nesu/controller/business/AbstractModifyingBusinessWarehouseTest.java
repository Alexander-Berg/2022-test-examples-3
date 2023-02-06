package ru.yandex.market.logistics.nesu.controller.business;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.model.warehouse.WarehouseContactDto;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.WarehouseAddress;
import ru.yandex.market.logistics.nesu.dto.WarehouseAddress.WarehouseAddressBuilder;
import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_EXTERNAL_ID_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_NAME_LENGTH;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.INVALID_SYMBOLS_IN_EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;

@DatabaseSetup({"/controller/business/before/setup.xml", "/repository/validation/default_validation_settings.xml"})
class AbstractModifyingBusinessWarehouseTest extends AbstractContextualTest {

    @Nonnull
    protected static Stream<Arguments> bodyValidationSourceBase() {
        return Stream.<Pair<ValidationErrorDataBuilder, Consumer<BusinessWarehouseRequest>>>of(
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
                fieldErrorBuilder("address.locality", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().locality(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.locality", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().locality(" \t\n ").build())
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
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.size(0, 256)),
                rq -> rq.setName(INVALID_NAME_LENGTH)
            ),
            Pair.of(
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.VALID_EXTERNAL_ID),
                rq -> rq.setExternalId(INVALID_SYMBOLS_IN_EXTERNAL_ID)
            ),
            Pair.of(
                fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.size(0, 64)),
                rq -> rq.setExternalId(INVALID_EXTERNAL_ID_LENGTH)
            )
        )
            .map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Nonnull
    protected static WarehouseAddressBuilder warehouseAddressMinimalBuilder() {
        return WarehouseAddress.builder()
            .geoId(65)
            .region("Новосибирская область")
            .locality("Новосибирск")
            .street("Николаева")
            .house("11")
            .postCode("649220");
    }

    @Nonnull
    protected static Stream<Arguments> bodyValidationSourceExtended() {
        return Stream.<Pair<ValidationErrorDataBuilder, Consumer<BusinessWarehouseRequest>>>of(
            Pair.of(
                fieldErrorBuilder("schedule", ErrorType.NOT_EMPTY),
                rq -> rq.setSchedule(null)
            ),
            Pair.of(
                fieldErrorBuilder("schedule", ErrorType.NOT_EMPTY),
                rq -> rq.setSchedule(Set.of())
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
                fieldErrorBuilder("address.street", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().street(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.street", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().street(" \t\n ").build())
            ),
            Pair.of(
                fieldErrorBuilder("address.postCode", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().postCode(null).build())
            ),
            Pair.of(
                fieldErrorBuilder("address.postCode", ErrorType.NOT_BLANK),
                rq -> rq.setAddress(warehouseAddressMinimalBuilder().postCode(" \t\n ").build())
            )
        )
            .map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Nonnull
    private static WarehouseContactDto.WarehouseContactDtoBuilder warehouseContactMinimalBuilder() {
        return WarehouseContactDto.builder()
            .firstName("Иван")
            .lastName("Иванов")
            .phoneNumber("+7 923 243 5555")
            .lastName("Иванов");
    }

    @Nonnull
    protected static ScheduleDayDto scheduleDay() {
        return new ScheduleDayDto()
            .setDay(1)
            .setTimeFrom(LocalTime.of(10, 0))
            .setTimeTo(LocalTime.of(18, 0));
    }
}
