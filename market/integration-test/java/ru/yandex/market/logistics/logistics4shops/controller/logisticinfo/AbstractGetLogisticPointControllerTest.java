package ru.yandex.market.logistics.logistics4shops.controller.logisticinfo;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.Contact;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticPointInfo;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ScheduleDay;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class AbstractGetLogisticPointControllerTest extends AbstractIntegrationTest {
    @Autowired
    protected LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(lmsClient);
    }

    @Nonnull
    protected static Stream<Arguments> pointConversion() {
        return Stream.of(
            Arguments.of(
                "Точка с пустыми полями",
                LogisticsPointResponse.newBuilder().id(400100L).build(),
                new LogisticPointInfo().id(400100L)
            ),
            Arguments.of(
                "Полностью заполненная точка",
                LogisticsPointResponse.newBuilder()
                    .id(400100L)
                    .name("Лучшая точка отгрузки в Галактике")
                    .address(Address.newBuilder()
                        .locationId(2)
                        .latitude(BigDecimal.valueOf(100))
                        .longitude(BigDecimal.valueOf(90))
                        .postCode("656038")
                        .country("Россия")
                        .region("Московская область")
                        .subRegion("Раменский район")
                        .settlement("село Раменки")
                        .street("Строителей")
                        .house("25")
                        .housing("2")
                        .building("4")
                        .estate("25")
                        .apartment("13")
                        .km(225)
                        .comment("Вход со двора")
                        .build()
                    )
                    .instruction("Залетайте прямо во двор и ищите желтую дверь с синей табличкой")
                    .schedule(Set.of(
                        new ScheduleDayResponse(1L, 3, LocalTime.of(8, 0), LocalTime.of(18, 55)),
                        new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(18, 55)),
                        new ScheduleDayResponse(3L, 1, LocalTime.of(8, 0), LocalTime.of(18, 55)),
                        new ScheduleDayResponse(4L, 4, LocalTime.of(0, 0), LocalTime.of(23, 59))
                    ))
                    .phones(Set.of(new Phone("8-800-555-3535", "54321", null, PhoneType.PRIMARY)))
                    .build(),
                new LogisticPointInfo()
                    .id(400100L)
                    .name("Лучшая точка отгрузки в Галактике")
                    .address(new ru.yandex.market.logistics.logistics4shops.client.api.model.Address()
                        .locationId(2)
                        .latitude("100")
                        .longitude("90")
                        .postCode("656038")
                        .country("Россия")
                        .region("Московская область")
                        .subRegion("Раменский район")
                        .settlement("село Раменки")
                        .street("Строителей")
                        .house("25")
                        .housing("2")
                        .building("4")
                        .estate("25")
                        .apartment("13")
                        .km("225")
                        .comment("Вход со двора")
                    )
                    .instruction("Залетайте прямо во двор и ищите желтую дверь с синей табличкой")
                    .schedule(List.of(
                        new ScheduleDay().day(1).from(LocalTime.of(8, 0)).to(LocalTime.of(18, 55)),
                        new ScheduleDay().day(2).from(LocalTime.of(8, 0)).to(LocalTime.of(18, 55)),
                        new ScheduleDay().day(3).from(LocalTime.of(8, 0)).to(LocalTime.of(18, 55)),
                        new ScheduleDay().day(4).from(LocalTime.of(0, 0)).to(LocalTime.of(23, 59))
                    ))
                    .contact(new Contact().phoneNumber("8-800-555-3535").internalNumber("54321"))
            )
        );
    }

    @Nonnull
    protected AutoCloseable mockGetLogisticsPoint(Long id, LogisticsPointResponse response) {
        when(lmsClient.getLogisticsPoint(id)).thenReturn(Optional.of(response));
        return () -> verifyGetLogisticsPoint(id);
    }

    protected void verifyGetLogisticsPoint(Long id) {
        verify(lmsClient).getLogisticsPoint(id);
    }
}
