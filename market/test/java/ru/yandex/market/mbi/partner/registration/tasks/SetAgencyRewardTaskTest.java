package ru.yandex.market.mbi.partner.registration.tasks;

import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.AgencyCommissionRequest;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.balance.BalanceService;
import ru.yandex.market.mbi.partner.registration.model.BalanceClientInfo;
import ru.yandex.market.mbi.partner.registration.tasks.model.PartnerParams;
import ru.yandex.market.mbi.partner.registration.tasks.model.TaskContext;
import ru.yandex.market.mbi.partner.registration.util.IsRegistrationByAgencyAllowed;
import ru.yandex.mj.generated.server.model.SuggestedPartnerType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SetAgencyRewardTaskTest extends AbstractFunctionalTest {

    private static final Long AGENCY_UID = 1L;
    private static final Long SIMPLE_UID = 2L;
    private static final Long PARTNER_ID = 100L;

    @Autowired
    private BalanceService balanceService;
    @Autowired
    private IsRegistrationByAgencyAllowed isRegistrationByAgencyAllowed;
    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;
    @Autowired
    private SetAgencyRewardTask tested;

    @BeforeEach
    void configureMocks() {
        when(balanceService.getClientByUid(SIMPLE_UID)).thenReturn(
            Optional.of(BalanceClientInfo.builder()
                .withId(SIMPLE_UID)
                .withType(ClientType.OAO)
                .withIsAgency(false)
                .build())
        );
        when(balanceService.getClientByUid(AGENCY_UID)).thenReturn(
            Optional.of(BalanceClientInfo.builder()
                .withId(AGENCY_UID)
                .withType(ClientType.OAO)
                .withIsAgency(true)
                .build())
        );
        Mockito.reset(mbiOpenApiClient);
    }

    @Test
    @DisplayName("Вызов таски при выключенной регистрации агентствами")
    void testWhenRegistrationSwitchedOff() {
        when(isRegistrationByAgencyAllowed.get()).thenReturn(false);

        tested.executeVoid(new TaskContext<>(generateParamsForAgency(null), Mockito.mock(DelegateExecution.class)));
        verifyNoInteractions(mbiOpenApiClient);
    }

    @Test
    @DisplayName("Вызов таски при регистрации с обычного аккаунта")
    void testWhenRegistrationBySimpleUid() {
        when(isRegistrationByAgencyAllowed.get()).thenReturn(true);

        tested.executeVoid(new TaskContext<>(generateParamsForSimpleUid(), Mockito.mock(DelegateExecution.class)));
        verifyNoInteractions(mbiOpenApiClient);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getRegistrationByAgencyParams")
    @DisplayName("Вызов таски при регистрации агентством")
    void testWhenRegistrationViaAgency(
        String name,
        SuggestedPartnerType suggestedPartnerType,
        AgencyCommissionRequest.OnboardingRewardTypeEnum expectedRewardType
    ) {
        when(isRegistrationByAgencyAllowed.get()).thenReturn(true);

        tested.executeVoid(new TaskContext<>(
            generateParamsForAgency(suggestedPartnerType),
            Mockito.mock(DelegateExecution.class)
        ));
        verify(mbiOpenApiClient).setAgencyCommission(
            eq(AGENCY_UID),
            eq(new AgencyCommissionRequest()
                .partnerId(PARTNER_ID)
                .onboardingRewardType(expectedRewardType)
            ),
            eq(AGENCY_UID)
        );
    }

    public static Stream<Arguments> getRegistrationByAgencyParams() {
        return Stream.of(
            Arguments.of(
                "Проставление полной комиссии",
                SuggestedPartnerType.NEW_OGRN,
                AgencyCommissionRequest.OnboardingRewardTypeEnum.FULL
            ),
            Arguments.of(
                "Проставление частичной комиссии 1",
                SuggestedPartnerType.SUPPORT_EXISTING_LEAD,
                AgencyCommissionRequest.OnboardingRewardTypeEnum.PARTIAL
            ),
            Arguments.of(
                "Проставление частичной комиссии 2",
                SuggestedPartnerType.NEW_PLACEMENT_TYPE,
                AgencyCommissionRequest.OnboardingRewardTypeEnum.PARTIAL
            ),
            Arguments.of(
                "Без комиссии",
                null,
                AgencyCommissionRequest.OnboardingRewardTypeEnum.NONE
            )
        );
    }

    private PartnerParams generateParamsForAgency(SuggestedPartnerType type) {
        return PartnerParams.newBuilder()
            .setPartnerId(PARTNER_ID)
            .setUid(AGENCY_UID)
            .setSuggestedPartnerType(type)
            .build();
    }

    private PartnerParams generateParamsForSimpleUid() {
        return PartnerParams.newBuilder()
            .setPartnerId(PARTNER_ID)
            .setUid(SIMPLE_UID)
            .build();
    }
}
