package ru.yandex.market.deliverycalculator.indexer.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.job.ExportShopGenerationJob;
import ru.yandex.market.deliverycalculator.indexer.job.ExportYadoTariffGenerationJob;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;
import ru.yandex.market.deliverycalculator.storage.model.MarketDeliveryTariff;
import ru.yandex.market.deliverycalculator.storage.model.PartnerPlacementProgramType;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.FeedGeneration;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.RegularCourierGeneration;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.storage.util.PooledIdGenerator;
import ru.yandex.market.deliverycalculator.storage.util.StorageUtils;
import ru.yandex.market.deliverycalculator.workflow.test.WorkflowTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link RulesImportController}
 */
class RulesImportControllerTest extends FunctionalTest {
    private static final long SHOP_ID = 888L;
    private static final String SHOP_CAMPAIGN_TYPE = "SHOP";

    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private DeliveryCalculatorMetaStorageService metaStorageService;

    @Autowired
    private ExportYadoTariffGenerationJob exportWhiteTariffGenerationJob;

    @Autowired
    private ExportYadoTariffGenerationJob exportRegularTariffGenerationJob;

    @Autowired
    private ExportShopGenerationJob exportShopGenerationJob;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    private PooledIdGenerator mockedModifierIdGenerator;

    @BeforeEach
    void setUpConfiguration() {
        mockModifierIdGenerator();
    }

    @Test
    @DbUnitDataSet(before = "data/db/deleteShopRules.before.csv", after = "data/db/deleteShopRules.after.csv")
    void testForShopsDelete() {
        ResponseEntity<String> response = makeRequest(baseUrl + "/deleteShopRules", HttpMethod.POST,
                getBody("data/rest/deleteShopRules.xml"));
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void addNewTest() {
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNull(deliveryShop);
        checkCorrectnessAfterRequest();
    }

    @Test
    void updateExistingTest() {
        insertTestShop(SHOP_CAMPAIGN_TYPE);
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNotNull(deliveryShop);
        checkCorrectnessAfterRequest();
    }

    @Test
    @DbUnitDataSet(after = "RulesImportControllerTest.udpateShopDbsUCat.after.csv")
    void udpateShopDbsUCat() {
        sendRequestAndCheckResponse(getBody("data/rest/importShopDataDbsUcat.xml"));
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNotNull(deliveryShop);
        MatcherAssert.assertThat(deliveryShop, Matchers.hasProperty("unitedCatalog", Matchers.is(true)));
        MatcherAssert.assertThat(deliveryShop,
                Matchers.hasProperty("placementPrograms",
                        Matchers.equalTo(Set.of(PartnerPlacementProgramType.DROPSHIP_BY_SELLER))));
    }

    @Test
    @DbUnitDataSet(after = "RulesImportControllerTest.importWithNullPrice.after.csv")
    void testUpdateShopRuleWithNullableDeliveryCost() {
        sendRequestAndCheckResponse(getBody("data/rest/importRegularTariffWithNullCost.xml"));
    }

    @Test
    @DisplayName("Проверка обновления типа кампании у существующего магазина")
    void updateCampaignTypeForExistingShopTest() {
        insertTestShop(null);
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNotNull(deliveryShop);
        checkCorrectnessAfterRequest();
    }

    @Test
    void addNewSimpleTest() {
        // Проверяем что информации по магазину нет
        DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNull(deliveryShop);

        // Отправляем информацию по магазину
        final int newRegionId = 65;
        sendRequestAndCheckResponse(getBody("data/rest/importOnlyShopData.xml"));

        // Проверяем что информация по магазину успешно сохранена
        deliveryShop = storageService.getShop(SHOP_ID);
        Assertions.assertNotNull(deliveryShop);
        Assertions.assertTrue(deliveryShop.isUseYmlDelivery());
        Assertions.assertEquals(newRegionId, deliveryShop.getRegionId());
        Assertions.assertEquals(SHOP_CAMPAIGN_TYPE, deliveryShop.getCampaignType());
        Assertions.assertNull(deliveryShop.getCourierCurrency());
        Assertions.assertTrue(deliveryShop.getMarketTariffs().isEmpty());
    }

    /**
     * Проверяется корректность приготовленных джобой exportShopGenerationJob поколений.
     */
    @Test
    void checkExportTest() throws MalformedURLException {
        Mockito.when(mdsS3Client.getUrl(Mockito.any())).thenReturn(new URL("http://some.url.here"));

        sendRequestAndCheckResponse(getBody("data/rest/importRegularTariff.xml"));

        DeliveryShop currentShop = storageService.getShop(SHOP_ID);
        Assertions.assertEquals("RUR", currentShop.getCourierCurrency());
        Assertions.assertNull(currentShop.getCourierGenerationId());

        exportShopGenerationJob.doJob(null);
        exportRegularTariffGenerationJob.doJob(null);
        currentShop = storageService.getShop(SHOP_ID);
        Assertions.assertNotNull(currentShop.getCourierGenerationId());
        List<Generation> allCreatedGenerations = getAllCreatedGenerations();
        Assertions.assertEquals(2, allCreatedGenerations.size());
        Assertions.assertEquals(1, allCreatedGenerations.get(0).getFeedGenerations().size());
        Assertions.assertEquals(1, allCreatedGenerations.get(1).getRegularCourierGenerations().size());
        FeedGeneration feedGeneration = allCreatedGenerations.get(0).getFeedGenerations().iterator().next();
        checkFeedGeneration(feedGeneration, 1, null, false);
        RegularCourierGeneration regularCourierGeneration = allCreatedGenerations.get(1).getRegularCourierGenerations().iterator().next();
        checkRegularCourierGeneration(regularCourierGeneration, 2);
    }

    /**
     * Проверяется корректность приготовленных джобой exportShopGenerationJob
     * FeedSource для авторасчета Белого Маркета.
     */
    @Test
    @DbUnitDataSet(before = "data/db/regionsData.csv",
            after = "data/db/importWhiteShop.after.csv")
    void checkWhiteExportTest() throws MalformedURLException {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "tariff_1110.xml", getClass());
        Mockito.when(mdsS3Client.getUrl(Mockito.any())).thenReturn(new URL("http://some.url.here"));
        YaDeliveryTariffUpdatedInfo yaDeliveryTariff = WorkflowTestUtils.createMardoWhiteCourierTariff(1110, 101, 0.0);
        yaDeliveryTariffDbService.save(yaDeliveryTariff);

        sendRequestAndCheckResponse(getBody("data/rest/importWhiteShop.xml"));

        exportShopGenerationJob.doJob(null);
        exportRegularTariffGenerationJob.doJob(null);
        exportWhiteTariffGenerationJob.doJob(null);
    }

