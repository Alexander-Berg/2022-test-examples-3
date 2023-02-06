package ru.yandex.market.core.supplier.promo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import NMarketIndexer.Common.Common;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.core.supplier.promo.dao.PromoOffersValidationParamsDao;
import ru.yandex.market.core.supplier.promo.dto.PiPromoMechanicDto;
import ru.yandex.market.core.supplier.promo.model.ChangedOffer;
import ru.yandex.market.core.supplier.promo.model.PromoType;
import ru.yandex.market.core.supplier.promo.model.context.CashbackTemplateContext;
import ru.yandex.market.core.supplier.promo.model.context.CheapestAsGiftTemplateContext;
import ru.yandex.market.core.supplier.promo.model.context.DiscountTemplateContext;
import ru.yandex.market.core.supplier.promo.model.context.PromocodeTemplateContext;
import ru.yandex.market.core.supplier.promo.model.multi.BasePromoDetails;
import ru.yandex.market.core.supplier.promo.model.multi.CashbackDetails;
import ru.yandex.market.core.supplier.promo.model.multi.CustomCashbackInfo;
import ru.yandex.market.core.supplier.promo.model.multi.DateRange;
import ru.yandex.market.core.supplier.promo.model.multi.OfferPromosEnablingInfo;
import ru.yandex.market.core.supplier.promo.model.multi.PromoEnablingInfo;
import ru.yandex.market.core.supplier.promo.model.multi.StandardCashbackInfo;
import ru.yandex.market.core.supplier.promo.model.offer.xls.CashbackXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.offer.xls.CheapestAsGiftXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.offer.xls.DiscountXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.offer.xls.PromocodeXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.validation.CashbackTariffs;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationError;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationResult;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationStats;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationStatus;
import ru.yandex.market.core.supplier.promo.model.validation.ValidationUpload;
import ru.yandex.market.core.supplier.promo.model.validation.request.PromoOfferXlsValidationRequest;
import ru.yandex.market.core.supplier.promo.model.validation.strategy.xls.CashbackOfferXlsValidationStrategy;
import ru.yandex.market.core.supplier.promo.model.validation.strategy.xls.CheapestAsGiftOfferXlsValidationStrategy;
import ru.yandex.market.core.supplier.promo.model.validation.strategy.xls.DiscountOfferXlsValidationStrategy;
import ru.yandex.market.core.supplier.promo.model.validation.strategy.xls.PromocodeOfferXlsValidationStrategy;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyRestClientImpl;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyTariff;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyTariffResponse;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.supplier.promo.service.PromoService.getDateInSeconds;

