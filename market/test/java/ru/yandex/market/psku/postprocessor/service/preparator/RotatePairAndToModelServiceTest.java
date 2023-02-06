package ru.yandex.market.psku.postprocessor.service.preparator;

import com.google.common.collect.ImmutableMultimap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.MemorizingLongGenerator;
import ru.yandex.market.psku.postprocessor.TestDataGenerator;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PairDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuToModelDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.SessionState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Session;
import ru.yandex.market.psku.postprocessor.service.MockUtils;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;
import ru.yandex.market.psku.postprocessor.service.uc.UCService;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtEnrichSessionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shadoff
 * created on 2019-12-24
 */
public class RotatePairAndToModelServiceTest extends BaseDBTest {
    @Mock
    private YtEnrichSessionService ytEnrichSessionService;
    @Mock
    private YtDataService ytDataService;
    @Mock
    private UCService ucService;

    @Autowired
    private PairDao pairDao;

    @Autowired
    private PskuToModelDao pskuToModelDao;

    private RotatePairAndToModelService rotatePairAndToModelService;

    private List<PskuInfo> testPskuInfos;
    private Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer;
    private ImmutableMultimap pairIds;

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
            pskuInfoToUCOffer.put(pskuInfo, TestDataGenerator.generateUCOffer(pskuInfo, idsGenerator));
            idsGenerator.getGenerated().forEach(id -> idsMapBuilder.put(pskuInfo.getId(), id));
            idsGenerator.clearGenerated();
        });

        pairIds = idsMapBuilder.build();

        MockUtils.mockYtEnrichSessionService(ytEnrichSessionService);
        MockUtils.mockUCService(ucService, testPskuInfos, pskuInfoToUCOffer);
        MockUtils.mockYtDataService(ytDataService, testPskuInfos, pskuInfoToUCOffer);

        rotatePairAndToModelService = new RotatePairAndToModelService(ytEnrichSessionService, ytDataService, pairDao,
            pskuToModelDao, sessionDao);
    }


    @Test
    public void rotatePairTest() {
        sessionDao.insert(MockUtils.getTestSession(SessionState.ACTIVE));
        Session enrichedSession = MockUtils.getTestSession(SessionState.ENRICHED_LOADED);
        enrichedSession.setId(MockUtils.LAST_ENRICHED_SESSION_ID);
        sessionDao.insert(enrichedSession);
        rotatePairAndToModelService.rotatePairAndToModel();

        List<Pair> pairs = pairDao.findAll();
        Assertions.assertThat(pairs).hasSize(pairIds.size());
        Assertions.assertThat(pairs)
                .allMatch(pair -> pairIds.containsEntry(pair.getPskuId(), pair.getMskuId()));

        pairs.forEach(pair -> Assert.assertEquals(MockUtils.TEST_SESSION_ID, pair.getSessionId()));
    }

    @Test
    public void rotatePairNothingToDoTest() {
        Mockito.when(ytEnrichSessionService.getLastSessionId()).thenReturn(MockUtils.TEST_SESSION_ID);
        sessionDao.insert(MockUtils.getTestSession(SessionState.ACTIVE));

        rotatePairAndToModelService.rotatePairAndToModel();

        Mockito.verify(ytDataService, Mockito.never()).getEnrichedPskusToHandle(Mockito.any());
    }
}