    /**
     * Проверяем, что настройки ЯДо нормально прорастают.
     */
    @Test
    void checkYaDeliverySettingsExportTest() throws MalformedURLException {
        Mockito.when(mdsS3Client.getUrl(Mockito.any())).thenReturn(new URL("http://some.url.here"));
        Long senderId = 12131415L;
        sendRequestAndCheckResponse(getBody("data/rest/importShopUsingYaDoSettings.xml"));

        DeliveryShop currentShop = storageService.getShop(SHOP_ID);
        Assertions.assertTrue(currentShop.isUseYaDeliverySettings());
        Assertions.assertEquals(senderId, currentShop.getSenderId());

        exportShopGenerationJob.doJob(null);

        List<Generation> allCreatedGenerations = getAllCreatedGenerations();
        FeedGeneration feedGeneration = allCreatedGenerations.get(0).getFeedGenerations().iterator().next();
        checkFeedGeneration(feedGeneration, 1, senderId, true);
    }

    @Test
    @DbUnitDataSet(before = "data/db/regionsData.csv",
            after = "data/db/importShopSeveralAutoCalculatedCarriers.after.csv")
    void testDifferentAutoCalculatedCarriersForDifferentRegions() throws MalformedURLException {
        Mockito.when(mdsS3Client.getUrl(Mockito.any())).thenReturn(new URL("http://some.url.here"));

        sendRequestAndCheckResponse(getBody("data/rest/importShopWithSeveralAutoCalculatedCarriers.xml"));

        exportShopGenerationJob.doJob(null);
        exportRegularTariffGenerationJob.doJob(null);
    }

    @Test
    @DbUnitDataSet(after = "data/db/importShopWithNoAutoCalculatedCarrier.after.csv")
    void testNoAutoCalculatedCarriers() throws MalformedURLException {
        Mockito.when(mdsS3Client.getUrl(Mockito.any())).thenReturn(new URL("http://some.url.here"));

        sendRequestAndCheckResponse(getBody("data/rest/importShopWithNoAutoCalculatedCarrier.xml"));
        exportShopGenerationJob.doJob(null);
        exportWhiteTariffGenerationJob.doJob(null);
        exportRegularTariffGenerationJob.doJob(null);
    }

