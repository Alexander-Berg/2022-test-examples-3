package ru.yandex.market.core.partner.onboarding.sender.calculators;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.partner.onboarding.sender.MailingInfo;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.supplier.model.PartnerPlacementType;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipBySellerState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestFulfillmentState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.getContainerByName;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.getContainersByName;

class StuckUserSecondMessageMailingCalculatorTest {
    private ContactService contactService = Mockito.mock(ContactService.class);
    private BusinessService businessService = Mockito.mock(BusinessService.class);
    private StuckUserSecondMessageMailingCalculator mailing
            = new StuckUserSecondMessageMailingCalculator(contactService, businessService);

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMailingSentIfUserHasAvailableSteps(
            String name,
            PartnerOnboardingState partnerIdWithOnboardingState,
            PartnerPlacementType partnerPlacementType
    ) {
        String phone = "89164490000";
        String email = "ya@ya.ru";
        long businessId = 1000;
        ContactWithEmail contact = new ContactWithEmail();
        contact.setPhone(phone);
        contact.setEmails(Set.of(new ContactEmail(1, email, true, true)));
        Mockito.when(
                contactService.getContactsWithEmailWithRoles(
                        Mockito.eq(partnerIdWithOnboardingState.getCampaignId()),
                        Mockito.eq(List.of(InnerRole.SHOP_ADMIN))
                )
        ).thenReturn(List.of(contact));
        Mockito.when(
                businessService.getBusinessIdByPartner(Mockito.eq(partnerIdWithOnboardingState.getPartnerIdAsLong()))
        ).thenReturn(businessId);
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(StuckUserSecondMessageMailingCalculator.TEMPLATE_ID);

        Map<String, NamedContainer> containerMap = getContainersByName(info.getNotificationData());
        assertEquals(partnerPlacementType.name(), containerMap.get("model").getContent());
        assertEquals(phone, containerMap.get("phone").getContent());
        assertEquals(email, containerMap.get("email").getContent());
        assertEquals(businessId, containerMap.get("business-id").getContent());
    }

    /**
     * Если последний шаг завершился по нашей инициативе (напр. подтвердили заявление) - шлем, в отличие от первого,
     * т.к. это повторное письмо
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMailSentAfterOurApprove(
            String name,
            PartnerOnboardingState partnerIdWithOnboardingState,
            PartnerPlacementType partnerPlacementType
    ) {
        var now = Instant.now();

        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(StuckUserSecondMessageMailingCalculator.TEMPLATE_ID);

        NamedContainer model = getContainerByName(info.getNotificationData(), "model");
        assertEquals(model.getContent(), partnerPlacementType.name());
    }

    static public Stream<Arguments> testMailingSentIfUserHasAvailableSteps() {
        var now = Instant.now();
        return Stream.of(
                Arguments.of("Отправка письма FBS",
                        createTestDropshipState(
                                PARTNER_ID_ENTITY,
                                now.minus(Duration.ofDays(10L)),
                                List.of(
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SUPPLIER_INFO,
                                                Status.FULL,
                                                now.minus(Duration.ofDays(5L))
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.ASSORTMENT,
                                                Status.FILLED,
                                                now
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SELF_CHECK,
                                                Status.FILLED,
                                                now
                                        )
                                )
                        ),
                        PartnerPlacementType.DROPSHIP
                ),
                Arguments.of("Отправка письма DBS",
                        createTestDropshipBySellerState(
                                PARTNER_ID_ENTITY,
                                now.minus(Duration.ofDays(10L)),
                                List.of(
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SUPPLIER_INFO,
                                                Status.FULL,
                                                now.minus(Duration.ofDays(5L))
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.ASSORTMENT,
                                                Status.FILLED,
                                                now
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SELF_CHECK,
                                                Status.FILLED,
                                                now
                                        )
                                )
                        ),
                        PartnerPlacementType.DROPSHIP_BY_SELLER
                ),
                Arguments.of("Отправка письма FBY",
                        createTestFulfillmentState(
                                PARTNER_ID_ENTITY,
                                now.minus(Duration.ofDays(10L)),
                                List.of(
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SUPPLIER_INFO,
                                                Status.FULL,
                                                now.minus(Duration.ofDays(5L))
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.ASSORTMENT,
                                                Status.FILLED,
                                                now
                                        ),
                                        new PartnerOnboardingState.WizardStepData(
                                                WizardStepType.SELF_CHECK,
                                                Status.FILLED,
                                                now
                                        )
                                )
                        ),
                        PartnerPlacementType.FULFILLMENT
                )
        );
    }

    static public Stream<Arguments> testMailSentAfterOurApprove() {
        var now = Instant.now();
        return Stream.of(
                Arguments.of("Отправка письма FBS",
                        createTestDropshipState(
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
                        ),
                        PartnerPlacementType.DROPSHIP
                ),
                Arguments.of("Отправка письма DBS",
                        createTestDropshipBySellerState(
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
                        ),
                        PartnerPlacementType.DROPSHIP_BY_SELLER
                ),
                Arguments.of("Отправка письма FBY",
                        createTestFulfillmentState(
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
                        ),
                        PartnerPlacementType.FULFILLMENT
                )
        );
    }
}
