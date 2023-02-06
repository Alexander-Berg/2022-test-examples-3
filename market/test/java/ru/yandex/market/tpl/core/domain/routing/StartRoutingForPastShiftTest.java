package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routing.PartnerRoutingInfoDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.TplRoutingShiftManager;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.service.routing.PartnerRoutingService;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class StartRoutingForPastShiftTest {

    private final Clock clock;

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;

    private final ShiftManager shiftManager;
    private final TplRoutingShiftManager routingShiftManager;
    private final PartnerRoutingService partnerRoutingService;

    @Test
    public void shouldCreateTaskForPerformRoutingRequest() {
        LocalDate shiftDate = LocalDate.now(clock);

        orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(shiftDate)
                        .build()
        );

        userHelper.findOrCreateUserForSc(789654L, shiftDate, SortingCenter.DEFAULT_SC_ID);

        shiftManager.assignShiftsForDate(shiftDate, SortingCenter.DEFAULT_SC_ID);

        var shift = userHelper.findOrCreateOpenShift(shiftDate);

        // именно с такими параметрами запускается маршрутизация из ПИ
        String routingRequestId = routingShiftManager.routeShiftGroupAsync(
                shift, RoutingMockType.MANUAL, RoutingProfileType.GROUP, 0L);

        PartnerRoutingInfoDto routingInfoByRequestId =
                partnerRoutingService.findRoutingInfoByRequestId(routingRequestId);

        assertThat(routingInfoByRequestId.getRoutingDate()).isEqualTo(shiftDate);
    }
}
