package ru.yandex.market.logshatter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.fileupload.util.Streams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickhouse.ddl.DdlQueryType;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.ddl.ManualDDLExecutionResult;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLWorker;
import ru.yandex.market.logshatter.rotation.DataRotationService;
import ru.yandex.market.logshatter.rotation.ObsoletePartition;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    private MockMvc mockMvc;
    private final LogShatterMonitoring logShatterMonitoring = new LogShatterMonitoring();

    private ConfigurationService configurationService() {
        UpdateDDLWorker updateDDLWorker = mock(UpdateDDLWorker.class);
        DDL successfulDDL = new DDL(CLICKHOUSE_HOST);
        successfulDDL.addManualQuery(new DdlQuery(DdlQueryType.DROP_COLUMN, SUCCESSFUL_DROP_QUERY));
        DDL failingDDL = new DDL(CLICKHOUSE_HOST);
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

    private DataRotationService dataRotationService() {
        final Multimap<String, ObsoletePartition> obsoletePartitions = ArrayListMultimap.create();
        obsoletePartitions.put("host1", new ObsoletePartition("host1", "market.table1", "201601"));
        obsoletePartitions.put("host1", new ObsoletePartition("host1", "tsum.table2", "201602"));
        obsoletePartitions.put("host2", new ObsoletePartition("host2", "market.table1", "201601"));

        final DataRotationService dataRotationServiceMock = Mockito.mock(DataRotationService.class);
        Mockito.when(dataRotationServiceMock.getObsoletePartitionsByHostMap()).thenReturn(obsoletePartitions);
        return dataRotationServiceMock;
    }

    @Before
    public void setUp() throws Exception {
        LogshatterRestController controller = new LogshatterRestController(
            logShatterMonitoring, configurationService(), dataRotationService(), null, PORT
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

        logShatterMonitoring.getHostCritical().addTemporaryCritical("Important", "А вот это плохо", 42, TimeUnit.SECONDS);

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

        logShatterMonitoring.getClusterCritical().addTemporaryCritical("Important", "А вот это плохо", 42, TimeUnit.SECONDS);

        mockMvc.perform(get("/monitoringClusterCritical"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("2;CRIT {Important: А вот это плохо} WARN {Minor: Фигня}"));
    }

    @Test
    public void getDropPartitionsScripts() throws Exception {

        String expectedJson = Streams.asString(getClass().getResourceAsStream("dataRotation.json"));
        mockMvc.perform(post("/getDropPartitionsScripts").param("hash", HASH))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson, false));
    }

}