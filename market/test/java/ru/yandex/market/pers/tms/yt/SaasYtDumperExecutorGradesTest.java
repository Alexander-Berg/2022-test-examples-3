package ru.yandex.market.pers.tms.yt;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.grade.core.ugc.FactorService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.saas.IndexGenerationIdFactory;
import ru.yandex.market.pers.tms.yt.dumper.dumper.reader.saas.SaasDocumentYt;
import ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService;
import ru.yandex.market.pers.tms.yt.saas.SaasYtDumperService;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.saas.indexer.SaasIndexerAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class SaasYtDumperExecutorGradesTest extends MockedPersTmsTest {

    protected static final long SHOP_ID = -237432;
    protected static final long MODEL_ID = -94687689;
    protected static final long AUTHOR_ID = -786432;
    private final static Logger log = Logger.getLogger(SaasYtDumperExecutorGradesTest.class);

    protected long gradeId;
    protected long resourceId;

    @Autowired
    YtClient ytClient;
    @Autowired
    GradeQueueService gradeQueueService;

    @Autowired
    SaasIndexDumperService saasIndexDumperService;
    SaasIndexDumperService saasIndexDumperServiceSpy;

    @Autowired
    SaasYtDumperService saasYtDumperService;

    @Autowired
    GradeCreator gradeCreator;

    @Autowired
    FactorService factorService;

    @Autowired
    DbGradeService gradeService;

    @Autowired
    IndexGenerationIdFactory indexGenerationIdFactory;

    private static Predicate<SaasDocumentYt> getSaasDocYtPredicate(String url, SaasIndexerAction action) {
        String p = String.format(".*\"url\":\"%s\".*\"action\":\"%s\".*", url, action.value());
        Pattern pattern = Pattern.compile(p);
        return (SaasDocumentYt document) -> pattern.matcher(document.toString()).matches();
    }

    @Before
    public void setUp() throws Exception {
        saasIndexDumperServiceSpy = spy(saasIndexDumperService);
        saasYtDumperService.changeJdbcTemplateForTests(pgJdbcTemplate);
    }

    public void initGradeIdAndResource(String sqlQueryForGradeAndResource) {
        Pair<Long, Long> grade = queryForGradeIdAndResource(sqlQueryForGradeAndResource);
        assertNotNull("no data for test", grade);
        gradeId = grade.first;
        resourceId = grade.second;
    }

    protected ArgumentCaptor<List<SaasDocumentYt>> dumpedDiffDocs() throws Exception {
        doReturn(false).when(saasIndexDumperServiceSpy).isSnapshot();
        mockBuildGenerationId("GENERATION" + GradeCreator.rndLong());

        gradeQueueService.clearAll();
        log.info(String.format("put %s to grade queue", gradeId));
        gradeQueueService.put(gradeId);

        saasIndexDumperServiceSpy.dumpGrades();

        ArgumentCaptor<List<SaasDocumentYt>> captor = ArgumentCaptor.forClass(List.class);
        verify(ytClient, atLeastOnce()).createTable(isNull(), any(), isNull());
        verify(ytClient, atLeastOnce()).append(anyObject(), any(), captor.capture());
        return captor;
    }

    protected ArgumentCaptor<List<SaasDocumentYt>> dumpedSnapshotDocs() throws Exception {
        doReturn(true).when(saasIndexDumperServiceSpy).isSnapshot();
        mockBuildGenerationId("GENERATION" + GradeCreator.rndLong());

        saasIndexDumperServiceSpy.dumpGrades();

        ArgumentCaptor<List<SaasDocumentYt>> captor = ArgumentCaptor.forClass(List.class);
        verify(ytClient, atLeastOnce()).createTable(isNull(), any(), isNull());
        verify(ytClient, atLeastOnce()).append(any(), any(), captor.capture());
        return captor;
    }

    protected void assertDumpToYtDocs(ArgumentCaptor<List<SaasDocumentYt>> captor, int size) {
        assertEquals(String.format("dump to yt %s documents", size), size,
                captor.getAllValues().stream().flatMap(Collection::stream).count());
    }

    protected void assertDumpToYtDocWithAction(ArgumentCaptor<List<SaasDocumentYt>> captor, String url, SaasIndexerAction action) {
        assertTrue(String.format("has %s action for document url=%s", action, url),
                captor.getAllValues().stream().anyMatch(
                        it -> it.stream().anyMatch(getSaasDocYtPredicate(url, action))));
    }

    protected void deleteGrade(long gradeId, long uid) {
        SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSelector("id", gradeId);
        final AuthorIdAndYandexUid authorIdAndYandexUid = new AuthorIdAndYandexUid(uid, null);
        gradeService.killGrades(filter, authorIdAndYandexUid);
    }

    private void mockBuildGenerationId(String generationName) {
        when(indexGenerationIdFactory.buildNewGenerationId(anyLong())).thenReturn(generationName);
    }

    private Pair<Long, Long> queryForGradeIdAndResource(String sql) {
        try {
            return pgJdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> new Pair(rs.getLong("id"), rs.getLong("resource_id")));
        } catch (EmptyResultDataAccessException emptyResultDataAccessException) {
            return null;
        }
    }
}
