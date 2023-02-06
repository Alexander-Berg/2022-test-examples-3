package ru.yandex.market.partner.mvc.controller.wizard;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.api.cpa.yam.dto.AutoFilledStateDTO;
import ru.yandex.market.api.cpa.yam.dto.ContactInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.functional.ExceptionfulRunnable;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.tax.model.ShopVat;
import ru.yandex.market.core.wizard.SupplierCheckOrderAssortmentRequirement;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.StepStatusCalculator;
import ru.yandex.market.core.wizard.step.dto.PartnerApplicationWizardDto;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.partner.mvc.controller.wizard.utils.DatacampFlagResponseMocker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorListMatchesInAnyOrder;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorMatches;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Общие функциональные тесты для {@link WizardController}.
 */
@DbUnitDataSet(before = "csv/partnerOnboardingUseAssortmentCalculator.csv")
class WizardControllerCommonFunctionalTest extends AbstractWizardControllerFunctionalTest {
    @Autowired
    private TestableClock clock;

    @Autowired
    SupplierCheckOrderAssortmentRequirement supplierCheckOrderAssortmentRequirement;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    @Autowired
    private LogisticPartnerService logisticPartnerService;

    private DatacampFlagResponseMocker datacampMocker;

    @BeforeEach
    void setUp() {
        supplierCheckOrderAssortmentRequirement.close(); // clear cache
        datacampMocker = new DatacampFlagResponseMocker(dataCampShopClient);
    }

    /**
     * Проверить что для каждого шага, есть подходящий калькулятор.
     */
    @Test
    void testCalculatorsCount() {
        List<WizardStepType> stepTypes = Arrays.stream(WizardStepType.values())
                .filter(v -> !WizardStepType.getDeprecated().contains(v)).collect(Collectors.toList());

        Collection<StepStatusCalculator> calculators =
                applicationContext.getBeansOfType(StepStatusCalculator.class).values();

        assertThat(calculators.size(), equalTo(stepTypes.size()));

        for (WizardStepType stepType : stepTypes) {
            boolean hasCorrespondingCalculator = calculators.stream()
                    .map(StepStatusCalculator::getType)
                    .anyMatch(type -> type == stepType);

            assertThat(hasCorrespondingCalculator, equalTo(true));
        }
    }

    /**
     * Получить статусы по всем шагам.
     */
    @Test
    @DbUnitDataSet(before = {
            "csv/commonWhiteWizardData.before.csv",
            "csv/testDeliveryStepFilledDepot.before.csv",
            "csv/testFeedStepFilled.before.csv",
            "csv/testLegalStepFilled.before.csv",
            "csv/testProgramStepCpcFilledWithPhone.before.csv"
    })
    void testAllSteps() {
        var response = requestAllSteps(CAMPAIGN_ID);
        assertResponse(response, List.of(
                makeResponseStepStatus(WizardStepType.LEGAL, Status.FILLED),
                makeResponseStepStatus(WizardStepType.FEED, Status.FILLED),
                makeResponseStepStatus(WizardStepType.DELIVERY, Status.FILLED),
                makeResponseStepStatus(WizardStepType.SETTINGS, Status.FILLED)
        ));
    }

    /**
     * Проверяет все статусы для SMB магазина.
     */
    @Test
    @DbUnitDataSet(before = {
            "csv/commonWhiteWizardData.before.csv",
            "csv/testDeliveryStepFilledDepot.before.csv",
            "csv/testFeedStepFilled.before.csv",
            "csv/testLegalStepFilled.before.csv",
            "csv/testProgramStepCpcFilledWithPhone.before.csv"
    })
    void testAllSmbSteps() {
        mockPartnerOffers(1);

        prepareTarifficatorResponseEmpty(SMB_PARTNER_ID);

        var response = requestAllSteps(SMB_CAMPAIGN_ID);
        assertResponse(response, List.of(
                makeResponseStepStatus(WizardStepType.LEGAL, Status.EMPTY),
                makeResponseStepStatus(WizardStepType.OFFER, Status.FILLED),
                makeResponseStepStatus(WizardStepType.DELIVERY, Status.EMPTY),
                makeResponseStepStatus(WizardStepType.SETTINGS, Status.EMPTY)
        ));
    }

