package ru.yandex.market.hrms.tms.manager.oebs;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.OebsSyncManager;

@DbUnitDataSet(before = "GetDepartmentInfoSyncManagerTest.before.csv")
public class GetDepartmentInfoSyncManagerTest extends AbstractTmsTest {
    private static final String DEP_INFO_JSON_PATH_SOF_11_01 = "/results/oebs/get_department_info_20211101_sof.json";
    private static final String DEP_INFO_JSON_PATH_TML_11_01 = "/results/oebs/get_department_info_20211101_tml.json";
    private static final String DEP_INFO_JSON_PATH_SOF_11_02 = "/results/oebs/get_department_info_20211102_1_sof.json";
    private static final String DEP_INFO_JSON_PATH_TML_11_02 = "/results/oebs/get_department_info_20211102_1_tml.json";
    private static final String DEP_INFO_JSON_PATH_SOF_1101_1 = "/results/oebs/get_department_info_20211101_1_sof.json";
    private static final String DEP_INFO_JSON_PATH_TML_1101_1 = "/results/oebs/get_department_info_20211101_1_tml.json";
    private static final String LOST_MAPPING_PATH_SOF_11_01 =
            "/results/oebs/get_department_info_lost_20211101_sof.json";
    private static final String MAPPING_SIMILARITY_PATH_SOF_11_01 =
            "/results/oebs/get_department_info_mapping_similarity_sof.json";
    private static final String DEP_INFO_JSON_PATH_EMPTY = "/results/oebs/empty_response.json";


    private static final String SOF = "ОП ООО Маркет.Операции МО Софьино";
    private static final String TML = "ОП ООО Маркет.Операции МО Томилино";

    @Autowired
    private OebsSyncManager oebsSyncManager;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(after = "GetDepartmentInfoSyncManagerTest.after.csv")
    @Test
    void shouldRunSync() {
        mockClock(LocalDate.of(2021, 11, 1));

        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_SOF_11_01, SOF, "2021-10-31");
        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_TML_11_01, TML, "2021-10-31");

        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_SOF_11_01, SOF, "2021-11-01");
        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_TML_11_01, TML, "2021-11-01");

        oebsSyncManager.syncCurrentMonthDepartmentInfo();
        oebsSyncManager.syncPastMonthDepartmentInfo();
        oebsSyncManager.syncFutureMonthDepartmentInfo();
    }

    @DbUnitDataSet(
            before = "GetDepartmentInfoSyncManagerTest.update.before.csv",
            after = "GetDepartmentInfoSyncManagerTest.update.after.csv")
    @Test
    void shouldUpdateRows() {
        mockClock(LocalDate.of(2021, 11, 2));

        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_SOF_1101_1, SOF, "2021-11-01");
        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_TML_1101_1, TML, "2021-11-01");

        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_SOF_11_02, SOF, "2021-11-02");
        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_TML_11_02, TML, "2021-11-02");

        oebsSyncManager.syncCurrentMonthDepartmentInfo();
        oebsSyncManager.syncPastMonthDepartmentInfo();
        oebsSyncManager.syncFutureMonthDepartmentInfo();
    }


    @DbUnitDataSet(
            before = "GetDepartmentInfoSyncManagerTest.close_old.before.csv",
            after = "GetDepartmentInfoSyncManagerTest.close_old.before.csv")
    @Test
    void shouldNotCloseRowsBecauseOfTimeout() {
        mockClock(LocalDate.of(2021, 11, 1));

        oebsSyncManager.syncCurrentMonthDepartmentInfo();
        oebsSyncManager.syncPastMonthDepartmentInfo();
        oebsSyncManager.syncFutureMonthDepartmentInfo();
    }

    @DbUnitDataSet(
            before = "GetDepartmentInfoSyncManagerTest.close_old.before.csv",
            after = "GetDepartmentInfoSyncManagerTest.close_old.after.csv")
    @Test
    void shouldCloseOldRows() {
        mockClock(LocalDate.of(2021, 11, 1));

        oebsApiConfigurer.mockGetDepartmentInfo(DEP_INFO_JSON_PATH_EMPTY, SOF, "2021-10-20");

        oebsSyncManager.syncCurrentMonthDepartmentInfo();
        oebsSyncManager.syncPastMonthDepartmentInfo();
        oebsSyncManager.syncFutureMonthDepartmentInfo();
    }

    @DbUnitDataSet(before = "GetDepartmentInfoSyncManagerTest.LostMappings.before.csv",
            after = "GetDepartmentInfoSyncManagerTest.LostMappings.after.csv")
    @Test
    void shouldRunSyncWithLostMappings() {
        mockClock(LocalDate.of(2021, 11, 1));
        oebsApiConfigurer.mockGetDepartmentInfo(LOST_MAPPING_PATH_SOF_11_01, SOF, "2021-11-01");

        List<String> errors = oebsSyncManager.syncCurrentMonthDepartmentInfo();

        Assertions.assertTrue(errors.size() > 0);
        Assertions.assertTrue(errors.contains("Found new lost mappings! Please check employee_oebs_lost_mapping table!"
                + " Logins: [m2bm2b, notprimary]"));
    }

    @DbUnitDataSet(after = "GetDepartmentInfoSyncManagerTest.MappingSimilarity.after.csv")
    @Test
    void checkSimilarityNames() {
        mockClock(LocalDate.of(2021, 11, 1));
        oebsApiConfigurer.mockGetDepartmentInfo(MAPPING_SIMILARITY_PATH_SOF_11_01, SOF, "2021-11-01");
        oebsSyncManager.syncCurrentMonthDepartmentInfo();
    }
}
