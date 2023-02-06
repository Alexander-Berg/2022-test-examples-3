package ru.yandex.market.hrms.core.service.outstaff.create;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Company;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Post;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.WorkingArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.config.TestMockConfig;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffBeginnerShiftEnd;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffService;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffTimexLoaderService;
import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.outstaff.dto.YaDiskResponseDto;
import ru.yandex.market.hrms.core.service.outstaff.enums.OutstaffFormEnum;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffStartrekClientStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffYqlRepoStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.S3ServiceStub;
import ru.yandex.market.hrms.core.service.timex.FakeTimexApiFacade;
import ru.yandex.market.hrms.core.service.timex.TimexOperationStatus;
import ru.yandex.market.hrms.core.service.timex.dto.TimexResponseDto;
import ru.yandex.market.hrms.core.service.wms.WmsUserStateManager;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class OutstaffServiceTest extends AbstractCoreTest {

    @Autowired
    private OutstaffService service;

    @MockBean
    private YaDiskClient yaDiskClient;

    @MockBean
    private WmsUserStateManager wmsUserStateManager;

    @Autowired
    private OutstaffTimexLoaderService timexLoaderService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    @MockBean(name = "hrmsRestTemplate")
    private RestTemplate hrmsRestTemplate;

    @Captor
    private ArgumentCaptor<Map<Long, List<OutstaffBeginnerShiftEnd>>> argumentCaptor;

    private final String INPUT_JSON = "/inputs/outstaff_yt_single_record.json";
    private final String INPUT_JSON_WRONG_AGE = "/inputs/outstaff_yt_single_record_wrong_age.json";
    private final String INPUT_JSON_SAME_PHONE_NEW_AREA = "/inputs/outstaff_yt_single_record_new_area.json";
    private final String INPUT_JSON_MULTIPLE_SC = "/inputs/outstaff_yt_single_record_multiple_sc.json";
    private final String INPUT_JSON_MULTIPLE_FFC = "/inputs/outstaff_yt_single_record_multiple_ffc.json";
    private final String INPUT_JSON_EKB_FFC = "/inputs/outstaff_yt_single_record_ekb_ffc.json";
    private final String INPUT_JSON_EKB_SC = "/inputs/outstaff_yt_single_record_ekb_sc.json";


    private byte[] getFileFromResource(String resource) throws Exception {
        return IOUtils.toByteArray(TestMockConfig.class.getResourceAsStream(resource));
    }

    private String getStringFromResource(String resource) throws Exception {
        return IOUtils.toString(TestMockConfig.class.getResourceAsStream(resource),
                StandardCharsets.UTF_8);
    }

    private void configureTimexApiFacade(String accessLevel, String timexOid, TimexOperationStatus opStatus,
                                         WorkingArea[] areas) {
        var acl = new AccessLevel();
        acl.setName(accessLevel);

        var fakeTimexApiFacade = context.getBean(FakeTimexApiFacade.class);
        fakeTimexApiFacade.withCompanies(new Company[]{});
        fakeTimexApiFacade.withPosts(new Post[]{});
        fakeTimexApiFacade.withAccessLevels(new AccessLevel[]{acl});
        fakeTimexApiFacade.withTimexResponseDto(new TimexResponseDto(opStatus, timexOid));
        fakeTimexApiFacade.withEmployee(new Employee());
        fakeTimexApiFacade.withWorkingArea(areas);
    }


    private void configureYaDiskClient(String yaDiskUrl, int statusCode, byte[] file) throws Exception {
        when(yaDiskClient.getFileDirectDownloadLink(anyString()))
                .thenReturn(new YaDiskResponseDto(statusCode, null, yaDiskUrl,
                        null, false, null, null, null));
        when(yaDiskClient.downloadFile(anyString()))
                .thenReturn(Optional.ofNullable(file));
    }

    @AfterEach
    public void clearMocks() {
        Mockito.reset(yaDiskClient);
    }

    @BeforeEach
    public void init() throws Exception {
        byte[] photo = getFileFromResource("/inputs/input_photo.jpg");
        configureYaDiskClient("http://disk.yandex.ru/photo.jpg", 200, photo);
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.add("Content-Type", MediaType.IMAGE_JPEG_VALUE);
        when(hrmsRestTemplate.headForHeaders(ArgumentMatchers.any())).thenReturn(mockHeaders);
    }

    private void configureS3Service(String bucketName, String key, byte[] object) {
        var s3Stub = context.getBean(S3ServiceStub.class);
        s3Stub.resetCounters();
        s3Stub.withData(bucketName, key, object);
    }

    private void configureOutstaffStartrekClient(String issueId, String ticketKey, String ticketSummary,
                                                 boolean issueOpened) {
        var map = new EmptyMap<String, Object>();
        var issue = new Issue(issueId, null, ticketKey, ticketSummary, 1, map, null);

        var startrekClientStub = context.getBean(OutstaffStartrekClientStub.class);
        startrekClientStub.withIssue(issue);
        startrekClientStub.withIssueOpened(issueOpened);
    }

    private void configureOutstaffYqlRepo(String ytTable, String resource, long ytId, long ytUid,
                                          Instant createdDate) throws Exception {
        String json = getStringFromResource(resource);

        List<YtTableDto> testDtos = new ArrayList<>();
        testDtos.add(new YtTableDto(ytId, ytUid, json, createdDate));

        var stub = (OutstaffYqlRepoStub) context.getBean("outstaffYqlRepo");
        stub.withData(ytTable, testDtos);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.empty.tables.csv",
            after = "OutstaffServiceTest.empty.tables.csv")
    public void shouldLoad0RowsInOutstaffTableAndReturn1Warning() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(1, warnings.size());
        assertEquals("Detected employee of unknown company with UID = 326611127, will not process it.",
                warnings.get(0));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.2rows_company_table.csv",
            after = "OutstaffServiceTest.multiplesc.after.csv")
    public void shouldCreateOutstaffInAllScs() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_MULTIPLE_SC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    //Костыль
    @Test
    @DbUnitDataSet(
            before = {"OutstaffServiceTest.2rows_company_table.csv", "OutstaffServiceTest.ekbflag.before.csv"},
            after = "OutstaffServiceTest.ekb_ffc.after.csv")
    public void shouldCreateForEkbFfc() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_EKB_FFC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(
            before = {"OutstaffServiceTest.2rows_company_table.csv", "OutstaffServiceTest.ekbflag.before.csv"},
            after = "OutstaffServiceTest.ekb_sc.after.csv")
    public void shouldCreateForEkbSc() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_EKB_SC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.2rows_company_table.csv",
            after = "OutstaffServiceTest.multipleffc.after.csv")
    public void shouldCreateOutstaffInAllFfcs() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_MULTIPLE_FFC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(
            before = {"OutstaffServiceTest.2rows_company_table.csv",
                    "OutstaffServiceTest.deactivated_duplicate.before.csv"},
            after = "OutstaffServiceTest.update_deactivated.after.csv")
    public void shouldReincarnateOutstaffIfExisted() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_MULTIPLE_FFC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(
            before = {"OutstaffServiceTest.2rows_company_table.csv",
                    "OutstaffServiceTest.deactivated_duplicate_old_position.before.csv"},
            after = "OutstaffServiceTest.update_deactivated_old_position.after.csv")
    public void shouldReincarnateOutstaffIfExistedWithOldPosition() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_MULTIPLE_FFC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(
            before = {"OutstaffServiceTest.2rows_company_table.csv",
                    "OutstaffServiceTest.deactivated_diff_position.before.csv"},
            after = "OutstaffServiceTest.update_deactivated_diff_position.after.csv")
    public void shouldReincarnateOutstaffIfExistedWithNewPosition() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_MULTIPLE_FFC, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.2rows_company_table.csv",
            after = "OutstaffServiceTest.2rows_outstaff_table.csv")
    public void shouldLoad2RowsInOutstaffTableAndReturn0Warning() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.1row_active_outstaff.table.csv",
            after = "OutstaffServiceTest.2rows_outstaff.table.no_error.csv")
    public void shouldLoad1RowInOutstaffTableAndReturnNoDupWarning() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.push_to_timex.before.csv",
            after = "OutstaffServiceTest.push_to_timex.after.csv")
    public void shouldPushToTimexWithoutIncident() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        byte[] photo = getFileFromResource("/inputs/input_photo.jpg");
        configureTimexApiFacade("СЦ Екатеринбург", "test_oid1", TimexOperationStatus.SUCCESS,
                new WorkingArea[]{new WorkingArea(null, null, null,
                        false, "test_working_area", "ABC=")});
        configureYaDiskClient("fake_url", 200, photo);

        timexLoaderService.pushOutstaffToTimex();
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.push_to_timex.before.csv",
            after = "OutstaffServiceTest.push_to_timex.create_ticket.csv")
    public void shouldNotPushToTimexAndCreateIncident() throws Exception {

        // arrange

        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        byte[] photo = getFileFromResource("/inputs/input_photo.jpg");
        configureTimexApiFacade("СЦ Екатеринбург", "test_oid2",
                TimexOperationStatus.PHOTO_BIOMETRY_CHECK_FAILURE,
                new WorkingArea[]{new WorkingArea(null, null, null,
                        false, "test_working_area", "ABC=")});
        configureYaDiskClient("fake_url", 200, photo);
        configureOutstaffStartrekClient("test_id", "TESTQUEUE-1", "test summary", false);

        // act

        timexLoaderService.pushOutstaffToTimex();
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.push_to_timex.before.csv",
            after = "OutstaffServiceTest.didnt_download_photo.csv")
    public void shouldFailToDownloadPhoto() throws Exception {

        // arrange

        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        configureTimexApiFacade("СЦ Екатеринбург", "test_oid2",
                TimexOperationStatus.PHOTO_BIOMETRY_CHECK_FAILURE,
                new WorkingArea[]{new WorkingArea(null, null, null,
                        false, "test_working_area", "ABC=")});
        configureYaDiskClient("fake_url", 404, null);
        configureOutstaffStartrekClient("test_id", "TESTQUEUE-1", "test summary", false);

        // act

        timexLoaderService.pushOutstaffToTimex();
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.1row_outstaff.age_check.before.csv",
            after = "OutstaffServiceTest.1row_outstaff.age_check.after.csv")
    public void shouldNotLoadRowsBecauseOfAgeCheck() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_WRONG_AGE, 129205766,
                326611127, Instant.now());
        configureOutstaffStartrekClient("test_id", "TESTQUEUE-1", "test summary", true);

        var warnings = service.loadOutstaffFromYt();

        assertEquals(1, warnings.size());
        assertEquals(
                "Employee фамилия2 имя2 отч2 did not pass age check. Will not save it in DB. Created ticket " +
                        "TESTQUEUE-1.",
                warnings.get(0));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.upload_to_s3.before.csv",
            after = "OutstaffServiceTest.upload_to_s3.after.csv")
    public void shouldUpdateS3ColumnsInTable() throws Exception {

        // arrange
        var s3Stub = context.getBean(S3ServiceStub.class);
        s3Stub.resetCounters();

        configureYaDiskClient("http://site.ru/direct.utl?other1=1&filename=bla-bla.abc&other2=2",
                200, new byte[]{0, 1, 2});

        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        // assert


        assertEquals(0, warnings.size());

        // 5 раз для Тестович и 1 раз для фамилия2
        assertEquals(6, s3Stub.getPutObjectCalled());
        assertEquals(0, s3Stub.getGetObjectCalled());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.1row_outstaff.table.csv",
            after = "OutstaffServiceTest.add_same_outstaff.after.csv")
    public void shouldLoadNewUserInAdditionToExistingOne() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.1row_active_outstaff.table.csv",
            after = "OutstaffServiceTest.2rows_outstaff.different_areas.csv")
    public void shouldAddNewUserWithSameDbPhoneToAnotherArea() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON_SAME_PHONE_NEW_AREA, 129205766,
                326611127, Instant.now());
        configureOutstaffStartrekClient("test_id", "TESTQUEUE-1", "test summary", true);

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.2rows_company_table.csv",
            after = "OutstaffServiceTest.1new_row_unknown.table.csv")
    public void shouldSaveRecordToUnknownCompaniesTable() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 123,
                321, Instant.parse("2021-07-23T13:28:23.0Z"));

        var warnings = service.loadOutstaffFromYt();

        assertEquals(1, warnings.size());
        assertEquals(
                "Detected employee of unknown company with UID = 321, will not process it.",
                warnings.get(0));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffServiceTest.unknown_company.before.csv",
            after = "OutstaffServiceTest.unknown_company.before.csv")
    public void shouldNotAddDuplicateToUnknownCompanies() throws Exception {
        configureOutstaffYqlRepo(OutstaffFormEnum.CREATE.getYtTablePath(), INPUT_JSON, 129205766,
                326611127, Instant.now());

        var warnings = service.loadOutstaffFromYt();

        assertEquals(0, warnings.size());
    }
}
