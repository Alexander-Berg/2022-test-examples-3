package ru.yandex.market.olap2.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.olap2.dao.LoadStatus;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.model.SlaCube;
import ru.yandex.market.olap2.model.SlaCubesHolder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.config.SlaMonitoringConfig.DEFAULT_SLA_HOUR;
import static ru.yandex.market.olap2.controller.MonitorLoadsController.JUGGLER_TO_NORMAL;
import static ru.yandex.market.olap2.controller.MonitorLoadsController.MONITORING_START_CRIT_HOUR;
import static ru.yandex.market.olap2.controller.MonitorLoadsController.MONITORING_START_WARN_HOUR;

@RunWith(MockitoJUnitRunner.class)
public class MonitorLoadsControllerTest {

    private static final String YT_PATH = "//tmp/cube_path";
    private static final Set<String> CUBES = new HashSet<>(
            Arrays.asList("cube_ok",
                    "cube_still_loading",
                    "cube_firstload",
                    "cube_rejected",
                    "cube_failing",
                    "cube_long"
            ));
    private static final String OK_STATUS = "OK";
    private static final String LOADING_INTO_CH = "LOADING[ch]:LOADING_INTO_CH";
    private static final String LOADING_INTO_CH_MORE_THAN_HOUR = "LOADING[ch] 1h+:LOADING_INTO_CH";
    private static final String LOADING_INTO_VERICA = "LOADING[v]:COPYING_INTO_VERTICA";
    private static final String REJECTED = "REJECTED";
    private static final String FAILURE_RETRYING = "FAILING[ch] 1h+:FAILURE, retry:12";
    private static final String LOADING_TOO_LONG = "LOADING[ch] too long:LOADING_INTO_CH";
    private static final String NO_LOADS_STATUS = "NO_LOADS";

    private MonitorLoadsController controller;

    @Mock
    private MetadataDao metadataDao;


