package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.moderation.self.SelfCheckScenario;
import ru.yandex.market.core.moderation.self.SelfCheckScenarioStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тест шага "Самопроверка белых магазинов".
 * {@link ru.yandex.market.core.wizard.step.ShopSelfCheckStepStatusCalculator}.
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerShopSelfCheckFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private WizardExperimentService disableIndexingCheckExperiment;

    @BeforeEach
    void before() {
        disableIndexingCheckExperiment.close();
    }

    /**
     * Проверить, что возвращается NONE для недонастроенных магазинов.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getNonFullyConfiguredPartnerCampaigns")
    @DbUnitDataSet(before = "csv/dsbsNotFullyConfigured.csv")
    void testDsbsNotFullyConfigured(String name, long campaignId) {
        mockDataCamp(4006L, "json/datacamp.empty.json");

        ResponseEntity<String> response = requestStep(campaignId, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(Status.NONE, Map.of("selfCheckScenarios", List.of())));
    }

    /**
     * Проверить, что возвращается NONE для недонастроенных магазинов.
     */
    @Test
    @DisplayName("Проверка стоков перед самопроверкой, нет стоков")
    @DbUnitDataSet(before = "csv/testDsbsNotFullyConfiguredWithSaasStocks.csv")
    void testDsbsNotFullyConfiguredWithSaasStocks() {
        mockSaaSWithStocks(0);

        ResponseEntity<String> response = requestStep(14006L, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(Status.NONE, Map.of("selfCheckScenarios", List.of())));
    }

    /**
     * Проверка возврата EMPTY при отсутствии фичи MARKETPLACE_SELF_DELIVERY и выключенной проверки окончания индексации
     */
    @Test
    @DbUnitDataSet(before = "csv/dsbsFullyConfiguredNoSelfCheck.csv")
    @DbUnitDataSet(before = "csv/environmentSet.csv")
    void testMarketplaceSelfDeliveryNotNewOrSuccessIndexingCheckDisabled() {

        when(aboPublicRestClient.getSelfCheckScenarios(eq(4013L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4013L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())));

        ResponseEntity<String> response = requestStep(14013L, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY, Map.of("selfCheckScenarios",
                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null)))));
    }

    /**
     * Проверка корректности выставления статуса самопроверки при выключенной проверке окончания индексации
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getCampaignsSelfCheckInProgressNew")
    @DbUnitDataSet(before = "csv/dsbsFullyConfiguredNoSelfCheck.csv")
    @DbUnitDataSet(before = "csv/environmentSet.csv")
    void testSelfCheckInProgressIndexingCheckingDisabled(String name, long campaignId, Status status, Map<String,Object> details) {
        mockAboForChecking();
        mockPartnerOffers(1);

        var response = requestStep(campaignId, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(status, details));
    }

    /**
     * Проверить, что возвращается NONE для недонастроенных магазинов.
     */
    @Test
    @DisplayName("Проверка стоков перед самопроверкой, есть стоки")
    @DbUnitDataSet(before = "csv/testDsbsConfiguredWithSaasStocks.csv")
    void testDsbsConfiguredWithSaasStocks() {
        mockSaaSWithStocks(10);

        when(aboPublicRestClient.getSelfCheckScenarios(eq(4007L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4007L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())));

        ResponseEntity<String> response = requestStep(14007L, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(Status.FILLED, Map.of("selfCheckScenarios",
                List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))))
        );
    }

    /**
     * Проверить, что возвращается корректный статус для магазинов, проходящих
     * самопроверку.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getCampaignsSelfCheckInProgress")
    @DbUnitDataSet(before = "csv/dsbsFullyConfiguredNoSelfCheck.csv")
    void testSelfCheckInProgress(String name, long campaignId, Status status, Map<String,Object> details) {
        mockDataCamp(4002L, "json/datacamp.filled.json");
        mockAbo();
        mockPartnerOffers(1);

        var response = requestStep(campaignId, WizardStepType.SHOP_SELF_CHECK);
        assertResponse(response, makeResponseStepStatus(status, details));
    }

    private void mockDataCamp(long partnerId, String responseFile) {
        final var dataCampResponse = ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class, responseFile, getClass());
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(request -> Objects.equals(request.getPartnerId(), partnerId))
                );
    }

    private void mockAbo() {
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4005L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4005L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()),
                        new SelfCheckDTO(4005L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build())
                ));
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4006L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.API)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4006L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build()),
                        new SelfCheckDTO(4006L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build())
                ));
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4004L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.API)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4004L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build()),
                        new SelfCheckDTO(4004L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())
                ));
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4007L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4007L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())));
    }

    private void mockAboForChecking() {
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4014L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4014L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()),
                        new SelfCheckDTO(4014L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build())
                ));
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4015L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4015L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()),
                        new SelfCheckDTO(4015L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())
                ));
        when(aboPublicRestClient.getSelfCheckScenarios(eq(4017L), eq(PlacementType.DSBS),
                eq(OrderProcessMethod.PI)))
                .thenReturn(List.of(
                        new SelfCheckDTO(4017L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build())));
    }


    private static Stream<Arguments> getNonFullyConfiguredPartnerCampaigns() {
        return Stream.of(
                Arguments.of("Магазин сконфигуренный, но без prepay-request", 14004L),
                Arguments.of("Магазин сконфигуренный, но без настроенного способа обработки заказов", 14003L),
                Arguments.of("Магазин сконфигуренный, но без доставки", 14001L),
                Arguments.of("Магазин сконфигуренный, но без фида", 14002L),
                Arguments.of("Магазин сконфигуренный, заявка не отправлена на проверку", 14005L),
                Arguments.of("Пуш магазин без офферов в датакэмпе", 14006L)
        );
    }

    private static Stream<Arguments> getCampaignsSelfCheckInProgress() {
        return Stream.of(
                Arguments.of("Селф чек в статусе инит", 14001L, Status.EMPTY, Map.of("selfCheckScenarios", List.of())),
                Arguments.of("Фид индексируется в ПШ", 14002L, Status.ENABLING, Map.of("selfCheckScenarios", List.of())),
                Arguments.of("При идексации в ПШ ошибки", 14003L, Status.FAILED, Map.of("selfCheckScenarios", List.of())),
                Arguments.of("Нет датасурс_ин_тестинг, но сценарии зафейлены все (может быть при повторных " +
                        "проверках)", 14004L, Status.FULL, Map.of(
                        "selfCheckScenarios",
                        List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null),
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null)
                        )
                )),
                Arguments.of("Селф чек в чекинг, все сценарии пройдены", 14005L, Status.FULL, Map.of(
                        "selfCheckScenarios",
                        List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null),
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null)
                        )
                )),
                Arguments.of("Селф чек удален, сценарии частчно пройдены", 14006L, Status.FULL, Map.of(
                        "selfCheckScenarios",
                        List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null),
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null)
                        )
                )),
                Arguments.of("Датасурс_ин_тестинг в CHECKING, но ни один сценарий не пройден", 14007L, Status.FILLED, Map.of(
                        "selfCheckScenarios",
                        List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                )),
                Arguments.of("Датасурс_ин_тестинг в INIT, но фид уже в ПШ", 14008L, Status.FILLED, Map.of("selfCheckScenarios", List.of()))
        );
    }

    private static Stream<Arguments> getCampaignsSelfCheckInProgressNew() {
        return Stream.of(
                Arguments.of("Выключена проверка окончания индексации, статус CHECKING, все сценарии пройдены", 14014L, Status.FULL, Map.of(
                        "selfCheckScenarios",
                        List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null),
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null)
                        )
                )),
                Arguments.of("Выключена проверка окончания индексации, статус CHECKING, не все сценарии пройдены", 14015L, Status.RESTRICTED, Map.of(
                        "selfCheckScenarios",
                        List.of(
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.SUCCESS, null),
                                new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null)
                        )
                )),
                Arguments.of("Выключена проверка окончания индексации, статус CHECKING, ни один сценарий не пройден", 14017L, Status.FILLED, Map.of(
                        "selfCheckScenarios",
                        List.of(new SelfCheckScenario(null, SelfCheckScenarioStatus.NEW, null))
                ))
        );
    }

    private static WizardStepStatus makeResponseStepStatus(Status status, Map<String, Object> details) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_SELF_CHECK)
                .withStatus(status)
                .withDetails(details)
                .build();
    }
}
