package ru.yandex.market.rg.asyncreport.assortment;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import Market.DataCamp.SyncAPI.GetVerdicts;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.config.FunctionalTestConfig;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.report.client.model.PriceRecommendationsDTO;
import ru.yandex.market.core.report.client.model.ReportRecommendationsResultDTO;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.config.FunctionalTestEnvironmentConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

/**
 * Ручной тест для генерации отчета Ассортимент.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Disabled
public class AssortmentGeneratorManualTest extends FunctionalTest {

    @Autowired
    @Qualifier("assortmentGenerator")
    private AbstractAssortmentGenerator assortmentGenerator;

    @Autowired
    @Qualifier("assortmentPriceGenerator")
    private AbstractAssortmentGenerator assortmentPriceGenerator;

    @Autowired
    @Qualifier("assortmentBusinessGenerator")
    private AbstractAssortmentGenerator assortmentBusinessGenerator;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    private AsyncMarketReportService asyncMarketReportService;

    /**
     * Тест на генерацию отчета Ассортимент.
     * Шаблон берется из бина {@link FunctionalTestConfig#dbsFeedXlsTemplateResource()}.
     * Оффер берется из мока.
     * Файл сохраняется в {@code ~/reports/}.
     */
    @Test
    @DisplayName("Выгрузка ассортимента для DBS из ЕКата")
    @DbUnitDataSet(before = "csv/AssortmentGeneratorManualTest.generateWhite.before.csv")
    void generateWhite() {
        mockMds("dbs-");
        mockDatacampClient(dataCampShopClient, "proto/AssortmentGeneratorManualTest.feed.json",
                "proto/AssortmentGeneratorManualTest.verdicts.json");
        generate(false, false, false);
    }

    /**
     * Тест на генерацию отчета Ассортимент.
     * Шаблон берется из бина {@link FunctionalTestConfig#advFeedXlsTemplateResource()}.
     * Оффер берется из мока.
     * Файл сохраняется в {@code ~/reports/}.
     */
    @Test
    @DisplayName("Выгрузка ассортимента для ADV из ЕКата")
    @DbUnitDataSet(before = {"csv/AssortmentGeneratorManualTest.generateWhite.before.csv",
            "csv/AssortmentSeparateADVTemplate.csv"})
    void generateADV() {
        mockMds("adv-");
        mockDatacampClient(dataCampShopClient, "proto/AssortmentGeneratorManualTest.feed.json",
                "proto/AssortmentGeneratorManualTest.verdicts.json");
        generate(false, false, false);
    }

    /**
     * Тест на генерацию отчета Ассортимент.
     * Шаблон берется из бина {@link FunctionalTestEnvironmentConfig#unitedSupplierXlsHelper()}.
     * Оффер берется из мока.
     * Файл сохраняется в {@code ~/reports/}.
     */
    @Test
    @DisplayName("Выгрузка ассортимента для синего из ЕКата")
    @DbUnitDataSet(before = "csv/AssortmentGeneratorManualTest.generateBlue.before.csv")
    void generateBlue() {
        mockMds("blue-");
        mockDatacampClient(dataCampShopClient, "proto/AssortmentGeneratorManualTest.feed.json",
                "proto/AssortmentGeneratorManualTest.verdicts.json");
        generate(false, false, false);
    }

    @Test
    @DisplayName("Выгрузка ассортимента для бизнеса")
    @DbUnitDataSet(before = "csv/AssortmentGeneratorManualTest.generateBlue.before.csv")
    void manualGenerateBusiness() {
        mockMds("business-");
        mockDatacampClient(dataCampShopClient, "proto/AssortmentGeneratorManualTest.feed.json",
                "proto/AssortmentGeneratorManualTest.verdicts.json");
        generate(false, true, false);
    }

    @Test
    @DisplayName("Выгрузка цен для синего из ЕКата")
    @DbUnitDataSet(before = {"csv/AssortmentGeneratorManualTest.generateBlue.before.csv", "csv/AssortmentUniteEnv.csv"})
    void generateBluePrice() {
        mockMds("price-");
        mockSuggest("151515");
        mockDatacampClient(dataCampShopClient, "proto/AssortmentGeneratorManualTest.feed.json",
                "proto/AssortmentGeneratorManualTest.verdicts.json");
        generate(true, false, false);
    }

    private void mockMds(String filePrefix) {
        Mockito.doAnswer(invocation -> {
            File tmpDir = SystemUtils.getUserHome();
            File dir = new File(tmpDir, "reports");
            FileUtils.forceMkdir(dir);

            File resultFile = ((FileContentProvider) invocation.getArgument(1)).getFile();
            Path sourcePath = resultFile.toPath();
            File target = new File(dir, filePrefix + sourcePath.getFileName());


            Files.newByteChannel(target.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING).close();
            Files.copy(sourcePath, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return "";
        }).when(mdsS3Client).upload(any(), any());
    }

    private void mockDatacampClient(DataCampClient dataCampClient, String offersPath, String verdictsPath) {
        SyncGetOffer.GetUnitedOffersResponse rawProto = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                offersPath,
                getClass()
        );
        SearchBusinessOffersResult responseProto = DataCampStrollerConversions.fromStrollerResponse(rawProto);
        Mockito.doReturn(responseProto)
                .when(dataCampClient).searchBusinessOffers(any());

        GetVerdicts.GetVerdictsBatchResponse verdictsResponse = ProtoTestUtil.getProtoMessageByJson(
                GetVerdicts.GetVerdictsBatchResponse.class,
                verdictsPath,
                getClass()
        );
        Mockito.doReturn(verdictsResponse)
                .when(dataCampClient).getVerdicts(any(), anyLong(), anyLong());
        Mockito.doReturn(dataCampClient).when(dataCampService).chooseClient(anyLong());
    }

    private void mockSuggest(String marketSku) {
        ReportRecommendationsResultDTO mockedReportResult = new ReportRecommendationsResultDTO();
        mockedReportResult.setRecommendations(List.of(new PriceRecommendationsDTO(
                marketSku,
                List.of(
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(150),
                                BigDecimal.valueOf(500),
                                "buybox"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(200),
                                BigDecimal.valueOf(750),
                                "minPriceMarket"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(250),
                                BigDecimal.valueOf(1000),
                                "defaultOffer"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(300),
                                BigDecimal.valueOf(1250),
                                "maxOldPrice"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(350),
                                BigDecimal.valueOf(1500),
                                "priceLimit"
                        )
                )
        )));
        doReturn(CompletableFuture.completedFuture(mockedReportResult))
                .when(asyncMarketReportService)
                .async(any(), any());
    }

    private void generate(boolean isSuggest, boolean isBusiness, boolean isPriceMode) {
        AssortmentParams params = new AssortmentParams();
        params.setEntityId(1001L);
        params.setUseSuggesting(isSuggest);

        getAssortmentGenerator(isBusiness, isPriceMode).generate("report_id", params);
    }

    protected AbstractAssortmentGenerator getAssortmentGenerator(boolean isBusiness, boolean isPriceMode) {
        if (isBusiness) {
            return assortmentBusinessGenerator;
        } else if (isPriceMode) {
            return assortmentPriceGenerator;
        } else {
            return assortmentGenerator;
        }
    }
}
