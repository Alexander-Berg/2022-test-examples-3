package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOfferStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.offer.mapping.OfferProcessingStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.supplier.summary.SupplierMappingSummaryService;
import ru.yandex.market.core.wizard.model.StatusDetail;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.mbi.datacamp.model.search.filter.CpaContentStatus;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Общий класс для функциональных тестов wizard'a настроек подключения.
 *
 * @author Vladislav Bauer
 */
@ParametersAreNonnullByDefault
abstract class AbstractWizardControllerFunctionalTest extends FunctionalTest {

    /**
     * Обычный Белый мгаазин.
     */
    static final long CAMPAIGN_ID = 10774;
    static final long SHOP_ID = 774;
    /**
     * SMB магазин с офферами.
     */
    static final long SMB_CAMPAIGN_ID = 10800;
    static final long SMB_PARTNER_ID = 800;
    /**
     * SMB магазин без офферов.
     */
    static final long NO_OFFER_SMB_CAMPAIGN_ID = 10321;
    static final long NO_OFFER_SMB_PARTNER_ID = 321;

    static final long CROSSBORDER_CAMPAIGN_ID = 10000;
    /**
     * Компания поставщика Fulfillment.
     * файл commonWizardBlueData.before.csv.
     */
    static final long FULFILLMENT_CAMPAIGN_ID = 12000;

    /**
     * Компания поставщика Fulfillment с неподтвержденным заявлением на подключение
     * файл commonWizardBlueData.before.csv.
     */
    static final long SUPPLIER_WITH_FAILED_REQ = 280000;

    /**
     * Компания поставщика Dropship.
     * файл commonWizardBlueData.before.csv.
     */
    static final long DROPSHIP_SUPPLIER_CAMPAIGN_ID = 12501;

    /**
     * Компания поставщика Dropship, настроен.
     * файл commonWizardBlueData.before.csv.
     */
    static final long DS_SUPPLIER_CAMPAIGN_ID = 12100;

    /**
     * Компания поставщика Dropship, работает через партнерский интерфейс.
     * файл commonWizardBlueData.before.csv.
     */
    static final long DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID = 12101;

    /**
     * Кампания поставщика Dropship, нет подтвержденного заявления
     */
    static final long DROPSHIP_NO_APPLICATION_CAMPAIGN_ID = 12105;

    /**
     * Кампания поставщика Dropship, фид только в продовом индексе
     */
    static final long DROPSHIP_FEED_ONLY_IN_PROD_INDEX_CAMPAIGN_ID = 12107L;

    /**
     * Кампания поставщика Dropship, нет фида в индексе
     */
    static final long DROPSHIP_NOT_IN_INDEX_CAMPAIGN_ID = 12106;

    /**
     * Кампания поставщика Crossdock.
     * файл commonWizardBlueData.before.csv.
     */
    static final long CROSSDOCK_SUPPLIER_CAMPAIGN_ID = 12601;

    /**
     * Кампания поставщика Crossdock, нет подтвержденного заявления
     */
    static final long CROSSDOCK_NO_APPLICATION_CAMPAIGN_ID = 12606;

    /**
     * Кампания поставщика Dropship. Click and collect.
     * файл commonWizardBlueData.before.csv.
     */
    static final long DS_CLICK_AND_COLLECT_CAMPAIGN_ID = 12603;

    /**
     * Кампания поставщика Dropship, имеющая несколько договоров на бизнесе
     * файл commonWizardBlueData.before.csv.
     */
    static final long DS_WITH_COMPLETED_REQ = 12108;

    /**
     * Кампания Fulfilment с контрактом
     */
    static final long FF_WITH_CONTRACT = 12110;

    /**
     * Кампания Dropship с контрактом
     */
    static final long DROPSHIP_WITH_CONTRACT = 12111;

    /**
     * Кампания поставщика Dropship с подтвержденой заявкой, но без контракта
     */
    static final long DS_WITH_REQUEST_BUT_WITHOUT_CONTRACT = 12112;

    /**
     * Кампания поставщика Dropship, нет данных в PUSHAPI_LOG.
     * файл pushapilogsBasedTest.before.csv.
     */
    static final long NO_PUSHAPI_LOGS_LOGS_CAMPAIGN_ID = 12200;
    /**
     * Кампания поставщика Dropship, нет успешных сообщений в PUSHAPI_LOG.
     * файл pushapilogsBasedTest.before.csv.
     */
    static final long NO_PUSHAPI_SUCCESS_LOGS_CAMPAIGN_ID = 12201;

    /**
     * Кампания поставщика Dropship, есть успешные сообщения в PUSHAPI_LOG.
     * файл pushapilogsBasedTest.before.csv.
     */
    static final long MORE_THAN_100_PUSHAPI_SUCCESS_LOG_RECORD_CAMPAIGN_ID = 12202;

    /**
     * Кампания поставщика Dropship, настроен, фича в SUCCESS
     * файл commonWizardBlueData.before.csv.
     */
    static final long DS_SUPPLIER_WITH_SUCCESS_DS_FEATURE = 270400;

    /**
     * Кампания поставщика Dropship, не подтверждена заявка в АБО.
     * файл pushapilogsBasedTest.before.csv.
     */
    static final long NOT_APPROVED_PREPAY_REQUEST_CAMPAIGN_ID = 12203;

