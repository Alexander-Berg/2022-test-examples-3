package ru.yandex.market.psku.postprocessor.service.preparator;

import com.google.common.collect.ImmutableMultimap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.MemorizingLongGenerator;
import ru.yandex.market.psku.postprocessor.TestDataGenerator;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.SessionState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;
import ru.yandex.market.psku.postprocessor.service.MockUtils;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;
import ru.yandex.market.psku.postprocessor.service.uc.UCService;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPskuSessionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author shadoff
 * created on 2019-12-24
 */
@SuppressWarnings("checkstyle:magicnumber")
public class LoadPskusServiceTest extends BaseDBTest {
    @Mock
    private YtPskuSessionService ytPskuSessionService;
    @Mock
    private YtDataService ytDataService;
    @Mock
    private UCService ucService;

    @Autowired
    private PskuKnowledgeDao pskuKnowledgeDao;

    private LoadPskusService loadPskusService;

    private List<PskuInfo> testPskuInfos;
    private Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testPskuInfos = new ArrayList<>();
        pskuInfoToUCOffer = new HashMap<>();

        ModelStorage.Model stubModel = ModelStorage.Model.newBuilder().setSupplierId(1).build();
        testPskuInfos.addAll(Arrays.asList(
                PskuInfo.builder().setId(1L).setTitle("psku_1").setModel(stubModel).build(),
                PskuInfo.builder().setId(2L).setTitle("psku_2").setModel(stubModel).build(),
                PskuInfo.builder().setId(3L).setTitle("psku_3").setModel(stubModel).build()));

        ImmutableMultimap.Builder<Long, Long> idsMapBuilder = ImmutableMultimap.builder();

        MemorizingLongGenerator idsGenerator = new MemorizingLongGenerator();

        testPskuInfos.forEach(pskuInfo -> {
            idsGenerator.getGenerated().forEach(id -> idsMapBuilder.put(pskuInfo.getId(), id));
            idsGenerator.clearGenerated();
        });

        testPskuInfos.forEach(pskuInfo -> {
            pskuInfoToUCOffer.put(pskuInfo, TestDataGenerator.generateUCOffer(pskuInfo, idsGenerator));
            idsGenerator.getGenerated().forEach(id -> idsMapBuilder.put(pskuInfo.getId(), id));
            idsGenerator.clearGenerated();
        });

        MockUtils.mockYtPskuSessionService(ytPskuSessionService);
        MockUtils.mockUCService(ucService, testPskuInfos, pskuInfoToUCOffer);
        MockUtils.mockYtDataService(ytDataService, testPskuInfos, pskuInfoToUCOffer);
        loadPskusService = new LoadPskusService(ytPskuSessionService, ytDataService, pskuKnowledgeDao, sessionDao);
    }

    @Test
    public void loadPskusTest() {
        InOrder inOrderMain = Mockito.inOrder(ytPskuSessionService, ytDataService, ucService);

        loadPskusService.loadPskus();

        inOrderMain.verify(ytDataService).getRecentExportCreationDate();
        inOrderMain.verify(ytPskuSessionService).getLastSessionExportDate();

        inOrderMain.verify(ytPskuSessionService).startNewPskuSession();
        inOrderMain.verify(ytDataService).loadPskus(eq(MockUtils.TEST_SESSION_NAME));
        inOrderMain.verify(ytPskuSessionService).writeSessionExportDate(
                eq(MockUtils.RECENT_EXPORT_CREATION_DATE),
                eq(MockUtils.TEST_SESSION_NAME));

        // fillPskuKnowledge
        inOrderMain.verify(ytDataService).processPskus(eq(MockUtils.TEST_SESSION_NAME), any());
        List<PskuKnowledge> pskuKnowledges = pskuKnowledgeDao.findAll();
        Assertions.assertThat(pskuKnowledges).hasSize(testPskuInfos.size());

        SessionParam sessionParam = new SessionParam(MockUtils.TEST_SESSION_ID, MockUtils.TEST_SESSION_NAME);
        inOrderMain.verify(ytPskuSessionService)
                .markSessionFinished(eq(sessionParam), eq(SessionState.PSKU_LOADED), anyInt());

        inOrderMain.verifyNoMoreInteractions();
    }

    @Test
    public void loadPskusNullRecentSessionTest() {
        Mockito.when(ytPskuSessionService.getLastSessionExportDate()).thenReturn(null);
        loadPskusService.loadPskus();
        Mockito.verify(ytPskuSessionService).markSessionFinished(any(), any(), anyInt());
    }

    @Test
    public void loadPskusNothingToDoSessionTest() {
        Mockito.when(ytPskuSessionService.getLastSessionExportDate()).thenReturn("it's time!");
        Mockito.when(ytDataService.getRecentExportCreationDate()).thenReturn("it's time!");
        loadPskusService.loadPskus();
        Mockito.verify(ytDataService, Mockito.never()).processPskus(eq(MockUtils.TEST_SESSION_NAME), any());
        Mockito.verify(ytPskuSessionService, Mockito.never()).finishSession(eq(MockUtils.TEST_SESSION_PARAM));
    }
}
