package ru.yandex.market.psku.postprocessor.service.preparator;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.MemorizingLongGenerator;
import ru.yandex.market.psku.postprocessor.TestDataGenerator;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.SessionState;
import ru.yandex.market.psku.postprocessor.service.MockUtils;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;
import ru.yandex.market.psku.postprocessor.service.uc.UCQueryRequest;
import ru.yandex.market.psku.postprocessor.service.uc.UCService;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtEnrichSessionService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPskuSessionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;

/**
 * @author shadoff
 * created on 2019-12-24
 */
@SuppressWarnings("checkstyle:magicnumber")
public class EnrichPskusServiceTest extends BaseDBTest {
    @Mock
    private YtEnrichSessionService ytEnrichSessionService;
    @Mock
    private YtPskuSessionService ytPskuSessionService;
    @Mock
    private YtDataService ytDataService;
    @Mock
    private UCService ucService;

    private EnrichPskusService enrichPskusService;

    private Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        List<PskuInfo> testPskuInfos = new ArrayList<>();
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

        MockUtils.mockYtEnrichSessionService(ytEnrichSessionService);
        MockUtils.mockYtPskuSessionService(ytPskuSessionService);
        MockUtils.mockUCService(ucService, testPskuInfos, pskuInfoToUCOffer);
        MockUtils.mockYtDataService(ytDataService, testPskuInfos, pskuInfoToUCOffer);

        enrichPskusService = new EnrichPskusService(
                ytEnrichSessionService, ytPskuSessionService, ytDataService,
                ucService, sessionDao);
    }

    @Test
    public void loadEnrichedTest() {
        sessionDao.insert(MockUtils.getTestSession(SessionState.PSKU_LOADED));

        InOrder inOrderMain = Mockito.inOrder(
                ytEnrichSessionService, ytPskuSessionService, ytDataService, ucService);

        enrichPskusService.enrichPskus();

        inOrderMain.verify(ytPskuSessionService).getLastSessionId();
        inOrderMain.verify(ytEnrichSessionService).getLastSessionId();

        inOrderMain.verify(ytEnrichSessionService).startNewEnrichSession(MockUtils.TEST_SESSION_NAME);
        inOrderMain.verify(ytDataService).processPskus(eq(MockUtils.TEST_SESSION_NAME), any());

        inOrderMain.verify(ytDataService, atLeastOnce()).saveEnrichedPskus(
                eq(MockUtils.TEST_SESSION_NAME), anyList());

        SessionParam sessionParam = new SessionParam(MockUtils.TEST_SESSION_ID, MockUtils.TEST_SESSION_NAME);
        inOrderMain.verify(ytEnrichSessionService)
                .markSessionFinished(eq(sessionParam), eq(SessionState.ENRICHED_LOADED), anyInt());

        inOrderMain.verifyNoMoreInteractions();

        InOrder inOrderYtUc = Mockito.inOrder(ytDataService, ucService);
        inOrderYtUc.verify(ytDataService).processPskus(eq(MockUtils.TEST_SESSION_NAME), any());
        inOrderYtUc.verify(ucService, atLeastOnce()).callUC(any(UCQueryRequest.class));
        inOrderYtUc.verify(ytDataService, atLeastOnce()).saveEnrichedPskus(
                eq(MockUtils.TEST_SESSION_NAME), anyList());

        InOrder inOrderYtReport = Mockito.inOrder(ytDataService);
        inOrderYtReport.verify(ytDataService).processPskus(eq(MockUtils.TEST_SESSION_NAME), any());
        inOrderYtReport.verify(ytDataService, atLeastOnce()).saveEnrichedPskus(
                eq(MockUtils.TEST_SESSION_NAME), anyList());
    }

    @Test
    public void loadEnrichedNullRecentSessionTest() {
        sessionDao.insert(MockUtils.getTestSession(SessionState.PSKU_LOADED));
        Mockito.when(ytEnrichSessionService.getLastSessionId()).thenReturn(null);
        enrichPskusService.enrichPskus();
        Mockito.verify(ytEnrichSessionService).markSessionFinished(any(), any(), anyInt());
    }

    @Test
    public void loadEnrichedNothingToDoSessionTest() {
        Mockito.when(ytPskuSessionService.getLastSessionId()).thenReturn(15L);
        Mockito.when(ytEnrichSessionService.getLastSessionId()).thenReturn(15L);
        enrichPskusService.enrichPskus();
        Mockito.verify(ytEnrichSessionService, Mockito.never()).markSessionFinished(any(), any(), anyInt());
    }

}
