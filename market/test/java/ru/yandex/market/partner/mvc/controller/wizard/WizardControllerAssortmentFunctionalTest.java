package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты для шага wizard'a "Шаг каталог товаров".
 * См {@link ru.yandex.market.core.wizard.step.AssortmentStepStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv",
                         "csv/partnerOnboardingUseAssortmentCalculator.csv"})
class WizardControllerAssortmentFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    private WizardExperimentService alwaysCheckStocksExperiment;

    @BeforeEach
    void setUp() {
        environmentService.setValue(WizardExperimentsConfig.ALWAYS_CHECK_STOCKS_VAR, "1");
        alwaysCheckStocksExperiment.close();
    }

    @Test
    @DisplayName("Проверить, что шаг товаров зеленый для дропшипа в ЕКат, когда есть стоки и цены")
    void testAssortmentStepUnitedCatalogPiFull() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(5);

        var response = requestStep(810000L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "warehouseSet", false,
                        "hasValidOffer", true
                ))
                .build());
    }

    @Test
    @DisplayName("Проверить, что товаров шаг пустой когда нет стоков для дропшипа в ЕКат")
    void testAssortmentStepUnitedCatalogPiEmpty() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(0);

        var response = requestStep(810000L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FILLED)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "warehouseSet", false,
                        "hasValidOffer", false
                ))
                .build());
    }

    @Test
    @DisplayName("Проверить, что шаг товаров зеленый для дропшипа в ЕКат АПИ есть цены")
    void testAssortmentStepUnitedCatalogApiFull() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(5);

        var response = requestStep(810200L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "hasValidOffer", true,
                        "warehouseSet", false
                ))
                .build());
    }

    @Test
    @DisplayName("Проверить, что шаг товаров пустой когда нет цен для дропшипа в ЕКат АПИ")
    void testAssortmentStepUnitedCatalogApiEmpty() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(0);

        var response = requestStep(810200L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FILLED)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "hasValidOffer", false,
                        "warehouseSet", false
                ))
                .build());
    }

    @Test
    @DisplayName("Проверить, что шаг товаров зеленый когда Saas возвращает годные офера")
    void testAssortmentStepUnitedCatalogSaas() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(5);

        var response = requestStep(810200L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "hasValidOffer", true,
                        "warehouseSet", false
                ))
                .build());
        // Ходим три раза - один раз для бизнеса и два раза для партнера (все и годные)
        verify(saasService, times(3)).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Проверить, что шаг товаров зеленый когда Saas возвращает годные офера и не показываем детали")
    void testAssortmentStepUnitedCatalogSaasWithoutStats() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(5);

        var response = requestStep(810200L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "hasValidOffer", true,
                        "warehouseSet", false
                ))
                .build());
        // Ходим три раза - один раз для бизнеса и два раза для партнера (все и годные)
        verify(saasService, times(3)).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Проверить, что шаг товаров EMPTY когда Saas не возвращает оферов и не показываем детали")
    void testAssortmentStepUnitedCatalogSaasWithoutStatsNoOffers() {
        mockPartnerOffers(0);

        var response = requestStep(810200L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.EMPTY)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 0,
                        "isUnitedCatalog", true
                ))
                .build());
        // Ходим два раза - один раз для бизнеса, второй для партнера
        verify(saasService, times(2)).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Не проверяем стоки у FBY")
    void testAssortmentFullNoStocksFby() {
        mockPartnerOffers(1);
        mockSaaSWithoutStocks(1);

        var response = requestStep(290000L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 1,
                        "isUnitedCatalog", true,
                        "hasValidOffer", true
                ))
                .build());
        // Ходим три раза - один раз для бизнеса и два раза для партнера (все и годные)
        verify(saasService, times(3)).searchBusinessOffers(any());
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/dsbsAssortmentStep.before.csv",
            "csv/dsbsAssortmentStep.stocksSwitcher.before.csv"
    })
    @DisplayName("Вызов шага для ДБС с запросом обязательных стоков")
    void dbsWithStocks() {
        mockPartnerOffers(10);
        mockSaaSWithStocks(5);

        var response = requestStep(10321L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FULL)
                .withDetails(Map.of(
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true,
                        "hasValidOffer", true,
                        "warehouseSet", false
                ))
                .build());
        // Ходим три раза - один раз для бизнеса и два раза для партнера (все и годные)
        verify(saasService, times(3)).searchBusinessOffers(any());
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/dsbsAssortmentStep.before.csv"
    })
    @DisplayName("Вызов шага для ДБС без запроса обязательных стоков")
    void dbsWithoutStocks() {
        mockPartnerOffers(10);
        mockSaaSWithoutStocks(0);

        var response = requestStep(10321L, WizardStepType.ASSORTMENT);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ASSORTMENT)
                .withStatus(Status.FILLED)
                .withDetails(Map.of(
                        "hasValidOffer", false,
                        "numberOfUnitedOffersAvailable", 0,
                        "numberOfUnitedOffersPartner", 10,
                        "isUnitedCatalog", true
                ))
                .build());
        // Ходим три раза - один раз для бизнеса и два раза для партнера (все и годные)
        verify(saasService, times(3)).searchBusinessOffers(any());
    }
}