    @Test
    //все выгрузки ок
    public void testSlaHourOK() {
        initFor(successfulLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR, false);
        verify(metadataDao).getSlaErrors(CUBES, YT_PATH);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //выгрузки падают, а уже sla
    public void testFailingLoadsAfterSlaCrit() {
        initFor(someFailingLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR, false);
        checkMsgForErrorsAndNoLoads(result, JugglerConstants.CRIT);
    }

    @Test
    //выгрузки падают, ещё не sla, но уже не тихий период
    public void testFailingLoadsBeforeSlaCrit() {
        initFor(someFailingLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(MONITORING_START_CRIT_HOUR, false);
        checkMsgForErrorsAndNoLoads(result, JugglerConstants.CRIT);
    }

    @Test
    //выгрузки падают, ещё не sla, но еще только WARN период
    public void testFailingLoadsBeforeSlaWarn() {
        initFor(someFailingLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(MONITORING_START_WARN_HOUR, false);
        checkMsgForErrors(result, JugglerConstants.WARN);
    }

    @Test
    //выгрузки падают, но ещё тихий период
    public void testSleepHourOk() {
        initFor(someFailingLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(MONITORING_START_WARN_HOUR - 1, false);
        checkIsOk(result.getBody());
    }

    @Test
    //выгрузки в процессе, а уже sla
    public void testLoadingAfterSlaWarn() {
        initFor(someOkSomeLoading(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR, false);
        checkMsgLoading(result, JugglerConstants.WARN);
    }

    @Test
    //выгрузки в процессе, но ещё не sla
    public void testLoadingBeforeSlaOk() {
        initFor(someOkSomeLoading(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR - 1, false);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //нет выгрузок, до sla больше часа
    public void testOkForNoLoadsWhenHaveTime() {
        initFor(someOkSomeNoLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR - 2, false);
        assertThat(result.getBody(), is(JugglerConstants.OK));
    }

    @Test
    //нет выгрузок, до sla меньше часа
    public void testWarnForNoLoadsWhen1hLeft() {
        initFor(someOkSomeNoLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR - 1, false);
        checkMsgNoLoads(result, JugglerConstants.WARN);
    }

    @Test
    //проверка, что криты выводятся первыми
    public void testSortingByWorstStatus() {
        initFor(someCritSomeWarnLoads(), defaultMonitoringConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR, false);
        assertThat(result.getBody(), startsWith(JugglerConstants.CRIT + JUGGLER_TO_NORMAL.get(JugglerConstants.CRIT)));
    }

    @Test
    public void testSlaHourOnlyWarn() {
        initFor(someFailingLoads(), onlyWarningMonitoringsConf());
        ResponseEntity<String> result = controller.getSlaErrors(DEFAULT_SLA_HOUR, false);
        checkMsgForErrorsAndNoLoads(result, JugglerConstants.WARN);
    }

    private void initFor(List<LoadStatus> loadStatuses, SlaCubesHolder slaCubesHolder) {
        when(metadataDao.getSlaErrors(CUBES, YT_PATH)).thenReturn(loadStatuses);
        initFor(slaCubesHolder);
    }

    private void initFor(SlaCubesHolder slaCubesHolder) {
        controller = new MonitorLoadsController(metadataDao, slaCubesHolder, "hahn", YT_PATH);
    }

    private SlaCubesHolder defaultMonitoringConf() {
        return new SlaCubesHolder(slaConfig(false));
    }

    private SlaCubesHolder onlyWarningMonitoringsConf() {
        return new SlaCubesHolder(slaConfig(true));
    }


    private Map<String, SlaCube> slaConfig(boolean onlyWarning) {
        Map<String, SlaCube> params = new HashMap<>();
        params.put("cube_ok",
                new SlaCube("cube_ok", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, false));
        params.put("cube_still_loading",
                new SlaCube("cube_still_loading", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, false));
        params.put("cube_firstload", new
                SlaCube("cube_firstload", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, onlyWarning));
        params.put("cube_rejected",
                new SlaCube("cube_rejected", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, onlyWarning));
        params.put("cube_failing",
                new SlaCube("cube_failing", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, onlyWarning));
        params.put("cube_long",
                new SlaCube("cube_long", DEFAULT_SLA_HOUR, DEFAULT_SLA_HOUR + 3, onlyWarning));


        return params;
    }

    private List<LoadStatus> successfulLoads() {
        return Arrays.asList(
                new LoadStatus("cube_ok", withStatus(OK_STATUS)),
                new LoadStatus("cube_still_loading", withStatus(OK_STATUS)),
                new LoadStatus("cube_rejected", withStatus(OK_STATUS)),
                new LoadStatus("cube_firstload", withStatus(OK_STATUS)),
                new LoadStatus("cube_failing", withStatus(OK_STATUS)),
                new LoadStatus("cube_long", withStatus(OK_STATUS))
        );
    }

    private List<LoadStatus> someOkSomeLoading() {
        return Arrays.asList(
                new LoadStatus("cube_ok", withStatus(OK_STATUS)),
                new LoadStatus("cube_still_loading", withStatus(LOADING_INTO_CH)),
                new LoadStatus("cube_rejected", withStatus(OK_STATUS)),
                new LoadStatus("cube_firstload", withStatus(OK_STATUS)),
                new LoadStatus("cube_failing", withStatus(LOADING_INTO_CH_MORE_THAN_HOUR)),
                new LoadStatus("cube_long", withStatus(LOADING_INTO_VERICA))
        );
    }

    private List<LoadStatus> someFailingLoads() {
        return Arrays.asList(
                new LoadStatus("cube_ok", withStatus(OK_STATUS)),
                new LoadStatus("cube_still_loading", withStatus(LOADING_INTO_CH)),
                new LoadStatus("cube_rejected", withStatus(REJECTED)),
                new LoadStatus("cube_failing", withStatus(FAILURE_RETRYING)),
                new LoadStatus("cube_long", withStatus(LOADING_TOO_LONG))
        );
    }

    private List<LoadStatus> someCritSomeWarnLoads() {
        return Arrays.asList(
                new LoadStatus("cube_ok", withStatus(LOADING_INTO_VERICA)),
                new LoadStatus("cube_still_loading", withStatus(LOADING_INTO_CH)),
                new LoadStatus("cube_rejected", withStatus(REJECTED)),
                new LoadStatus("cube_firstload", withStatus(LOADING_INTO_CH_MORE_THAN_HOUR)),
                new LoadStatus("cube_failing", withStatus(FAILURE_RETRYING)),
                new LoadStatus("cube_long", withStatus(LOADING_TOO_LONG))
        );
    }

    private List<LoadStatus> someOkSomeNoLoads() {
        return Arrays.asList(
                new LoadStatus("cube_ok", withStatus(OK_STATUS)),
                new LoadStatus("cube_still_loading", withStatus(OK_STATUS)),
                new LoadStatus("cube_rejected", withStatus(OK_STATUS)),
                new LoadStatus("cube_failing", withStatus(OK_STATUS))
        );
    }

    private void checkIsOk(String result) {
        assertThat(result, startsWith(JugglerConstants.OK));
        assertThat(result, not(containsString("cube_still_loading:" + LOADING_INTO_CH)));
        assertThat(result, not(containsString("cube_rejected:" + REJECTED)));
        assertThat(result, not(containsString("cube_firstload:" + NO_LOADS_STATUS)));
        assertThat(result, not(containsString("cube_ok")));
    }

    private void checkMsgForErrors(ResponseEntity<String> result, String status) {
        assertThat(result.getBody(), startsWith(status + JUGGLER_TO_NORMAL.get(status)));
        assertThat(result.getBody(), containsString("cube_rejected:" + REJECTED));
        assertThat(result.getBody(), containsString("cube_failing:" + FAILURE_RETRYING));
        assertThat(result.getBody(), containsString("cube_long:" + LOADING_TOO_LONG));
        assertThat(result.getBody(), not(containsString("cube_ok")));
    }

    private void checkMsgForErrorsAndNoLoads(ResponseEntity<String> result, String status) {
        checkMsgForErrors(result,status);
        assertThat(result.getBody(), containsString("cube_firstload:" + NO_LOADS_STATUS));
    }

    private void checkMsgLoading(ResponseEntity<String> result, String status) {
        assertThat(result.getBody(), startsWith(status));
        assertThat(result.getBody(), containsString("cube_still_loading:" + LOADING_INTO_CH));
        assertThat(result.getBody(), containsString("cube_failing:" + LOADING_INTO_CH_MORE_THAN_HOUR));
        assertThat(result.getBody(), containsString("cube_long:" + LOADING_INTO_VERICA));

        assertThat(result.getBody(), not(containsString("cube_rejected")));
        assertThat(result.getBody(), not(containsString("cube_firstload")));
        assertThat(result.getBody(), not(containsString("cube_ok")));
    }

    private void checkMsgNoLoads(ResponseEntity<String> result, String status) {
        assertThat(result.getBody(), startsWith(status));
        assertThat(result.getBody(), containsString("cube_firstload:" + NO_LOADS_STATUS));
        assertThat(result.getBody(), containsString("cube_long:" + NO_LOADS_STATUS));
        assertThat(result.getBody(), not(containsString("cube_still_loading:")));
        assertThat(result.getBody(), not(containsString("cube_rejected")));
        assertThat(result.getBody(), not(containsString("cube_failing:")));
        assertThat(result.getBody(), not(containsString("cube_ok")));
    }

    private String withStatus(String baseMsg) {
        switch (baseMsg) {
            case OK_STATUS:
                return "0;" + baseMsg;
            case NO_LOADS_STATUS:
                return "4;" + baseMsg;
            case FAILURE_RETRYING:
            case REJECTED:
            case LOADING_TOO_LONG:
                return "3;" + baseMsg;
            default:
                return "1;" + baseMsg;

        }
    }
}
