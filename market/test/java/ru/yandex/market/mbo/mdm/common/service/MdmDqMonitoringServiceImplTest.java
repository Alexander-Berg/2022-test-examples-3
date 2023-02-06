package ru.yandex.market.mbo.mdm.common.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmMonitoringResult;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmDqMonitoringService;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmDqMonitoringServiceImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;


/**
 * @author albina-gima
 * @date 6/14/21
 */
public class MdmDqMonitoringServiceImplTest extends MdmBaseDbTestClass {
    private static final String CREATION_TIME_FIELD_DATA = "creation_time";
    private static final String ROW_COUNT_FIELD_DATA = "row_count";
    private static final String CYPRESS_TYPE_ATTRIBUTE = "type";
    private static final String CYPRESS_TABLE_TYPE = "table";
    private static final List<String> SOURCES = List.of("ssku", "msku", "category", "wms", "dictionary", "error");

    private static final String FRESH_CHECK_RESULT_DATE = OffsetDateTime.now().toString();
    private static final String NON_FRESH_CHECK_RESULT_DATE = OffsetDateTime.now().minusHours(13).toString();
    private static final String OUTDATED_CHECK_RESULT_DATE = "2021-06-12T17:02:12.873269Z";

    private static final String PATH = "//home/some/path";

    @Autowired
    StorageKeyValueService keyValueService;

    private MdmDqMonitoringService monitoringService;
    private Yt ytMock;
    private YTreeNode yTreeNode;

    @Before
    public void setup() {
        ytMock = Mockito.mock(Yt.class);
        var ytUnstableInit = UnstableInit.simple(ytMock);
        monitoringService = new MdmDqMonitoringServiceImpl(ytUnstableInit, keyValueService);

        keyValueService.putValue(MdmProperties.DQ_YT_PATH_TO_ALL_CHECKS_AND_DICTS, PATH);
        keyValueService.putValue(MdmProperties.DQ_CHECK_SOURCES, SOURCES);

        //mock yt
        UnstableInit<Yt> ytMockUnstable = Mockito.mock(UnstableInit.class);
        Mockito.when(ytMockUnstable.get()).thenReturn(ytMock);

        Cypress cypressMock = Mockito.mock(Cypress.class);
        Mockito.when(ytMockUnstable.get().cypress()).thenReturn(cypressMock);

        yTreeNode = Mockito.mock(YTreeNode.class);
        for (String source : SOURCES) {
            Mockito.when(ytMockUnstable.get().cypress()
                .get(YPath.simple(PATH + "/" + source + "/latest"), List.of(CREATION_TIME_FIELD_DATA)))
                .thenReturn(yTreeNode);
            Mockito.when(ytMockUnstable.get().cypress()
                .get(YPath.simple(PATH + "/" + source + "/latest"), List.of(ROW_COUNT_FIELD_DATA)))
                .thenReturn(yTreeNode);
        }

        Optional<YTreeNode> creationTimeField = Optional.of(Mockito.mock(YTreeNode.class));
        Mockito.when(yTreeNode.getAttribute(CREATION_TIME_FIELD_DATA)).thenReturn(creationTimeField);

        Optional<YTreeNode> rowCountField = Optional.of(Mockito.mock(YTreeNode.class));
        Mockito.when(yTreeNode.getAttribute(ROW_COUNT_FIELD_DATA)).thenReturn(rowCountField);
    }

    @Test
    public void whenRequestFreshChecksLoadedToYtShouldReturnOkAnswer() {
        Mockito.when(yTreeNode.getAttribute(CREATION_TIME_FIELD_DATA).get().stringValue())
            .thenReturn(FRESH_CHECK_RESULT_DATE);

        MdmMonitoringResult monitoringResult = monitoringService.getFreshChecksLoadedToYtMonitoringResult();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void whenRequestFreshChecksLoadedToYtShouldReturnWarnAnswer() {
        Mockito.when(yTreeNode.getAttribute(CREATION_TIME_FIELD_DATA).get().stringValue())
            .thenReturn(NON_FRESH_CHECK_RESULT_DATE);

        MdmMonitoringResult monitoringResult = monitoringService.getFreshChecksLoadedToYtMonitoringResult();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.WARN);
    }

