package ru.yandex.market.tpl.core.domain.usershift.partner.order.materialized;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class MaterializedReportOrderServiceClientReturnTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final PartnerReportOrderService partnerReportOrderService;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnCommandService clientReturnCommandService;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private User user;
    private Shift shift;


    private final Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUser(356L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @ParameterizedTest
    @MethodSource("refusalReasonToFilter")
    @DisplayName("Тест, что поиск отображает все отмененные клиентские возвраты")
    void findAllClientReturns_WhenClientRefused(OrderDeliveryTaskFailReasonType type,
                                                PartnerReportOrderParamsDto.OrderFilter filter) {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var userShiftId = createUserShift(clientReturn);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);

        clientReturnCommandService.cancel(
                new ClientReturnCommand.Cancel(
                        clientReturn.getId(), new OrderDeliveryFailReason(type, "some comment"), Source.CLIENT
                )
        );

        var params = new PartnerReportOrderParamsDto();
        params.setOrderFilter(filter);

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var temp = userShiftRepository.findByIdOrThrow(userShiftId);
                    var task = temp.streamOrderDeliveryTasks().findFirst().orElseThrow();
                    assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                    assertThat(task.getFailReason().getType()).isEqualTo(type);
                }
        );

        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageable).isNotEmpty();
        assertThat(resultWithPageable.get(0).getOrderType()).isEqualTo(PartnerOrderType.CLIENT_RETURN_AT_CLIENT_ADDRESS);
    }

    private long createUserShift(ClientReturn clientReturn) {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.clientReturn("addr3", 14, clientReturn.getId()))
                .build();
        return userShiftTestHelper.start(createCommand);
    }


    public static Stream<Arguments> refusalReasonToFilter() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED,
                        PartnerReportOrderParamsDto.OrderFilter.clientRefused),
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_ITEMS_MISMATCH,
                        PartnerReportOrderParamsDto.OrderFilter.itemsMismatch),
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH,
                        PartnerReportOrderParamsDto.OrderFilter.itemsQuantityMismatch)
        );
    }
}
