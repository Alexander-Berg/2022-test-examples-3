package ru.yandex.market.checkerxservice.mock;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkerxservice.TestUtils;
import ru.yandex.market.checkerxservice.chekservice.CheckErxProperties;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByEsklpRequest;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByMnnRequest;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataStatusRequest;
import ru.yandex.market.checkerxservice.config.SpringApplicationConfig;
import ru.yandex.market.checkerxservice.testdata.DbTestData;
import ru.yandex.market.checkerxservice.testdata.TestData;
import ru.yandex.market.checkerxservice.utils.JsonUtils;
import ru.yandex.market.checkerxservice.utils.ResourceFileReader;

@TestPropertySource(locations = "classpath:00_application.properties")
@ContextConfiguration(classes = SpringApplicationConfig.class)
@WebAppConfiguration
public class TestMock {
    @Autowired
    private CheckErxProperties properties;
    @Autowired
    private ResourceFileReader fileReader;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private NamedParameterJdbcTemplate jdbcTemplate;
    /*@MockBean
    private TransactionTemplate transactionTemplate;*/
    private URI checkDrugsUri;
    private URI checkDrugs2Uri;
    private URI getPrescriptionForStatusUri;
    private String reportOfferInfoUriStr;
    private String reportModelInfoUriStr;
    private final TestData TEST_DATA = TestData.getInstance();
    private static final Map<String, List<String>> HEADERS =
            Map.of("Content-Type", List.of("application/json;charset=UTF-8"));
    // <sql_text, query_name>
    private Map<String, String> queryMap = new HashMap<>();
    private Map<String, List<String>> getSavedParamMap = new HashMap<>();

    @Before
    public void init() {
        this.checkDrugsUri = URI.create(properties.getMedicataUrl() + "/checkDrugs");
        this.checkDrugs2Uri = URI.create(properties.getMedicataUrl() + "/checkDrugs2");
        this.getPrescriptionForStatusUri = URI.create(properties.getMedicataUrl() + "/getPrescriptionsForRegion");
        mockMedicataClient();
        this.reportOfferInfoUriStr = properties.getReportPath() + "?currency=";
        this.reportModelInfoUriStr = properties.getReportPath() + "?base=market.yandex.ru";
        mockReportClient();
        mockJdbc();
    }

    private void mockMedicataClient() {
        Mockito.when(restTemplate.postForEntity(Mockito.eq(checkDrugsUri), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    MedicataGuidByMnnRequest request =
                            ((HttpEntity<MedicataGuidByMnnRequest>)invocation.getArgument(1)).getBody();
                    return new ResponseEntity<>(
                            TEST_DATA.getMedicataGuidMnnRespose(JsonUtils.toJson(request)), HttpStatus.OK);
                });
        Mockito.when(restTemplate.postForEntity(Mockito.eq(checkDrugs2Uri), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    MedicataGuidByEsklpRequest request =
                            ((HttpEntity<MedicataGuidByEsklpRequest>)invocation.getArgument(1)).getBody();
                    return new ResponseEntity<>(
                            TEST_DATA.getMedicataGuidEsklpRespose(JsonUtils.toJson(request)), HttpStatus.OK);
                });
        Mockito.when(restTemplate.postForEntity(Mockito.eq(getPrescriptionForStatusUri), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    MedicataStatusRequest request =
                            ((HttpEntity<MedicataStatusRequest>)invocation.getArgument(1)).getBody();
                    return new ResponseEntity<>(
                            TEST_DATA.getMedicataStatusResponse(JsonUtils.toJson(request)), HttpStatus.OK);
                });
    }

