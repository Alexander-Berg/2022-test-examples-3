package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.controller.model.FeedDeliveryOptionsMetaDTO;
import ru.yandex.market.deliverycalculator.searchengine.task.ActiveGenerationUpdaterTask;
import ru.yandex.market.deliverycalculator.searchengine.task.ImportNewGenerationsTask;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonTestUtil;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStageType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.GenerationCachingBoilingKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM;

@DbUnitDataSet(before = {
        "db/warehouses.before.csv"
})
class SearchEngineControllerFunctionalTest extends FunctionalTest {

    private final static String MDS_PATH = "https://market-mbi-test.s3.mdst.yandex.net/delivery-calculator/buckets/%s/gen-%d/%d-%s.pb.sn";
    public static final List<Long> COURIER_BUCKET_EXPECTED_IDS = Arrays.asList(322L, 321L);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImportNewGenerationsTask importNewGenerationsTask;

    @Autowired
    private ActiveGenerationUpdaterTask activeGenerationUpdaterTask;

    @Autowired
    private BoilingSolomonService boilingSolomonService;

    /**
     * 1. В базу добавляется информация о поколениях 1, 2 и 3.
     * 2. Запускается таска ImportNewGenerationsTask, которая из базы импортирует поколения 1, 2, 3 (feed_generation, mardo_post_generation,
     * mardo_pickup_generation). Информация выгружается в соответствующие кэши feedSourceCache, tariffCache (x3).
     * 3. Посылается запрос /feedDeliveryOptionsMeta с программой синей доставки, проверяющий, что информация успешно выгрузилась
     * и из tariffCache достаются BucketsUrl.
     * 4. Добавляется новое фидовое поколение 100.
     * 5. Запускается таска импорта поколений.
     * 6. Посылается новый запрос /feedDeliveryOptionsMeta, проверяющий, что возвращается новое realGenerationId.
     */
    @Test
    void testFeedDeliveryOptionsMeta() throws Exception {
        long feedGenerationId = 1;
        long mardoPostGenerationId = 2;
        long mardoPickupGenerationId = 3;
        long postTariffId = 1;
        String postRandomString = "uoykitrocp";
        long pickupTariffId = 2;
        String pickupRandomString = "avytedrasb";
        long feedId = 111;
        long shopId = 101;
        int regionId = 11474;
        List<String> programs = Collections.singletonList(DeliveryTariffProgramType.MARKET_DELIVERY.getSEProgramName());

        fillDBAndCachesForBlueDelivery(feedGenerationId, mardoPostGenerationId, postTariffId, postRandomString,
                mardoPickupGenerationId, pickupTariffId, pickupRandomString, feedId, shopId, regionId);

        sendFeedDeliveryOptionsMetaRequest(feedId, programs, mardoPostGenerationId, postTariffId,
                postRandomString, mardoPickupGenerationId, pickupTariffId, pickupRandomString, mardoPickupGenerationId, mardoPickupGenerationId);

        feedGenerationId = 100;
        String insertGeneration = getInsertGenerationsQuery(feedGenerationId);
        String insertFeedGeneration = getInsertFeedGenerationQuery(feedGenerationId, feedId, shopId, regionId);
        jdbcTemplate.update(insertGeneration);
        jdbcTemplate.update(insertFeedGeneration);

        activeGenerationUpdaterTask.run();
        importNewGenerationsTask.run();

        sendFeedDeliveryOptionsMetaRequest(feedId, programs, mardoPostGenerationId, postTariffId,
                postRandomString, mardoPickupGenerationId, pickupTariffId, pickupRandomString, feedGenerationId, feedGenerationId);

        final GenerationCachingBoilingKey key1 = GenerationCachingBoilingKey.of(1L, 2L, 2L, DeliveryTariffProgramType.MARKET_DELIVERY, BoilingStageType.GENERATION_CACHING_STAGE);
        final GenerationCachingBoilingKey key2 = GenerationCachingBoilingKey.of(2L, 3L, 3L, DeliveryTariffProgramType.MARKET_DELIVERY, BoilingStageType.GENERATION_CACHING_STAGE);
        BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService, key1, key2);
    }

    /**
     * Тест проверяет корректность отданных по запросу /feedOffers бакетов для синей достаки.
     */
    @Test
    void testFeedOffersForBlueDelivery() throws IOException {
        long feedId = 123;

        createBlueGenerations(feedId);

        long maxFenerationId = getMaxGenerationId();
        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 13);
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), Collections.singletonList(189318L), COURIER_BUCKET_EXPECTED_IDS);

        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 18);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, Collections.singletonList(188909L), Collections.singletonList(308L));

        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 147, 7);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(233L), Collections.singletonList(189333L), Collections.singletonList(223L));

        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 147, 30);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(233L), null, null);

        // проверяется отсечение по физическому весу
        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 30);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, null, null);

        // проверяется отсечение по габаритам
        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 15, 30, 80, 45);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, Collections.singletonList(189318L), null);

        // проверяется отсечение по сумме габаритов
        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 15, 59, 39, 100);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, Collections.singletonList(189318L), null);

        // проверяется включение верхних границ ограничений
        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 15, 60, 120, 70);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, Collections.singletonList(189318L), null);
    }

    /**
     * Тест проверяет корректность отданных по запросу /feedOffers бакетов для синей достаки (батч).
     */
    @Test
    void testFeedOffersForBlueDeliveryBatch() throws IOException {
        long feedId = 123;

        createBlueGenerations(feedId);

        long maxFenerationId = getMaxGenerationId();
        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(maxFenerationId, feedId, 145, List.of(
                createOffer("15807", MARKET_DELIVERY_PROGRAM, 8000, 13, 0, 0, 0, null),
                createOffer("15808", MARKET_DELIVERY_PROGRAM, 8000, 18, 0, 0, 0, null),
                createOffer("15809", MARKET_DELIVERY_PROGRAM, 8000, 30, 0, 0, 0, null),
                createOffer("15810", MARKET_DELIVERY_PROGRAM, 8000, 15, 30, 80, 45, null),
                createOffer("15811", MARKET_DELIVERY_PROGRAM, 8000, 15, 59, 39, 100, null),
                createOffer("15812", MARKET_DELIVERY_PROGRAM, 8000, 15, 60, 120, 70, null)
        ));
        byte[] responseEntity = sendFeedOffersRequest(request);
        DeliveryCalcProtos.FeedOffersResp response = readPbSn(responseEntity);

        assertThat(response.getOffersCount(), is(6));
        checkFeedOffersResponse(response.getOffers(0), List.of(133L), List.of(189318L), COURIER_BUCKET_EXPECTED_IDS);
        checkFeedOffersResponse(response.getOffers(1), null, List.of(188909L), List.of(308L));
        checkFeedOffersResponse(response.getOffers(2), null, null, null);
        checkFeedOffersResponse(response.getOffers(3), null, List.of(189318L), null);
        checkFeedOffersResponse(response.getOffers(4), null, List.of(189318L), null);
        checkFeedOffersResponse(response.getOffers(5), null, List.of(189318L), null);
    }

    /**
     * Тест что один и тот же тариф используется и для программы MARKET_DELIVERY и для программы BERU_CROSSDOCK.
     */
    @Test
    void testFeedOffersForBlueDeliveryMultiplePrograms() throws IOException {
        long feedId = 123;

        createBlueGenerations(feedId);

        long maxFenerationId = getMaxGenerationId();
        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 13);
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), Collections.singletonList(189318L),
                COURIER_BUCKET_EXPECTED_IDS);

        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                DeliveryCalcProtos.ProgramType.BERU_CROSSDOCK, 8000, 145, 13);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), Collections.singletonList(189318L),
                COURIER_BUCKET_EXPECTED_IDS);

        request = createFeedOffersReq(maxFenerationId, feedId, "15807",
                DeliveryCalcProtos.ProgramType.FF_HEAVY_PROGRAM, 8000, 145, 13);
        responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, null, null);
    }

    /**
     * Тест проверяет корректность отданных по запросу /feedOffers бакетов для синей достаки c карго типами для pickup.
     */
    @Test
    void testFeedOffersCargoTypesPickup() throws IOException {
        long feedId = 123;
        createBlueGenerations(feedId);

        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(getMaxGenerationId(), feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 1, 1, 1, 1, ImmutableSet.of(1, 2));
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), null, COURIER_BUCKET_EXPECTED_IDS);
    }

    /**
     * Тест проверяет корректность отданных по запросу /feedOffers бакетов для синей достаки c карго типами для post.
     */
    @Test
    void testFeedOffersCargoTypesPost() throws IOException {
        long feedId = 123;
        createBlueGenerations(feedId);

        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(getMaxGenerationId(), feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 1, 1, 1, 1, ImmutableSet.of(5));
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), Collections.singletonList(189317L), null);
    }

    /**
     * Тест проверяет корректность отданных по запросу /feedOffers бакетов для синей достаки c карго типами для post.
     */
    @Test
    void testFeedOffersCargoTypesCourier() throws IOException {
        long feedId = 123;
        createBlueGenerations(feedId);

        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(getMaxGenerationId(), feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 1, 1, 1, 1, ImmutableSet.of(3));
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, null, Collections.singletonList(189317L), COURIER_BUCKET_EXPECTED_IDS);
    }

    @Test
    void testLocationCargoTypes() throws IOException {
        long feedId = 123;
        createBlueGenerations(feedId);

        DeliveryCalcProtos.FeedOffersReq request = createFeedOffersReq(getMaxGenerationId(), feedId, "15807",
                MARKET_DELIVERY_PROGRAM, 8000, 145, 1, 1, 1, 1, ImmutableSet.of(11));
        byte[] responseEntity = sendFeedOffersRequest(request);
        checkFeedOffersResponse(responseEntity, Collections.singletonList(133L), null, Collections.singletonList(321L));
    }


    private Long getMaxGenerationId() {
        return jdbcTemplate.queryForObject("select max(id) from public.generations", Long.class);
    }

    private void createBlueGenerations(long feedId) {
        long feedGenerationId = 1;
        long mardoPickupGenerationId = 2;
        long mardoPostGenerationId = 3;
        long mardoCourierGenerationId = 4;

        String insertGeneration = getInsertGenerationsQuery(feedGenerationId, mardoPickupGenerationId, mardoPostGenerationId, mardoCourierGenerationId);
        String insertFeedGeneration = getInsertFeedGenerationQuery(feedGenerationId, feedId, 1357, 90);
        String insertMardoPickupGeneration = getInsertMardoPickupGenerationQuery(mardoPickupGenerationId, 1);
        String insertMardoPostGeneration = getInsertMardoPostGenerationQuery(mardoPostGenerationId, 2);
        String insertMardoCourierGeneration = getInsertMardoCourierGenerationQuery(mardoCourierGenerationId, 3);

        jdbcTemplate.update(insertGeneration);
        jdbcTemplate.update(insertFeedGeneration);
        jdbcTemplate.update(insertMardoPickupGeneration);
        jdbcTemplate.update(insertMardoPostGeneration);
        jdbcTemplate.update(insertMardoCourierGeneration);

        activeGenerationUpdaterTask.run();
        importNewGenerationsTask.run();
    }

    private void fillDBAndCachesForBlueDelivery(long feedGenerationId, long mardoPostGenerationId, long postTariffId, String postRandomString,
                                                long mardoPickupGenerationId, long pickupTariffId, String pickupRandomString,
                                                long feedId, long shopId, int regionId) {
        String insertGenerations = getInsertGenerationsQuery(feedGenerationId, mardoPostGenerationId, mardoPickupGenerationId);
        String insertFeedGeneration = getInsertFeedGenerationQuery(feedGenerationId, feedId, shopId, regionId);
        String insertMardoPostGeneration = String.format("insert into public.mardo_post_generations (generation_id, tariff_id, deleted, buckets_url, tariff_info) " +
                "values (%d, %d, false, '" + MDS_PATH + "', " +
                "'<tariff min-weight=\"-1.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"180.0\" volume-weight-coefficient=\"0.0\">\n" +
                "   <rule location-from-id=\"%d\" min-weight=\"0.0\" max-weight=\"1.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"44193\"/>\n" +
                "   </rule>\n" +
                "</tariff>')", mardoPostGenerationId, postTariffId, "mardo-post", mardoPostGenerationId, postTariffId, postRandomString, regionId);
        String insertMardoPickupGeneration = String.format("insert into public.mardo_pickup_generations (generation_id, tariff_id, deleted, buckets_url, tariff_info) " +
                "values (%d, %d, false, '" + MDS_PATH + "', " +
                "'<tariff min-weight=\"-1.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"180.0\" volume-weight-coefficient=\"5.6E-5\">\n" +
                "   <rule location-from-id=\"%d\" min-weight=\"-1.0\" max-weight=\"5.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"555\"/>\n" +
                "   </rule>\n" +
                "</tariff>')", mardoPickupGenerationId, pickupTariffId, "mardo-pickup", mardoPickupGenerationId, pickupTariffId, pickupRandomString, regionId);
        String insertShop = String.format("insert into public.shops (id) values (%d)", shopId);
        jdbcTemplate.update(insertGenerations);
        jdbcTemplate.update(insertFeedGeneration);
        jdbcTemplate.update(insertMardoPostGeneration);
        jdbcTemplate.update(insertMardoPickupGeneration);
        jdbcTemplate.update(insertShop);

        // заполняются кэши feedSourceCache, tariffCache и TariffWorkflow
        activeGenerationUpdaterTask.run();
        importNewGenerationsTask.run();
    }

    private String getInsertGenerationsQuery(long... ids) {
        StringJoiner joiner = new StringJoiner(", ", "insert into public.generations (id, external_generation_id, time) values ", "");
        for (long id : ids) {
            joiner.add(String.format("(%d, %d, '2018-09-05')", id, id));
        }
        return joiner.toString();
    }

    private String getInsertFeedGenerationQuery(long feedGenerationId, long feedId, long shopId, int regionId) {
        return String.format("insert into public.feed_generations (generation_id, feed_id, deleted, source_info) " +
                "values (%d, %d, false, '<feed-source type=\"SHOP\" id=\"%d\" region-id=\"%d\"/>')",
                feedGenerationId, feedId, shopId, regionId);
    }

    private String getInsertMardoPickupGenerationQuery(long mardoPickupGenerationId, long tariffId) {
        return String.format("insert into public.mardo_pickup_generations (generation_id, tariff_id, deleted, tariff_info) " +
                "values (%d, %d, false, " +
                "'<tariff min-weight=\"-1.0\" max-weight=\"28.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"65.0 70.0 120.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"300.0\" volume-weight-coefficient=\"0.0\">\n" +
                "   <cargo-type-blacklist>1</cargo-type-blacklist>\n" +
                "   <cargo-type-blacklist>2</cargo-type-blacklist>\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\" />\n" +
                "      <program name-key=\"BERU_CROSSDOCK\" />\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-weight=\"-1.0\" max-weight=\"1.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 70.0 120.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"250.0\"> \n" +
                "      <bucket id=\"189317\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"1.0\" max-weight=\"15.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 70.0 120.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"250.0\"> \n" +
                "      <bucket id=\"189318\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"15.0\" max-weight=\"31.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 70.0 120.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"250.0\">\n" +
                "      <bucket id=\"188909\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"39\" min-weight=\"-1.0\" max-weight=\"10.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 70.0 120.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"250.0\">\n" +
                "      <bucket id=\"189333\"/>\n" +
                "   </rule>\n" +
                "</tariff>')", mardoPickupGenerationId, tariffId);
    }

    private String getInsertMardoPostGenerationQuery(long mardoPostGenerationId, long tariffId) {
        return String.format("insert into public.mardo_post_generations (generation_id, tariff_id, deleted, tariff_info) " +
                "values (%d, %d, false, " +
                "'<tariff min-weight=\"-1.0\" max-weight=\"40.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"40.0 60.0 160.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"200.0\" volume-weight-coefficient=\"0.0\">\n" +
                "   <cargo-type-blacklist>3</cargo-type-blacklist>\n" +
                "   <cargo-type-blacklist>4</cargo-type-blacklist>\n" +
                "   <programs>\n" +
                "      <program name-key=\"MARKET_DELIVERY\" />\n" +
                "      <program name-key=\"BERU_CROSSDOCK\" />\n" +
                "   </programs>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"-1.0\" max-weight=\"15.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"160.0 160.0 160.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"133\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"213\" min-weight=\"15.0\" max-weight=\"17.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"133\"/>\n" +
                "   </rule>\n" +
                "   <rule location-from-id=\"39\" min-weight=\"-1.0\" max-weight=\"50.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"120.0\">\n" +
                "      <bucket id=\"233\"/>\n" +
                "   </rule>\n" +
                "</tariff>')", mardoPostGenerationId, tariffId);
    }

    private String getInsertMardoCourierGenerationQuery(long mardoCourierGenerationId, long tariffId) {
        // language=xml
        String tariffXml = ""
                + "<tariff min-weight=\"-1.0\" max-weight=\"30.0\" min-dimension=\"-1.0 -1.0 -1.0\" max-dimension=\"60.0 60.0 60.0\" min-dim-sum=\"-1.0\" max-dim-sum=\"180.0\" volume-weight-coefficient=\"2.0E-4\" shop-volume-weight-coefficient=\"2.0E-4\">\n"
                + "   <cargo-type-blacklist>5</cargo-type-blacklist>\n"
                + "   <cargo-type-blacklist>6</cargo-type-blacklist>\n"
                + "   <programs>\n"
                + "      <program name-key=\"MARKET_DELIVERY\" />\n"
                + "      <program name-key=\"BERU_CROSSDOCK\" />\n"
                + "   </programs>\n"
                + "   <rule location-from-id=\"213\" min-customer-weight=\"-1.0\" max-customer-weight=\"15.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"19.0\">\n"
                + "      <bucket id=\"321\"/>\n"
                + "   </rule>\n"
                + "   <rule location-from-id=\"213\" location-cargo-type-blacklist=\"[11]\" min-customer-weight=\"-1.0\" max-customer-weight=\"15.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"19.0\">\n"
                + "      <bucket id=\"322\"/>\n"
                + "   </rule>\n"
                + "   <rule location-from-id=\"39\" min-customer-weight=\"-1.0\" max-customer-weight=\"15.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"19.0\">\n"
                + "      <bucket id=\"223\"/>\n"
                + "   </rule>\n"
                + "   <rule location-from-id=\"213\" min-customer-weight=\"15.0\" max-customer-weight=\"19.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"19.0\">\n"
                + "      <bucket id=\"308\"/>\n"
                + "   </rule>\n"
                + "   <rule location-from-id=\"39\" min-customer-weight=\"15.0\" max-customer-weight=\"19.0\" min-shop-weight=\"-1.0\" max-shop-weight=\"19.0\">\n"
                + "      <bucket id=\"223\"/>\n"
                + "   </rule>\n"
                + "</tariff>";
        // language=sql
        String insertTariffSql = ""
                + " insert into public.mardo_courier_generations"
                + " (generation_id, customer_tariff_id, shop_tariff_id, deleted, tariff_info)"
                + " values"
                + " (%d, %d, %d, false, '%s')";
        return String.format(insertTariffSql, mardoCourierGenerationId, tariffId, tariffId, tariffXml);
    }

    private DeliveryCalcProtos.FeedOffersReq createFeedOffersReq(long genId, long feedId, String offerId,
                                                                 DeliveryCalcProtos.ProgramType programType,
                                                                 int priceInRur, Integer warehouseId, double weight) {
        return createFeedOffersReq(genId, feedId, offerId, programType, priceInRur, warehouseId, weight, 0, 0, 0);
    }

    private DeliveryCalcProtos.FeedOffersReq createFeedOffersReq(long genId, long feedId, String offerId,
                                                                 DeliveryCalcProtos.ProgramType programType,
                                                                 int priceInRur, Integer warehouseId, double weight,
                                                                 double width, double height, double length) {
        return createFeedOffersReq(genId, feedId, offerId, programType, priceInRur, warehouseId, weight, width, height, length, null);
    }

    private DeliveryCalcProtos.FeedOffersReq createFeedOffersReq(long genId, long feedId, String offerId,
                                                                 DeliveryCalcProtos.ProgramType programType,
                                                                 int priceInRur, Integer warehouseId, double weight,
                                                                 double width, double height, double length,
                                                                 Set<Integer> cargoTypes) {
        return createFeedOffersReq(genId, feedId, warehouseId,
                List.of(createOffer(offerId, programType, priceInRur, weight, width, height, length, cargoTypes)));
    }

    private DeliveryCalcProtos.FeedOffersReq createFeedOffersReq(long genId, long feedId, Integer warehouseId,
                                                                 List<DeliveryCalcProtos.Offer> offers) {
        DeliveryCalcProtos.FeedOffersReq.Builder request = DeliveryCalcProtos.FeedOffersReq.newBuilder();
        request.setGenerationId(genId);
        request.setFeedId(feedId);
        request.setRequestId("testReq");
        if (warehouseId != null) {
            request.setWarehouseId(warehouseId);
        }
        request.addAllOffers(offers);
        return request.build();
    }

    private DeliveryCalcProtos.Offer createOffer(String offerId, DeliveryCalcProtos.ProgramType programType,
                                                         int priceInRur, double weight,
                                                         double width, double height, double length,
                                                         Set<Integer> cargoTypes) {
        DeliveryCalcProtos.Offer.Builder offer = DeliveryCalcProtos.Offer.newBuilder();
        offer.setOfferId(offerId);
        offer.addProgramType(programType);
        offer.addPriceMap(DeliveryCalcProtos.OfferPrice.newBuilder().setCurrency("RUR").setValue(priceInRur));
        offer.setWeight(weight);
        if (width != 0) {
            offer.setWidth(width);
        }
        if (height != 0) {
            offer.setHeight(height);
        }
        if (length != 0) {
            offer.setLength(length);
        }
        if (cargoTypes != null) {
            cargoTypes.forEach(offer::addCargoTypes);
        }
        return offer.build();
    }

    private void checkFeedOffersResponse(byte[] responseEntity, List<Long> postBucketExpectedIds,
                                         List<Long> pickupBucketExpectedIds, List<Long> courierBucketExpectedIds) throws IOException {
        DeliveryCalcProtos.FeedOffersResp response = readPbSn(responseEntity);
        assertEquals(1, response.getOffersCount());
        checkFeedOffersResponse(response.getOffers(0), postBucketExpectedIds, pickupBucketExpectedIds,
                courierBucketExpectedIds);
    }

    private void checkFeedOffersResponse(DeliveryCalcProtos.OffersDelivery offersDelivery,
                                         List<Long> postBucketExpectedIds,
                                         List<Long> pickupBucketExpectedIds, List<Long> courierBucketExpectedIds) {
        DeliveryCalcProtos.OffersDelivery.Builder expected = DeliveryCalcProtos.OffersDelivery.newBuilder();
        if (CollectionUtils.isNotEmpty(postBucketExpectedIds)) {
            postBucketExpectedIds.forEach(expected::addPostBucketIds);
        }
        if (CollectionUtils.isNotEmpty(pickupBucketExpectedIds)) {
            pickupBucketExpectedIds.forEach(expected::addPickupBucketIds);
        }
        if (CollectionUtils.isNotEmpty(courierBucketExpectedIds)) {
            courierBucketExpectedIds.forEach(expected::addDeliveryOptBucketIds);
        }
        assertEquals(expected.build(), offersDelivery);
    }

    private void sendFeedDeliveryOptionsMetaRequest(long feedId, List<String> programs,
                                                    long mardoPostGenerationId, long postTariffId, String postRandomString,
                                                    long mardoPickupGenerationId, long pickupTariffId, String pickupRandomString,
                                                    long maxGenerationId, long realGenerationId) throws Exception {
        String url = String.format("/feedDeliveryOptionsMeta?feed-id=%d&program=%s", feedId, String.join(",", programs));
        String json = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        FeedDeliveryOptionsMetaDTO meta = new ObjectMapper().readValue(json, FeedDeliveryOptionsMetaDTO.class);

        assertEquals(maxGenerationId, meta.getGenerationId());
        assertEquals(realGenerationId, meta.getRealGenerationId());
        assertThat(meta.getBucketUrls(), Matchers.containsInAnyOrder(
                String.format(MDS_PATH, "mardo-post", mardoPostGenerationId, postTariffId, postRandomString),
                String.format(MDS_PATH, "mardo-pickup", mardoPickupGenerationId, pickupTariffId, pickupRandomString)
        ));
    }

    private byte[] sendFeedOffersRequest(DeliveryCalcProtos.FeedOffersReq request) {
        try {
            byte[] body = createPbSn(request);

            return mockMvc.perform(post("/feedOffers").content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
        } catch (final Exception ex) {
            throw new RuntimeException("Could not send data", ex);
        }
    }

    private DeliveryCalcProtos.FeedOffersResp readPbSn(byte[] body) throws IOException {
        return PbSnUtils.readPbSnMessage("DCOA", DeliveryCalcProtos.FeedOffersResp.parser(),
                new ByteArrayInputStream(body));
    }

    private byte[] createPbSn(DeliveryCalcProtos.FeedOffersReq request) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PbSnUtils.writePbSnMessage("DCOR", request, outputStream);
        return outputStream.toByteArray();
    }
}