    @Test
    void testInvalidRequest() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendUpdateRequest(getBody("data/rest/importShopInvalidRequest.xml"))
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        // language=json
        String expectedResponse = "{\"message\":\"Following validation errors occurred:\\nField: 'shops[0]" +
                ".deliveryModifiers[0].action', message: 'At least one parameter should be set for modifier's " +
                "action'\\nField: 'shops[0].deliveryModifiers[1].condition', message: 'At least one condition should " +
                "be set'\"}";
        assertEquals(expectedResponse, exception.getResponseBodyAsString());
    }

    private void checkFeedGeneration(FeedGeneration generation, long expectedId,
                                     Long senderId, boolean useYaDeliverySettings
    ) {
        Assertions.assertEquals(expectedId, generation.getGeneration().getId());
        Assertions.assertEquals(76632, generation.getFeedId());
        Assertions.assertFalse(generation.isDeleted());
        String expectedSourceInfo =
                "<feed-source " +
                        "type=\"SHOP\" " +
                        "id=\"888\"" +
                        (senderId == null ? "" : " sender-id=\"" + senderId + "\"") +
                        " region-id=\"213\" " +
                        "use-yadelivery-settings=\"" + useYaDeliverySettings + "\" " +
                        "use-yml-delivery=\"false\"" +
                        "/>";
        Assertions.assertEquals(expectedSourceInfo, generation.getSourceInfo());
    }

    private void checkRegularCourierGeneration(RegularCourierGeneration generation, long expectedId) {
        Assertions.assertEquals(expectedId, generation.getGeneration().getId());
        Assertions.assertEquals(SHOP_ID, generation.getShopId());
        Assertions.assertFalse(generation.isDeleted());
        String expectedTariffInfo =
                "<tariff campaign-type=\"SHOP\">\n" +
                        "   <matrix strategy=\"OLD_TARIFF\" currency=\"RUR\">\n" +
                        "      <weight-borders>1.0 50.0</weight-borders>\n" +
                        "      <price-borders>500.0</price-borders>\n" +
                        "      <default-category-index>0</default-category-index>\n" +
                        "      <bucket-ids>-1 -1 1 -1 -1 -1</bucket-ids>\n" +
                        "   </matrix>\n" +
                        "</tariff>";
        Assertions.assertEquals(expectedTariffInfo, generation.getTariffInfo());
    }

    private void checkCorrectnessAfterRequest() {
        sendRequestAndCheckResponse(getBody("data/rest/importShopData.xml"));

        transactionTemplate.execute(status -> {
            final DeliveryShop deliveryShop = storageService.getShop(SHOP_ID);
            Assertions.assertNotNull(deliveryShop);
            Assertions.assertTrue(deliveryShop.isUseYmlDelivery());
            Assertions.assertEquals(213, deliveryShop.getRegionId());
            Assertions.assertEquals("BYN", deliveryShop.getCourierCurrency());
            Assertions.assertEquals(SHOP_CAMPAIGN_TYPE, deliveryShop.getCampaignType());

            final Set<MarketDeliveryTariff> marketTariffs = deliveryShop.getMarketTariffs();
            MatcherAssert.assertThat(marketTariffs, Matchers.hasSize(1));

            return null;
        });
    }

    private void sendRequestAndCheckResponse(String body) {
        ResponseEntity<String> response = sendUpdateRequest(body);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @NotNull
    private ResponseEntity<String> sendUpdateRequest(String body) {
        return makeRequest(baseUrl + "/updateShopRules", HttpMethod.POST, body);
    }

    private void insertTestShop(@Nullable String campaignType) {
        Generation generation = new Generation();
        generation.setId(131313L);
        metaStorageService.addGeneration(generation);
        DeliveryShop deliveryShop = new DeliveryShop();
        deliveryShop.setId(SHOP_ID);
        deliveryShop.setCampaignType(campaignType);
        deliveryShop.setUseYmlDelivery(false);
        deliveryShop.setRegionId(213);
        deliveryShop.setCourierCurrency("KZT");
        storageService.insertShop(deliveryShop);
    }

    private ResponseEntity<String> makeRequest(String url, HttpMethod method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        return REST_TEMPLATE.exchange(url, method, request, String.class);
    }

    private List<Generation> getAllCreatedGenerations() {
        return StorageUtils.doInEntityManager(transactionTemplate, entityManager -> {
            String hql = "select gen from Generation gen order by gen.id";
            return entityManager.createQuery(hql, Generation.class).getResultList();
        });
    }

    private String getBody(String filePath) {
        return StringTestUtil.getString(this.getClass(), filePath);
    }

    private void mockModifierIdGenerator() {
        when(mockedModifierIdGenerator.generate()).thenAnswer(new Answer<>() {
            private long count = 0;

            @Override
            public Object answer(InvocationOnMock invocation) {
                return count++;
            }
        });
    }
}