public class PromoOffersValidationServiceFunctionalTest extends FunctionalTest {
    private static final Instant NOW = Instant.now();
    private final S3Object s3Object = mock(S3Object.class);
    @Autowired
    private PromoOffersFetcher promoOffersFetcher;
    @Autowired
    private PromoDescriptionFetcher promoDescriptionFetcher;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private ValidationMdsService validationMdsService;
    @Autowired
    private AmazonS3 amazonS3;
    private final Resource discountPromoTemplate = new ClassPathResource("reports/marketplace-sales.xlsm");
    private final Resource discountPromoTemplateMultiPromo =
            new ClassPathResource("reports/marketplace-sales-multi-promo.xlsm");
    private final Resource cheapestAsGiftPromoTemplate = new ClassPathResource("reports/marketplace-sales-three-as" +
            "-two.xlsm");
    private final Resource cheapestAsGiftPromoTemplateMultiPromo =
            new ClassPathResource("reports/marketplace-sales-three-as-two-multi-promo.xlsm");
    Resource promocodePromoTemplate = new ClassPathResource("reports/marketplace-promocode.xlsm");
    Resource promocodePromoTemplateMultiPromo =
            new ClassPathResource("reports/marketplace-promocode-multi-promo.xlsm");
    Resource cashbackPromoTemplate = new ClassPathResource("reports/marketplace-sales-loyalty-program.xlsm");
    PromoOffersValidationService validationService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    @Qualifier("marketReportService")
    private AsyncMarketReportService marketReportService;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;
    @Autowired
    private SaasService saasDataCampShopService;
    @Autowired
    private PromoOffersValidationParamsDao promoOffersValidationParamsDao;
    @Autowired
    @Qualifier("promoOfferLogbrokerService")
    private LogbrokerService promoOfferLogbrokerService;
    @Autowired
    private DiscountTemplateContext discountTemplateContext;
    @Autowired
    private CheapestAsGiftTemplateContext cheapestAsGiftTemplateContext;
    @Autowired
    private PromocodeTemplateContext promocodeTemplateContext;
    @Autowired
    private CashbackTemplateContext cashbackTemplateContext;
    @Autowired
    @Qualifier("loyaltyRestClientImpl")
    private LoyaltyRestClientImpl loyaltyClient;
    @Autowired
    private PromoService promoService;
    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void beforeEach() {
        validationService = PromoOffersValidationService.Builder.newBuilder()
                .withValidationMdsService(validationMdsService)
                .withCheapestAsGiftTemplate(cheapestAsGiftPromoTemplate)
                .withCheapestAsGiftTemplateMultiPromo(cheapestAsGiftPromoTemplateMultiPromo)
                .withPromocodePromoTemplate(promocodePromoTemplate)
                .withPromocodePromoTemplateMultiPromo(promocodePromoTemplateMultiPromo)
                .withDiscountTemplate(discountPromoTemplate)
                .withDiscountTemplateMultiPromo(discountPromoTemplateMultiPromo)
                .withPromoOffersFetcher(promoOffersFetcher)
                .withBusinessService(businessService)
                .withPromoFetcher(promoDescriptionFetcher)
                .withPromoOffersValidationParamsDao(promoOffersValidationParamsDao)
                .withDiscountTemplateContext(discountTemplateContext)
                .withCheapestAsGiftTemplateContext(cheapestAsGiftTemplateContext)
                .withPromocodeTemplateContext(promocodeTemplateContext)
                .withCashbackPromoTemplate(cashbackPromoTemplate)
                .withCashbackTemplateContext(cashbackTemplateContext)
                .withBatchOffersSize(30)
                .withLoyaltyClient(loyaltyClient)
                .withPromoService(promoService)
                .withEnvironmentService(environmentService)
                .build();

        // TODO
        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(774L)
                .addOfferId("0516465165")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .when(saasDataCampShopService).searchBusinessOffers(any());
    }

    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testValidateDiscountOffers" +
             "/testValidateDiscountOffers_before.csv"
    )
    public void testValidateDiscountOffers() throws IOException {
        // тестовые данные, которые дб записаны в таблицу с валидацией
        String keyWithPrefix = "eligible_s3_key";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String validatedUploadUrl = "http://test.url/file-uploaded-with_results.xslm";
        long supplierId = 1L;
        String testFilePath = "promoOffersValidationServiceFunctionalTest/testValidateDiscountOffers" +
                "/testValidateDiscountOffers.xlsm";

        InputStream inputStream = mockMds(keyWithPrefix, testFilePath, validatedUploadUrl);

        String promoId = "#1234";
        DataCampPromo.PromoDescription promo = createDiscountPromoDescription(promoId, 1, 2, 10);
        final DataCampOffer.Offer dataCampOffer1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) supplierId)
                        .setWarehouseId(1)
                        .setOfferId("1")
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 2000L)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("name")
                                .build()
                        )
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                        .build())
                .build();
        final DataCampOffer.Offer dataCampOffer2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) supplierId)
                        .setWarehouseId(1)
                        .setOfferId("2")
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 2000L)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(100500)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("name")
                                .build()
                        )
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(2L)
                                )
                        )
                        .build())
                .build();
        final DataCampOffer.Offer dataCampOfferNotFromFile = dataCampOffer1.toBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("this shop sku is not in file")
                        .setBusinessId(1)
                        .setShopId(1)
                        .setWarehouseId(1)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo_2", null, null)),
                                                Collections.singletonList(
                                                        createPromo("promo_2", null, null)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("name")
                                .build()
                        )
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                        .build()
                )
                .build();

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOffer1, dataCampOffer2, dataCampOfferNotFromFile))
                        .build())
                .when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());
        String reportResponse = StringTestUtil.getString(
                this.getClass(),
                "promoOffersValidationServiceFunctionalTest/testValidateDiscountOffers" +
                 "/emptyOffersPrices_reportResponse.json");
        mockEmptyOffersPricesReportResponse(reportResponse);
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        final PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(supplierId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, keyWithPrefix))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.DISCOUNT)
                        .withTemplateContext(discountTemplateContext)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(false))
                        .build();

        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("#1234")
                .addTotalOffers(2L)
                .addCorrectSelectedOffers(1L)
                .addInvalidOffers(1L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "http://test.url/file-uploaded-with_results" +
                 ".xslm", "eligible_s3_key"))
                .withChangedOffer(new ChangedOffer("1", 0, 1, true))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligible_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        Assertions.assertEquals(promoOfferValidationStats, expectedStats);
        inputStream.close();
    }

    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testValidatePromocodeOffers" +
             "/testValidatePromocodeOffers_before.csv"
    )
    public void testValidatePromocodeOffers() throws IOException {
        long shopId = 1;
        String promoId = shopId + "_promo";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String keyWithPrefix = "eligible_s3_key";
        String validatedUploadUrl = "file://promo-offers.xlsx";
        String testFilePath = "promoOffersValidationServiceFunctionalTest/testValidatePromocodeOffers" +
         "/testValidatePromocodeOffers.xlsm";

        InputStream inputStream = mockMds(keyWithPrefix, testFilePath, validatedUploadUrl);

        DataCampPromo.PromoDescription promo = createValuePromocodePromoDescription(promoId, 1, 2, 20);
        DataCampOffer.Offer dataCampOffer1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("1")
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("2")
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(2L)
                                )
                        )
                )
                .build();

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder()
                .addAllOffer(List.of(dataCampOffer1, dataCampOffer2))
                .build())
                .when(dataCampShopClient).getOffers(anyLong(), anyLong(),
                        any(SyncChangeOffer.ChangeOfferRequest.class));
        String reportResponse =
                StringTestUtil.getString(this.getClass(),
                        "promoOffersValidationServiceFunctionalTest/testValidatePromocodeOffers" +
                         "/notEmptyOffersPrices_reportResponse.json");
        mockNotEmptyOffersPricesReportResponse(reportResponse);

        final PromoOfferXlsValidationRequest<PromocodeXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<PromocodeXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(shopId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, keyWithPrefix))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.MARKET_PROMOCODE)
                        .withTemplateContext(promocodeTemplateContext)
                        .withValidationStrategy(new PromocodeOfferXlsValidationStrategy())
                        .build();
        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("1_promo")
                .addTotalOffers(3L)
                .addCorrectSelectedOffers(2L)
                .addInvalidOffers(1L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "file://promo-offers.xlsx",
                "eligible_s3_key"))
                .withChangedOffer(new ChangedOffer("1", 0, 1, true))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligible_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        Assertions.assertEquals(promoOfferValidationStats, expectedStats);
        inputStream.close();
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/testValidateCashbackOffers" +
     "/testValidateCashbackOffers_before.csv")
    public void testValidateCashbackOffers() throws IOException {
        long shopId = 1;
        String promoId = shopId + "_PCC_125123123";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String keyWithPrefix = "eligible_s3_key";
        String bucketName = "bucket";
        String validatedUploadUrl = "file://promo-offers.xlsx";

        // скачивание файла
        when(resourceLocationFactory.createLocation(keyWithPrefix))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getObject(bucketName, keyWithPrefix))
                .thenReturn(s3Object);
        // TODO перейти на кешбечный шаблон MBI-67589
        File file = new File(getClass()
                .getResource("promoOffersValidationServiceFunctionalTest/testValidatePromocodeOffers" +
                 "/testValidatePromocodeOffers.xlsm").getPath());
        InputStream inputStream = new FileInputStream(file);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        // загрузка xls файла
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getUrl(bucketName, keyWithPrefix)).thenReturn(new URL(validatedUploadUrl));
        doReturn(new LoyaltyTariffResponse(0, List.of(new LoyaltyTariff(12345, 0.1, 25, 1), new LoyaltyTariff(23456,
         1, 30, 5))))
                .when(loyaltyClient).getActualLoyaltyTariffs(any());

        DataCampOffer.Offer dataCampOffer1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("1")
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(12345))
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(1L)
                                )
                        )
                )
                .build();
        DataCampOffer.Offer dataCampOffer2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("2")
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(23456)
                        )
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(2L)
                                )
                        )
                )
                .build();

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder()
                .addAllOffer(List.of(dataCampOffer1, dataCampOffer2))
                .build())
                .when(dataCampShopClient).getOffers(anyLong(), anyLong(),
                        any(SyncChangeOffer.ChangeOfferRequest.class));
        DataCampPromo.PromoDescription promoDescription = createCashbackPromoDescription(promoId, 10);

        final PromoOfferXlsValidationRequest<CashbackXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<CashbackXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(shopId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, keyWithPrefix))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.PARTNER_CUSTOM_CASHBACK)
                        .withTemplateContext(cashbackTemplateContext)
                        .withValidationStrategy(new CashbackOfferXlsValidationStrategy())
                        .build();
        PromoOfferValidationStats stats = validationService.validate(validationRequest, promoDescription, false);

        CashbackTariffs cashbackTariffs = new CashbackTariffs.Builder()
                .withMaxMarketTariff(1.0)
                .withMinMarketTariff(0.1)
                .withMinCashbackNominal(5)
                .withMaxCashbackNominal(25)
                .withMarketTariffsVersionId(0)
                .build();
        Assertions.assertEquals(stats.getCashbackTariffs(), cashbackTariffs);
        Assertions.assertEquals(stats.getInvalidOffers(), 1);
        Assertions.assertEquals(stats.getCorrectSelectedOffers(), 2);
        Assertions.assertEquals(stats.getChangedOffer().getOfferId(), "1");
        inputStream.close();
    }

    private void mockEmptyOffersPricesReportResponse(String reportResponse) {
        mockOffersPricesReportResponse(reportResponse);
    }

    private void mockNotEmptyOffersPricesReportResponse(String reportResponse) {
        mockOffersPricesReportResponse(reportResponse);
    }

    private void mockOffersPricesReportResponse(String reportResponse) {
        when(marketReportService.async(
                ArgumentMatchers.argThat(request ->
                        request != null && request.getPlace() == MarketReportPlace.CHECK_PRICES
                ),
                Mockito.any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }

    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testValidateCheapestAsGiftOffers" +
             "/testValidateCheapestAsGiftOffers_before.csv"
    )
    public void testValidateCheapestAsGiftOffers() throws IOException {
        String promo_1 = "#1";
        long shopId = 1;
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String keyWithPrefix = "eligible_s3_key";
        String validatedUploadUrl = "file://promo-offers.xlsx";
        String testFilePath = "promoOffersValidationServiceFunctionalTest/testValidateCheapestAsGiftOffers" +
         "/testValidateCheapestAsGiftOffers.xlsm";
        String promo_2 = "#2";

        mockMds(keyWithPrefix, testFilePath, validatedUploadUrl);

        final ValidationUpload upload = new ValidationUpload(validationId, originalUploadUrl, keyWithPrefix);

        DataCampPromo.PromoDescription promo1 = createCheapestAsGiftPromoDescription(promo_1, 145, 1, 2);
        final DataCampOffer.Offer dataCampOffer = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .setWarehouseId(1)
                        .setOfferId("1")
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build()
                )
                .setPromos(
                        createAllPromos(promo_1)
                )
                .build();
        final DataCampOffer.Offer dataCampOfferNotFromFile = dataCampOffer.toBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) shopId)
                        .setWarehouseId(1)
                        .setOfferId("this shop sku is not in file")
                )
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo(promo_2, null, null)),
                                                Collections.singletonList(
                                                        createPromo(promo_2, null, null)
                                                ))))
                .build();

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOffer, dataCampOfferNotFromFile))
                        .build())
                .when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());
        String reportResponse = StringTestUtil.getString(
                this.getClass(),
                "promoOffersValidationServiceFunctionalTest/testValidateCheapestAsGiftOffers" +
                 "/emptyOffersPrices_reportResponse.json");
        mockEmptyOffersPricesReportResponse(reportResponse);
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo1)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        final PromoOfferXlsValidationRequest<CheapestAsGiftXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<CheapestAsGiftXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(shopId)
                        .withOriginalUpload(upload)
                        .withPromoId(promo_1)
                        .withPromoType(PromoType.CHEAPEST_AS_GIFT)
                        .withTemplateContext(cheapestAsGiftTemplateContext)
                        .withValidationStrategy(new CheapestAsGiftOfferXlsValidationStrategy())
                        .build();

        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("#1")
                .addTotalOffers(1L)
                .addCorrectSelectedOffers(1L)
                .addInvalidOffers(0L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "file://promo-offers.xlsx",
                "eligible_s3_key"))
                .withChangedOffer(new ChangedOffer("1", 1, 1, true))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligible_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        Assertions.assertEquals(promoOfferValidationStats, expectedStats);
    }

    /**
     * ssku1: promoId было включено, обновляют цену по акции
     * ssku2: promoId было включено, выключают
     * ssku3: promoId было выключено, включают
     * ssku4: promoId было включено, не меняется
     * ssku5: promoId было выключено, не меняется
     * <p>
     * + у ssku5 выключенная promoId2, которая остается без изменений
     */
    @Test
    public void testEnableDirectDiscountPromoPrice() {
        String promoId = "#1";
        String promoId2 = "#2";
        DataCampPromo.PromoDescription promoDescription = createDiscountPromoDescription(promoId, 1, 2, 50);
        DataCampPromo.PromoDescription promoDescription2 = createDiscountPromoDescription(promoId2, 5, 6, 50);

        DiscountXlsPromoOffer fileOffer1 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("ssku1")
                .withPromoPrice(400L)
                .withOldPrice(600L)
                .build();
        DiscountXlsPromoOffer fileOffer2 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("ssku2")
                .withOldPrice(340L)
                .build();
        DiscountXlsPromoOffer fileOffer3 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("ssku3")
                .withPromoPrice(250L)
                .withOldPrice(570L)
                .build();
        DiscountXlsPromoOffer fileOffer4 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("ssku4")
                .withPromoPrice(512L)
                .withOldPrice(590L)
                .build();
        DiscountXlsPromoOffer fileOffer5 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("ssku5")
                .build();

        DataCampOffer.Offer dcOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ssku1")
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo(promoId, 560L, null)
                                                ),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 600L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ssku2")
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo(promoId, 200L, null)
                                                ),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 340L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer3 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ssku3")
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo(promoId2, null, 570L)
                                                ),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 570L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer4 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ssku4")
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo(promoId, 512L, null)
                                                ),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 590L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer5 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("ssku5")
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Arrays.asList(
                                                        createPromo(promoId, null, 330L),
                                                        createPromo(promoId2, null, 1470L)
                                                )
                                        )
                                )
                )
                .build();

        List<DataCampOffer.Offer> updatedDcOffers = validationService.updateOffersPromoInfo(
                promoDescription,
                List.of(fileOffer1, fileOffer2, fileOffer3, fileOffer4, fileOffer5),
                Map.of(
                        dcOffer1.getIdentifiers().getOfferId(), dcOffer1,
                        dcOffer2.getIdentifiers().getOfferId(), dcOffer2,
                        dcOffer3.getIdentifiers().getOfferId(), dcOffer3,
                        dcOffer4.getIdentifiers().getOfferId(), dcOffer4,
                        dcOffer5.getIdentifiers().getOfferId(), dcOffer5
                ),
                Map.of(
                        promoId, promoDescription,
                        promoId2, promoDescription2
                ),
                false
        );

        Assertions.assertEquals(5, updatedDcOffers.size());

        // ssku1
        List<DataCampOfferPromos.Promo> activePromosList1 =
                updatedDcOffers.get(0).getPromos()
                        .getAnaplanPromos()
                        .getActivePromos()
                        .getPromosList();
        Assertions.assertEquals(1, activePromosList1.size());
        Assertions
                .assertEquals(0, updatedDcOffers.get(0).getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromo11 = activePromosList1.get(0);
        Assertions.assertTrue(activePromo11.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(400L)),
                activePromo11.getDirectDiscount().getPrice().getPrice());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(600L)),
                activePromo11.getDirectDiscount().getBasePrice().getPrice());

        //ssku2
        Assertions.assertEquals(0,
                updatedDcOffers.get(1).getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions
                .assertEquals(0, updatedDcOffers.get(1).getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        //ssku3
        List<DataCampOfferPromos.Promo> activePromosList3 =
                updatedDcOffers.get(2).getPromos()
                        .getAnaplanPromos()
                        .getActivePromos()
                        .getPromosList();
        Assertions.assertEquals(1, activePromosList3.size());
        Assertions
                .assertEquals(0, updatedDcOffers.get(2).getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromo31 = activePromosList3.get(0);
        Assertions.assertTrue(activePromo31.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(250L)),
                activePromo31.getDirectDiscount().getPrice().getPrice());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(570L)),
                activePromo31.getDirectDiscount().getBasePrice().getPrice());

        //ssku4
        List<DataCampOfferPromos.Promo> activePromosList4 =
                updatedDcOffers.get(3).getPromos()
                        .getAnaplanPromos()
                        .getActivePromos()
                        .getPromosList();
        Assertions.assertEquals(1, activePromosList4.size());
        Assertions
                .assertEquals(0, updatedDcOffers.get(3).getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromo41 = activePromosList4.get(0);
        Assertions.assertTrue(activePromo41.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(512L)),
                activePromo41.getDirectDiscount().getPrice().getPrice());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(590L)),
                activePromo41.getDirectDiscount().getBasePrice().getPrice());

        //ssku5
        Assertions.assertEquals(0,
                updatedDcOffers.get(4).getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions
                .assertEquals(0, updatedDcOffers.get(4).getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
    }


    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testOffersValidationFromFile.before.csv"
    )
    // подгружается файл из 35 офферов.
    // читается в два захода по 30
    // в первом прочтении берет оффер, у которго есть цена, по которой он будет продаваться в акции
    // во втором нет
    public void testBatchReadAndWrite() throws IOException {
        // тестовые данные, которые дб записаны в таблицу с валидацией
        String bucketName = "bucket";
        String validatedS3Key = "validated_s3_key";
        String eligibleS3Key = "eligable_s3_key";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        long partnerId = 1L;
        long business_id = 1;

        mockReadingOffersFromFile("promoOffersValidationServiceFunctionalTest/testBatchReadAndWrite/testBatchReadAndWrite_35.xlsm");

        String promoId = "#1234";
        DataCampPromo.PromoDescription promo = createDiscountPromoDescription(promoId, 1, 2, 10);
        String offerIdFromFirsIteration = "hid.216900390";
        final DataCampOffer.Offer dataCampOfferInFirstIteration = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) partnerId)
                        .setWarehouseId(1)
                        .setOfferId(offerIdFromFirsIteration)
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 5000L)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(9999)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("Сено Vitaline Сбор луговых трав 0.4 кг")
                                .build()
                        )
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(216900390L)
                                )
                        )
                        .build())
                .build();
        String offerIdFromSecondIteration = "hid.100438949769";
        final DataCampOffer.Offer dataCampOfferInSecondIteration = dataCampOfferInFirstIteration.toBuilder()
                .setIdentifiers(dataCampOfferInFirstIteration.getIdentifiers().toBuilder()
                        .setOfferId(offerIdFromSecondIteration)
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                        .setMarketSkuId(100438949769L)
                                )
                        )
                )
                .build();
        final DataCampOffer.Offer dataCampOfferNotFromFile =
                dataCampOfferInFirstIteration.toBuilder()
                        .setIdentifiers(dataCampOfferInFirstIteration.getIdentifiers().toBuilder()
                                .setOfferId("this shop sku is not in file")
                        )
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                        )
                        .build();

        // возвращаем ответ от офферного хранилища на первый батч
        // в первой итерации записывается один оффер, у которого проставлена цена для участия в акции
        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOfferInFirstIteration, dataCampOfferNotFromFile))
                        .build())
                .when(dataCampShopClient).getOffers(
                        eq(partnerId), eq(business_id), Mockito.argThat(
                                request -> request.getOfferList().stream()
                                        .map(DataCampOffer.Offer::getIdentifiers)
                                        .map(DataCampOfferIdentifiers.OfferIdentifiers::getOfferId)
                                        .anyMatch(offer -> offer.equals(offerIdFromFirsIteration))
                        ));

        // возвращаем ответ от офферного хранилища на второй батч
        // во второй итерации записывается еще один оффер
        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOfferInSecondIteration, dataCampOfferNotFromFile))
                        .build())
                .when(dataCampShopClient).getOffers(
                        eq(partnerId), eq(business_id), Mockito.argThat(
                                request -> request.getOfferList().stream()
                                        .map(DataCampOffer.Offer::getIdentifiers)
                                        .map(DataCampOfferIdentifiers.OfferIdentifiers::getOfferId)
                                        .anyMatch(offer -> offer.equals(offerIdFromSecondIteration))
                        ));
        String reportResponse = StringTestUtil.getString(
                this.getClass(),
                "promoOffersValidationServiceFunctionalTest/testValidateDiscountOffers" +
                 "/emptyOffersPrices_reportResponse.json");
        mockEmptyOffersPricesReportResponse(reportResponse);
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        final PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(partnerId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, validatedS3Key))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.DISCOUNT)
                        .withTemplateContext(discountTemplateContext)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(false))
                        .build();
        ArgumentCaptor<DatacampMessageLogbrokerEvent> datacampMessageLogbrokerEventArgumentCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        PromoLogbrokerService promoLogbrokerService = new PromoLogbrokerService(promoOfferLogbrokerService);
        PromoProtoService promoProtoService = new PromoProtoService(promoLogbrokerService);
        doNothing().when(promoOfferLogbrokerService).publishEvent(datacampMessageLogbrokerEventArgumentCaptor.capture());

        //достаем pbsn файл
        when(amazonS3.putObject(eq(bucketName), eq(eligibleS3Key), any(File.class)))
                .then(a -> {
                    var p = promoProtoService.getReaderInstance();
                    System.out.println("Inside putObject");
                    try (InputStream in = Files.newInputStream(((File) a.getArgument(2)).toPath())) {
                        p.consume(in);
                    } catch (Exception e) {
                        System.out.println("Что-то пошло не так" + e);
                    }
                    return null;
                });
        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("#1234")
                .addTotalOffers(35L)
                .addCorrectSelectedOffers(1L)
                .addInvalidOffers(33L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "http://test.url/file-uploaded-with_results" +
                 ".xslm", "validated_s3_key"))
                .withChangedOffer(new ChangedOffer("hid.216900390", 1, 1, true))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligable_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        Assertions.assertEquals(promoOfferValidationStats, expectedStats);

        DatacampMessageOuterClass.DatacampMessage datacampMessage =
         datacampMessageLogbrokerEventArgumentCaptor.getValue().getPayload();
        List<DataCampOffer.Offer> resultPbsnFile = datacampMessage.getOffersList().get(0).getOfferList();
        Assertions.assertEquals(2, resultPbsnFile.size());
        resultPbsnFile.forEach(
                offer -> {
                    List<DataCampOfferPromos.Promo> promosList =
                            offer.getPromos().getAnaplanPromos().getActivePromos().getPromosList();
                    if (offer.getIdentifiers().getOfferId().equals(offerIdFromFirsIteration)) {
                        Assertions.assertEquals(1, promosList.size());
                        Assertions.assertEquals("#1234", promosList.get(0).getId());
                        Assertions.assertEquals(40000000000L,
                                promosList.get(0).getDirectDiscount().getPrice().getPrice());
                    } else {
                        Assertions.assertEquals(0, promosList.size());
                    }
                }
        );
    }

    @DisplayName("Тест загрузки нешаблонного файла на валидацию. Ожидаем 0 офферов в результате.")
    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testOffersValidationFromFile.before.csv"
    )
    public void testWrongXlsFile() throws IOException {
        // тестовые данные, которые дб записаны в таблицу с валидацией
        String validatedS3Key = "validated_s3_key";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String promoId = "#1234";
        long partnerId = 1L;
        long business_id = 1;

        mockReadingOffersFromFile("promoOffersValidationServiceFunctionalTest/testWrongXlsFile/wrongXlsFile.xlsm");

        DataCampPromo.PromoDescription promo = createDiscountPromoDescription(promoId, 1, 2, 10);
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder().build())
                .when(dataCampShopClient).getOffers(eq(partnerId), eq(business_id), any(SyncChangeOffer.ChangeOfferRequest.class));

        PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(partnerId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, validatedS3Key))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.DISCOUNT)
                        .withTemplateContext(discountTemplateContext)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(false))
                        .build();

        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("#1234")
                .addTotalOffers(0L)
                .addCorrectSelectedOffers(0L)
                .addInvalidOffers(0L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "http://test.url/file-uploaded-with_results.xslm", "validated_s3_key"))
                .withChangedOffer(null)
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligable_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        assertThat(promoOfferValidationStats).isEqualTo(expectedStats);
    }

    @DisplayName("Тест файла с офферами, ни один из которых не прошёл валидацию.")
    @Test
    @DbUnitDataSet(
            before = "promoOffersValidationServiceFunctionalTest/testOffersValidationFromFile.before.csv"
    )
    public void testAllFailedValidations() throws IOException {
        // тестовые данные, которые дб записаны в таблицу с валидацией
        String validatedS3Key = "validated_s3_key";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String promoId = "#1234";
        long partnerId = 1L;
        long business_id = 1;

        mockReadingOffersFromFile("promoOffersValidationServiceFunctionalTest/testAllFailedValidations/allFailedValidations.xlsm");

        DataCampPromo.PromoDescription promo = createDiscountPromoDescription(promoId, 1, 2, 10);
        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder().build())
                .when(dataCampShopClient).getOffers(eq(partnerId), eq(business_id), any(SyncChangeOffer.ChangeOfferRequest.class));

        String offerId1 = "hid.216900390";
        final DataCampOffer.Offer offer1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(Math.toIntExact(business_id))
                        .setShopId(Math.toIntExact(partnerId))
                        .setWarehouseId(1)
                        .setOfferId(offerId1)
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 5000L)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(9999)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("Сено Vitaline Сбор луговых трав 0.4 кг")
                                .build()
                        )
                        .build())
                .build();

        String offerId2 = "hid.100438949735";
        final DataCampOffer.Offer offer2 = offer1.toBuilder()
                .setIdentifiers(offer1.getIdentifiers().toBuilder()
                        .setOfferId(offerId2)
                )
                .build();

        String offerId3 = "hid.100438950083";
        final DataCampOffer.Offer offer3 = offer1.toBuilder()
                .setIdentifiers(offer1.getIdentifiers().toBuilder()
                        .setOfferId(offerId3)
                )
                .build();

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(offer1, offer2, offer3))
                        .build()
        ).when(dataCampShopClient).getOffers(eq(partnerId), eq(business_id), any(SyncChangeOffer.ChangeOfferRequest.class));

        PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(partnerId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, validatedS3Key))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.DISCOUNT)
                        .withTemplateContext(discountTemplateContext)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(true))
                        .build();
        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId("#1234")
                .addTotalOffers(8L)
                .addCorrectSelectedOffers(0L)
                .addInvalidOffers(7L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "http://test.url/file-uploaded-with_results.xslm", "validated_s3_key"))
                .withChangedOffer(null)
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligable_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        assertThat(promoOfferValidationStats).isEqualTo(expectedStats);
    }

    private void mockReadingOffersFromFile(String path) throws FileNotFoundException, MalformedURLException {
        String bucketName = "bucket";
        String validatedS3Key = "validated_s3_key";
        String eligibleS3Key = "eligable_s3_key";
        String validatedUploadUrl = "http://test.url/file-uploaded-with_results.xslm";
        String eligibleUploadUrl = "http://key_url.pbsn";

        // скачивание файла
        when(resourceLocationFactory.createLocation(validatedS3Key))
                .thenReturn(ResourceLocation.create(bucketName, validatedS3Key));
        when(amazonS3.getObject(bucketName, validatedS3Key))
                .thenReturn(s3Object);
        File file = new File(getClass().getResource(path).getPath());
        InputStream inputStream = new FileInputStream(file);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        // загрузка файла xlsm файла
        when(resourceLocationFactory.createLocation(Mockito.contains("xlsm")))
                .thenReturn(ResourceLocation.create(bucketName, validatedS3Key));
        // достаем урл для скачивания xlsm-файла из mds
        when(amazonS3.getUrl(bucketName, validatedS3Key)).thenReturn(new URL(validatedUploadUrl));

        // загрузка pbsn файла
        when(resourceLocationFactory.createLocation(Mockito.contains("pbsn")))
                .thenReturn(ResourceLocation.create(bucketName, eligibleS3Key));
        // достаем урл для скачивания pbsn-файла из mds
        when(amazonS3.getUrl(bucketName, eligibleS3Key)).thenReturn(new URL(eligibleUploadUrl));
    }

    private DataCampPromo.PromoDescription createDiscountPromoDescription(
            String promoId, int startDateOffset, int endDateOffset, int minDiscount
    ) {
        long startDate = NOW.plus(startDateOffset, ChronoUnit.DAYS).getEpochSecond();
        long endDate = NOW.plus(endDateOffset, ChronoUnit.DAYS).getEpochSecond();
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo " + promoId)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addAllPromoCategory(
                                                        List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1L)
                                                                        .setMinDiscount(minDiscount)
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                                .build())
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .build())
                .build();
    }

    private DataCampPromo.PromoDescription createValuePromocodePromoDescription(
            String promoId, int startDateOffset, int endDateOffset, long discountValue
    ) {
        long startDate = NOW.plus(startDateOffset, ChronoUnit.DAYS).getEpochSecond();
        long endDate = NOW.plus(endDateOffset, ChronoUnit.DAYS).getEpochSecond();
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo " + promoId)
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.VALUE)
                                .setRatingRub(discountValue)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .build())
                .build();
    }

    private DataCampPromo.PromoDescription createPercentagePromocodePromoDescription(
            String promoId, int startDateOffset, int endDateOffset, long discountValue
    ) {
        long startDate = NOW.plus(startDateOffset, ChronoUnit.DAYS).getEpochSecond();
        long endDate = NOW.plus(endDateOffset, ChronoUnit.DAYS).getEpochSecond();
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo " + promoId)
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                                .setRatingRub(discountValue)
                                .build())
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .build())
                .build();
    }

    private DataCampPromo.PromoDescription createCheapestAsGiftPromoDescription(
            String promoId, int warehouseId, int startDateOffset, int endDateOffset
    ) {
        long startDate = NOW.plus(startDateOffset, ChronoUnit.DAYS).getEpochSecond();
        long endDate = NOW.plus(endDateOffset, ChronoUnit.DAYS).getEpochSecond();
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo " + promoId)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setWarehouseRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                        .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                .addId(warehouseId)
                                                .build()
                                        )
                                        .build()
                                )
                                .build())
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .build())
                .build();
    }

    private DataCampPromo.PromoDescription createCashbackPromoDescription(
            String promoId,
            int cashcbackValue
    ) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK)
                )
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setPartnerCustomCashback(DataCampPromo.PromoMechanics.PartnerCustomCashback.newBuilder()
                                .setCashbackValue(cashcbackValue)))
                .build();
    }

    /**
     * целевое промо = #0
     * shop-sku добавляем, не было активного #0, был активный промокод #4, который выключился
     * shop-sku-1 удаляем, было активное промо #0 и неактивное #1
     * shop-sku-2 добавляем, было активное промо #2, неактивное #0, активное промо #3,
     * которое пересекается по времени с #0
     * <p>
     * Все остальные промо не пересекаются по датам проведения
     * <p>
     * В конечном итоге должно получится, что
     * shop-sku участвует в #0
     * shop-sku-1 НЕ участвует в #0 и НЕ участвует в #1
     * shop-sku-2 участвует в #0, участвует в #2, НЕ участвует в #3
     */
    @Test
    public void testEnableDisablePromoPrice() {
        String promoId = "#0";
        String promoId1 = "#1";
        String promoId2 = "#2";
        String promoId3 = "#3";
        String promoId4 = "#4";
        DataCampPromo.PromoDescription targetPromo = createDiscountPromoDescription(promoId, 1, 2, 50);
        Map<String, DataCampPromo.PromoDescription> promoDescriptionById = Map.of(
                promoId, targetPromo,
                promoId1, createDiscountPromoDescription(promoId1, 3, 4, 50),
                promoId2, createDiscountPromoDescription(promoId2, 5, 6, 50),
                promoId3, createDiscountPromoDescription(promoId3, 1, 2, 50),
                promoId4, createPercentagePromocodePromoDescription(promoId4, 2, 10, 100)
        );
        final List<DiscountXlsPromoOffer> fileOffers = List.of(
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .withPromoPrice(400L)
                        .withOldPrice(500L)
                        .build(),
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withPromoPrice(null)
                        .build(),
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-2")
                        .withPromoPrice(200L)
                        .withOldPrice(500L)
                        .build());

        final DataCampOffer.Offer dcOffer = createDataCampOffer("shop-sku", Collections.emptyList(),
                Collections.singletonList(createPromo(promoId, null, 500L)),
                Collections.singletonList(createPromocodePromo(promoId4)),
                Collections.emptyList()
        );


        final DataCampOffer.Offer dcOffer1 = createDataCampOffer(
                "shop-sku-1",
                Collections.singletonList(createPromo(promoId, 100L, null)),
                Arrays.asList(
                        createPromo(promoId, null, 500L),
                        createPromo(promoId1, null, 888L)
                ),
                Collections.emptyList(),
                Collections.emptyList()
        );

        final DataCampOffer.Offer dcOffer2 = createDataCampOffer("shop-sku-2",
                Arrays.asList(
                        createPromo(promoId2, 4500L, null),
                        createPromo(promoId3, 120L, null)
                ),
                Arrays.asList(
                        createPromo(promoId, null, 500L),
                        createPromo(promoId2, null, 5210L),
                        createPromo(promoId3, null, 270L)
                ),
                Collections.emptyList(),
                Collections.emptyList()
        );

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of(dcOffer.getIdentifiers().getOfferId(), dcOffer,
                        dcOffer1.getIdentifiers().getOfferId(), dcOffer1,
                        dcOffer2.getIdentifiers().getOfferId(), dcOffer2);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService.updateOffersPromoInfo(targetPromo, fileOffers, eligibleDCOffers,
                        promoDescriptionById, false);

        Assertions.assertEquals(3, updatedDcOffers.size());
        // shop-sku
        DataCampOffer.Offer updatedDcOffer1 = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku", updatedDcOffer1.getIdentifiers().getOfferId());
        List<DataCampOfferPromos.Promo> activePromoList1 = updatedDcOffer1
                .getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals(1, activePromoList1.size());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromo11 = activePromoList1.get(0);
        Assertions.assertTrue(activePromo11.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(400L)),
                activePromo11.getDirectDiscount().getPrice().getPrice());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(500L)),
                activePromo11.getDirectDiscount().getBasePrice().getPrice());

        // shop-sku-1
        DataCampOffer.Offer updatedDcOffer2 = updatedDcOffers.get(1);
        Assertions.assertEquals("shop-sku-1", updatedDcOffer2.getIdentifiers().getOfferId());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        // shop-sku-2
        DataCampOffer.Offer updatedDcOffer3 = updatedDcOffers.get(2);
        Assertions.assertEquals("shop-sku-2", updatedDcOffer3.getIdentifiers().getOfferId());
        List<DataCampOfferPromos.Promo> activePromoList3 = updatedDcOffer3
                .getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals(2, activePromoList3.size());
        Assertions.assertEquals(0, updatedDcOffer3.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromo31 = activePromoList3.get(0);
        Assertions.assertEquals(promoId, activePromo31.getId());
        Assertions.assertTrue(activePromo31.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(200L)),
                activePromo31.getDirectDiscount().getPrice().getPrice());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(500L)),
                activePromo31.getDirectDiscount().getBasePrice().getPrice());

        DataCampOfferPromos.Promo activePromo32 = activePromoList3.get(1);
        Assertions.assertEquals(promoId2, activePromo32.getId());
        Assertions.assertTrue(activePromo32.hasDirectDiscount());
        Assertions.assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(4500L)),
                activePromo32.getDirectDiscount().getPrice().getPrice());
        Assertions.assertFalse(activePromo32.getDirectDiscount().hasBasePrice());
    }

    /**
     * целевое промо = #0
     * shop-sku добавляем, не было активного #0, было активное #1
     * shop-sku-1 удаляем, было активное промо #0 и неактивное #2
     * shop-sku-2 добавляем, было активное промо #2, неактивное #0, активное промо #3,
     * которое пересекается по времени с #0
     * <p>
     * Все остальные промо не пересекаются по датам проведения
     * <p>
     * В конечном итоге должно получится, что
     * shop-sku участвует в #0
     * shop-sku-1 НЕ участвует в #0 и НЕ участвует в #1
     * shop-sku-2 участвует в #0, в #2 и в #3
     */
    @Test
    public void testEnableDisablePromocode() {
        String promoId = "#0";
        String promoId1 = "#1";
        String promoId2 = "#2";
        String promoId3 = "#3";
        DataCampPromo.PromoDescription targetPromo = createValuePromocodePromoDescription(promoId, 1, 2, 50);
        Map<String, DataCampPromo.PromoDescription> promoDescriptionById = Map.of(
                promoId, targetPromo,
                promoId1, createValuePromocodePromoDescription(promoId1, 3, 4, 50),
                promoId2, createDiscountPromoDescription(promoId2, 5, 6, 50),
                promoId3, createDiscountPromoDescription(promoId3, 1, 2, 50)
        );
        final List<PromocodeXlsPromoOffer> fileOffers = List.of(
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .withParticipate(true)
                        .build(),
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withParticipate(false)
                        .build(),
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-2")
                        .withParticipate(true)
                        .build());

        final DataCampOffer.Offer dcOffer = createDataCampOffer("shop-sku", Collections.emptyList(),
                Collections.emptyList(), Collections.singletonList(createPromocodePromo(promoId1)),
                Collections.emptyList()
        );

        final DataCampOffer.Offer dcOffer1 = createDataCampOffer(
                "shop-sku-1",
                Collections.emptyList(),
                Collections.singletonList(
                        createPromo(promoId2, null, 888L)
                ),
                Collections.singletonList(createPromocodePromo(promoId)),
                Collections.emptyList()
        );

        final DataCampOffer.Offer dcOffer2 = createDataCampOffer("shop-sku-2",
                Arrays.asList(
                        createPromo(promoId2, 4500L, null),
                        createPromo(promoId3, 120L, null)
                ),
                Arrays.asList(
                        createPromo(promoId2, null, 5210L),
                        createPromo(promoId3, null, 270L)
                ),
                Collections.emptyList(),
                Collections.emptyList()
        );

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of(dcOffer.getIdentifiers().getOfferId(), dcOffer,
                        dcOffer1.getIdentifiers().getOfferId(), dcOffer1,
                        dcOffer2.getIdentifiers().getOfferId(), dcOffer2);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService.updateOffersPromoInfo(targetPromo, fileOffers, eligibleDCOffers,
                        promoDescriptionById, false);

        Assertions.assertEquals(3, updatedDcOffers.size());
        // shop-sku
        DataCampOffer.Offer updatedDcOffer1 = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku", updatedDcOffer1.getIdentifiers().getOfferId());

        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        List<DataCampOfferPromos.Promo> activePromocodePromoList1 = updatedDcOffer1
                .getPromos()
                .getPartnerPromos()
                .getPromosList();
        Assertions.assertEquals(2, activePromocodePromoList1.size());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        DataCampOfferPromos.Promo activePromoCode1 = activePromocodePromoList1.get(0);
        Assertions.assertEquals(activePromoCode1.getId(), promoId);
        DataCampOfferPromos.Promo activePromoCode2 = activePromocodePromoList1.get(1);
        Assertions.assertEquals(activePromoCode2.getId(), promoId1);

        // shop-sku-1
        DataCampOffer.Offer updatedDcOffer2 = updatedDcOffers.get(1);
        Assertions.assertEquals("shop-sku-1", updatedDcOffer2.getIdentifiers().getOfferId());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getPartnerPromos().getPromosCount());

        // shop-sku-2
        DataCampOffer.Offer updatedDcOffer3 = updatedDcOffers.get(2);
        Assertions.assertEquals("shop-sku-2", updatedDcOffer3.getIdentifiers().getOfferId());
        // Нет обновленных активных анаплановских, т к их список не изменился
        Assertions.assertEquals(0, updatedDcOffer3.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer3.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(1, updatedDcOffer3.getPromos().getPartnerPromos().getPromosCount());

        List<DataCampOfferPromos.Promo> promocodeList3 = updatedDcOffer3
                .getPromos()
                .getPartnerPromos()
                .getPromosList();
        DataCampOfferPromos.Promo activePromoCode3 = promocodeList3.get(0);
        Assertions.assertEquals(promoId, activePromoCode3.getId());
    }

    @Test
    public void testEnableDisableCashback() {
        String promoId1 = "1_PCC_123456";
        String promoId2 = "1_PCC_234566";
        String promoId3 = "1_PCC_345678";
        DataCampPromo.PromoDescription targetPromo = createCashbackPromoDescription(promoId1, 10);

        final List<PromocodeXlsPromoOffer> fileOffers = List.of(
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withParticipate(false)
                        .build(),
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-2")
                        .withParticipate(true)
                        .build());

        final DataCampOffer.Offer dcOffer1 = createDataCampOffer("shop-sku-1", Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId1)
                                .build(),
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId2)
                                .build()
                )
        );

        final DataCampOffer.Offer dcOffer2 = createDataCampOffer("shop-sku-2",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId2)
                                .build(),
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId3)
                                .build()
                )
        );

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of(dcOffer1.getIdentifiers().getOfferId(), dcOffer1,
                        dcOffer2.getIdentifiers().getOfferId(), dcOffer2);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService.updateOffersPromoInfo(targetPromo, fileOffers, eligibleDCOffers, null, false);

        Assertions.assertEquals(2, updatedDcOffers.size());

        // shop-sku-1
        DataCampOffer.Offer updatedDcOffer1 = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku-1", updatedDcOffer1.getIdentifiers().getOfferId());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerPromos().getPromosCount());
        Assertions.assertEquals(1, updatedDcOffer1.getPromos().getPartnerCashbackPromos().getPromosCount());
        List<DataCampOfferPromos.Promo> cashbackList1 = updatedDcOffer1
                .getPromos()
                .getPartnerCashbackPromos()
                .getPromosList();
        DataCampOfferPromos.Promo activeCashback1 = cashbackList1.get(0);
        Assertions.assertEquals(activeCashback1.getId(), promoId2);

        // shop-sku-2
        DataCampOffer.Offer updatedDcOffer2 = updatedDcOffers.get(1);
        Assertions.assertEquals("shop-sku-2", updatedDcOffer2.getIdentifiers().getOfferId());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer2.getPromos().getPartnerPromos().getPromosCount());
        Assertions.assertEquals(3, updatedDcOffer2.getPromos().getPartnerCashbackPromos().getPromosCount());
    }

    @Test
    public void testEnableTargetPromo() {
        String promoId1 = "#11111";
        String promoId2 = "#22222";
        String targetPromoId = "#12345";
        DataCampPromo.PromoDescription targetPromo = createDiscountPromoDescription(targetPromoId, 1, 10, 10);
        DataCampPromo.PromoDescription anaplanPromoDiscount = createDiscountPromoDescription(promoId1, 2, 8, 20);
        DataCampPromo.PromoDescription partnerPromoPromocode = createPercentagePromocodePromoDescription(promoId2, 5,
15, 5);

        final List<PromocodeXlsPromoOffer> fileOffers = List.of(
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withParticipate(true)
                        .build());

        final DataCampOffer.Offer dcOffer1 = createDataCampOffer(
                "shop-sku-1",
                Collections.singletonList(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId1)
                                .build()
                ),
                List.of(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId1)
                                .build(),
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(targetPromoId)
                                .build()
                ),
                Collections.singletonList(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId2)
                                .build()
                ),
                Collections.emptyList()
        );

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of(dcOffer1.getIdentifiers().getOfferId(), dcOffer1);

        Map<String, DataCampPromo.PromoDescription> promoDescriptionById =
                Map.of(promoId1, anaplanPromoDiscount, promoId2, partnerPromoPromocode, targetPromoId, targetPromo);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService.updateOffersPromoInfo(
                        targetPromo, fileOffers, eligibleDCOffers, promoDescriptionById, false);

        Assertions.assertEquals(1, updatedDcOffers.size());

        DataCampOffer.Offer updatedDcOffer1 = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku-1", updatedDcOffer1.getIdentifiers().getOfferId());
        DataCampOfferPromos.Promos activePromos = updatedDcOffer1.getPromos().getAnaplanPromos().getActivePromos();
        Assertions.assertEquals(1, activePromos.getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerCashbackPromos().getPromosCount());
        Assertions.assertEquals(targetPromoId, activePromos.getPromos(0).getId());
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/before_multiPromo.csv")
    public void testEnableTargetPromo_multiPromo() {
        String targetPromoId = "#12345";
        String promoId1 = "#11111";
        String promoId2 = "#22222";
        DataCampPromo.PromoDescription targetPromo = createDiscountPromoDescription(targetPromoId, 1, 10, 10);
        DataCampPromo.PromoDescription anaplanPromoDiscount = createDiscountPromoDescription(promoId1, 2, 8, 20);
        DataCampPromo.PromoDescription partnerPromoPromocode = createPercentagePromocodePromoDescription(promoId2, 5,
15, 5);

        final List<PromocodeXlsPromoOffer> fileOffers = List.of(
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withParticipate(true)
                        .build());

        final DataCampOffer.Offer dcOffer1 = createDataCampOffer(
                "shop-sku-1",
                Collections.singletonList(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId1)
                                .build()
                ),
                List.of(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId1)
                                .build(),
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(targetPromoId)
                                .build()
                ),
                Collections.singletonList(
                        DataCampOfferPromos.Promo.newBuilder()
                                .setId(promoId2)
                                .build()
                ),
                Collections.emptyList()
        );

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of(dcOffer1.getIdentifiers().getOfferId(), dcOffer1);

        Map<String, DataCampPromo.PromoDescription> promoDescriptionById =
                Map.of(promoId1, anaplanPromoDiscount, promoId2, partnerPromoPromocode, targetPromoId, targetPromo);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService.updateOffersPromoInfo(
                        targetPromo, fileOffers, eligibleDCOffers, promoDescriptionById, true);

        Assertions.assertEquals(1, updatedDcOffers.size());

        DataCampOffer.Offer updatedDcOffer1 = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku-1", updatedDcOffer1.getIdentifiers().getOfferId());
        List<String> activePromoIds =
                updatedDcOffer1.getPromos().getAnaplanPromos().getActivePromos().getPromosList().stream()
                        .map(DataCampOfferPromos.Promo::getId)
                        .collect(Collectors.toList());
        Assertions.assertEquals(2, activePromoIds.size());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerPromos().getPromosCount());
        Assertions.assertEquals(0, updatedDcOffer1.getPromos().getPartnerCashbackPromos().getPromosCount());
        Assertions.assertTrue(CollectionUtils.isEqualCollection(List.of(targetPromoId, promoId1), activePromoIds));
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/before_multiPromo.csv")
    public void testGetPromosIntersectionsInfo() {
        long partnerId = 10;
        long businessId = 1;
        String offerId = "shop-sku-1";
        int year = LocalDateTime.now().getYear() + 1;
        LocalDateTime startDate = LocalDateTime.of(year, 9, 10, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 9, 30, 23, 59, 0);

        List<DataCampOfferPromos.Promo> activePromos = createActivePromosForIntersectionInfoTest();
        final DataCampOffer.Offer offer = createDataCampOffer(
                offerId,
                activePromos, //active
                Collections.emptyList(), //all
                Collections.emptyList(), //partner
                List.of(DataCampOfferPromos.Promo.newBuilder()
                        .setId("#8")
                        .build()) //cashback
        );

        DataCampPromo.PromoDescription targetPromo = createCheapestAsGiftPromo(
                "#6",
                "Six",
                1,
                LocalDateTime.of(year, 3, 7, 0, 0),
                LocalDateTime.of(year, 3, 23, 23, 59, 59)
        );
        CheapestAsGiftXlsPromoOffer excelOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku(offerId)
                .withParticipate(true)
                .build();
        CashbackDetails cashbackDetails = createCashbackDetails(year);

        mockIntersectedPromos(year);

        Map<String, DataCampOffer.Offer> datacampOffersBySsku = Map.of(
                offerId, offer
        );

        validationService.enrichOffersWithIntersectionInfo(
                partnerId,
                businessId,
                datacampOffersBySsku,
                targetPromo,
                cashbackDetails,
                List.of(excelOffer)
        );

        String expected = "" +
                "• Участвует весь период:\n" +
                "Кешбэк на группу «Eight»\n" +
                "Флеш-акция «Three»\n" +
                "Флеш-акция «Two»\n" +
                "\n" +
                "• Участвует частично:\n" +
                "Кешбэк на весь ассортимент, 23 — 25 марта\n" +
                "Флеш-акция «One», 6 — 11 марта\n" +
                "Флеш-акция «Seven», 12 — 14 марта, 20 марта\n" +
                "\n" +
                "• Не будет участвовать:\n" +
                "Прямая скидка «Five»\n" +
                "Промокод «Ten»\n" +
                "Акция 2=1 «Six»";

        Assertions.assertEquals(expected, excelOffer.getIntersectionsInfo());
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/before_multiPromo.csv")
    public void testIntersectionsInfoIsNullForNotParticipatingOffer() {
        long partnerId = 10;
        long businessId = 1;
        String offerId = "shop-sku-1";
        int year = LocalDateTime.now().getYear() + 1;
        LocalDateTime startDate = LocalDateTime.of(year, 9, 10, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 9, 30, 23, 59, 0);

        List<DataCampOfferPromos.Promo> activePromos = createActivePromosForIntersectionInfoTest();
        final DataCampOffer.Offer offer = createDataCampOffer(
                offerId,
                activePromos, //active
                Collections.emptyList(), //all
                Collections.emptyList(), //partner
                List.of(DataCampOfferPromos.Promo.newBuilder()
                        .setId("#8")
                        .build()) //cashback
        );

        DataCampPromo.PromoDescription targetPromo = createCheapestAsGiftPromo(
                "#6",
                "Six",
                1,
                LocalDateTime.of(year, 3, 7, 0, 0),
                LocalDateTime.of(year, 3, 23, 23, 59, 59)
        );
        CheapestAsGiftXlsPromoOffer excelOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku(offerId)
                .build();
        CashbackDetails cashbackDetails = createCashbackDetails(year);

        mockIntersectedPromos(year);

        Map<String, DataCampOffer.Offer> datacampOffersBySsku = Map.of(
                offerId, offer
        );

        validationService.enrichOffersWithIntersectionInfo(
                partnerId,
                businessId,
                datacampOffersBySsku,
                targetPromo,
                cashbackDetails,
                List.of(excelOffer)
        );

        Assertions.assertNull(excelOffer.getIntersectionsInfo());
    }

    private void mockIntersectedPromos(int year) {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#3")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#4")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#5")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#10")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#7")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#1")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN)
                                                .build())
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#2")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .setEnabled(true)
                        .setOnlyUnfinished(true)
                        .build();

        SyncGetPromo.GetPromoBatchResponse promo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#1",
                                                "One",
                                                4,
                                                LocalDateTime.of(year, 3, 3, 0, 0),
                                                LocalDateTime.of(year, 3, 11, 23, 59, 59)))
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#2",
                                                "Two",
                                                2,
                                                LocalDateTime.of(year, 3, 15, 0, 0),
                                                LocalDateTime.of(year, 3, 19, 23, 59, 59)))
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#3",
                                                "Three",
                                                3,
                                                LocalDateTime.of(year, 3, 21, 0, 0),
                                                LocalDateTime.of(year, 3, 25, 23, 59, 59)))
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#4",
                                                "Four",
                                                6,
                                                LocalDateTime.of(year, 3, 1, 0, 0),
                                                LocalDateTime.of(year, 3, 5, 23, 59, 59)))
                                .addPromo(
                                        createDirectDiscountPromo(
                                                "#5",
                                                "Five",
                                                5,
                                                LocalDateTime.of(year, 3, 9, 0, 0),
                                                LocalDateTime.of(year, 3, 17, 23, 59, 59)))
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#7",
                                                "Seven",
                                                0,
                                                LocalDateTime.of(year, 3, 6, 0, 0),
                                                LocalDateTime.of(year, 3, 22, 23, 59, 59)))
                                .addPromo(
                                        createPromocodePromo(
                                                "#10",
                                                "Ten",
                                                8,
                                                LocalDateTime.of(year, 3, 12, 0, 0),
                                                LocalDateTime.of(year, 3, 14, 23, 59, 59)))
                        )
                        .build();
        doReturn(promo)
                .when(dataCampShopClient).getPromos(ArgumentMatchers.eq(requestForPromo));
    }

    private DataCampPromo.PromoDescription createPromocodePromo(
            String promoId,
            String promocode,
            int priority,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        DataCampPromo.PromoMechanics.MarketPromocode.Builder marketPromocodeBuilder =
                DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                        .setPromoCode(promocode)
                        .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                        .setValue(20);

        DataCampPromo.PromoConstraints.Builder promoConstraintsBuilder =
                DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .setEnabled(true);

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promocode")
                        .setPriority(priority)
                        .setSendPromoPi(true);

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                )
                .setConstraints(
                        promoConstraintsBuilder
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setMarketPromocode(marketPromocodeBuilder)
                )
                .setAdditionalInfo(
                        promoAdditionalInfoBuilder
                )
                .build();
    }

    private DataCampPromo.PromoDescription createBlueFlashPromo(
            String promoId,
            String promoName,
            int priority,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory> promoCategories = List.of(
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                        .setId(90595L)
                        .setMinDiscount(20)
                        .build()
        );
        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addAllPromoCategory(promoCategories)
                                        .build()
                        )
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setPriority(priority)
                        .setSendPromoPi(true)
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.BLUE_FLASH)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }

    private DataCampPromo.PromoDescription createDirectDiscountPromo(
            String promoId,
            String promoName,
            int priority,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory> promoCategories = List.of(
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                        .setId(90595L)
                        .setMinDiscount(25)
                        .build()
        );
        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addAllPromoCategory(promoCategories)
                                        .build()
                        )
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setPriority(priority)
                        .setSendPromoPi(true)
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }

    private DataCampPromo.PromoDescription createCheapestAsGiftPromo(
            String promoId,
            String promoName,
            int priority,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setWarehouseRestriction(createWarehouseRestriction(48339))
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .setEnabled(true)
                        .build();

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(promoName)
                        .setPriority(priority)
                        .setSendPromoPi(true);

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfoBuilder)
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setCheapestAsGift(
                                        DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                                .setCount(2)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    public static WarehouseRestriction createWarehouseRestriction(int warehouseId) {
        return DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                        .addId(warehouseId)
                        .build()
                )
                .build();
    }

    private List<DataCampOfferPromos.Promo> createActivePromosForIntersectionInfoTest() {
        return List.of(
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#1")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#2")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#3")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#4")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#5")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#7")
                        .build(),
                DataCampOfferPromos.Promo.newBuilder()
                        .setId("#10")
                        .build()
        );
    }

    private CashbackDetails createCashbackDetails(int year) {
        BasePromoDetails standardCashback = new BasePromoDetails.Builder()
                .setPromoId("#9")
                .setName("Nine")
                .setStartDate(LocalDateTime.of(year, 3, 16, 0, 0))
                .setEndDate(LocalDateTime.of(year, 3, 25, 23, 59, 59))
                .setMechanic(PiPromoMechanicDto.PARTNER_STANDART_CASHBACK)
                .build();

        BasePromoDetails customCashback = new BasePromoDetails.Builder()
                .setPromoId("#8")
                .setPriority(0L)
                .setName("Eight")
                .setStartDate(LocalDateTime.of(year, 3, 6, 0, 0))
                .setEndDate(LocalDateTime.of(year, 3, 22, 23, 59, 59))
                .setMechanic(PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK)
                .build();

        return new CashbackDetails(
                new StandardCashbackInfo(standardCashback),
                Map.of(
                        customCashback.getPromoId(),
                        new CustomCashbackInfo(
                                customCashback,
                                Collections.emptySet(),
                                Collections.emptySet(),
                                DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.FILE
                        )
                )
        );
    }

    private DataCampOffer.Offer createDataCampOffer(
            String offerId,
            List<DataCampOfferPromos.Promo> activePromos,
            List<DataCampOfferPromos.Promo> allPromos,
            List<DataCampOfferPromos.Promo> partnerPromos,
            List<DataCampOfferPromos.Promo> cashbackPromos
    ) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(offerId))
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                activePromos,
                                                allPromos
                                        )
                                )
                                .setPartnerPromos(DataCampOfferPromos.Promos.newBuilder()
                                        .addAllPromos(partnerPromos))
                                .setPartnerCashbackPromos(DataCampOfferPromos.Promos.newBuilder()
                                        .addAllPromos(cashbackPromos))
                )
                .build();
    }

    /**
     * Для "Самый дешевый в подарок" и для Прямой скидки выгружаются оффера одинаковым образом
     * <p>
     * Запрашивам информацию из ОХ об офферах: shop-sku и shop-sku-1
     * #0 для "Самый дешевый в подарок" для 461 склада
     * <p>
     * из ОХ возвращается:
     * shop-sku - 461
     * shop-sku-1 461
     * shop-sku - 462
     * this shop sku is not in file - 461 - c активным promo_2
     * this shop sku is not in file - 462 - c активным promo_1
     * this shop sku is not in file - 463 - c активным #0
     * <p>
     * Должно получиться:
     * <p>
     * shop-sku: shop-sku - 461, shop-sku - 462
     * shop-sku-1: shop-sku - 461
     */
    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/fetchDataCampOffersTestCheapestAsGift" +
     "/fetchDataCampOffersTestCheapestAsGift_before.csv")
    public void fetchDataCampOffersTestCheapestAsGift() {
        String promoId = "#0";
        final PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withPromoId(promoId)
                        .withValidationId("validation-id")
                        .withSupplierId(1L)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(false))
                        .withTemplateContext(discountTemplateContext)
                        .withOriginalUpload(new ValidationUpload(null, null, null))
                        .withPromoType(PromoType.CHEAPEST_AS_GIFT)
                        .build();

        final List<DiscountXlsPromoOffer> fileOffers = List.of(
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .build(),
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .build());

        final DataCampOffer.Offer dataCampOffer_461 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(461)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build()
                )
                .setPromos(
                        createAllPromos(promoId)
                )
                .build();

        final DataCampOffer.Offer dataCampOffer_1_461 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(461)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build()
                )
                .setPromos(
                        createAllPromos(promoId)
                )
                .build();

        final DataCampOffer.Offer dataCampOffer_462 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(462)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build()
                )
                .setPromos(
                        createAllPromos(promoId)
                )
                .build();

        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder()
                .addAllOffer(List.of(dataCampOffer_461, dataCampOffer_462, dataCampOffer_1_461))
                .build()).when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());

        Map<String, DataCampOffer.Offer> result = validationService.fetchDataCampOffers(validationRequest, fileOffers);
        Assertions.assertEquals(2, result.size());
        //shop-sku
        var shop_sku0 = result.get("shop-sku");
        Assertions.assertEquals("shop-sku", shop_sku0.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, shop_sku0.getIdentifiers().getWarehouseId());
        // shop-sku-1
        var shop_sku_1 = result.get("shop-sku-1");
        Assertions.assertEquals("shop-sku-1", shop_sku_1.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, shop_sku_1.getIdentifiers().getWarehouseId());
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/fetchDataCampOffersTestDiscount" +
     "/fetchDataCampOffersTestDiscount_before.csv")
    public void fetchDataCampOffersTestDiscount() {
        String promoId = "#0";
        final PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withPromoId(promoId)
                        .withValidationId("validation-id")
                        .withSupplierId(1L)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(false))
                        .withTemplateContext(discountTemplateContext)
                        .withOriginalUpload(new ValidationUpload(null, null, null))
                        .withPromoType(PromoType.DISCOUNT)
                        .build();

        final List<DiscountXlsPromoOffer> fileOffers = List.of(
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .build(),
                new DiscountXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .build());

        final DataCampOffer.Offer dataCampOffer_461 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(461)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build())
                .setPromos(createAllPromos(promoId))
                .build();

        final DataCampOffer.Offer dataCampOffer_462 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(462)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(23L)
                                        )
                                )
                                .build()
                )
                .setPromos(
                        createAllPromos(promoId)
                )
                .build();

        final DataCampOffer.Offer dataCampOffer_1_461 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .setBusinessId(1)
                        .setWarehouseId(1)
                        .setShopId(1)
                        .setWarehouseId(461)
                        .build())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1)
                                                .setProductName("name")
                                                .build()
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(1L)
                                        )
                                )
                                .build())
                .setPromos(
                        createAllPromos(promoId))
                .build();

        doReturn(SyncChangeOffer.FullOfferResponse.newBuilder()
                .addAllOffer(List.of(dataCampOffer_461, dataCampOffer_462, dataCampOffer_1_461))
                .build()).when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());

        Map<String, DataCampOffer.Offer> result = validationService.fetchDataCampOffers(validationRequest, fileOffers);
        Assertions.assertEquals(2, result.size());
        //shop-sku
        var shop_sku = result.get("shop-sku");
        Assertions.assertEquals("shop-sku", shop_sku.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, shop_sku.getIdentifiers().getWarehouseId());
        // shop-sku-1
        var shop_sku_1 = result.get("shop-sku-1");
        Assertions.assertEquals("shop-sku-1", shop_sku_1.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, shop_sku_1.getIdentifiers().getWarehouseId());
    }


    /**
     * Проверяем выход из промо для "Самый дешевый в подарок" акции
     * <p>
     * targetPromo - #1 ("Самый дешевый в подарок" 461)
     * <p>
     * #2 ("Самый дешевый в подарок" 462)
     * #3 (Прямая скидка)
     * #4 (Прямая скидка)
     * #5 ("Самый дешевый в подарок" 461 НЕ пересекается по времени с #1 )
     * #6 ("Самый дешевый в подарок" 461 пересекается по времени)
     * <p>
     * В файле для #1:
     * shop-sku ДА
     * shop-sku-1 НЕТ
     * shop-sku-2 ДА
     * shop-sku-3 ДА
     * shop-sku-4 НЕТ
     * shop-sku-5 ДА
     * <p>
     * НЕТ - не участвует
     * ДА - участвует
     * <p>
     * В ОХ:
     * shop-sku, #1 НЕТ, #2 ДА, #3 НЕТ, #4 НЕТ, #5 ДА, #6 ДА
     * shop-sku-1, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 НЕТ, #6 ДА
     * shop-sku-2, #1 НЕТ, #2 НЕТ, #3 ДА, #4 НЕТ, #5 ДА, #6 ДА
     * shop-sku-3, #1 НЕТ, #2 НЕТ, #3 ДА, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-4, #1 ДА, #2 ДА, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-5, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * <p>
     * Что в итоге:
     * shop-sku, #1 ДА, #2 ДА, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-1, #1 НЕТ, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 НЕТ, #6 ДА
     * shop-sku-2, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-3, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-4, #1 НЕТ, #2 ДА, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     * shop-sku-5, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 ДА, #6 НЕТ
     */
    @Test
    public void testOutCheapestAsGiftPromo() {
        String promoId1 = "#1";
        String promoId2 = "#2";
        String promoId3 = "#3";
        String promoId4 = "#4";
        String promoId5 = "#5";
        String promoId6 = "#6";
        DataCampPromo.PromoDescription targetPromo = createCheapestAsGiftPromoDescription(promoId1, 461, 2, 4);
        var promoDescriptionByPromoId =
                Map.of(promoId1, targetPromo,
                        promoId2, createCheapestAsGiftPromoDescription(promoId2, 462, 1, 2),
                        promoId3, createDiscountPromoDescription(promoId3, 4, 6, 50),
                        promoId4, createDiscountPromoDescription(promoId4, 1, 10, 50),
                        promoId5, createCheapestAsGiftPromoDescription(promoId5, 461, 11, 15),
                        promoId6, createCheapestAsGiftPromoDescription(promoId6, 461, 3, 8)
                );

        final List<CheapestAsGiftXlsPromoOffer> fileOffers = List.of(
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .withParticipate(true)
                        .build(),
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-1")
                        .withParticipate(false)
                        .build(),
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-2")
                        .withParticipate(true)
                        .build(),
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-3")
                        .withParticipate(true)
                        .build(),
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-4")
                        .withParticipate(false)
                        .build(),
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku-5")
                        .withParticipate(true)
                        .build()
        );

        final DataCampOffer.Offer shopSku = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final DataCampOffer.Offer shopSku1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId6, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final DataCampOffer.Offer shopSku2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-2")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final DataCampOffer.Offer shopSku3 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-3")
                        .setWarehouseId(462)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId5, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final DataCampOffer.Offer shopSku4 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-4")
                        .setWarehouseId(462)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId5, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final DataCampOffer.Offer shopSku5 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-5")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId5, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null)
                                                )
                                        )
                                )
                )
                .build();

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of("shop-sku", shopSku,
                        "shop-sku-1", shopSku1,
                        "shop-sku-2", shopSku2,
                        "shop-sku-3", shopSku3,
                        "shop-sku-4", shopSku4,
                        "shop-sku-5", shopSku5);

        List<DataCampOffer.Offer> updatedDcOffers =
                validationService
                        .updateOffersPromoInfo(targetPromo, fileOffers, eligibleDCOffers, promoDescriptionByPromoId, false);

        Assertions.assertEquals(6, updatedDcOffers.size());
        // shop-sku
        DataCampOffer.Offer updatedOffer = updatedDcOffers.get(0);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku = updatedOffer.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku", updatedOffer.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, updatedOffer.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(3, activePromoList_shopSku.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku = shopSku.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku = activePromoList_shopSku.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku = allPromoList_shopSku.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku.containsKey(promo_id_1_shopSku.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku = allPromoList_shopSku.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku.containsKey(promo_id_2_shopSku.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku = allPromoList_shopSku.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku.containsKey(promo_id_3_shopSku.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku = allPromoList_shopSku.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku.containsKey(promo_id_4_shopSku.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku = allPromoList_shopSku.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku.containsKey(promo_id_5_shopSku.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku = allPromoList_shopSku.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku.containsKey(promo_id_6_shopSku.getId()));

        // shop-sku-1
        DataCampOffer.Offer updatedOffer_1 = updatedDcOffers.get(1);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku_1 = updatedOffer_1.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku-1", updatedOffer_1.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, updatedOffer_1.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer_1.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(1, activePromoList_shopSku_1.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku_1 = shopSku1.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku_1 = activePromoList_shopSku_1.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku_1 = allPromoList_shopSku_1.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku_1.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_1.containsKey(promo_id_1_shopSku_1.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku_1 = allPromoList_shopSku_1.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku_1.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_1.containsKey(promo_id_2_shopSku_1.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku_1 = allPromoList_shopSku_1.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku_1.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_1.containsKey(promo_id_3_shopSku_1.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku_1 = allPromoList_shopSku_1.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku_1.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_1.containsKey(promo_id_4_shopSku_1.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku_1 = allPromoList_shopSku_1.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku_1.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_1.containsKey(promo_id_5_shopSku_1.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku_1 = allPromoList_shopSku_1.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku_1.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_1.containsKey(promo_id_6_shopSku_1.getId()));

        // shop-sku-2
        DataCampOffer.Offer updatedOffer_2 = updatedDcOffers.get(2);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku_2 = updatedOffer_2.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku-2", updatedOffer_2.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, updatedOffer_2.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer_2.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(2, activePromoList_shopSku_2.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku_2 = shopSku2.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku_2 = activePromoList_shopSku_2.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku_2 = allPromoList_shopSku_2.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku_2.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_2.containsKey(promo_id_1_shopSku_2.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku_2 = allPromoList_shopSku_2.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku_2.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_2.containsKey(promo_id_2_shopSku_2.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku_2 = allPromoList_shopSku_2.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku_2.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_2.containsKey(promo_id_3_shopSku_2.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku_2 = allPromoList_shopSku_2.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku_2.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_2.containsKey(promo_id_4_shopSku_2.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku_2 = allPromoList_shopSku_2.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku_2.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_2.containsKey(promo_id_5_shopSku_2.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku_2 = allPromoList_shopSku_2.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku_2.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_2.containsKey(promo_id_6_shopSku_2.getId()));

        // shop-sku-3
        DataCampOffer.Offer updatedOffer_3 = updatedDcOffers.get(3);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku_3 = updatedOffer_3.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku-3", updatedOffer_3.getIdentifiers().getOfferId());
        Assertions.assertEquals(462, updatedOffer_3.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer_3.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(2, activePromoList_shopSku_3.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku_3 = shopSku3.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku_3 = activePromoList_shopSku_3.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku_3 = allPromoList_shopSku_3.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku_3.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_3.containsKey(promo_id_1_shopSku_3.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku_3 = allPromoList_shopSku_3.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku_3.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_3.containsKey(promo_id_2_shopSku_3.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku_3 = allPromoList_shopSku_3.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku_3.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_3.containsKey(promo_id_3_shopSku_3.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku_3 = allPromoList_shopSku_3.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku_3.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_3.containsKey(promo_id_4_shopSku_3.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku_3 = allPromoList_shopSku_3.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku_3.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_3.containsKey(promo_id_5_shopSku_3.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku_3 = allPromoList_shopSku_3.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku_3.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_3.containsKey(promo_id_6_shopSku_3.getId()));

        // shop-sku-4
        DataCampOffer.Offer updatedOffer_4 = updatedDcOffers.get(4);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku_4 = updatedOffer_4.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku-4", updatedOffer_4.getIdentifiers().getOfferId());
        Assertions.assertEquals(462, updatedOffer_4.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer_4.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(2, activePromoList_shopSku_4.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku_4 = shopSku4.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku_4 = activePromoList_shopSku_4.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku_4 = allPromoList_shopSku_4.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku_4.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_4.containsKey(promo_id_1_shopSku_4.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku_4 = allPromoList_shopSku_4.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku_4.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_4.containsKey(promo_id_2_shopSku_4.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku_4 = allPromoList_shopSku_4.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku_4.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_4.containsKey(promo_id_3_shopSku_4.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku_4 = allPromoList_shopSku_4.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku_4.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_4.containsKey(promo_id_4_shopSku_4.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku_4 = allPromoList_shopSku_4.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku_4.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_4.containsKey(promo_id_5_shopSku_4.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku_4 = allPromoList_shopSku_4.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku_4.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_4.containsKey(promo_id_6_shopSku_4.getId()));

        // shop-sku-5
        DataCampOffer.Offer updatedOffer_5 = updatedDcOffers.get(5);
        List<DataCampOfferPromos.Promo> activePromoList_shopSku_5 = updatedOffer_5.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList();
        Assertions.assertEquals("shop-sku-5", updatedOffer_5.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, updatedOffer_5.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedOffer_5.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());
        Assertions.assertEquals(2, activePromoList_shopSku_5.size());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku_5 = shopSku5.getPromos()
                .getAnaplanPromos()
                .getAllPromos()
                .getPromosList();
        Map<String, DataCampOfferPromos.Promo> activePromosBySsku_shopSku_5 = activePromoList_shopSku_5.stream()
                .collect(
                        Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                );
        // #1
        DataCampOfferPromos.Promo promo_id_1_shopSku_5 = allPromoList_shopSku_5.get(0);
        Assertions.assertEquals(promoId1, promo_id_1_shopSku_5.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_5.containsKey(promo_id_1_shopSku_5.getId()));
        // #2
        DataCampOfferPromos.Promo promo_id_2_shopSku_5 = allPromoList_shopSku_5.get(1);
        Assertions.assertEquals(promoId2, promo_id_2_shopSku_5.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_5.containsKey(promo_id_2_shopSku_5.getId()));
        // #3
        DataCampOfferPromos.Promo promo_id_3_shopSku_5 = allPromoList_shopSku_5.get(2);
        Assertions.assertEquals(promoId3, promo_id_3_shopSku_5.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_5.containsKey(promo_id_3_shopSku_5.getId()));
        // #4
        DataCampOfferPromos.Promo promo_id_4_shopSku_5 = allPromoList_shopSku_5.get(3);
        Assertions.assertEquals(promoId4, promo_id_4_shopSku_5.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_5.containsKey(promo_id_4_shopSku_5.getId()));
        // #5
        DataCampOfferPromos.Promo promo_id_5_shopSku_5 = allPromoList_shopSku_5.get(4);
        Assertions.assertEquals(promoId5, promo_id_5_shopSku_5.getId());
        Assertions.assertTrue(activePromosBySsku_shopSku_5.containsKey(promo_id_5_shopSku_5.getId()));
        // #6
        DataCampOfferPromos.Promo promo_id_6_shopSku_5 = allPromoList_shopSku_5.get(5);
        Assertions.assertEquals(promoId6, promo_id_6_shopSku_5.getId());
        Assertions.assertFalse(activePromosBySsku_shopSku_5.containsKey(promo_id_6_shopSku_5.getId()));
    }

    /**
     * Проверяем работоспособность по пересечению акций по времени
     * <p>
     * targetPromo - #0 (Прямая скидка) с 5 по 10
     * <p>
     * #1 (Прямая скидка) с 1 по 2 (НЕ пересекается)
     * #2 (Прямая скидка) с 3 по 5 (пересекается)
     * #3 (Прямая скидка) с 5 по 6 (пересекается)
     * #4 (Прямая скидка) с 5 по 10 (пересекается)
     * #5 (Прямая скидка) с 7 по 12 (пересекается)
     * #6 (Прямая скидка) с 10 по 15 (пересекается)
     * #7 (Прямая скидка) с 11 по 20 (НЕ пересекается)
     * #8 (Промокод) с 2 по 7 (пересекается)
     * #9 (Промокод) с 11 по 20 (НЕ пересекается)
     * <p>
     * В файле для #0:
     * shop-sku ДА
     * <p>
     * (Изначально не может быть такого, чтобы shop-sku участвовал одновременно в #2 и #3, но чтобы не
     * раздувать тест предполагается такая умышленная оплошность)
     * В ОХ:
     * shop-sku: #0 НЕТ, #1 ДА, #2 ДА, #3 ДА, #4 ДА, #5 ДА,
     * #6 ДА, #7 ДА
     * <p>
     * Что в итоге:
     * shop-sku: #0 ДА, #1 ДА, #2 НЕТ, #3 НЕТ, #4 НЕТ, #5 НЕТ,
     * #6 НЕТ, #7 ДА
     */
    @Test
    public void intersectionPromoTest() {
        String targetPromoId = "target-promo";
        String promoId1 = "#1";
        String promoId2 = "#2";
        String promoId3 = "#3";
        String promoId4 = "#4";
        String promoId5 = "#5";
        String promoId6 = "#6";
        String promoId7 = "#7";
        String promoId8 = "#8";
        String promoId9 = "#9";

        DataCampPromo.PromoDescription targetPromo = createDiscountPromoDescription(targetPromoId, 5, 10, 50);
        var promoDescriptionByPromoId =
                Map.of(targetPromoId, targetPromo,
                        promoId1, createDiscountPromoDescription(promoId1, 1, 2, 50),
                        promoId2, createDiscountPromoDescription(promoId2, 3, 5, 50),
                        promoId3, createDiscountPromoDescription(promoId3, 5, 6, 50),
                        promoId4, createDiscountPromoDescription(promoId4, 5, 10, 50),
                        promoId5, createDiscountPromoDescription(promoId5, 7, 12, 50),
                        promoId6, createDiscountPromoDescription(promoId6, 10, 15, 50),
                        promoId7, createDiscountPromoDescription(promoId7, 11, 20, 50),
                        promoId8, createPercentagePromocodePromoDescription(promoId8, 2, 7, 10),
                        promoId9, createValuePromocodePromoDescription(promoId9, 11, 20, 10)
                );

        final List<CheapestAsGiftXlsPromoOffer> fileOffers = List.of(
                new CheapestAsGiftXlsPromoOffer.Builder()
                        .withShopSku("shop-sku")
                        .withParticipate(true)
                        .build());

        final DataCampOffer.Offer shopSku = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku")
                        .setWarehouseId(461)
                        .build())
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null),
                                                        createPromo(promoId7, null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo(targetPromoId, null, null),
                                                        createPromo(promoId1, null, null),
                                                        createPromo(promoId2, null, null),
                                                        createPromo(promoId3, null, null),
                                                        createPromo(promoId4, null, null),
                                                        createPromo(promoId5, null, null),
                                                        createPromo(promoId6, null, null),
                                                        createPromo(promoId7, null, null)
                                                )
                                        )
                                )
                                .setPartnerPromos(
                                        createPartnerPromos(
                                                Arrays.asList(
                                                        createPromocodePromo(promoId8),
                                                        createPromocodePromo(promoId9)
                                                )
                                        )
                                )
                )
                .build();

        final Map<String, DataCampOffer.Offer> eligibleDCOffers =
                Map.of("shop-sku", shopSku);

        final List<DataCampOffer.Offer> updatedDcOffers =
                validationService
                        .updateOffersPromoInfo(
                                targetPromo, fileOffers, eligibleDCOffers, promoDescriptionByPromoId, false);

        Assertions.assertEquals(1, updatedDcOffers.size());
        // shop-sku 461
        DataCampOffer.Offer updatedDcOffer = updatedDcOffers.get(0);
        Assertions.assertEquals("shop-sku", updatedDcOffer.getIdentifiers().getOfferId());
        Assertions.assertEquals(461, updatedDcOffer.getIdentifiers().getWarehouseId());
        Assertions.assertEquals(0, updatedDcOffer.getPromos().getAnaplanPromos().getAllPromos().getPromosCount());

        List<DataCampOfferPromos.Promo> allPromoList_shopSku = new ArrayList<>();
        allPromoList_shopSku.addAll(
                shopSku.getPromos()
                        .getAnaplanPromos()
                        .getAllPromos()
                        .getPromosList()
        );
        allPromoList_shopSku.addAll(shopSku.getPromos().getPartnerPromos().getPromosList());
        Set<String> activePromosIds = updatedDcOffer.getPromos()
                .getAnaplanPromos()
                .getActivePromos()
                .getPromosList()
                .stream()
                .map(DataCampOfferPromos.Promo::getId)
                .collect(Collectors.toSet());
        // target-promo
        final DataCampOfferPromos.Promo target_promo = allPromoList_shopSku.get(0);
        Assertions.assertEquals(targetPromoId, target_promo.getId());
        Assertions.assertTrue(activePromosIds.contains(targetPromoId));
        // #1
        final DataCampOfferPromos.Promo promoId_1 = allPromoList_shopSku.get(1);
        Assertions.assertEquals(promoId1, promoId_1.getId());
        Assertions.assertTrue(activePromosIds.contains(promoId1));
        // #2
        final DataCampOfferPromos.Promo promoId_2 = allPromoList_shopSku.get(2);
        Assertions.assertEquals(promoId2, promoId_2.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId2));
        // #3
        final DataCampOfferPromos.Promo promoId_3 = allPromoList_shopSku.get(3);
        Assertions.assertEquals(promoId3, promoId_3.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId3));
        // #4
        final DataCampOfferPromos.Promo promoId_4 = allPromoList_shopSku.get(4);
        Assertions.assertEquals(promoId4, promoId_4.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId4));
        // #5
        final DataCampOfferPromos.Promo promoId_5 = allPromoList_shopSku.get(5);
        Assertions.assertEquals(promoId5, promoId_5.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId5));
        // #6
        final DataCampOfferPromos.Promo promoId_6 = allPromoList_shopSku.get(6);
        Assertions.assertEquals(promoId6, promoId_6.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId6));
        // #7
        final DataCampOfferPromos.Promo promoId_7 = allPromoList_shopSku.get(7);
        Assertions.assertEquals(promoId7, promoId_7.getId());
        Assertions.assertTrue(activePromosIds.contains(promoId7));
        // #8
        final DataCampOfferPromos.Promo promoId_8 = allPromoList_shopSku.get(8);
        Assertions.assertEquals(promoId8, promoId_8.getId());
        Assertions.assertFalse(activePromosIds.contains(promoId6));
        // #9
        final DataCampOfferPromos.Promo promoId_9 = allPromoList_shopSku.get(9);
        Assertions.assertEquals(promoId9, promoId_9.getId());
        Assertions.assertTrue(activePromosIds.contains(promoId7));
    }

    /**
     * Промо включается у двух офферов shop_sku_1 и shop_sku_3 и остается не включенной у shop_sku_3.
     */
    @Test
    void calculateStatisticsForSeveralCorrectSelectedOffers() {
        DiscountXlsPromoOffer validatedOffer1 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_1")
                .withMarketSku(1L)
                .withPromoPrice(1000L)
                .build();
        DiscountXlsPromoOffer validatedOffer2 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_2")
                .withMarketSku(1L)
                .withPromoPrice(1100L)
                .build();
        DiscountXlsPromoOffer validatedOffer3 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_3")
                .withMarketSku(1L)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers = List.of(validatedOffer1, validatedOffer2, validatedOffer3);

        DataCampOffer.Offer dcOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_1")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                // Активная акция, которой нет в списке всех акций.
                                                // Должна быть проигнорирована.
                                                Collections.singletonList(
                                                        createPromo("promo_id_666", null, 2000L)
                                                ),
                                                Collections.singletonList(
                                                        createPromo("promo_id_1", null, 2000L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_2")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Arrays.asList(
                                                        createPromo("promo_id_1", null, 2000L),
                                                        createPromo("promo_id_2", null, null)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer3 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_3")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo_id_2", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("promo_id_1", null, 2000L),
                                                        createPromo("promo_id_2", null, null)
                                                )
                                        )
                                )
                )
                .build();
        Map<String, DataCampOffer.Offer> datacampOffersBySsku = Map.of(
                "shop_sku_1", dcOffer1,
                "shop_sku_2", dcOffer2,
                "shop_sku_3", dcOffer3
        );

        DataCampPromo.PromoDescription targetPromoDescription = createDiscountPromoDescription("promo_id_1", 1, 2, 50);

        DataCampPromo.PromoDescription promo2 = createCheapestAsGiftPromoDescription("promo_id_2", 0, 1, 2);

        Map<String, DataCampPromo.PromoDescription> promoDescriptionById = Map.of(
                "promo_id_1", targetPromoDescription,
                "promo_id_2", promo2
        );

        PromoOfferValidationResult<DiscountXlsPromoOffer> validationResult =
                validationService.calculateStatistics(validatedOffers, datacampOffersBySsku,
                        targetPromoDescription, promoDescriptionById).build();
        Assertions.assertEquals(3L, validationResult.getTotal().longValue());
        Assertions.assertEquals(2L, validationResult.getCorrectSelected().longValue());
        Assertions.assertEquals(0L, validationResult.getInvalid().longValue());
        Assertions.assertEquals(0L, validationResult.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(3, validationResult.getValidatedOffers().size());
        Assertions.assertTrue(validationResult.getChangedOffer().isPresent());
        Assertions.assertEquals("shop_sku_1", validationResult.getChangedOffer().get().getOfferId());
        Assertions.assertTrue(validationResult.getChangedOffer().get().isActive());
    }

    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/before_multiPromo.csv")
    void testCalculateStatisticsMultiPromo() {
        DiscountXlsPromoOffer validatedOffer1 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_1")
                .withMarketSku(1L)
                .withPromoPrice(1000L)
                .build();
        DiscountXlsPromoOffer validatedOffer2 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_2")
                .withMarketSku(1L)
                .withPromoPrice(1100L)
                .build();
        DiscountXlsPromoOffer validatedOffer3 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_3")
                .withMarketSku(1L)
                .withPromoPrice(1200L)
                .build();
        DiscountXlsPromoOffer validatedOffer4 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_4")
                .withMarketSku(1L)
                .build();
        DiscountXlsPromoOffer validatedOffer5 = new DiscountXlsPromoOffer.Builder()
                .withShopSku("shop_sku_5")
                .withMarketSku(1L)
                .build();
        List<DiscountXlsPromoOffer> validatedOffers =
                List.of(validatedOffer1, validatedOffer2, validatedOffer3, validatedOffer4, validatedOffer5);

        DataCampPromo.PromoDescription targetPromoDescription = createDiscountPromoDescription("#1", 1, 2, 50);

        DataCampOffer.Offer dcOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_1")
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#1", null, 2000L)
                                                ),
                                                Collections.singletonList(
                                                        createPromo("#2", null, 2000L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_2")
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Arrays.asList(
                                                        createPromo("#1", null, 2000L),
                                                        createPromo("#2", null, null)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer3 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_3")
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#2", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("#1", null, 2000L),
                                                        createPromo("#2", null, null)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer4 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_4")
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("#1", null, 2000L)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer5 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_5")
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("#1", null, 2000L)
                                                        )
                                        )
                                )
                )
                .build();
        Map<String, DataCampOffer.Offer> datacampOffersBySsku = Map.of(
                "shop_sku_1", dcOffer1,
                "shop_sku_2", dcOffer2,
                "shop_sku_3", dcOffer3,
                "shop_sku_4", dcOffer4,
                "shop_sku_5", dcOffer5
        );

        BasePromoDetails promoDetails1 = new BasePromoDetails.Builder()
                .setPromoId("#1")
                .setMechanic(PiPromoMechanicDto.DIRECT_DISCOUNT)
                .setDiscountPercentage(10)
                .setName("One")
                .setStartDate(LocalDateTime.of(2022, 3, 3, 00, 00))
                .setEndDate(LocalDateTime.of(2022, 3, 11, 23, 59, 59))
                .setPriority(7L)
                .build();

        BasePromoDetails promoDetails2 = new BasePromoDetails.Builder()
                .setPromoId("#2")
                .setMechanic(PiPromoMechanicDto.DIRECT_DISCOUNT)
                .setDiscountPercentage(10)
                .setName("Two")
                .setStartDate(LocalDateTime.of(2022, 3, 15, 00, 00))
                .setEndDate(LocalDateTime.of(2022, 3, 19, 23, 59, 59))
                .setPriority(5L)
                .build();

        Map<String, OfferPromosEnablingInfo> offerPromosEnablingInfoByOfferId = new HashMap<>();
        offerPromosEnablingInfoByOfferId.put("shop_sku_1", new OfferPromosEnablingInfo(
                List.of(promoDetails1), Collections.emptyList(), List.of(promoDetails2)));
        offerPromosEnablingInfoByOfferId.put(
                "shop_sku_2",
                new OfferPromosEnablingInfo(
                        Collections.emptyList(),
                        List.of(
                                new PromoEnablingInfo(
                                        promoDetails1,
                                        List.of(
                                                new DateRange(
                                                        LocalDateTime.of(2022, 3, 5, 0, 0),
                                                        LocalDateTime.of(2022, 3, 8, 23, 59, 59)
                                                )
                                        )
                                )
                        ),
                        List.of(promoDetails2)
                )
        );
        offerPromosEnablingInfoByOfferId.put("shop_sku_3", new OfferPromosEnablingInfo(
                List.of(promoDetails2), Collections.emptyList(), List.of(promoDetails1)));

        PromoOfferValidationResult<DiscountXlsPromoOffer> validationResult =
                validationService.calculateStatisticsMultiPromo(
                        validatedOffers,
                        datacampOffersBySsku,
                        targetPromoDescription,
                        offerPromosEnablingInfoByOfferId
                ).build();

        Assertions.assertEquals(1L, validationResult.getFullyParticipating());
        Assertions.assertEquals(1L, validationResult.getPartiallyParticipating());
        Assertions.assertEquals(1L, validationResult.getNotParticipating());
        Assertions.assertEquals(2L, validationResult.getDisabled());
    }

    /**
     * Оффер shop_sku_1 заполнен некорректно.
     * У оффера shop_sku_2 включается промо promo_id_1 и остается включенным непересекающееся с ним промо promo_id_3
     * (с разных складов).
     * У оффера shop_sku_3 должна выключиться акция promo_id_2, чтобы могла включиться promo_id_1.
     */
    @Test
    void calculateStatisticsForInvalidOffers() {
        CheapestAsGiftXlsPromoOffer validatedOffer1 = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop_sku_1")
                .withParticipate(true)
                .withErrors(new TreeSet<>(Collections.singleton(PromoOfferValidationError.BLANK_MARKET_SKU)))
                .build();
        CheapestAsGiftXlsPromoOffer validatedOffer2 = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop_sku_2")
                .withMarketSku(1L)
                .withParticipate(true)
                .build();
        CheapestAsGiftXlsPromoOffer validatedOffer3 = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop_sku_3")
                .withMarketSku(1L)
                .withParticipate(true)
                .build();
        List<CheapestAsGiftXlsPromoOffer> validatedOffers = List.of(validatedOffer1, validatedOffer2, validatedOffer3);

        DataCampOffer.Offer dcOffer1 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_1")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo("promo_id_1", null, null)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer2 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_2")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo_id_3", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("promo_id_1", null, null),
                                                        createPromo("promo_id_2", null, null),
                                                        createPromo("promo_id_3", null, null)
                                                )
                                        )
                                )
                )
                .build();
        DataCampOffer.Offer dcOffer3 = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId("shop_sku_3")
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setMarketSkuId(1L)
                                        .build()
                                )
                                .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("promo_id_2", null, null)
                                                ),
                                                Arrays.asList(
                                                        createPromo("promo_id_1", null, null),
                                                        createPromo("promo_id_2", null, null)
                                                )
                                        )
                                )
                )
                .build();
        Map<String, DataCampOffer.Offer> datacampOffersBySsku = Map.of(
                "shop_sku_1", dcOffer1,
                "shop_sku_2", dcOffer2,
                "shop_sku_3", dcOffer3
        );

        DataCampPromo.PromoDescription targetPromoDescription = createCheapestAsGiftPromoDescription("promo_id_1",
                142, 1, 3);


        DataCampPromo.PromoDescription promo2 = createCheapestAsGiftPromoDescription("promo_id_2", 142, 2, 6);

        DataCampPromo.PromoDescription promo3 = createCheapestAsGiftPromoDescription("promo_id_3", 175, 3, 4);

        Map<String, DataCampPromo.PromoDescription> promoDescriptionById = Map.of(
                "promo_id_1", targetPromoDescription,
                "promo_id_2", promo2,
                "promo_id_3", promo3
        );

        PromoOfferValidationResult<CheapestAsGiftXlsPromoOffer> validationResult =
                validationService.calculateStatistics(validatedOffers, datacampOffersBySsku,
                        targetPromoDescription, promoDescriptionById).build();
        Assertions.assertEquals(3L, validationResult.getTotal().longValue());
        Assertions.assertEquals(2L, validationResult.getCorrectSelected().longValue());
        Assertions.assertEquals(1L, validationResult.getInvalid().longValue());
        Assertions.assertEquals(1L, validationResult.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(3, validationResult.getValidatedOffers().size());
        Assertions.assertTrue(validationResult.getChangedOffer().isPresent());
        Assertions.assertEquals("shop_sku_2", validationResult.getChangedOffer().get().getOfferId());
        Assertions.assertTrue(validationResult.getChangedOffer().get().isActive());
    }

    /**
     * Оффер выходит из акции.
     */
    @Test
    public void changedOffer1Test() {
        CheapestAsGiftXlsPromoOffer fileOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .build();
        DataCampPromo.PromoDescription promoId = createDiscountPromoDescription("#1", 1, 2, 50);

        DataCampOffer.Offer dcOfferWithActivePromo = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                ),
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                )
                                        )
                                )
                )
                .build();

        PromoOfferValidationResult<CheapestAsGiftXlsPromoOffer> result =
                validationService.calculateStatistics(List.of(fileOffer), Map.of("shop-sku-1", dcOfferWithActivePromo),
                        promoId, Map.of("#1", promoId)).build();

        Assertions.assertEquals(1L, result.getTotal().longValue());
        Assertions.assertEquals(0L, result.getCorrectSelected().longValue());
        Assertions.assertEquals(0L, result.getInvalid().longValue());
        Assertions.assertEquals(0L, result.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(1, result.getValidatedOffers().size());
        Assertions.assertTrue(result.getChangedOffer().isPresent());
        Assertions.assertEquals("shop-sku-1", result.getChangedOffer().get().getOfferId());
        Assertions.assertFalse(result.getChangedOffer().get().isActive());
    }


    /**
     * Статус участия оффера в акции не меняется: он продолжает участвовать.
     */
    @Test
    public void changedOffer2Test() {
        CheapestAsGiftXlsPromoOffer fileOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .withParticipate(true)
                .build();
        DataCampPromo.PromoDescription promoId = createDiscountPromoDescription("#1", 1, 2, 50);

        DataCampOffer.Offer dcOfferWithActivePromo = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                ),
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                )
                                        )
                                )
                )
                .build();

        PromoOfferValidationResult<CheapestAsGiftXlsPromoOffer> result =
                validationService.calculateStatistics(List.of(fileOffer), Map.of("shop-sku-1", dcOfferWithActivePromo),
                        promoId, Map.of("#1", promoId)).build();

        Assertions.assertEquals(1L, result.getTotal().longValue());
        Assertions.assertEquals(1L, result.getCorrectSelected().longValue());
        Assertions.assertEquals(0L, result.getInvalid().longValue());
        Assertions.assertEquals(0L, result.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(1, result.getValidatedOffers().size());
        Assertions.assertFalse(result.getChangedOffer().isPresent());
    }

    /**
     * Статус участия оффера в акции не меняется: он НЕ участвует.
     */
    @Test
    public void changedOffer3Test() {
        CheapestAsGiftXlsPromoOffer fileOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .build();
        DataCampPromo.PromoDescription promoId = createDiscountPromoDescription("#1", 1, 2, 50);

        DataCampOffer.Offer dcOffer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                )
                                        )
                                )
                )
                .build();

        PromoOfferValidationResult<CheapestAsGiftXlsPromoOffer> result =
                validationService.calculateStatistics(List.of(fileOffer), Map.of("shop-sku-1", dcOffer),
                        promoId, Map.of("#1", promoId)).build();

        Assertions.assertEquals(1L, result.getTotal().longValue());
        Assertions.assertEquals(0L, result.getCorrectSelected().longValue());
        Assertions.assertEquals(0L, result.getInvalid().longValue());
        Assertions.assertEquals(0L, result.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(1, result.getValidatedOffers().size());
        Assertions.assertFalse(result.getChangedOffer().isPresent());
    }

    /**
     * Оффер добавляется к участию в акции.
     */
    @Test
    public void changedOffer4Test() {
        CheapestAsGiftXlsPromoOffer fileOffer = new CheapestAsGiftXlsPromoOffer.Builder()
                .withShopSku("shop-sku-1")
                .withParticipate(true)
                .build();
        DataCampPromo.PromoDescription promoId = createDiscountPromoDescription("#1", 1, 2, 50);

        DataCampOffer.Offer dcOffer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("shop-sku-1")
                        .build()
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo("#1", null, null)
                                                )
                                        )
                                )
                )
                .build();

        PromoOfferValidationResult<CheapestAsGiftXlsPromoOffer> result =
                validationService.calculateStatistics(List.of(fileOffer), Map.of("shop-sku-1", dcOffer),
                        promoId, Map.of("#1", promoId)).build();

        Assertions.assertEquals(1L, result.getTotal().longValue());
        Assertions.assertEquals(1L, result.getCorrectSelected().longValue());
        Assertions.assertEquals(0L, result.getInvalid().longValue());
        Assertions.assertEquals(0L, result.getParticipatingInOtherPromos().longValue());
        Assertions.assertEquals(1, result.getValidatedOffers().size());
        Assertions.assertTrue(result.getChangedOffer().isPresent());
        Assertions.assertEquals("shop-sku-1", result.getChangedOffer().get().getOfferId());
        Assertions.assertTrue(result.getChangedOffer().get().isActive());
    }

    /**
     * Проверка, что оффер без msku успешно валидируется при выставленном флаге partner.promos.msku.remove
     */
    @Test
    @DbUnitDataSet(before = "promoOffersValidationServiceFunctionalTest/removeMskuFromPartnerPromos.before.csv")
    public void noMskuTest() throws IOException {
        // тестовые данные, которые дб записаны в таблицу с валидацией
        String keyWithPrefix = "eligible_s3_key";
        String validationId = "validationId";
        String originalUploadUrl = "file://promo-offers.xlsx";
        String validatedUploadUrl = "http://test.url/file-uploaded-with_results.xslm";
        long supplierId = 1L;
        String testFilePath = "promoOffersValidationServiceFunctionalTest/removeMskuFromPartnerPromos.xlsm";

        InputStream inputStream = mockMds(keyWithPrefix, testFilePath, validatedUploadUrl);

        String promoId = "cf_1234";
        DataCampPromo.PromoDescription promo = createDiscountPromoDescription(promoId, 1, 2, 10);
        final DataCampOffer.Offer dataCampOffer = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setShopId((int) supplierId)
                        .setWarehouseId(1)
                        .setOfferId("1")
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Collections.singletonList(
                                                        createPromo(promoId, null, 2000L)))))
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(1)
                                .setProductName("name")
                                .build()
                        )
                        .build())
                .build();

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOffer))
                        .build())
                .when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        final PromoOfferXlsValidationRequest<DiscountXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<DiscountXlsPromoOffer>()
                        .withValidationId(validationId)
                        .withSupplierId(supplierId)
                        .withOriginalUpload(new ValidationUpload(validationId, originalUploadUrl, keyWithPrefix))
                        .withPromoId(promoId)
                        .withPromoType(PromoType.DISCOUNT)
                        .withTemplateContext(discountTemplateContext)
                        .withValidationStrategy(new DiscountOfferXlsValidationStrategy(true))
                        .build();

        PromoOfferValidationStats promoOfferValidationStats = validationService.validateSync(validationRequest, false);
        ArgumentCaptor<GetPromoBatchRequestWithFilters> captor =
                ArgumentCaptor.forClass(GetPromoBatchRequestWithFilters.class);
        verify(dataCampShopClient, times(2)).getPromos(captor.capture());
        GetPromoBatchRequestWithFilters promoRequest = captor.getValue();
        assertThat(promoRequest.getRequest().getEntriesList())
                .singleElement()
                .satisfies(promoFromRequest ->
                        assertThat(promoFromRequest.getSource()).isEqualTo(Promo.ESourceType.ANAPLAN)
                );

        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("validationId")
                .withPromoId(promoId)
                .addTotalOffers(1L)
                .addCorrectSelectedOffers(1L)
                .addInvalidOffers(0L)
                .addParticipatingInOtherPromos(0L)
                .withValidatedUpload(new ValidationUpload("validationId", "http://test.url/file-uploaded-with_results" +
                 ".xslm", "eligible_s3_key"))
                .withChangedOffer(new ChangedOffer("1", 0, 1, true))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .withEligibleS3Key("eligible_s3_key")
                .withHost(promoOfferValidationStats.getHost())
                .build();
        Assertions.assertEquals(expectedStats, promoOfferValidationStats);
        inputStream.close();
    }

    private DataCampOfferPromos.Promo createPromo(String id, Long price, Long oldPrice) {
        DataCampOfferPromos.Promo.DirectDiscount.Builder directDiscountBuilder =
                DataCampOfferPromos.Promo.DirectDiscount.newBuilder();
        if (price != null) {
            directDiscountBuilder.setPrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(price))
                            )
                            .build()
            );
        }
        if (oldPrice != null) {
            directDiscountBuilder.setBasePrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice))
                            )
                            .build()
            );
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .setDirectDiscount(directDiscountBuilder)
                .build();
    }

    private DataCampOfferPromos.MarketPromos createMarketPromos(
            List<DataCampOfferPromos.Promo> activePromos,
            List<DataCampOfferPromos.Promo> allPromos
    ) {
        DataCampOfferPromos.MarketPromos.Builder anaplanPromosBuilder = DataCampOfferPromos.MarketPromos.newBuilder();
        if (CollectionUtils.isNotEmpty(activePromos)) {
            anaplanPromosBuilder.setActivePromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(activePromos)
                            .build()
            );
        }
        if (CollectionUtils.isNotEmpty(allPromos)) {
            anaplanPromosBuilder.setAllPromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(allPromos)
                            .build()
            );
        }
        return anaplanPromosBuilder.build();
    }

    private DataCampOfferPromos.Promos createPartnerPromos(
            List<DataCampOfferPromos.Promo> promos
    ) {
        return DataCampOfferPromos.Promos.newBuilder().addAllPromos(promos).build();
    }

    private DataCampOfferPromos.Promo createPromocodePromo(String id) {
        DataCampOfferPromos.Promo.DirectDiscount.Builder directDiscountBuilder =
                DataCampOfferPromos.Promo.DirectDiscount.newBuilder();
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .setDirectDiscount(directDiscountBuilder)
                .build();
    }

    private DataCampOfferPromos.OfferPromos createAllPromos(String promoId) {
        return DataCampOfferPromos.OfferPromos.newBuilder()
                .setAnaplanPromos(
                        createMarketPromos(
                                Collections.emptyList(),
                                Collections.singletonList(
                                        DataCampOfferPromos.Promo.newBuilder()
                                                .setId(promoId)
                                                .build()
                                )
                        )
                )
                .build();
    }

    private InputStream mockMds(String keyWithPrefix, String testFilePath, String validatedUploadUrl)
            throws FileNotFoundException, MalformedURLException {
        // скачивание файла
        String bucketName = "bucket";
        when(resourceLocationFactory.createLocation(keyWithPrefix))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getObject(bucketName, keyWithPrefix))
                .thenReturn(s3Object);
        File file = new File(getClass()
                .getResource(testFilePath).getPath());
        InputStream inputStream = new FileInputStream(file);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        // загрузка файла
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getUrl(bucketName, keyWithPrefix)).thenReturn(new URL(validatedUploadUrl));
        return inputStream;
    }
}
