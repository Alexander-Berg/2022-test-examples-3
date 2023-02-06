package ru.yandex.market.core.partner.onboarding.sender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.notification.history.service.PartnerLastNotificationService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.partner.onboarding.sender.calculators.FailedOnboardingMailingCalculator;
import ru.yandex.market.core.partner.onboarding.sender.calculators.StuckUserFirstMessageMailingCalculator;
import ru.yandex.market.core.partner.onboarding.sender.calculators.StuckUserHelpDeskMailingCalculator;
import ru.yandex.market.core.partner.onboarding.sender.calculators.StuckUserSecondMessageMailingCalculator;
import ru.yandex.market.core.partner.onboarding.sender.calculators.SuccessfulOnboardingMailingCalculator;
import ru.yandex.market.core.partner.onboarding.sender.delay.OnboardingDelayCalculator;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.CHILD_PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_MARKET_URL;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.assertTemplateContainsSections;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestCrossdockState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestNotDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.getContainerByName;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.simpleStateDelayCalculator;

/**
 * Функциональные тесты для {@link PartnerOnboardingNotificationSender}.
 */
@DbUnitDataSet(before = "PartnerOnboardingNotificationSenderImplTest.before.csv")
class PartnerOnboardingNotificationSenderFunctionalTest extends FunctionalTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PartnerLastNotificationService partnerLastNotificationService;

    @Autowired
    private PartnerLinkService partnerLinkService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private BusinessService businessService;

    private PartnerOnboardingNotificationConfigurator configurator;
    private PartnerOnboardingNotificationSender partnerOnboardingNotificationSender;

    @BeforeEach
    void init() {
        configurator = new PartnerOnboardingNotificationConfigurator(List.of(
                new PartnerOnboardingNotificationStep(0, List.of(
                        new SuccessfulOnboardingMailingCalculator(PARTNER_MARKET_URL)
                )),
                new PartnerOnboardingNotificationStep(1, List.of(
                        new StuckUserFirstMessageMailingCalculator()
                )),
                new PartnerOnboardingNotificationStep(4, List.of(
                        new StuckUserSecondMessageMailingCalculator(contactService, businessService)
                )),
                new PartnerOnboardingNotificationStep(7, List.of(
                        new StuckUserHelpDeskMailingCalculator()
                )),
                new PartnerOnboardingNotificationStep(10, List.of(
                        new FailedOnboardingMailingCalculator()
                ))
        ));
        OnboardingDelayCalculator dayRangeCalculator = simpleStateDelayCalculator();

        partnerOnboardingNotificationSender = new PartnerOnboardingNotificationSender(
                notificationService,
                partnerLastNotificationService,
                transactionTemplate,
                new PartnerOnboardingMainMailingCalculator(configurator, dayRangeCalculator, partnerLinkService)
        );
    }


    @Test
    void testNoMessageForReplicatedPartner() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                CHILD_PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.COMMON_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(2))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        ArgumentCaptor<NotificationSendContext> notificationCaptor = ArgumentCaptor
                .forClass(NotificationSendContext.class);
        verify(notificationService, never()).send(notificationCaptor.capture());
    }

    @Test
    @DbUnitDataSet(after = "PartnerOnboardingNotificationSenderImplTest.onSendFirstNotification.after.csv")
    void onSendFirstNotification() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.COMMON_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(2))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserFirstMessageMailingCalculator.TEMPLATE_ID);
        var container = getContainerByName(notificationSendContext.getData(), "available-steps");
        assertTemplateContainsSections(container, List.of("ASSORTMENT", "STOCK_UPDATE"));
    }


    /**
     * Убеждаемся, что сообщение не посылается, если мы висим по нашей вине
     */
    @Test
    void onSendNotificationNotSentOnOurDelay() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(5L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(5L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.TEST_ORDER,
                                Status.FAILED,
                                now.minus(Duration.ofDays(5L))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserSecondMessageMailingCalculator.TEMPLATE_ID);
        var namedContainer = (NamedContainer) notificationSendContext.getData().get(3);
        assertEquals(namedContainer.getContent(), "DROPSHIP");
    }

    /**
     * Убеждаемся, что сообщение не посылается, если мы висим по нашей вине
     */
    @Test
    void onSendNotificationOnNotDropship() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestNotDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        ArgumentCaptor<NotificationSendContext> notificationCaptor = ArgumentCaptor
                .forClass(NotificationSendContext.class);
        verify(notificationService, never()).send(notificationCaptor.capture());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotification.before.csv",
            after = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotification.after.csv"
    )
    void onSendSecondNotification() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(10L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(5L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(5L))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserSecondMessageMailingCalculator.TEMPLATE_ID);
        var namedContainer = (NamedContainer) notificationSendContext.getData().get(3);
        assertEquals(namedContainer.getContent(), "DROPSHIP");
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingNotificationSenderImplTest.onSendThirdNotification.before.csv",
            after = "PartnerOnboardingNotificationSenderImplTest.onSendThirdNotification.after.csv"
    )
    void onSendThirdNotification() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(50L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(50L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(8L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(8L))
                        )
                )
        );

        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserHelpDeskMailingCalculator.TEMPLATE_ID);
        var namedContainer = getContainerByName(notificationSendContext.getData(), "partner-id");
        assertThat(namedContainer.getContent()).isEqualTo(PARTNER_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingNotificationSenderImplTest.onSendFourthNotification.before.csv",
            after = "PartnerOnboardingNotificationSenderImplTest.onSendFourthNotification.after.csv"
    )
    void onSendFourthNotification() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(20L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(30L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(20L))
                        )
                )
        );

        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                FailedOnboardingMailingCalculator.TEMPLATE_ID);
        var namedContainer = getContainerByName(notificationSendContext.getData(), "partner-id");
        assertThat(namedContainer.getContent()).isEqualTo(PARTNER_ID);
    }

    @Test
    @DbUnitDataSet(after = "PartnerOnboardingNotificationSenderImplTest.onSendSuccessNotification.after.csv")
    void onSendSuccessNotification() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestCrossdockState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(25L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(20L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FULL,
                                now.minus(Duration.ofDays(10L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCKS,
                                Status.FULL,
                                now.minus(Duration.ofDays(5L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.FULL,
                                now.minus(Duration.ofDays(4L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.FULL,
                                now.minus(Duration.ofDays(3L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.TEST_ORDER,
                                Status.FULL,
                                now
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                SuccessfulOnboardingMailingCalculator.FBY_PLUS_TEMPLATE_ID);
        var namedContainer = getContainerByName(notificationSendContext.getData(), "partner-id");
        assertThat(namedContainer.getContent()).isEqualTo(PARTNER_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotificationNotSent.before.csv",
            after = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotificationNotSent.before.csv"
    )
    void onSendSecondNotificationNotSent() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(2L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.ENABLING,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.ENABLING,
                                now
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        ArgumentCaptor<NotificationSendContext> notificationCaptor = ArgumentCaptor
                .forClass(NotificationSendContext.class);
        verify(notificationService, never()).send(notificationCaptor.capture());
    }


    @Test
    @DbUnitDataSet(after = "PartnerOnboardingNotificationSenderImplTest.onSendFirstNotification.after.csv")
    void onSendFirstNotificationWithoutCompletedSteps() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(1L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1L))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserFirstMessageMailingCalculator.TEMPLATE_ID);
        var namedContainer = (NamedContainer) notificationSendContext.getData().get(0);
        assertTemplateContainsSections(namedContainer, List.of("SUPPLIER_INFO", "ASSORTMENT"));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotification.before.csv",
            after = "PartnerOnboardingNotificationSenderImplTest.onSendSecondNotification.after.csv"
    )
    void onSendSecondNotificationWithoutCompletedSteps() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(5L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(5L))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        // Отправляем второй раз, чтобы проверить, что отправлено будет только одно письмо
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                StuckUserSecondMessageMailingCalculator.TEMPLATE_ID);
        var namedContainer = (NamedContainer) notificationSendContext.getData().get(3);
        assertEquals(namedContainer.getContent(), "DROPSHIP");
    }

    @Test
    @DbUnitDataSet(after = "PartnerOnboardingNotificationSenderImplTest.onSendLastNotificationForLongDelayed.after.csv")
    void onSendLastNotificationForLongDelayed() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(50L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(50))
                        )
                )
        );
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);
        // Отправляем второй раз, чтобы проверить, что отправлено будет только одно письмо
        partnerOnboardingNotificationSender.sendOnboardingNotifications(partnerIdWithOnboardingState);

        var notificationSendContext = checkNotificationSendContext(PARTNER_ID,
                FailedOnboardingMailingCalculator.TEMPLATE_ID);
        var namedContainer = (NamedContainer) notificationSendContext.getData().get(0);
        assertThat(namedContainer.getContent()).isEqualTo(PARTNER_ID);
    }

    private NotificationSendContext checkNotificationSendContext(long partnerId, int templateId) {
        var notificationCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService).send(notificationCaptor.capture());
        NotificationSendContext notification = notificationCaptor.getValue();
        assertThat(notification.getShopId()).isEqualTo(partnerId);
        assertThat(notification.getTypeId()).isEqualTo(templateId);
        return notification;
    }


}
