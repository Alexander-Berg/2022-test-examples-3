package ru.yandex.direct.jobs.cashback;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.converters.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardsImportParams;
import ru.yandex.direct.core.entity.cashback.service.ImportCashbackRewardsService;
import ru.yandex.direct.jobs.abt.check.TableExistsChecker;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.db.PpcPropertyNames.IMPORT_CASHBACK_REWARDS_DETAILS_ENABLED;
import static ru.yandex.direct.common.db.PpcPropertyNames.LAST_IMPORTED_CASHBACK_TABLE;
import static ru.yandex.direct.ytwrapper.YtPathUtil.generatePath;
import static ru.yandex.direct.ytwrapper.model.YtCluster.ARNOLD;
import static ru.yandex.direct.ytwrapper.model.YtCluster.HAHN;

@ParametersAreNonnullByDefault
class ImportCashbackRewardsDetailsJobTest {
    private static final String BASE_PATH = "//my/base/path";
    private static final LocalDate AUGUST = LocalDate.of(2020, 8, 1);
    private static final LocalDate SEPTEMBER = LocalDate.of(2020, 9, 1);
    private static final LocalDate OCTOBER = LocalDate.of(2020, 10, 1);
    private static final LocalDate NOVEMBER = LocalDate.of(2020, 11, 1);

    private static final String AUGUST_TABLE = "202008";
    private static final String SEPTEMBER_TABLE = "202009";
    private static final String SEPTEMBER_ADD1_TABLE = "202009_1";
    private static final String SEPTEMBER_ADD2_TABLE = "202009_2";
    private static final String SEPTEMBER_ADD3_TABLE = "202009_3";
    private static final String OCTOBER_TABLE = "202010";
    private static final String NOVEMBER_TABLE = "202011";


    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private TableExistsChecker tableExistsChecker;

    @Mock
    private ImportCashbackRewardsService service;

    @Captor
    private ArgumentCaptor<List<CashbackRewardsImportParams>> paramsCaptor;