    /**
     * Проверяет все статусы для SMB магазина.
     */
    @Test
    @DbUnitDataSet(before = {
            "csv/commonWhiteWizardData.before.csv",
            "csv/testDeliveryStepFilledDepot.before.csv",
            "csv/testFeedStepFilled.before.csv",
            "csv/testLegalStepFilled.before.csv",
            "csv/testProgramStepCpcFilledWithPhone.before.csv"
    })
    void testAllNoOffersSmbSteps() {
        mockPartnerOffers(0);

        prepareTarifficatorResponseEmpty(NO_OFFER_SMB_PARTNER_ID);

        var response = requestAllSteps(NO_OFFER_SMB_CAMPAIGN_ID);
        assertResponse(response, List.of(
                makeResponseStepStatus(WizardStepType.LEGAL, Status.EMPTY),
                makeResponseStepStatus(WizardStepType.OFFER, Status.EMPTY),
                makeResponseStepStatus(WizardStepType.DELIVERY, Status.EMPTY),
                makeResponseStepStatus(WizardStepType.SETTINGS, Status.EMPTY)
        ));
    }

    /**
     * Проверить что нельзя получить статус всех шагов по несуществующей кампании.
     */
    @Test
    void testAllStepsUnknownCampaign() {
        sendBadRequest(
                () -> requestAllSteps(-1),
                ex -> {
                    assertThat(ex, hasErrorCode(HttpStatus.NOT_FOUND));
                    assertThat(ex, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "campaignId", "INVALID")));
                }
        );
    }

    /**
     * Проверить что нельзя получить статус конкретного шага по несуществующей кампании.
     */
    @Test
    void testStepUnknownCampaign() {
        for (WizardStepType stepType : WizardStepType.values()) {
            sendBadRequest(
                    () -> requestStep(-1, stepType),
                    ex -> {
                        assertThat(ex, hasErrorCode(HttpStatus.NOT_FOUND));
                        assertThat(ex, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "campaignId", "INVALID")));
                    }
            );
        }
    }

    /**
     * Проверить что нельзя получить статус несуществующего шага.
     */
    @Test
    @DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
    void testStepUnknownStep() {
        sendBadRequest(
                () -> requestStep(CAMPAIGN_ID, null),
                ex -> {
                    assertThat(ex, hasErrorCode(HttpStatus.NOT_FOUND));
                    assertThat(ex, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "step", "INVALID")));
                }
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
    void testAllStepsForFulfillmentSupplierUnitedCatalog() {
        mockPartnerOffers(0);
        mockSaaSWithoutStocks(0);

        var response = requestAllSteps(290000L);
        assertResponse(response, List.of(
                makeResponseStepStatus(
                        WizardStepType.COMMON_INFO,
                        Status.FULL,
                        Map.of("emails", List.of(), "isCpaPartnerInterface", false)
                ),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.SUPPLIER_INFO)
                        .withStatus(Status.FULL)
                        .withDetails(makeSupplierInfoDetails("fulfillment", "s2900", 2900L))
                        .build(),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.ASSORTMENT)
                        .withStatus(Status.EMPTY)
                        .withDetails(Map.of(
                                "numberOfUnitedOffersAvailable", 0,
                                "numberOfUnitedOffersPartner", 0,
                                "isUnitedCatalog", true
                        ))
                        .build(),
                makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE)
        ));
    }

    @Test
    @DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
    void testAllStepsForFulfillmentSupplierUnitedCatalogWithPrices() {
        mockPartnerOffers(5);
        mockSaaSWithoutStocks(5);

        var response = requestAllSteps(290000L);
        assertResponse(response, List.of(
                makeResponseStepStatus(
                        WizardStepType.COMMON_INFO,
                        Status.FULL,
                        Map.of("emails", List.of(), "isCpaPartnerInterface", false)
                ),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.SUPPLIER_INFO)
                        .withStatus(Status.FULL)
                        .withDetails(makeSupplierInfoDetails("fulfillment", "s2900", 2900L))
                        .build(),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.ASSORTMENT)
                        .withStatus(Status.FULL)
                        .withDetails(Map.of(
                                "numberOfUnitedOffersAvailable", 0,
                                "numberOfUnitedOffersPartner", 5,
                                "isUnitedCatalog", true,
                                "hasValidOffer", true
                        ))
                        .build(),
                makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY)
        ));
    }

    /**
     * Получить статус по шагам дропшипа с Единым каталогом и ассортиментом.
     */
    @Test
    @DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
    void testAllStepsForDropshipSupplierUnitedCatalogWithAssortment() {
        mockPartnerOffers(5);
        mockSaaSWithStocks(1);
        mockSaaSWithoutStocks(1);

        var response = requestAllSteps(810000L);
        assertResponse(response, List.of(
                makeResponseStepStatus(
                        WizardStepType.COMMON_INFO,
                        Status.FULL,
                        Map.of("emails", List.of(), "isCpaPartnerInterface", true)
                ),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.SUPPLIER_INFO)
                        .withStatus(Status.FULL)
                        .withDetails(makeSupplierInfoDetails("dropship", true, "s2905", 8100L))
                        .build(),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.ASSORTMENT)
                        .withStatus(Status.FULL)
                        .withDetails(Map.of(
                                "numberOfUnitedOffersAvailable", 0,
                                "numberOfUnitedOffersPartner", 5,
                                "isUnitedCatalog", true,
                                "warehouseSet", false,
                                "hasValidOffer", true
                        ))
                        .build(),
                makeResponseStepStatus(WizardStepType.WAREHOUSE, Status.EMPTY),
                WizardStepStatus.newBuilder()
                        .withStep(WizardStepType.MARKETPLACE)
                        .withDetails(Map.of("featureStatus", "DONT_WANT"))
                        .withStatus(Status.NONE)
                        .build()
        ));
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    private static void sendBadRequest(
            ExceptionfulRunnable<HttpClientErrorException> operation,
            Consumer<? super HttpClientErrorException> onError
    ) {
        HttpClientErrorException error =
                Assertions.assertThrows(HttpClientErrorException.class, operation::run);

        onError.accept(error);
    }

    private static Map<String, ?> makeCheckOrderDetails(
            boolean isPartnerInterface
    ) {
        return Map.of(
                "isPartnerInterface", isPartnerInterface,
                "attempts", 0,
                "offersCurrent", 0,
                "offersRequired", 0
        );
    }

    private static Map<String, ?> makeSupplierInfoDetails(
            String deliveryServiceType,
            boolean isCpaPartnerInterface,
            String name,
            Long datasourceId
    ) {
        return Map.of(
                "deliveryServiceType", deliveryServiceType,
                "isCpaPartnerInterface", isCpaPartnerInterface,
                "isClickAndCollect", false,
                "partnerApplication", getPartnerApplication(name, datasourceId),
                "payoutFrequencyEnabled", true,
                "contracts", Collections.emptyList()
        );
    }

    private static Map<String, ?> makeSupplierInfoDetails(
            String deliveryServiceType, String name, long datasourceId
    ) {
        return makeSupplierInfoDetails(deliveryServiceType, false, name, datasourceId);
    }

    private static PartnerApplicationWizardDto getPartnerApplication(String name, long datasourceId) {
        OrganizationInfoDTO.Builder orgInfoBuilder = OrganizationInfoDTO.builder()
                .name("orgName")
                .type(OrganizationType.OOO)
                .ogrn("12345")
                .inn("7743880975")
                .factAddress("factAddr")
                .juridicalAddress("jurAddrr")
                .accountNumber("12345678901234567890")
                .corrAccountNumber("12345678901234567890")
                .bik("123456789")
                .bankName("bankName")
                .licenseNumber("licenseNum")
                .licenseDate(LocalDate.of(2017, 11, 17))
                .postcode("109341")
                .kpp("123456789")
                .isAutoFilled(false)
                .autoFilledState(AutoFilledStateDTO.EMPTY);

        ShopVat vatInfo = new ShopVat();
        vatInfo.setDatasourceId(datasourceId);

        return new PartnerApplicationWizardDto(
                name, orgInfoBuilder.build(), new ContactInfoDTO(), vatInfo, Collections.emptyList()
        );
    }

    private void prepareTarifficatorResponseOK(long shopId) {
        ResponseDefinitionBuilder shopStateResponse = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "json/tarifficatorShopStateResponseOk.json"));

        tarifficatorWireMockServer.stubFor(post("/v2/shops/delivery/state")
                .withRequestBody(equalToJson(String.format(
                        "{\"shopIds\": %s}", List.of(shopId)
                )))
                .willReturn(shopStateResponse));
    }

    private void prepareTarifficatorResponseEmpty(long shopId) {
        ResponseDefinitionBuilder shopStateResponse = aResponse().withStatus(200)
                .withBody("{}");

        tarifficatorWireMockServer.stubFor(post("/v2/shops/delivery/state")
                .withRequestBody(equalToJson(String.format(
                        "{\"shopIds\": %s}", List.of(shopId)
                )))
                .willReturn(shopStateResponse));
    }

}
