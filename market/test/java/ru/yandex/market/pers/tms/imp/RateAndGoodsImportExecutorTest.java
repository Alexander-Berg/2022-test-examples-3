package ru.yandex.market.pers.tms.imp;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.grade.core.util.rateAndGoods.RateAndGoodsClient;
import ru.yandex.market.pers.grade.core.util.rateAndGoods.RngReviewsDto;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.11.2018
 */
public class RateAndGoodsImportExecutorTest extends MockedPersTmsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final long MODEL_ID = 100;
    private static final long BARCODE = 4000000000000L;

    @Autowired
    private RateAndGoodsImportExecutor rateAndGoodsImportExecutor;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("rateAndGoodsRestTemplate")
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        configurationService.mergeValue(RateAndGoodsImportExecutor.BATCH_SIZE_KEY, 1000L);
    }

    private static String getGradeImportData(int gradeValue, String date, String comment, long modelId) {
        return "{\"import_as_approved\":\"true\"," +
            "\"grade\":\"" + gradeValue + "\"," +
            "\"create_dt\":\"" + date + "\"," +
            "\"comment\":\"" + comment + "\"," +
            "\"model_id\":\"" + modelId + "\"" +
            "}";
    }

    @Test
    public void testImport() throws Exception {
        final Instant time = Instant.now().minus(4, ChronoUnit.DAYS);

        // нужно обновить в первый раз, есть в апи
        long modelId1 = MODEL_ID + 1;
        long barcode1 = BARCODE + 1;
        add(modelId1, barcode1, RngImportState.NEED_UPLOAD, null);
        String expectedDataImport1 = getGradeImportData(3, "2019-12-02 12:19:49", "Так себе сгущенка", modelId1);
        mockResponse("/testdata/rateAndGoods/reviews1.json", barcode1);

        // нужно обновить не в первый раз, есть в апи
        long modelId2 = MODEL_ID + 2;
        long barcode2 = BARCODE + 2;
        add(modelId2, barcode2, RngImportState.NEED_UPLOAD, time);
        String expectedDataImport2 = getGradeImportData(4, "2019-01-01 06:51:08", "Настоящее сгущенное молоко",
            modelId2);
        String expectedDataImport3 = getGradeImportData(5, "2019-11-03 14:55:18", "Алексеевская сгущенка - топ",
            modelId2);
        mockResponse("/testdata/rateAndGoods/reviews2.json", barcode2, time);

        // нужно обновить в первый раз, нет в апи
        long modelId3 = MODEL_ID + 3;
        long barcode3 = BARCODE + 3;
        add(modelId3, barcode3, RngImportState.NEED_UPLOAD, null);
        mockResponse("/testdata/rateAndGoods/reviews_empty.json", barcode3);

        // нужно обновить не в первый раз, нет в апи
        long modelId4 = MODEL_ID + 4;
        long barcode4 = BARCODE + 4;
        add(modelId4, barcode4, RngImportState.NEED_UPLOAD, time);
        mockResponse("/testdata/rateAndGoods/reviews_empty.json", barcode4, time);

        // не нужно обновлять, есть в апи
        long modelId5 = MODEL_ID + 5;
        long barcode5 = BARCODE + 5;
        add(modelId5, barcode5, RngImportState.ALREADY_UPLOAD, time);

        // не нужно обновлять, нет в апи
        long modelId6 = MODEL_ID + 6;
        long barcode6 = BARCODE + 6;
        add(modelId6, barcode6, RngImportState.ALREADY_UPLOAD, time);

        rateAndGoodsImportExecutor.runTmsJob();

        final Long count = getImportCount();
        assertEquals(3, count.longValue());
        final List<String> importData = getDataToImport();
        assertEquals(3, importData.size());
        assertTrue(importData.contains(expectedDataImport1));
        assertTrue(importData.contains(expectedDataImport2));
        assertTrue(importData.contains(expectedDataImport3));
    }

    @Test
    public void testZeroGradeValue() throws Exception {
        // нужно обновить в первый раз, есть в апи
        long modelId = MODEL_ID;
        long barcode = BARCODE;
        add(modelId, barcode, RngImportState.NEED_UPLOAD, null);
        String expectedDataImport = getGradeImportData(1, "2019-12-02 12:19:49", "Так себе сгущенка", modelId);
        mockResponse("/testdata/rateAndGoods/reviews_zero_grade_value.json", barcode);

        rateAndGoodsImportExecutor.runTmsJob();

        final Long count = getImportCount();
        assertEquals(1, count.longValue());
        final List<String> importData = getDataToImport();
        assertEquals(1, importData.size());
        assertTrue(importData.contains(expectedDataImport));
    }

    @Test
    public void testEmptyGrade() throws Exception {
        final Instant time = Instant.now().minus(4, ChronoUnit.DAYS);

        // есть отзыв с пустым текстом
        long modelId1 = MODEL_ID + 1;
        long barcode1 = BARCODE + 1;
        add(modelId1, barcode1, RngImportState.NEED_UPLOAD, null);
        mockResponse("/testdata/rateAndGoods/reviews_empty_text.json", barcode1);
        String expectedDataImport1 = getGradeImportData(3, "2019-12-02 12:19:49", "Так себе сгущенка", modelId1);

        long modelId2 = MODEL_ID + 2;
        long barcode2 = BARCODE + 2;
        add(modelId2, barcode2, RngImportState.NEED_UPLOAD, time);
        String expectedDataImport2 = getGradeImportData(4, "2019-01-01 06:51:08", "Настоящее сгущенное молоко",
            modelId2);
        String expectedDataImport3 = getGradeImportData(5, "2019-11-03 14:55:18", "Алексеевская сгущенка - топ",
            modelId2);
        mockResponse("/testdata/rateAndGoods/reviews2.json", barcode2, time);

        rateAndGoodsImportExecutor.runTmsJob();

        final Long count = getImportCount();
        assertEquals(3, count.longValue());
        final List<String> importData = getDataToImport();
        assertEquals(3, importData.size());
        assertTrue(importData.contains(expectedDataImport1));
        assertTrue(importData.contains(expectedDataImport2));
        assertTrue(importData.contains(expectedDataImport3));
    }

    @Test
    public void test5xx() throws Exception {
        // есть отзыв с пустым текстом
        long modelId1 = MODEL_ID + 1;
        long barcode1 = BARCODE + 1;
        add(modelId1, barcode1, RngImportState.NEED_UPLOAD, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(RngReviewsDto.class),
            argThat(paramMatcher(barcode1))))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY));

        long modelId2 = MODEL_ID + 2;
        long barcode2 = BARCODE + 2;
        add(modelId2, barcode2, RngImportState.NEED_UPLOAD, null);
        String expectedDataImport2 = getGradeImportData(4, "2019-01-01 06:51:08", "Настоящее сгущенное молоко",
            modelId2);
        String expectedDataImport3 = getGradeImportData(5, "2019-11-03 14:55:18", "Алексеевская сгущенка - топ",
            modelId2);
        mockResponse("/testdata/rateAndGoods/reviews2.json", barcode2);

        boolean thrown = false;
        try {
            rateAndGoodsImportExecutor.runTmsJob();
        } catch (Exception e) {
            thrown = true;
            assertEquals(e.getMessage(), String.format("Skip 1 models from 2: [%s]", modelId1));
        }
        if (!thrown) {
            Assert.fail("Date must be Exception");
        }

        final Long count = getImportCount();
        assertEquals(2, count.longValue());
        final List<String> importData = getDataToImport();
        assertEquals(2, importData.size());
        assertTrue(importData.contains(expectedDataImport2));
        assertTrue(importData.contains(expectedDataImport3));
        checkState(modelId1, RngImportState.ALREADY_UPLOAD);
    }

    @Test
    public void testRepeatedImport() throws Exception {
        // нужно обновить в первый раз, есть в апи
        long modelId = MODEL_ID + 1;
        long barcode = BARCODE + 1;
        add(modelId, barcode, RngImportState.NEED_UPLOAD, null);
        String expectedDataImport1 = getGradeImportData(3, "2019-12-02 12:19:49", "Так себе сгущенка", modelId);
        mockResponse("/testdata/rateAndGoods/reviews1.json", barcode);

        rateAndGoodsImportExecutor.runTmsJob();

        checkState(modelId, RngImportState.ALREADY_UPLOAD);
        Long count = getImportCount();
        assertEquals(1, count.longValue());
        List<String> importData = getDataToImport();
        assertEquals(1, importData.size());
        assertTrue(importData.contains(expectedDataImport1));

        pgJdbcTemplate.update("delete from grade_import_queue");
        pgJdbcTemplate.update("update model_rng_import set state = ? where model_id = ? ",
            RngImportState.NEED_UPLOAD.getValue(), modelId);
        String expectedDataImport2 = getGradeImportData(4, "2019-01-01 06:51:08", "Настоящее сгущенное молоко",
            modelId);
        String expectedDataImport3 = getGradeImportData(5, "2019-11-03 14:55:18", "Алексеевская сгущенка - топ",
            modelId);
        mockResponse("/testdata/rateAndGoods/reviews2.json", barcode);

        rateAndGoodsImportExecutor.runTmsJob();

        count = getImportCount();
        assertEquals(2, count.longValue());
        importData = getDataToImport();
        assertEquals(2, importData.size());
        assertTrue(importData.contains(expectedDataImport2));
        assertTrue(importData.contains(expectedDataImport3));
    }

    private void mockResponse(String filename, long barcode) throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(RngReviewsDto.class),
            argThat(paramMatcher(barcode))))
            .thenReturn(getResponse(filename));
    }

    private void mockResponse(String filename, long barcode, Instant time) throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(RngReviewsDto.class),
            argThat(paramMatcher(barcode, time))))
            .thenReturn(getResponse(filename));
    }

    private void checkState(long modelId1, RngImportState state) {
        final Long state5xxModel = pgJdbcTemplate.queryForObject(
            "select state from model_rng_import where model_id = ?", Long.class, modelId1);
        assertEquals(state.getValue(), state5xxModel.longValue());
    }

    public void add(long modelId, long barcode, RngImportState state, Instant updatedTime) {
        pgJdbcTemplate.update("insert into model_rng_import(model_id, barcode, state, update_time) " +
                "values(?, ?, ?, ?)",
            modelId, barcode, state.getValue(),
            Optional.ofNullable(updatedTime).map(Timestamp::from).orElse(null));
    }

    private RngReviewsDto getAnswer(String filename) throws Exception {
        return OBJECT_MAPPER.readValue(getClass().getResourceAsStream(filename), RngReviewsDto.class);
    }

    private ResponseEntity<RngReviewsDto> getResponse(String filename) throws Exception {
        return new ResponseEntity<>(getAnswer(filename), HttpStatus.OK);
    }

    private ArgumentMatcher<Map<String, Object>> paramMatcher(long barcode, Instant time) {
        return o -> {
            boolean barcodeResult = o.get("filter").toString().contains(String.valueOf(barcode));
            boolean timeResult = time == null ||
                o.get("filter").toString().contains(RateAndGoodsClient.formatTime(time.truncatedTo(ChronoUnit.SECONDS)));
            return barcodeResult && timeResult;
        };
    }

    private ArgumentMatcher<Map<String, Object>> paramMatcher(long barcode) {
        return o -> {
            return o.get("filter").toString().contains(String.valueOf(barcode));
        };
    }

    private Long getImportCount() {
        return pgJdbcTemplate.queryForObject("select count(*) from grade_import_queue", Long.class);
    }

    @NotNull
    private List<String> getDataToImport() {
        return pgJdbcTemplate.queryForList("select data from grade_import_queue", String.class);
    }

}
