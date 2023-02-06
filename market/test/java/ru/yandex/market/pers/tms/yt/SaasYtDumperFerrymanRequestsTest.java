package ru.yandex.market.pers.tms.yt;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.saas.GenerationState;
import ru.yandex.market.pers.tms.saas.IndexGenerationIdFactory;
import ru.yandex.market.pers.tms.saas.IndexGenerationService;
import ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService;
import ru.yandex.market.pers.tms.yt.saas.SaasSnapshotGenerator;
import ru.yandex.market.pers.tms.yt.saas.SaasYtDumperService;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtUtils;
import ru.yandex.market.saas.indexer.ferryman.FerrymanService;
import ru.yandex.market.saas.indexer.ferryman.model.YtTableRef;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
public class SaasYtDumperFerrymanRequestsTest extends MockedPersTmsTest {

    @Autowired
    DbGradeAdminService gradeAdminService;
    @Autowired
    @Qualifier("ferrymanServiceForStatic")
    FerrymanService ferrymanServiceMock;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    IndexGenerationService indexGenerationService;
    @Autowired
    IndexGenerationIdFactory indexGenerationIdFactory;
    @Autowired
    YtClient ytClient;
    @Autowired
    SaasSnapshotGenerator saasSnapshotGenerator;
    @Autowired
    @Qualifier("ytJdbcTemplate")
    JdbcTemplate ytJdbcTemplate;

    private GradeQueueService gradeQueueServiceMock = PersTestMocksHolder.registerMock(GradeQueueService.class);
    private SaasIndexDumperService saasIndexDumperServiceSpy;
    private SaasYtDumperService saasYtDumperServiceMock;

    @Before
    public void setUp() {
        saasYtDumperServiceMock = getSaasYtDumperServiceMock();

        saasIndexDumperServiceSpy = spy(new SaasIndexDumperService(
            saasYtDumperServiceMock,
            null,
            Collections.emptyList(),
            ferrymanServiceMock,
            indexGenerationService,
            gradeQueueServiceMock,
            configurationService,
            ytClient,
            saasSnapshotGenerator));
    }

    @Test
    public void testSendSnapshotToFerryman() throws Exception {
        // mock check that new grades exists + check update does nothing
        when(ytJdbcTemplate.queryForObject(any(), eq(Long.class))).thenReturn(1L);
        when(ytJdbcTemplate.update(anyString())).thenReturn(1);

        mockBuildGenerationId("GENERATION" + GradeCreator.rndLong());
        doReturn(true).when(saasIndexDumperServiceSpy).isSnapshot();

        saasIndexDumperServiceSpy.dumpGrades();

        ArgumentCaptor<YtTableRef> ferrymanTableRef = ArgumentCaptor.forClass(YtTableRef.class);
        verify(ferrymanServiceMock).addTable(ferrymanTableRef.capture());
        Assert.assertEquals(false, ferrymanTableRef.getValue().isDelta());
    }

    @Test
    public void testSendDiffToFerryman() throws Exception {
        mockBuildGenerationId("GENERATION" + GradeCreator.rndLong());
        doReturn(false).when(saasIndexDumperServiceSpy).isSnapshot();
        when(gradeQueueServiceMock.isEmpty()).thenReturn(false);
        when(ytClient.getRowsCount(any())).thenReturn(1L);

        saasIndexDumperServiceSpy.dumpGrades();

        ArgumentCaptor<YtTableRef> ferrymanTableRef = ArgumentCaptor.forClass(YtTableRef.class);
        verify(ferrymanServiceMock).addTable(ferrymanTableRef.capture());
        Assert.assertEquals(true, ferrymanTableRef.getValue().isDelta());

        verify(saasYtDumperServiceMock).dump(any(), any(), eq(false));
    }

    @Test
    public void testNoDiffIfEmptyQueue() throws Exception {
        doReturn(false).when(saasIndexDumperServiceSpy).isSnapshot();
        when(gradeQueueServiceMock.isEmpty()).thenReturn(true);

        saasIndexDumperServiceSpy.dumpGrades();

        verify(ferrymanServiceMock, never()).addTable(any());
        verify(saasYtDumperServiceMock, never()).dump(any(), any(), anyBoolean());
    }

    @Test
    public void testNoFerrymanRequestIfEmptyDiff() throws Exception {
        doReturn(false).when(saasIndexDumperServiceSpy).isSnapshot();
        when(gradeQueueServiceMock.isEmpty()).thenReturn(false);
        when(ytClient.getRowsCount(any())).thenReturn(0L);

        saasIndexDumperServiceSpy.dumpGrades();

        verify(ferrymanServiceMock, never()).addTable(any());
        verify(saasYtDumperServiceMock).dump(any(), any(), eq(false));
    }

    @Test
    public void testFerrymanFailed() throws Exception {
        final String generationName = "GENERATION" + GradeCreator.rndLong();
        mockBuildGenerationId(generationName);
        doReturn(false).when(saasIndexDumperServiceSpy).isSnapshot();
        String ferrymanExceptionMsg = "Ferryman did not answer OK after 3 retries";
        String jobExceptionMsg = String.format("Generation %s mark as failed cause by Ferryman requesting: %s",
            generationName,
            ferrymanExceptionMsg);
        when(ferrymanServiceMock.addTable(any())).thenThrow(new Exception(ferrymanExceptionMsg));
        when(ytClient.getRowsCount(any())).thenReturn(1L);

        try {
            saasIndexDumperServiceSpy.dumpGrades();
        } catch (Exception e) {
            Assert.assertEquals(jobExceptionMsg, e.getMessage());
        }

        ArgumentCaptor<YtTableRef> ferrymanTableRef = ArgumentCaptor.forClass(YtTableRef.class);
        verify(ferrymanServiceMock).addTable(ferrymanTableRef.capture());
        Assert.assertEquals(true, ferrymanTableRef.getValue().isDelta());

        Assert.assertEquals(GenerationState.FAILED, indexGenerationService.getGenerationState(generationName));
        verify(gradeQueueServiceMock).unmark();
    }

    private void mockBuildGenerationId(String generationName) {
        when(indexGenerationIdFactory.buildNewGenerationId(anyLong())).thenReturn(generationName);
    }

    private SaasYtDumperService getSaasYtDumperServiceMock() {
        SaasYtDumperService mock = mock(SaasYtDumperService.class);
        YPath dir = getDirPathYtDumper();
        when(mock.dump(any(), any())).thenReturn(dir);
        when(mock.dump(any(), any(), anyBoolean())).thenReturn(dir);
        when(mock.dump(any())).thenReturn(dir);
        return mock;
    }

    private YPath getDirPathYtDumper() {
        YPath tablePath = YPath.simple("//home/market/testing/pers-grade").child("tables/saas_documents");
        return YtUtils.generateNewDirPath(tablePath);
    }
}
