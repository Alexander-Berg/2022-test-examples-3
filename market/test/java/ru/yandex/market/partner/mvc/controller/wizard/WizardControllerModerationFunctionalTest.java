package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.ModerationStepStatusCalculator;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig.SHOP_SELF_CHECK_STOCKS_EXP_VAR;

/**
 * Тест шага визарда {@link ModerationStepStatusCalculator}.
 */
@DbUnitDataSet(before = {"csv/commonWhiteWizardData.before.csv", "csv/dsbsModeration.before.csv"})
class WizardControllerModerationFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void mocks() {
        when(aboPublicRestClient.getSelfCheckScenarios(longThat(shopId ->
                        Set.of(4001L, 4002L, 4004L, 4005L, 4006L, 4007L, 4008L, 4011L).contains(shopId)),
                Mockito.eq(PlacementType.DSBS),
                Mockito.any(OrderProcessMethod.class)))
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
        when(aboPublicRestClient.getSelfCheckScenarios(
                longThat(shopId -> Set.of(4003L, 4005L).contains(shopId)), Mockito.eq(PlacementType.DSBS),
                Mockito.any(OrderProcessMethod.class)))
                .thenReturn(List.of(
                        new SelfCheckDTO(2100L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.IN_PROGRESS)
                                        .build())
                ));
        var dataCampResponse = ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class, "json" +
                "/datacamp.empty.json", getClass());
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(request -> Objects.equals(request.getPartnerId(), 4007L))
                );
        mockDatacampStocks(4008L);
        mockDatacampStocks(4009L);
        mockDatacampStocks(4011L);
    }

    private void mockDatacampStocks(long l) {
        var dataCampResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "json/datacamp.filled.json", getClass()
        );
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(request -> Objects.equals(request.getPartnerId(), l))
                );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getModerationNotRequestedPartnerCampaigns")
    void testCorrectStatusReturn(String name, long campaignId, Status expectedStatus, int offersCount) {
        if (campaignId == 14006L) {
            when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                    List.of(
                            createFrequencyDTO(40060, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false)
                    )
            );
        }
        mockPartnerOffers(offersCount);
        var response = requestStep(campaignId, WizardStepType.MODERATION);

        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MODERATION)
                .withStatus(expectedStatus)
                .build());
    }

    @Test
    void testCompletedShopWithoutPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(40060, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, true)
                )
        );

        var response = requestStep(14006L, WizardStepType.MODERATION);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MODERATION)
                .withStatus(Status.NONE)
                .build());
    }

    @Test
    void testCompletedShopWithPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(40060, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false)
                )
        );

        var response = requestStep(14006L, WizardStepType.MODERATION);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MODERATION)
                .withStatus(Status.FULL)
                .build());
    }

    @Test
    @DisplayName("Магазин настроен, есть стоки, модерация пройдена")
    void testModerationWithSaasStocks() {
        environmentService.setValue(SHOP_SELF_CHECK_STOCKS_EXP_VAR, "1");

        mockSaasService(10);

        var response = requestStep(14007L, WizardStepType.MODERATION);

        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MODERATION)
                .withStatus(Status.FULL)
                .build());
    }

    @Test
    @DisplayName("Магазин настроен, но нет стоков, модерация еще не начата")
    void testModerationNoSaasStocks() {
        environmentService.setValue(SHOP_SELF_CHECK_STOCKS_EXP_VAR, "1");

        mockSaasService(0);

        var response = requestStep(14007L, WizardStepType.MODERATION);

        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MODERATION)
                .withStatus(Status.NONE)
                .build());
    }

    private static Stream<Arguments> getModerationNotRequestedPartnerCampaigns() {
        return Stream.of(
                Arguments.of("Магазин настроен, модерация еще не начата", 14001L, Status.EMPTY, 1),
                Arguments.of("Магазин без фида, модерация еще не начата", 14002L, Status.NONE, 0),
                Arguments.of("Настроенный магазин с непройденной самопроверкой", 14003L, Status.NONE, 1),
                Arguments.of("Настроенный магазин без самопроверки в эксперименте", 14000L, Status.FILLED, 1),
                Arguments.of("Магазин, у которого фича в DONT_WANT но проверка зафейлена",
                        14004L, Status.FAILED, 1),
                Arguments.of("Модерация в прогрессе", 14005L, Status.FILLED, 1),
                Arguments.of("Модерация завершена", 14006L, Status.FULL, 1),
                Arguments.of("Модерация пуш магазина, чьи оффера не пролезли в офферное", 14007L, Status.NONE, 0),
                Arguments.of("Модерация пуш магазина завершена", 14008L, Status.FULL, 1),
                Arguments.of("Модерация недоступна без заапрувленной заявки", 14009L, Status.NONE, 1),
                Arguments.of("Модерация приостановлена", 14011L, Status.RESTRICTED, 1),
                Arguments.of("Есть заявка нет контракта -> выплату можно пропустить", 14012L, Status.FULL, 1)
        );
    }
}
