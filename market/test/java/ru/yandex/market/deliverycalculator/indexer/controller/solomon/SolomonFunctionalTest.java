package ru.yandex.market.deliverycalculator.indexer.controller.solomon;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.service.solomon.SensorFunctionTask;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.SolomonController;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStageType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingState;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStateType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BucketCountingBoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.PreparingTariffBoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.TariffBoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.UpdatingTariffBoilingKey;
import ru.yandex.solomon.sensors.SensorKind;
import ru.yandex.solomon.sensors.SensorsConsumer;
import ru.yandex.solomon.sensors.encode.SensorsDecoder;
import ru.yandex.solomon.sensors.encode.SensorsFormat;
import ru.yandex.solomon.sensors.encode.json.SensorsJsonDecoder;
import ru.yandex.solomon.sensors.encode.json.SensorsJsonEncoder;
import ru.yandex.solomon.sensors.encode.spack.SensorsSpackDecoder;
import ru.yandex.solomon.sensors.encode.spack.SensorsSpackEncoder;
import ru.yandex.solomon.sensors.encode.spack.format.CompressionAlg;
import ru.yandex.solomon.sensors.encode.spack.format.TimePrecision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для соломона.
 * {@link SolomonController}.
 *
 * @author yakun
 */
class SolomonFunctionalTest extends FunctionalTest {

    private final static RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    @Autowired
    private BoilingSolomonService boilingSolomonService;

    @Autowired
    private SensorFunctionTask sensorFunctionTask;

