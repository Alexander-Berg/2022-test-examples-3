package ru.yandex.market.logshatter;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.fileupload.util.Streams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickhouse.ddl.DdlQueryType;
import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.common.validation.DaoActionValidationException;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.ddl.ManualDDLExecutionResult;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLWorker;
import ru.yandex.market.logshatter.rotation.LogshatterDataRotationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 18/10/2017
 */
public class LogshatterRestControllerTest {

    private static final String CLICKHOUSE_HOST = "clickhouse.host";
    private static final String SUCCESSFUL_DROP_QUERY = "ALTER TABLE market.algebraic DROP COLUMN algebraic";
    private static final String FAILING_DROP_QUERY = "ALTER TABLE market.algebraic DROP COLUMN not_existing_column";
    private static final String HASH = "73c769d534337cfb4df87268e4336ea4";
    private static final int PORT = 42;
    private static final int WARN_STATUS_CODE = 200;
    private final LogShatterMonitoring logShatterMonitoring = new LogShatterMonitoring();
    private final ClickphiteConfigDao clickphiteConfigDao = Mockito.mock(ClickphiteConfigDao.class);
    private MockMvc mockMvc;

    private ConfigurationService configurationService() {
        UpdateDDLWorker updateDDLWorker = mock(UpdateDDLWorker.class);
        DDL successfulDDL = new DDL(
            CLICKHOUSE_HOST,
            new ClickHouseTableDefinitionImpl("market", "algebraic", Collections.emptyList(), null)
        );
        successfulDDL.addManualQuery(new DdlQuery(DdlQueryType.DROP_COLUMN, SUCCESSFUL_DROP_QUERY));
        DDL failingDDL = new DDL(
            CLICKHOUSE_HOST,
            new ClickHouseTableDefinitionImpl("market", "algebraic", Collections.emptyList(), null)
        );
        failingDDL.addManualQuery(new DdlQuery(DdlQueryType.DROP_COLUMN, FAILING_DROP_QUERY));
        List<DDL> manualDDLs = Arrays.asList(successfulDDL, failingDDL);
        when(updateDDLWorker.getManualDDLs()).thenReturn(manualDDLs);
        when(updateDDLWorker.executeManualDDLs(manualDDLs))
            .thenReturn(new ManualDDLExecutionResult(
                Collections.singletonList(successfulDDL), Collections.singletonList(failingDDL)
            ));

        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getDDLWorker()).thenReturn(updateDDLWorker);
        return configurationService;
    }

    private LogshatterDataRotationService dataRotationService() {

        final Map<String, Set<String>> obsoletePartitions =
            ImmutableMap.of(
                "market.table1", Collections.singleton("201601"),
                "tsum.table2", Collections.singleton("201602")
            );


        final LogshatterDataRotationService dataRotationServiceMock =
            Mockito.mock(LogshatterDataRotationService.class);

        Mockito.when(dataRotationServiceMock.getObsoletePartitions()).thenReturn(obsoletePartitions);

        return dataRotationServiceMock;
    }

    @Before
    public void setUp() {
        LogshatterRestController controller = new LogshatterRestController(
            logShatterMonitoring, configurationService(), dataRotationService(), null, null, clickphiteConfigDao, null,
            PORT, WARN_STATUS_CODE, true
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getManualDdl() throws Exception {
        String expectedJson = Streams.asString(getClass().getResourceAsStream("getManualDdl.json"));
        expectedJson = expectedJson.replace("LOCALHOST", InetAddress.getLocalHost().getCanonicalHostName());

        mockMvc.perform(get("/getManualDDL"))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson, true));
    }

    @Test
    public void applyManualDdlWrongRequest() throws Exception {

        mockMvc.perform(get("/applyManualDDL"))
            .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/applyManualDDL"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/applyManualDDL").param("hash", "invalid_hash"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void applyManualDdl() throws Exception {
        String expectedJson = Streams.asString(getClass().getResourceAsStream("applyManualDdl.json"));
        String clickphiteConfigId = "test666";
        long clickphiteConfigVersion = 666;

        Mockito.when(clickphiteConfigDao.getActiveConfigsByTable(any())).thenReturn(Collections.singletonList(
            new ClickphiteConfigGroupEntity(
                clickphiteConfigId, null, null, null, null, null,
                new ClickphiteConfigGroupVersionEntity(
                    new VersionedConfigEntity.VersionEntity.Id(clickphiteConfigId, clickphiteConfigVersion),
                    VersionedConfigSource.UI,
                    VersionStatus.PUBLIC,
                    null,
                    null,
                    null
                ),
                null
            )
        ));

        Mockito.doThrow(new DaoActionValidationException(Collections.emptyMap())).when(clickphiteConfigDao)
            .validateCreateVersion(
                new VersionedConfigEntity.VersionEntity.Id(clickphiteConfigId, clickphiteConfigVersion)
            );

        mockMvc.perform(post("/applyManualDDL").param("hash", HASH))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson, false));
    }

    @Test
    public void monitoringHostCritical() throws Exception {
        mockMvc.perform(get("/monitoringHostCritical"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));

        logShatterMonitoring.getHostCritical().addTemporaryWarning("Minor", "Фигня", 42, TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringHostCritical"))
            .andExpect(status().isOk())
            .andExpect(content().string("1;WARN {Minor: Фигня}"));

        logShatterMonitoring.getHostCritical().addTemporaryCritical("Important", "А вот это плохо", 42,
            TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringHostCritical"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("2;CRIT {Important: А вот это плохо} WARN {Minor: Фигня}"));
    }

    @Test
    public void monitoringClusterCritical() throws Exception {
        mockMvc.perform(get("/monitoringClusterCritical"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));

        logShatterMonitoring.getClusterCritical().addTemporaryWarning("Minor", "Фигня", 42, TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringClusterCritical"))
            .andExpect(status().isOk())
            .andExpect(content().string("1;WARN {Minor: Фигня}"));

        logShatterMonitoring.getClusterCritical().addTemporaryCritical("Important", "А вот это плохо", 42,
            TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringClusterCritical"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("2;CRIT {Important: А вот это плохо} WARN {Minor: Фигня}"));
    }

    @Test
    public void getDropPartitionsScripts() throws Exception {

        String expectedJson = Streams.asString(getClass().getResourceAsStream("dataRotation.json"));
        mockMvc.perform(
                get("/obsoletePartitions").param("hash", HASH)
            )
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson, false));
    }

    @Test
    public void verifyDeleteRotationMethodCallOnRequest() throws Exception {

        LogshatterDataRotationService dataRotationServiceMock = mock(LogshatterDataRotationService.class);
        LogshatterRestController controller = new LogshatterRestController(
            Mockito.mock(LogShatterMonitoring.class),
            Mockito.mock(ConfigurationService.class),
            dataRotationServiceMock,
            null,
            null,
            null,
            null,
            PORT,
            WARN_STATUS_CODE,
            true
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc
            .perform(delete("/obsoletePartitions"))
            .andExpect(status().isOk());

        Mockito.verify(dataRotationServiceMock, times(1)).deleteObsoletePartitions();
    }

    @Test
    public void verifyWarnStatusCode() throws Exception {
        LogshatterRestController controller = new LogshatterRestController(
            logShatterMonitoring, configurationService(), dataRotationService(), null, null, null, null, PORT, 400, true
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        logShatterMonitoring.getClusterCritical().addTemporaryWarning("Minor", "Фигня", 42, TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringClusterCritical"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("1;WARN {Minor: Фигня}"));
    }
}