    private ImportCashbackRewardsDetailsJob job;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);

        var commonConfig = mock(DirectConfig.class);
        var branchConfig = mock(DirectConfig.class);
        doReturn(branchConfig).when(commonConfig).getBranch("cashbacks.yt");
        doReturn(List.of("hahn", "arnold")).when(branchConfig).getStringList("clusters");
        doReturn(BASE_PATH).when(branchConfig).getString("rewards_table_base_path");

        var jobEnabledProperty = mock(PpcProperty.class);
        doReturn(true).when(jobEnabledProperty).get();
        //noinspection unchecked
        doCallRealMethod().when(jobEnabledProperty).getOrDefault(any());
        doReturn(jobEnabledProperty).when(ppcPropertiesSupport).get(IMPORT_CASHBACK_REWARDS_DETAILS_ENABLED);

        job = new ImportCashbackRewardsDetailsJob(tableExistsChecker, ppcPropertiesSupport, service, commonConfig);
    }

    static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "No previous import, no data to import", null, Map.of(), List.of()
                },

                {
                        "No previous import, single table to import",
                        null,
                        Map.of(HAHN, List.of(path(AUGUST_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                .withCluster(HAHN)
                                .withDate(AUGUST)
                                .withTablePath(path(AUGUST_TABLE))))
                },

                {
                        "No new tables",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(AUGUST_TABLE))),
                        List.of()
                },

                {
                        "Single new table",
                        SEPTEMBER_TABLE,
                        Map.of(HAHN, List.of(path(OCTOBER_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                .withCluster(HAHN)
                                .withDate(OCTOBER)
                                .withTablePath(path(OCTOBER_TABLE))))
                },

                {
                        "Several tables to import",
                        SEPTEMBER_TABLE,
                        Map.of(HAHN, List.of(path(OCTOBER_TABLE), path(NOVEMBER_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(OCTOBER)
                                        .withTablePath(path(OCTOBER_TABLE))),
                                List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(NOVEMBER)
                                        .withTablePath(path(NOVEMBER_TABLE))))
                },

                {
                        "Several table to import, not in row",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(NOVEMBER_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE))),
                                List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(NOVEMBER)
                                        .withTablePath(path(NOVEMBER_TABLE))))
                },

                {
                        "Two tables on different clusters",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE)),
                                ARNOLD, List.of(path(OCTOBER_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE))),
                                List.of(new CashbackRewardsImportParams()
                                        .withCluster(ARNOLD)
                                        .withDate(OCTOBER)
                                        .withTablePath(path(OCTOBER_TABLE))))
                },

                {
                        "Table exist on both clusters, only first will be chosen",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(OCTOBER_TABLE)),
                                ARNOLD, List.of(path(OCTOBER_TABLE))),
                        List.of(List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE))),
                                List.of(new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(OCTOBER)
                                        .withTablePath(path(OCTOBER_TABLE))))
                },

                {
                        "One addition table",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE))),
                        List.of(List.of(
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD1_TABLE))))
                },

                {
                        "Several addition tables",
                        AUGUST_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE),
                                path(SEPTEMBER_ADD2_TABLE), path(SEPTEMBER_ADD3_TABLE))),
                        List.of(List.of(
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD1_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD2_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD3_TABLE))))
                },

                {
                        "Reimport month",
                        SEPTEMBER_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE))),
                        List.of(List.of(
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD1_TABLE))))
                },

                {
                        "Reimport month with additional table",
                        SEPTEMBER_ADD1_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE), path(SEPTEMBER_ADD2_TABLE))),
                        List.of(List.of(
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD1_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD2_TABLE))))
                },

                {
                        "Reimport month and new month",
                        SEPTEMBER_ADD1_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE), path(SEPTEMBER_ADD2_TABLE), path(OCTOBER_TABLE))),
                        List.of(List.of(
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD1_TABLE)),
                                new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(SEPTEMBER)
                                        .withTablePath(path(SEPTEMBER_ADD2_TABLE))),
                                List.of( new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(OCTOBER)
                                        .withTablePath(path(OCTOBER_TABLE))))
                },

                {
                        "Not reimport month",
                        SEPTEMBER_ADD1_TABLE,
                        Map.of(HAHN, List.of(path(SEPTEMBER_TABLE), path(SEPTEMBER_ADD1_TABLE), path(OCTOBER_TABLE))),
                        List.of(List.of( new CashbackRewardsImportParams()
                                        .withCluster(HAHN)
                                        .withDate(OCTOBER)
                                        .withTablePath(path(OCTOBER_TABLE))))
                }
        });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parameters")
    void testTablesDetection(String description,
                             @Nullable String lastImportedTableName,
                             Map<YtCluster, List<String>> existingTables,
                             List<List<CashbackRewardsImportParams>> expectedImportedParams) {
        var ppcProperty = mock(PpcProperty.class);
        doReturn(lastImportedTableName).when(ppcProperty).get();
        //noinspection unchecked
        doCallRealMethod().when(ppcProperty).getOrDefault(any());
        doReturn(ppcProperty).when(ppcPropertiesSupport).get(LAST_IMPORTED_CASHBACK_TABLE);

        doAnswer(invocation -> {
            var cluster = (YtCluster) invocation.getArgument(0);
            var path = (String) invocation.getArgument(1);
            return existingTables.containsKey(cluster) && existingTables.get(cluster).contains(path);
        }).when(tableExistsChecker).check(any(), any());

        job.execute();

        verify(service, times(expectedImportedParams.size())).importRewardsTables(paramsCaptor.capture());
        var params = paramsCaptor.getAllValues();
        assertThat(params).hasSize(expectedImportedParams.size());


        params.forEach(param -> assertThat(expectedImportedParams).contains(param));
    }

    private static String path(String tableName) {
        return generatePath(BASE_PATH, tableName);
    }
}