    /**
     * Отправка данных о шаге подготовки тарифа в Соломон.
     * <p>
     * Тариф 1001 - сварен, не отслеживается. Не должен быть в Соломоне.
     * Тариф 1002 - обновлен (generation_id = null), но варка еще не началась. Должен быть в Соломоне.
     * Тариф 1003 - обновлен, врка началась по приборам. Должен быть в Соломоне. Только DAAS.
     * Тариф 1004 - сварен, варится по приборам. Не должен быть в Соломоне. Должен быть исключен из отслеживания.
     * Тариф 1005 - сварен, варка закончилась по приборам. Должен быть в Соломоне. Должен получить статус SENT. После этого должен перестать отслеживаться.
     * Тариф 1006 - отслеживается, но удален из базы. Не должен быть в Соломоне. Должен быть исключен из отслеживания.
     */
    @Test
    @DisplayName("Проверка работы метрик варки")
    @DbUnitDataSet(before = "data/db/indexerBoilingPreparingSolomonTest.before.csv")
    void indexerBoilingPreparingSolomonTest() {

        // Отслеживаем варку
        final long currentTime = System.currentTimeMillis() - 1000L;
        boilingSolomonService.startStage(PreparingTariffBoilingKey.of(1003L, DeliveryTariffProgramType.DAAS, BoilingStageType.TARIFF_PREPARING_STAGE), currentTime);
        boilingSolomonService.startStage(PreparingTariffBoilingKey.of(1004L, DeliveryTariffProgramType.DAAS, BoilingStageType.TARIFF_PREPARING_STAGE), currentTime);
        boilingSolomonService.startStage(PreparingTariffBoilingKey.of(1005L, DeliveryTariffProgramType.DAAS, BoilingStageType.TARIFF_PREPARING_STAGE), currentTime);
        boilingSolomonService.finishStage(PreparingTariffBoilingKey.of(1005L, DeliveryTariffProgramType.DAAS, BoilingStageType.TARIFF_PREPARING_STAGE), currentTime + 1000L);
        boilingSolomonService.startStage(PreparingTariffBoilingKey.of(1006L, DeliveryTariffProgramType.DAAS, BoilingStageType.TARIFF_PREPARING_STAGE), currentTime);

        // Сравниваем с ожидаемым
        final Map<String, String> sensor1002 = new HashMap<>() {{
            put("boilingStage", "2");
            put("carrierId", "35");
            put("program", "daas");
            put("tariffId", "1002");
            put("type", "COURIER");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        final Map<String, String> sensor1003 = new HashMap<>() {{
            put("boilingStage", "2");
            put("carrierId", "35");
            put("program", "daas");
            put("tariffId", "1003");
            put("type", "COURIER");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        final Map<String, String> sensor1005 = new HashMap<>() {{
            put("boilingStage", "2");
            put("carrierId", "35");
            put("program", "daas");
            put("tariffId", "1005");
            put("type", "COURIER");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        checkSensors(BoilingStageType.TARIFF_PREPARING_STAGE, sensor1002, sensor1003, sensor1005);

        // Проверяем, что остались отслеживаться только нужные тарифы
        final Map<Long, BoilingStateType> expectedStates = ImmutableMap.of(
                1002L, BoilingStateType.IN_PROGRESS,
                1003L, BoilingStateType.IN_PROGRESS,
                1005L, BoilingStateType.SENT
        );
        checkStates(BoilingStageType.TARIFF_PREPARING_STAGE, expectedStates);

        // Проверяем, что SENT статусы удалились
        checkSensors(BoilingStageType.TARIFF_PREPARING_STAGE, sensor1002, sensor1003);
        final Map<Long, BoilingStateType> expectedStatesAfterCleaning = ImmutableMap.of(
                1002L, BoilingStateType.IN_PROGRESS,
                1003L, BoilingStateType.IN_PROGRESS
        );
        checkStates(BoilingStageType.TARIFF_PREPARING_STAGE, expectedStatesAfterCleaning);
    }

    /**
     * Отправка данных о бакетах сваренного тарифа в Соломон.
     * <p>
     * Тариф 1001 - варка закончилась. Отслеживается. Есть количество рулов. Должен быть в Соломоне. Должен получить статус SENT. После этого должен перестать отслеживаться.
     * Тариф 1002 - варка закончилась. Отслеживается. Нет количества рулов. Должен быть в Соломоне. Должен получить статус SENT. После этого должен перестать отслеживаться.
     * Тариф 1003 - отслеживается, но удален из базы. Не должен быть в Соломоне. Должен быть исключен из отслеживания.
     */
    @Test
    @DbUnitDataSet(before = "data/db/indexerBoilingBucketsSolomonTest.before.csv")
    void indexerBoilingBucketsSolomonTest() {

        // Отслеживаем варку
        boilingSolomonService.doStage(BucketCountingBoilingKey.of(1001L, 1L, 1L, 10, DeliveryTariffProgramType.DAAS, BoilingStageType.BUCKET_COUNTING_STAGE), () -> {
        });
        boilingSolomonService.doStage(BucketCountingBoilingKey.of(1002L, 1L, 1L, 10, DeliveryTariffProgramType.DAAS, BoilingStageType.BUCKET_COUNTING_STAGE), () -> {
        });
        boilingSolomonService.doStage(BucketCountingBoilingKey.of(1003L, 1L, 1L, 10, DeliveryTariffProgramType.DAAS, BoilingStageType.BUCKET_COUNTING_STAGE), () -> {
        });

        final Map<String, String> baseSensor = ImmutableMap.<String, String>builder()
                .put("boilingStage", "4")
                .put("carrierId", "35")
                .put("program", "daas")
                .put("type", "COURIER")
                .put("sensor", "boilingCurrentDurationByStage")
                .build();

        // Сравниваем с ожидаемым
        // 1001
        final Map<String, String> sensor1001BucketsCount = ImmutableMap.<String, String>builder()
                .putAll(baseSensor)
                .put("tariffId", "1001")
                .put("subStage", "bucketsCount")
                .build();
        final Map<String, String> sensor1001RulesCount = ImmutableMap.<String, String>builder()
                .putAll(baseSensor)
                .put("tariffId", "1001")
                .put("subStage", "rulesCount")
                .build();
        final Map<String, String> sensor1001OriginalRulesCount = ImmutableMap.<String, String>builder()
                .putAll(baseSensor)
                .put("tariffId", "1001")
                .put("subStage", "originalRulesCount")
                .build();

        // 1002
        final Map<String, String> sensor1002BucketsCount = ImmutableMap.<String, String>builder()
                .putAll(baseSensor)
                .put("tariffId", "1002")
                .put("subStage", "bucketsCount")
                .build();


        checkSensors(BoilingStageType.BUCKET_COUNTING_STAGE, sensor1001BucketsCount, sensor1001RulesCount, sensor1001OriginalRulesCount, sensor1002BucketsCount);

        // Проверяем, что остались отслеживаться только нужные тарифы
        final Map<Long, BoilingStateType> expectedStates = ImmutableMap.of(
                1001L, BoilingStateType.SENT,
                1002L, BoilingStateType.SENT
        );
        checkStates(BoilingStageType.BUCKET_COUNTING_STAGE, expectedStates);

        // Проверяем, что SENT статусы удалились
        checkSensors(BoilingStageType.BUCKET_COUNTING_STAGE);
        checkStates(BoilingStageType.BUCKET_COUNTING_STAGE, Collections.emptyMap());
    }

    /**
     * Отправка данных об обновлении тарифа в Соломон.
     * <p>
     * Тариф 1001 - новый, еще нет в базе. Загрузка в процессе. Должен быть в Соломоне. Должен отслеживаться.
     * Тариф 1002 - есть в базе. Идет обновление по статусу. Хэши разные. Должен быть в Соломоне. Должен отслеживаться.
     * Тариф 1003 - есть в базе. Идет обновление по статусу. Хэши одинаковые. Не должен быть в Соломоне. Не должен отслеживаться.
     * Тариф 1004 - есть в базе. Закончено обновление по статусу. Должен быть в Соломоне. Статус должен стать SENT. После этого должен перестать отслеживаться.
     */
    @Test
    @DbUnitDataSet(before = "data/db/indexerBoilingUpdatingSolomonTest.before.csv")
    void indexerBoilingUpdatingSolomonTest() {

        // Отслеживаем варку
        final long currentTime = System.currentTimeMillis() - 1000L;
        boilingSolomonService.startStage(UpdatingTariffBoilingKey.of(1001L, "hash_1001", BoilingStageType.TARIFF_UPDATING_STAGE), currentTime);
        boilingSolomonService.startStage(UpdatingTariffBoilingKey.of(1002L, "hash_1002_new", BoilingStageType.TARIFF_UPDATING_STAGE), currentTime);
        boilingSolomonService.startStage(UpdatingTariffBoilingKey.of(1003L, "hash_1003", BoilingStageType.TARIFF_UPDATING_STAGE), currentTime);
        boilingSolomonService.startStage(UpdatingTariffBoilingKey.of(1004L, "hash_1004", BoilingStageType.TARIFF_UPDATING_STAGE), currentTime);
        boilingSolomonService.finishStage(UpdatingTariffBoilingKey.of(1004L, "hash_1004", BoilingStageType.TARIFF_UPDATING_STAGE), currentTime + 1000L);

        // Сравниваем с ожидаемым
        final Map<String, String> sensor1001 = new HashMap<>() {{
            put("boilingStage", "1");
            put("tariffId", "1001");
            put("tariffHash", "hash_1001");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        final Map<String, String> sensor1002 = new HashMap<>() {{
            put("boilingStage", "1");
            put("tariffId", "1002");
            put("tariffHash", "hash_1002_new");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        final Map<String, String> sensor1004 = new HashMap<>() {{
            put("boilingStage", "1");
            put("tariffId", "1004");
            put("tariffHash", "hash_1004");
            put("sensor", "boilingCurrentDurationByStage");
        }};

        checkSensors(BoilingStageType.TARIFF_UPDATING_STAGE, sensor1001, sensor1002, sensor1004);

        // Проверяем, что остались отслеживаться только нужные тарифы
        final Map<Long, BoilingStateType> expectedStates = ImmutableMap.of(
                1001L, BoilingStateType.IN_PROGRESS,
                1002L, BoilingStateType.IN_PROGRESS,
                1004L, BoilingStateType.SENT
        );
        checkStates(BoilingStageType.TARIFF_UPDATING_STAGE, expectedStates);

        // Проверяем, что SENT статусы удалились
        checkSensors(BoilingStageType.TARIFF_UPDATING_STAGE, sensor1001, sensor1002);
        final Map<Long, BoilingStateType> expectedStatesAfterCleaning = ImmutableMap.of(
                1001L, BoilingStateType.IN_PROGRESS,
                1002L, BoilingStateType.IN_PROGRESS
        );
        checkStates(BoilingStageType.TARIFF_UPDATING_STAGE, expectedStatesAfterCleaning);
    }

    /**
     * Проверить сенсоры варки.
     */
    @SafeVarargs
    private void checkSensors(final BoilingStageType stageType, final Map<String, String>... sensors) {
        sensorFunctionTask.run();

        // Получаем данные для Соломона
        final ResponseEntity<SensorLongValueList> responseEntity = FunctionalTestHelper.get(
                baseUrl + "/solomon",
                SensorLongValueList.class
        );

        final List<SensorLongValue> actualSensors = responseEntity.getBody().getSensors();
        final List<Map<String, String>> actualLabels = CollectionUtils.emptyIfNull(actualSensors).stream()
                .map(SensorLongValue::getLabels)
                .filter(e -> "boilingCurrentDurationByStage".equals(e.get("sensor")))
                .filter(e -> String.valueOf(stageType.getId()).equals(e.get("boilingStage")))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(actualLabels, Matchers.containsInAnyOrder(sensors));
    }

    /**
     * Проверить статусы, которые отслеживаются сейчас в метрике варки.
     */
    private void checkStates(final BoilingStageType stageType, final Map<Long, BoilingStateType> expectedStates) {
        // Вытаскиваем статусы со статусом SENT. Они могли уже удалиться из отслеживания, а могли еще не успеть.
        // Поэтому их проверяем только, если они есть.
        final Set<Long> expectedSent = expectedStates.entrySet().stream()
                .filter(e -> e.getValue() == BoilingStateType.SENT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        final Map<Long, BoilingStateType> expectedNotSent = expectedStates.entrySet().stream()
                .filter(e -> !expectedSent.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<Long, BoilingStateType> actualStates = boilingSolomonService.getStates(Collections.singleton(stageType)).stream()
                .collect(Collectors.toMap(e -> e.getBoilingKey().unwrap(TariffBoilingKey.class).getTariffId(), BoilingState::getStateType));

        final Map<Long, BoilingStateType> actualNotSentStates = actualStates.entrySet().stream()
                .filter(e -> {
                    if (e.getValue() == BoilingStateType.SENT) {
                        assertTrue(expectedSent.contains(e.getKey()));
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals(expectedNotSent, actualNotSentStates);
    }

    @Test
    @DisplayName("Проверка, что /solomon отвечает корректно")
    void solomonTestOk() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                baseUrl + "/solomon",
                String.class
        );
        assertSame(responseEntity.getStatusCode(), HttpStatus.OK);
    }

    @Test
    @DisplayName("Проверка работы spack")
    void solomonSpackTrueRequestTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, SensorsFormat.SPACK.contentType());
        headers.addAll(
                HttpHeaders.ACCEPT_ENCODING,
                Arrays.asList(
                        CompressionAlg.ZSTD.encoding(),
                        CompressionAlg.LZ4.encoding(),
                        CompressionAlg.ZLIB.encoding(),
                        "deflate"
                )
        );

        ResponseEntity<byte[]> result = solomonRequest(headers);
        checkResponse(
                new SensorsSpackDecoder(),
                new SensorsSpackEncoder(TimePrecision.SECONDS, CompressionAlg.LZ4, new ByteArrayOutputStream()),
                result
        );
    }

    @Test
    @DisplayName("Проверка получения данных о поколениях")
    @DbUnitDataSet(before = "data/db/generationMetaInfo.before.csv")
    void mardoCourierGenerationMetaInfo() {
        ResponseEntity<SensorLongValueList> responseEntity = FunctionalTestHelper.get(
                baseUrl + "/solomon",
                SensorLongValueList.class
        );

        Set<SensorLongValue> expected = ImmutableSet.of(
                createGenerationSensor(DeliveryTariffProgramType.MARKET_DELIVERY, 4207L, YaDeliveryTariffType.COURIER, 4),
                createGenerationSensor(DeliveryTariffProgramType.MARKET_DELIVERY, 4209L, YaDeliveryTariffType.PICKUP, 4),
                createGenerationSensor(DeliveryTariffProgramType.MARKET_DELIVERY, 4210L, YaDeliveryTariffType.POST, 4),
                createGenerationSensor(DeliveryTariffProgramType.WHITE_MARKET_DELIVERY, 321L, YaDeliveryTariffType.COURIER, 10),
                createGenerationSensor(DeliveryTariffProgramType.WHITE_MARKET_DELIVERY, 432L, YaDeliveryTariffType.PICKUP, 11)
        );

        checkResponse(responseEntity, expected);
    }

    private SensorLongValue createGenerationSensor(DeliveryTariffProgramType program,
                                                   Long tariffId,
                                                   YaDeliveryTariffType type,
                                                   int rulesCount) {
        return SensorLongValue.create(SensorKind.IGAUGE,
                ImmutableMap.of("generationId", Long.toString(1L),
                        "program", program.getSEProgramName(),
                        "tariffId", Long.toString(tariffId),
                        "type", type.name(),
                        "sensor", "rulesCount"), rulesCount);
    }

    private void checkResponse(ResponseEntity<SensorLongValueList> responseEntity, Set<SensorLongValue> expected) {
        assertTrue(responseEntity.getBody().getSensors().containsAll(expected));
    }

    @Test
    @DisplayName("Проверка работы json")
    void solomonJsonTrueRequestTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, SensorsFormat.JSON.contentType());

        ResponseEntity<byte[]> result = solomonRequest(headers);
        checkResponse(
                new SensorsJsonDecoder(),
                new SensorsJsonEncoder(new ByteArrayOutputStream()),
                result);
    }

    private ResponseEntity<byte[]> solomonRequest(HttpHeaders headers) {
        return REST_TEMPLATE.exchange(baseUrl + "/solomon",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class);

    }

    private ResponseEntity<byte[]> solomonJvmRequest(HttpHeaders headers) {
        return REST_TEMPLATE.exchange(baseUrl + "/solomon-jvm",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class);

    }

    private void checkResponse(SensorsDecoder decoder, SensorsConsumer consumer, ResponseEntity<byte[]> result) {
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertDoesNotThrow(() -> decoder.decode(result.getBody(), consumer));
    }

    @Test
    void solomonJvmTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, SensorsFormat.JSON.contentType());
        ResponseEntity<byte[]> result = solomonJvmRequest(headers);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
