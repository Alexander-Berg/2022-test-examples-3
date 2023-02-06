package ru.yandex.market.deliverycalculator.indexer.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.service.yado.TarifficatorClient;
import ru.yandex.market.deliverycalculator.indexer.util.HttpClientTestUtils;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.OfferRuleInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.TariffInfoDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliveryTariffTypeDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.meta.TariffMeta;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.meta.TariffMetaData;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.meta.TariffMetaRevision;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffSource;
import ru.yandex.market.deliverycalculator.storage.model.YaDeliveryTariffFilter;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliveryTariffMeta;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.YaDeliveryTariffLoadService;
import ru.yandex.market.deliverycalculator.test.TestUtils;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonTestUtil;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStageType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.UpdatingTariffBoilingKey;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeRestrictionsDto;
import ru.yandex.market.logistics.management.entity.response.tariff.TariffLocationCargoTypeDto;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест обновления тарифов калькулятора, проверяет корректность
 * обновления данных задачей {@link TariffImportService}. <p>
 * Калькулятор ходит в апи маркет.доставки за списком тарифов,
 * и обновляет их в калькуляторе исходя из полученных тарифов и дат их обновления.
 * <p>
 * Данные должны соответствовать полученному снэпшоту из доставки по правилам:
 * <ul>
 * <li> Если тариф есть в данных мар.до, то он должен быть и в бд калькулятора. </li>
 * <li> Если тарифа нет в данных мар.до, то его не должно быть и в бд калькулятора. </li>
 * <li> Если тариф из данных мар.до имеет дату обновления {@link TariffMetaData#getTimestamp()}
 * новее даты {@link YaDeliveryTariffMeta#getTimestamp()} текущего сохраненного тарифа,
 * то его данные должны быть обновлены.</li>
 * </ul>
 *
 * @author stani on 17.10.17.
 */
class TariffImportServiceTest extends FunctionalTest {
    private static final TariffMetaData TARIFF1_INITIAL_META =
            createTariffMetaData(1L, randomAlphanumeric(32), System.currentTimeMillis() / 1000);
    private static final TariffMetaData TARIFF2_INITIAL_META =
            createTariffMetaData(2L, randomAlphanumeric(32), System.currentTimeMillis() / 1000);
    private static final TariffMetaData TARIFF3_INITIAL_META =
            createTariffMetaData(3L, randomAlphanumeric(32), (System.currentTimeMillis() / 1000) + 100);
    private static final TariffMetaData TARIFF2_UPDATED_META =
            createTariffMetaData(2L, randomAlphanumeric(32), (System.currentTimeMillis() / 1000) + 200);
    private static final TariffMetaData TARIFF4_INITIAL_META =
            createTariffMetaData(4L, randomAlphanumeric(32), (System.currentTimeMillis() / 1000) + 400);


    private static final TariffMetaData TARIFF1001_INITIAL_META =
            createTariffMetaData(1001L, "hash_1", System.currentTimeMillis() / 1000);
    private static final TariffMetaData TARIFF1002_INITIAL_META =
            createTariffMetaData(1002L, "hash_2", System.currentTimeMillis() / 1000);
    private static final TariffMetaData TARIFF1003_INITIAL_META =
            createTariffMetaData(1003L, "hash_3", (System.currentTimeMillis() / 1000) + 100);
    private static final TariffMetaData TARIFF1004_INITIAL_META =
            createTariffMetaData(1004L, "hash_4", (System.currentTimeMillis() / 1000) + 200);
    private static final TariffMetaData TARIFF1005_INITIAL_META =
            createTariffMetaData(1005L, "hash_5", (System.currentTimeMillis() / 1000) + 200);
    private static final TariffMetaData TARIFF1006_INITIAL_META =
            createTariffMetaData(1006L, "hash_6", (System.currentTimeMillis() / 1000) + 200);
    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private YaDeliveryTariffLoadService yaDeliveryTariffLoadService;

    @Autowired
    @Qualifier("daasTariffImportService")
    private TariffImportService daasTariffImportService;

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private BoilingSolomonService boilingSolomonService;

    @Autowired
    private ResourceLocationFactory mdsS3LocationFactory;

    private TariffImportService tariffImportServiceMocked;
    private TarifficatorClient yaDeliveryClientMock;

    private static TariffMeta createTariffMeta(TariffMetaData... metaData) {
        TariffMeta tariffMeta = new TariffMeta();
        TariffMetaRevision trv = new TariffMetaRevision();
        trv.setHash(randomAlphanumeric(32));
        trv.setTimestamp(System.currentTimeMillis() / 1000);
        tariffMeta.setRevision(trv);
        tariffMeta.setData(Collections.unmodifiableCollection(Arrays.asList(metaData)));
        return tariffMeta;
    }

    private static TariffMetaData createTariffMetaData(long tariffId, String hash, long timestamp) {
        TariffMetaData tfd = new TariffMetaData();
        tfd.setHash(hash);
        tfd.setTariffId(tariffId);
        tfd.setTimestamp(timestamp);
        return tfd;
    }

    private static TariffInfoDTO getTariff(long id) {
        TariffInfoDTO ti = new TariffInfoDTO();
        ti.setId(id);
        ti.setCarrierId(123);
        ti.setCurrency("RUR");
        ti.setForCustomer(true);
        ti.setM3weight(BigDecimal.valueOf(new Random().nextInt(100000)).movePointLeft(2).doubleValue());
        ti.setRule(new OfferRuleInfoDTO());
        ti.setType(YaDeliveryTariffTypeDTO.COURIER);
        return ti;
    }

    @SuppressWarnings("SameParameterValue")
    private static TariffInfoDTO getTariff(long id, List<DeliveryTariffProgramType> programs) {
        TariffInfoDTO ti = getTariff(id);
        ti.setPrograms(programs);
        return ti;
    }

    @BeforeEach
    void init() throws MalformedURLException {
        yaDeliveryClientMock = mock(TarifficatorClient.class);

        tariffImportServiceMocked = new TariffImportService(
                DeliveryTariffSource.YADO, storageService, transactionTemplate, yaDeliveryClientMock,
                yaDeliveryTariffDbService, lmsClient, yaDeliveryTariffLoadService,
                boilingSolomonService, mdsS3LocationFactory);

        when(yaDeliveryClientMock.getTariffInfo(eq(1L), anyString(), any()))
                .thenReturn(getTariff(1));
        when(yaDeliveryClientMock.getTariffInfo(eq(2L), anyString(), any()))
                .thenReturn(getTariff(2));
        when(yaDeliveryClientMock.getTariffInfo(eq(3L), anyString(), any()))
                .thenReturn(getTariff(3));
        when(yaDeliveryClientMock.getTariffInfo(eq(4L), anyString(), any()))
                .thenReturn(getTariff(4, ImmutableList.of(DeliveryTariffProgramType.MARKET_DELIVERY)));

        CargoTypeRestrictionsDto firsRestriction = new CargoTypeRestrictionsDto(1L,
                Arrays.asList(17, 18, 19),
                Arrays.asList(new TariffLocationCargoTypeDto(11, Arrays.asList(11, 12, 13)),
                        new TariffLocationCargoTypeDto(12, Arrays.asList(14, 15, 16))));
        when(lmsClient.getCargoTypesByTariffId(eq(1L))).thenReturn(firsRestriction);

        CargoTypeRestrictionsDto secondRestriction = new CargoTypeRestrictionsDto(2L,
                Arrays.asList(27, 28, 29),
                Arrays.asList(new TariffLocationCargoTypeDto(21, Arrays.asList(21, 22, 23)),
                        new TariffLocationCargoTypeDto(22, Arrays.asList(24, 25, 26))));
        when(lmsClient.getCargoTypesByTariffId(eq(2L))).thenReturn(secondRestriction);

        CargoTypeRestrictionsDto thirdRestriction = new CargoTypeRestrictionsDto(null,
                Arrays.asList(37, 38, 39),
                Arrays.asList(new TariffLocationCargoTypeDto(31, Arrays.asList(31, 32, 33)),
                        new TariffLocationCargoTypeDto(32, Arrays.asList(34, 35, 36))));
        when(lmsClient.getCargoTypesByTariffId(eq(3L))).thenReturn(thirdRestriction);

        CargoTypeRestrictionsDto emptyRestriction = new CargoTypeRestrictionsDto(null,
                Collections.emptyList(),
                Arrays.asList(new TariffLocationCargoTypeDto(null, Collections.emptyList()),
                        new TariffLocationCargoTypeDto(null, Collections.emptyList())));

        when(lmsClient.getCargoTypesByTariffId(eq(4L))).thenReturn(emptyRestriction);
        when(lmsClient.getCargoTypesByTariffId(eq(5L))).thenReturn(emptyRestriction);

        when(mdsS3Client.getUrl(any())).thenReturn(new URL("http://test.yandex.ru/"));
    }

    /**
     * Проверить вставку тарифов в пустые таблицы. <p>
     * [ ] -> TARIFF1_INITIAL_META, TARIFF2_INITIAL_META
     * Проверить вставку cargoTypes
     */
    @Test
    @DbUnitDataSet(after = "testTariffsImportOnEmptyTable.after.csv")
    void testTariffsImportOnEmptyTables() {
        testImport(createTariffMeta(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META);
    }

    @Test
    @DbUnitDataSet(after = "testDaasTariffUploadToMds.after.csv")
    void testDaasTariffUploadToMds() throws Exception {
        testTariffUploadToMds(daasTariffImportService, "/revisions/last");
    }

    private void testTariffUploadToMds(TariffImportService tariffImportService, String metaUrl) throws IOException {
        String originalXml = TestUtils
                .extractFileContent("ru/yandex/market/deliverycalculator/indexer/service/small_tariff.xml");

        when(lmsClient.getCargoTypesByTariffId(eq(5380L)))
                .thenReturn(new CargoTypeRestrictionsDto(1L, Collections.emptyList(), Collections.emptyList()));

        when(httpClient.execute(any())).thenAnswer(invocation -> {
            HttpGet get = invocation.getArgument(0);

            String path = get.getURI().getPath();
            if (path.contains(metaUrl)) {
                return HttpClientTestUtils.mockResponse(
                        getSystemResourceAsStream("ru/yandex/market/deliverycalculator/indexer/service/metaTariff.json")
                );
            } else {
                return HttpClientTestUtils.mockResponse(IOUtils.toInputStream(originalXml, StandardCharsets.UTF_8));
            }
        });

        StringBuilder uploadedXml = new StringBuilder();
        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            uploadedXml.append(IOUtils.toString(contentProvider.getInputStream(), StandardCharsets.UTF_8));
            return null;
        }).when(mdsS3Client).upload(any(), any());

        tariffImportService.importYaDeliveryTariff();

        Assertions.assertEquals(originalXml, uploadedXml.toString());
        BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService,
                UpdatingTariffBoilingKey.of(5380L, "c62cdb1ea95d40e0072cf1e0e8ab925f", BoilingStageType.TARIFF_UPDATING_STAGE)
        );

    }

    /**
     * Проверить добавление тарифа TARIFF3. <p>
     * TARIFF1_INITIAL_META, TARIFF2_INITIAL_META -> TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF3_INITIAL_META
     */
    @Test
    void testAddTariff() {
        initialTariffs();
        testImport(createTariffMeta(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF3_INITIAL_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF3_INITIAL_META);
    }

    /**
     * Проверить обновление тарифа TARIFF2. <p>
     * TARIFF1_INITIAL_META, TARIFF2_INITIAL_META -> TARIFF1_INITIAL_META, TARIFF2_UPDATED_META
     */
    @Test
    void testUpdateTariff() {
        initialTariffs();
        testImport(createTariffMeta(TARIFF1_INITIAL_META, TARIFF2_UPDATED_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF2_UPDATED_META);
    }

    /**
     * Проверить удаление одного тарифа TARIFF1. <p>
     * TARIFF1_INITIAL_META, TARIFF2_INITIAL_META -> TARIFF2_INITIAL_META
     * Проверить удаление cargoTypes
     */
    @Test
    @DbUnitDataSet(after = "testRemoveTariff.after.csv")
    void testRemoveTariff() {
        initialTariffs();
        testImport(createTariffMeta(TARIFF2_INITIAL_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META);
    }

    /**
     * Проверить комплексное обновление: удалить  TARIFF1, обновить TARIFF2, добавить TARIFF3. <p>
     * TARIFF1_INITIAL_META, TARIFF2_INITIAL_META -> TARIFF2_UPDATED_META, TARIFF3_INITIAL_META
     * Симметричное комплексное обновление cargoTypes
     */
    @Test
    @DbUnitDataSet(after = "testAddUpdateAndRemoveTariff.after.csv")
    void testAddUpdateAndRemoveTariff() {
        when(yaDeliveryClientMock.getTariffInfo(eq(2L), anyString(), any()))
                .thenReturn(getTariff(2, ImmutableList.of(DeliveryTariffProgramType.MARKET_DELIVERY,
                        DeliveryTariffProgramType.BERU_CROSSDOCK)),
                        getTariff(2, ImmutableList.of(DeliveryTariffProgramType.MARKET_DELIVERY)));
        initialTariffs();
        testImport(createTariffMeta(TARIFF2_UPDATED_META, TARIFF3_INITIAL_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF2_UPDATED_META, TARIFF3_INITIAL_META);
    }

    /**
     * Проверить удаление всех тарифов. <p>
     * TARIFF1_INITIAL_META, TARIFF2_INITIAL_META -> [ ]
     */
    @Test
    void testRemoveAllTariffs() {
        initialTariffs();
        testImport(createTariffMeta());
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META);
    }

    @Test
    @DbUnitDataSet(after = "testImportMultipleProgramsTariff.after.csv")
    void testMultipleProgramsTariff() {
        initialTariffs();
        testImport(createTariffMeta(TARIFF4_INITIAL_META));
        assertBoilingSolomon(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META, TARIFF4_INITIAL_META);
    }

    /**
     * Для тарифа 1 добавление новых cargo types.
     * Для тарифа 2 удаление старых и добавление новых.
     * Для тарифа 3 удаление старых.
     * Для тарифа 4 совпали хеши и ничего не обновилось
     */
    @Test
    @DbUnitDataSet(before = "cargoTypesImportServiceTest.before.csv", after = "cargoTypesImportServiceTest.after.csv")
    void lmsExportJobTest() {
        prepareClient();
        doImport(createTariffMeta(TARIFF1001_INITIAL_META, TARIFF1002_INITIAL_META, TARIFF1003_INITIAL_META,
                TARIFF1004_INITIAL_META, TARIFF1005_INITIAL_META, TARIFF1006_INITIAL_META));
    }

    private void prepareClient() {
        CargoTypeRestrictionsDto firsRestriction = new CargoTypeRestrictionsDto(1L,
                Arrays.asList(17, 18, 19),
                Arrays.asList(new TariffLocationCargoTypeDto(11, Arrays.asList(11, 12, 13)),
                        new TariffLocationCargoTypeDto(12, Arrays.asList(14, 15, 16))));
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1001L))).thenReturn(firsRestriction);

        CargoTypeRestrictionsDto secondRestriction = new CargoTypeRestrictionsDto(2L,
                Arrays.asList(27, 28, 29),
                Arrays.asList(new TariffLocationCargoTypeDto(21, Arrays.asList(21, 22, 23)),
                        new TariffLocationCargoTypeDto(22, Arrays.asList(24, 25, 26))));
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1002L))).thenReturn(secondRestriction);

        CargoTypeRestrictionsDto thirdRestriction = new CargoTypeRestrictionsDto(null,
                Collections.emptyList(),
                Collections.emptyList());
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1003L))).thenReturn(thirdRestriction);

        CargoTypeRestrictionsDto fourthRestriction = new CargoTypeRestrictionsDto(4L,
                Collections.singletonList(57),
                Collections.singletonList(new TariffLocationCargoTypeDto(57, Collections.singletonList(51))));
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1004L))).thenReturn(fourthRestriction);

        CargoTypeRestrictionsDto notChangeLocation = new CargoTypeRestrictionsDto(5L,
                Collections.singletonList(57),
                Collections.singletonList(new TariffLocationCargoTypeDto(1051,
                        List.of(51, 52, 53))));
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1005L))).thenReturn(notChangeLocation);

        CargoTypeRestrictionsDto notChangeCargoType = new CargoTypeRestrictionsDto(5L,
                Collections.singletonList(57),
                Collections.singletonList(new TariffLocationCargoTypeDto(1051,
                        List.of(51, 52, 53))));
        Mockito.when(lmsClient.getCargoTypesByTariffId(eq(1006L))).thenReturn(notChangeCargoType);
    }

    /**
     * Создать тарифы эмулирующие текущее состояние базы
     */
    private void initialTariffs() {
        doImport(createTariffMeta(TARIFF1_INITIAL_META, TARIFF2_INITIAL_META));
    }

    private void doImport(TariffMeta tariffMeta) {
        when(yaDeliveryClientMock.getAllAvailableTariffs()).thenReturn(tariffMeta);
        tariffImportServiceMocked.importYaDeliveryTariff();
    }

    private void testImport(TariffMeta tariffMeta) {
        doImport(tariffMeta);
        assertMeta(tariffMeta);
        assertTariffs(tariffMeta);
    }

    private void assertMeta(TariffMeta tariffMeta) {
        tariffMeta.getData().forEach(metaData -> {
            YaDeliveryTariffMeta savedMeta = storageService.findMeta(metaData.getTariffId());
            Assertions.assertEquals(metaData.getHash(), savedMeta.getHash());
            Assertions.assertEquals(metaData.getTimestamp(), savedMeta.getTimestamp());
        });
    }

    private void assertTariffs(TariffMeta tariffMeta) {
        List<Long> tariffIds = tariffMeta.getData().stream().
                map(TariffMetaData::getTariffId).collect(Collectors.toList());
        assertThat(yaDeliveryTariffDbService.searchTariffIds(YaDeliveryTariffFilter.builder().build()),
                equalTo(tariffIds));
    }

    private void assertBoilingSolomon(final TariffMetaData... tariffs) {
        final BoilingKey[] boilingKeys = Arrays.stream(tariffs)
                .map(e -> UpdatingTariffBoilingKey.of(e.getTariffId(), e.getHash(), BoilingStageType.TARIFF_UPDATING_STAGE))
                .toArray(BoilingKey[]::new);

        BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService, boilingKeys);
    }

}