    private void mockReportClient() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        Mockito.when(restTemplate.exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.eq(entity),
                        Mockito.eq(String.class)))
                .thenAnswer(invocation -> generateReportResponse(invocation));
    }

    private ResponseEntity<String> generateReportResponse(InvocationOnMock invocation) {
        String url = ((String)invocation.getArgument(0));

        if (url.startsWith(reportOfferInfoUriStr)) {
            return generateOfferInfoResponse(url);
        }
        if (url.startsWith(reportModelInfoUriStr)) {
            return generateModelInfoResponse(url);
        }
        return new ResponseEntity<>("{}", HttpStatus.valueOf(404));  //not found
    }

    private ResponseEntity<String> generateOfferInfoResponse(String url) {
        final Pattern OFFER_ID_PATTERN = Pattern.compile("rids=(\\d{1,3})[&]{1}\\S*offerid=([^&]*)");
        Matcher matcher = OFFER_ID_PATTERN.matcher(url);

        if (!matcher.find()){
            return new ResponseEntity<>("{}", HttpStatus.valueOf(400));
        }
        int yandexRegionId = Integer.parseInt(matcher.group(1));
        String offerId = matcher.group(2);
        TestData.ReportData reportData = TEST_DATA.getReportDataByOfferId(offerId);

        if (reportData == null) {
            return new ResponseEntity<>("{}", HttpStatus.valueOf(500));
        }
        return new ResponseEntity<>(
                TestUtils.readFileToStrong("json/report/report_response_1.jsontpl")
                        .replace("${modelId}", String.valueOf(reportData.getModelId()))
                , HttpStatus.OK);
    }

    private ResponseEntity<String> generateModelInfoResponse(String url) {
        final Pattern MODEL_ID_PATTERN = Pattern.compile("rids=(\\d{1,3})[&]{1}\\S*hyperid=([\\d]*)");
        Matcher matcher = MODEL_ID_PATTERN.matcher(url);

        if (!matcher.find()){
            return new ResponseEntity<>("{}", HttpStatus.valueOf(400));
        }
        int yandexRegionId = Integer.parseInt(matcher.group(1));
        long modelId = Long.parseLong(matcher.group(2));
        TestData.ReportData reportData = TEST_DATA.getReportDataByModelId(modelId);

        if (reportData == null) {
            return new ResponseEntity<>("{}", HttpStatus.valueOf(500));
        }
        return new ResponseEntity<>(
                TestUtils.readFileToStrong("json/report/report_response_2.jsontpl")
                        .replace("${barCode}", reportData.getBarCode())
                , HttpStatus.OK);
    }

    private void mockJdbc() {
        // ReadingDao
        queryMap.put(fileReader.safeReadSqlToString("qGetMedicataStubDataByEsklp.sql"), "MedicataStubByEsklp");
        queryMap.put(fileReader.safeReadSqlToString("qGetMedicataStubDataByMnn.sql"), "MedicataStubByMnn");
        String baseSqlText = fileReader.safeReadSqlToString("qGetGuidRequestData.sql");
        Map<String, List<String>> offerIdsMap = DbTestData.getInstance().getOfferIdsMap();

        offerIdsMap.keySet().forEach(offerIdsString -> {
                    String resultSql = baseSqlText.replace("@offer_ids@", offerIdsString);
                    queryMap.put(resultSql, "getSaved");
                    getSavedParamMap.put(resultSql, offerIdsMap.get(offerIdsString));
                });
        // WritingDao
        queryMap.put(fileReader.safeReadSqlToString("f_ins_uid_to_esia_token.sql"), "insUidToToken");
        queryMap.put(fileReader.safeReadSqlToString("f_ins_offer_and_guids.sql"), "insOfferAndGuids");
        queryMap.put(fileReader.safeReadSqlToString("qGetEsklpByBarcode.sql"), "EsklpByBarCode");

        // implementation
        Mockito.when(jdbcTemplate.queryForRowSet(Mockito.anyString(), Mockito.any(SqlParameterSource.class)))
                .thenAnswer(invocation -> {
                    String sqlText = (String) invocation.getArgument(0);
                    String sqlName = queryMap.get(sqlText);
                    MapSqlParameterSource paramSource = invocation.getArgument(1);

                    return executeQuery(sqlName, paramSource, sqlText);
                });
    }

    private RowSetMock executeQuery(String sqlName, MapSqlParameterSource paramSource, String sqlText)
            throws SQLException {
        if (sqlName == null) {
            throw new SQLException("Запрос: \n" + sqlText + "\n не найден в mock-списке.");
        }
        switch (sqlName) {
            case "insUidToToken":
                return insUidToToken(paramSource);
            case "insOfferAndGuids":
                return insOfferAndGuids(paramSource);
            case "getSaved":
                return getSaved(paramSource, sqlText);
            case "EsklpByBarCode":
                return esklpByBarCode(paramSource);
            default:
                return new RowSetMock(Collections.emptyList());
        }
    }

    private RowSetMock insUidToToken(MapSqlParameterSource paramSource) {
        String esiaToken = (String) paramSource.getValue("esia_token");
        Long uid = (Long) paramSource.getValue("uid");
        Integer regionCode = (Integer) paramSource.getValue("region_code");

        Map<String, Object> fieldMap = new HashMap<>();
        DbTestData.getInstance().putUidToEsiaToken(uid, esiaToken, regionCode);
        fieldMap.put("id", DbTestData.getInstance().getNewuidToEsiaTokenId());

        return new RowSetMock(List.of(fieldMap));
    }

    private RowSetMock insOfferAndGuids(MapSqlParameterSource paramSource) {
        String offerId = (String) paramSource.getValue("offer_id");
        Long uid = (Long) paramSource.getValue("uid");
        String guids = (String) paramSource.getValue("guid_list");

        Map<String, Object> fieldMap = new HashMap<>();
        DbTestData.getInstance().putOfferToGuid(offerId, uid, guids);
        fieldMap.put("id", DbTestData.getInstance().getNewOfferToGuidsId());

        return new RowSetMock(List.of(fieldMap));
    }

    private RowSetMock getSaved(MapSqlParameterSource paramSource, String sqlText) {
        Long uid = (Long) paramSource.getValue("uid");
        List<String> offerIdList = getSavedParamMap.get(sqlText);
        DbTestData.TokenAndRegionData tokenAndRegion =
                DbTestData.getInstance().getTokenByUid(uid);
        if (tokenAndRegion == null) {
            return new RowSetMock(Collections.emptyList());
        }
        List<Map<String, Object>> data = new ArrayList<>();

        for (String offerId : offerIdList) {
            Map<String, Object> fieldMap = new HashMap<>();
            String guids = DbTestData.getInstance().getGuidsByOfferId(offerId, uid);

            if (guids != null) {
                fieldMap.put("esia_user_token", tokenAndRegion.userEsiaToken);
                fieldMap.put("region_code", tokenAndRegion.regionCode);
                fieldMap.put("guid_list", guids);
                fieldMap.put("offer_id", offerId);
                data.add(fieldMap);
            }
        }
        return new RowSetMock(data);
    }

    private RowSetMock esklpByBarCode(MapSqlParameterSource paramSource) {
        String barcode = (String) paramSource.getValue("gtin");

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("esklp_mnn", DbTestData.getInstance().getEsklpByBarcode(barcode));

        return new RowSetMock(List.of(fieldMap));
    }
}
