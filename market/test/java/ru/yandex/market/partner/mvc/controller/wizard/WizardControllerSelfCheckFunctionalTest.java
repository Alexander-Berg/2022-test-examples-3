package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.moderation.self.SelfCheckScenario;
import ru.yandex.market.core.moderation.self.SelfCheckScenarioStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "Шаг самопроверка".
 * См {@link ru.yandex.market.core.wizard.step.SelfCheckStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv", "csv/selfCheck.before.csv"})
class WizardControllerSelfCheckFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    LogisticPartnerService logisticPartnerService;

    @Autowired
    AboPublicRestClient aboPublicRestClient;

    @Autowired
    @Qualifier("environmentService")
    EnvironmentService environmentService;

    @Autowired
    private WizardExperimentService alwaysCheckStocksExperiment;

    @Autowired
    private WizardExperimentService fbsSkipNewExperiment;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @BeforeEach
    void setUp() {
        environmentService.setValue(WizardExperimentsConfig.ALWAYS_CHECK_STOCKS_VAR, "1");
        alwaysCheckStocksExperiment.close();

        environmentService.setValue(WizardExperimentsConfig.FBS_SKIP_NEW_VAR, "1");
        fbsSkipNewExperiment.close();
    }

    @Test
    void testSelfCheckReady() {
        mockSaaSWithStocks(1);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2904L), any(), any()))
                .thenReturn(createNewbieSelfCheckDto(2904L));
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        var response = requestStep(290400, WizardStepType.SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY, Map.of(
                "selfCheckScenarios",
                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
        )));
    }

    @Test
    @DisplayName("Ошибка, если шаг недоступен")
    void testNotAvailableIfNotDropship() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.SELF_CHECK))
                .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("CROSSDOCK. Ошибка, если шаг недоступен")
    void testNotAvailableIfCrossdock() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.SELF_CHECK))
                .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Status = NONE, если нет фида в индексе")
    void testNoneStatusWhenHasNoFeedInIndex() {
        when(logisticPartnerService.hasActivePartnerRelation(any())).thenReturn(false);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2102L), any(), any()))
                .thenReturn(createNewbieSelfCheckDto(2102L));
        mockSaaSWithStocks(1);

        // when
        var response = requestStep(12102L, WizardStepType.SELF_CHECK);

        // then
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.NONE,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                        )
                )
        );
    }

    @Test
    @DisplayName("Status = EMPTY, если нет ни одного пройденного сценария и есть фид в тестовом индексе")
    void testEmptyStatusWhenNoSuccessStatusAndFeedInTest() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());

        when(aboPublicRestClient.getSelfCheckScenarios(eq(2100L), any(), any()))
                .thenReturn(createNewbieSelfCheckDto(2100L));
        mockSaaSWithStocks(1);

        var response = requestStep(12100L, WizardStepType.SELF_CHECK);
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.EMPTY,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                        )
                )
        );
    }

    @Test
    @DisplayName("Status = NONE, если нет активной связки с логистическим партнером")
    void testEmptyStatusWhenNoSuccessStatusAndFeedInProdAndNoActiveLogisticPartner() {
        doReturn(false).when(logisticPartnerService).hasActivePartnerRelation(any());

        when(aboPublicRestClient.getSelfCheckScenarios(eq(2103L), any(), any()))
                .thenReturn(createNewbieSelfCheckDto(2103L));
        mockSaaSWithStocks(1);

        var response = requestStep(12103L, WizardStepType.SELF_CHECK);
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.NONE,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                        )
                )
        );
    }

    @Test
    @DisplayName("Status = NONE, если нет склада")
    void testEmptyStatusWhenNoSuccessStatusAndFeedInProdAndNoWarehouse() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2104L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2104L), any(), any())).thenReturn(scenarios);
        when(logisticPartnerService.hasActivePartnerRelation(any())).thenReturn(false);
        mockSaaSWithStocks(1);

        var response = requestStep(12104L, WizardStepType.SELF_CHECK);
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.NONE,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                        )
                )
        );
    }

    private List<SelfCheckDTO> createNewbieSelfCheckDto(long shopId) {
        return List.of(
                new SelfCheckDTO(shopId, CheckOrderScenarioDTO.builder(shopId)
                        .withStatus(CheckOrderScenarioStatus.NEW)
                        .build())
        );
    }

    @Test
    @DisplayName("Status = RESTRICTED, хотя бы один пройденный сценарий")
    void testRestrictredStatusWhenNotAllSuccessStatus() {
        // given
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2100L), eq(PlacementType.DSBB), any()))
                .thenReturn(List.of(
                        new SelfCheckDTO(2100L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build()),
                        new SelfCheckDTO(2100L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build())
                ));
        mockSaaSWithStocks(1);

        // when
        var response = requestStep(12100L, WizardStepType.SELF_CHECK);

        // then
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.RESTRICTED,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(
                                        new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null),
                                        new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null)
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Status = FULL, все сценарии пройдены")
    @DbUnitDataSet(after = "csv/selfCheck.full.after.csv")
    void testFullStatusWhenAllSuccessStatus() {
        mockMarketId();
        // given
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2100L), eq(PlacementType.DSBB), any()))
                .thenReturn(List.of(
                        new SelfCheckDTO(2100L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()),
                        new SelfCheckDTO(2100L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build())
                ));
        mockSaaSWithStocks(1);

        // when
        var response = requestStep(12100L, WizardStepType.SELF_CHECK);

        // then
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.FULL,
                        Map.of(
                                "selfCheckScenarios",
                                List.of(
                                        new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null),
                                        new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null)
                                )
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
    @DisplayName("Шаг не поддерживает ДСБС партнеров")
    void testNotSupportedForDsbs() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SELF_CHECK))
                .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void testSelfCheckReadyFromSuccess() {
        mockSaaSWithStocks(1);
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2905L), any(), any()))
                .thenReturn(createNewbieSelfCheckDto(2905L));
        var response = requestStep(290500, WizardStepType.SELF_CHECK);
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.EMPTY,
                        Map.of("selfCheckScenarios", List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null)
                        ))
                )
        );
    }

    private static WizardStepStatus makeResponseStepStatus(Status status, @Nullable Map<String, Object> details) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SELF_CHECK)
                .withStatus(status)
                .withDetails(details)
                .build();
    }

    private void mockMarketId() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(100500L).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }
}