    static final long DSBS_CAMPAIGN_ID = 14001;
    static final long DSBS_SHOP_ID = 4001;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected MboMappingsService patientMboMappingsService;

    @Autowired
    protected MbiLogProcessorClient logProcessorClient;

    @Autowired
    @Qualifier("dataCampShopClient")
    protected DataCampClient dataCampShopClient;

    @Autowired
    protected SupplierMappingSummaryService supplierMappingSummaryService;

    @Autowired
    protected SaasService saasService;

    @Autowired
    @Qualifier("environmentService")
    protected EnvironmentService environmentService;

    @Autowired
    protected WireMockServer integrationNpdWireMockServer;

    @Nonnull
    ResponseEntity<String> requestAllSteps(long campaignId) {
        String url = baseUrl + String.format("/campaigns/steps?id=%d&_user_id=123456", campaignId);
        return FunctionalTestHelper.get(url);
    }

    @Nonnull
    ResponseEntity<String> requestStep(long campaignId, @Nullable WizardStepType stepType) {
        String url = baseUrl + String.format("/campaigns/steps/%s?id=%d&_user_id=123456", stepType, campaignId);
        return FunctionalTestHelper.get(url);
    }

    static void assertResponse(ResponseEntity<String> response, Object object) {
        try {
            String string = OBJECT_MAPPER.writeValueAsString(object);
            JsonTestUtil.assertEquals(response, string);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    static void assertResponse(ResponseEntity<String> response, Object object, JSONCompareMode mode) {
        try {
            String string = OBJECT_MAPPER.writeValueAsString(object);
            JSONAssert.assertEquals(string,
                    new JSONObject(response.getBody()).getJSONObject("result").toString(), mode);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    void mockMboMappingService(SupplierOffer.OfferProcessingStatus... statuses) {
        when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(any())).
                thenReturn(
                        MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addAllOfferProcessingStatuses(Arrays.stream(statuses)
                                .map(status ->
                                        MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(status)
                                        .setOfferCount(3)
                                        .build()
                                )
                                .collect(Collectors.toList())
                        ).build()
                );
    }

    void mockSaasService(int result) {
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(result)
                .setOffers(List.of())
                .build();
        when(saasService.searchBusinessOffers(any()))
                .thenReturn(resultMock);
    }

    void mockPartnerOffers(int result) {
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(result)
                .setOffers(List.of())
                .build();
        Mockito.doReturn(resultMock).when(saasService).searchBusinessOffers(
                Mockito.argThat(filter -> CollectionUtils.isEmpty(filter.getResultOfferStatuses()))
        );
    }

    void mockSaaSWithStocks(int result) {
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(result)
                .setOffers(List.of())
                .build();
        Mockito.doReturn(resultMock).when(saasService).searchBusinessOffers(
                Mockito.argThat(filter -> filter.getResultOfferStatuses().equals(new TreeSet<>(List.of(
                        DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED,
                        DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED_AND_CHECKING,
                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED)))
                )
        );
    }

    void mockSaaSWithoutStocks(int result) {
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(result)
                .setOffers(List.of())
                .build();
        Mockito.doReturn(resultMock).when(saasService).searchBusinessOffers(
                Mockito.argThat(filter -> filter.getResultOfferStatuses().equals(new TreeSet<>(List.of(
                        DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED,
                        DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED_AND_CHECKING,
                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED,
                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_NO_STOCKS)))
                )
        );
    }

    Map<OfferProcessingStatus, StatusDetail> getUnitedCatalogMappingStats(int value) {
        return Arrays.stream(OfferProcessingStatus.values())
                .filter(s -> s != OfferProcessingStatus.NEED_MAPPING)
                .collect(Collectors.toMap(Function.identity(),
                        v -> StatusDetail.newBuilder().withCount(value).build()));
    }

    int getExpectedNumberOfUnitedCatalogOffers(int value) {
        return CpaContentStatus.values().length * value;
    }

    static WizardStepStatus makeResponseStepStatus(WizardStepType stepType, Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(stepType)
                .withStatus(status)
                .build();
    }

    static WizardStepStatus makeResponseStepStatus(
            WizardStepType stepType,
            Status status,
            Map<String, Object> details
    ) {
        return WizardStepStatus.newBuilder()
                .withStep(stepType)
                .withStatus(status)
                .withDetails(details)
                .build();
    }

    protected CurrentAndNextMonthPayoutFrequencyDTO createFrequencyDTO(
            long contractId,
            PayoutFrequencyDTO currentFrequency,
            PayoutFrequencyDTO nextFrequency,
            boolean isDefaultCurrentMonthFrequency
    ) {
        CurrentAndNextMonthPayoutFrequencyDTO dto = new CurrentAndNextMonthPayoutFrequencyDTO();
        dto.setContractId(contractId);
        dto.setCurrentMonthFrequency(currentFrequency);
        dto.setNextMonthFrequency(nextFrequency);
        dto.setIsDefaultCurrentMonthFrequency(isDefaultCurrentMonthFrequency);

        return dto;
    }

    protected void mockGetApplication(long partnerId, String partnerApplicationResponse) {
        integrationNpdWireMockServer.stubFor(get("/api/v1/partners/" + partnerId + "/application")
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withBody(partnerApplicationResponse))
        );
    }
}