    @Test
    public void whenRequestFreshChecksLoadedToYtShouldReturnCritAnswer() {
        Mockito.when(yTreeNode.getAttribute(CREATION_TIME_FIELD_DATA).get().stringValue())
            .thenReturn(OUTDATED_CHECK_RESULT_DATE);

        MdmMonitoringResult monitoringResult = monitoringService.getFreshChecksLoadedToYtMonitoringResult();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRIT);
    }

    @Test
    public void whenUsualNumberOfRowsShouldReturnOkAnswer() {
        // given
        for (String source : SOURCES) {
            String dirPath = PATH + "/" + source;
            Mockito.when(ytMock.cypress()
                    .list(YPath.simple(dirPath),
                        List.of(ROW_COUNT_FIELD_DATA, CREATION_TIME_FIELD_DATA, CYPRESS_TYPE_ATTRIBUTE)))
                .thenReturn(List.of(
                    new YTreeStringNodeImpl(
                        dirPath + "/1",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 100L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-13T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    ),
                    new YTreeStringNodeImpl(
                        dirPath + "/2",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 100L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-14T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    ),
                    new YTreeStringNodeImpl(
                        dirPath + "/3",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 100L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-15T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    )
                ));
        }

        // when
        MdmMonitoringResult monitoringResult = monitoringService.getFreshChecksHaveEnoughRowsInYtMonitoringResult();

        //then
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void whenUnusualNumberOfRowsShouldReturnOkAnswer() {
        // given
        for (String source : SOURCES) {
            String dirPath = PATH + "/" + source;
            Mockito.when(ytMock.cypress()
                    .list(YPath.simple(dirPath),
                        List.of(ROW_COUNT_FIELD_DATA, CREATION_TIME_FIELD_DATA, CYPRESS_TYPE_ATTRIBUTE)))
                .thenReturn(List.of(
                    new YTreeStringNodeImpl(
                        dirPath + "/1",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 100L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-13T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    ),
                    new YTreeStringNodeImpl(
                        dirPath + "/2",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 100L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-14T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    ),
                    new YTreeStringNodeImpl(
                        dirPath + "/3",
                        Map.of(
                            ROW_COUNT_FIELD_DATA, new YTreeIntegerNodeImpl(false, 150L, null),
                            CREATION_TIME_FIELD_DATA, new YTreeStringNodeImpl("2022-06-15T06:00:00.524629Z", null),
                            CYPRESS_TYPE_ATTRIBUTE, new YTreeStringNodeImpl(CYPRESS_TABLE_TYPE, null)
                        )
                    )
                ));
        }

        // when
        MdmMonitoringResult monitoringResult = monitoringService.getFreshChecksHaveEnoughRowsInYtMonitoringResult();

        //then
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRIT);
    }

    @Test
    public void whenDqChecksHasUnknownResults() {
        String message = "Some monitoring message here";
        keyValueService.putValue(MdmProperties.DQ_UNKNOWN_RESULTS_MESSAGE, message);
        keyValueService.putValue(MdmProperties.DQ_UNKNOWN_RESULTS_FIRE_MONITORING, true);

        MdmMonitoringResult monitoringResult = monitoringService.getDqChecksHasUnknownResults();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRIT);
        Assertions.assertThat(monitoringResult.getMessage()).isEqualTo(message);

        Boolean fireMonitoring = keyValueService.getBool(MdmProperties.DQ_UNKNOWN_RESULTS_FIRE_MONITORING, null);

        Assertions.assertThat(fireMonitoring).isFalse();
    }


    @Test
    public void whenDqChecksHasNoUnknownResults() {
        keyValueService.putValue(MdmProperties.DQ_UNKNOWN_RESULTS_FIRE_MONITORING, false);

        MdmMonitoringResult monitoringResult = monitoringService.getDqChecksHasUnknownResults();

        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);

        Boolean fireMonitoring = keyValueService.getBool(MdmProperties.DQ_UNKNOWN_RESULTS_FIRE_MONITORING, null);

        Assertions.assertThat(fireMonitoring).isFalse();

    }
}
