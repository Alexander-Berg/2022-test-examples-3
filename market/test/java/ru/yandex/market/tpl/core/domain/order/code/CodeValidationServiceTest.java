package ru.yandex.market.tpl.core.domain.order.code;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.communication.crm.model.CommunicationEventType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.crm.communication.AsyncCommunicationSender;
import ru.yandex.market.tpl.core.service.crm.communication.model.CourierPlatformCommunicationDto;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CHECK_USER_WHEN_VALIDATE_VERIFICATION_CODE_ENABLED;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

@RequiredArgsConstructor
public class CodeValidationServiceTest extends TplAbstractTest {
    private static final String VERIFICATION_CODE = "verificationCode";

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final OrderDeliveryCodeValidationService codeValidationService;
    private final SortingCenterService sortingCenterService;
    private final UserShiftCommandService commandService;
    private final TransactionTemplate transactionTemplate;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final Clock clock;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @SpyBean
    private AsyncCommunicationSender asyncCommunicationSender;

    private User user;
    private UserShift userShift;
    private long userId;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(2L);
        userId = user.getId();
        when(configurationProviderAdapter.isBooleanEnabled(CHECK_USER_WHEN_VALIDATE_VERIFICATION_CODE_ENABLED))
                .thenReturn(false);

    }

    @DisplayName("Проверка отправки кодов подтверждения для b2b заказов с 2 кодами")
    @Test
    void checkValidationCodeResendTwoCodes() {
        Long callTaskId = createUserShiftWithOrders(List.of(
                new OrderGenerationParams("o-10", VERIFICATION_CODE),
                new OrderGenerationParams("o-20", VERIFICATION_CODE)
        ));

        codeValidationService.resendCode(callTaskId.toString(), userId);

        verify(asyncCommunicationSender, times(2)).send(any());
    }

    @DisplayName("Проверка отправки кодов подтверждения для b2b заказов с 1 кодом")
    @Test
    void checkValidationCodeResend() {
        Long callTaskId = createUserShiftWithOrders(List.of(
                new OrderGenerationParams("o-10", null),
                new OrderGenerationParams("o-20", VERIFICATION_CODE)
        ));

        codeValidationService.resendCode(callTaskId.toString(), userId);

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        checkEvent((CourierPlatformCommunicationDto.CodeValidationResendEvent)
                eventCaptor.getValue(), "o-20", VERIFICATION_CODE
        );
    }

    private Long createUserShiftWithOrders(List<OrderGenerationParams> orderParams) {
        return transactionTemplate.execute(ts -> {
            var shift = testUserHelper.findOrCreateOpenShiftForSc(
                    LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId()
            );
            userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                    user));
            AddressGenerator.AddressGenerateParam addressGenerateParam =
                    AddressGenerator.AddressGenerateParam.builder()
                            .geoPoint(GeoPointGenerator.generateLonLat())
                            .street("Колотушкина")
                            .house("1")
                            .build();

            for (OrderGenerationParams orderParam : orderParams) {
                Order order = orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .itemsCount(2)
                                                .itemsItemCount(2)
                                                .itemsPrice(BigDecimal.valueOf(120))
                                                .build()
                                )
                                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                                .addressGenerateParam(addressGenerateParam)
                                .externalOrderId(orderParam.getExternalOrderId())
                                .deliveryDate(LocalDate.now(clock))
                                .deliveryServiceId(239L)
                                .properties(generateOrderProperties(orderParam.getCode()))
                                .build()
                );
                userShiftReassignManager.assign(userShift, order);
            }

            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

            Optional<CallToRecipientTask> callToRecipientTaskOpt = userShift.streamCallTasks().findFirst();
            return callToRecipientTaskOpt.get().getId();
        });
    }

    private Map<String, OrderProperty> generateOrderProperties(String code) {
        String verificationCodeProperty = TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name();
        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        return Map.of(
                verificationCodeProperty,
                new OrderProperty(null, TplPropertyType.STRING, verificationCodeProperty, code),
                customerTypeProperty,
                new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name())
        );
    }

    @Data
    @AllArgsConstructor
    private static class OrderGenerationParams {
        private String externalOrderId;
        private String code;

    }

    private void checkEvent(CourierPlatformCommunicationDto.CodeValidationResendEvent event,
                            String yandexOrderId, String code) {
        assertThat(event.getEventType()).isEqualTo(CommunicationEventType.CODE_VALIDATION_RESEND_EVENT);
        assertThat(event.getYandexOrderIds()).contains(yandexOrderId);
        assertThat(event.getCode()).isEqualTo(code);
        assertThat(event.getRecipientEmailPersonalId()).isEqualTo(DEFAULT_EMAIL_PERSONAL_ID);
        assertThat(event.getRecipientPhonePersonalId()).isEqualTo(DEFAULT_PHONE_PERSONAL_ID);
    }
}
