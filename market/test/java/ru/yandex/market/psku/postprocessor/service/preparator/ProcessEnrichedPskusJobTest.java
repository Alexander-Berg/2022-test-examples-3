package ru.yandex.market.psku.postprocessor.service.preparator;

import com.google.common.collect.ImmutableMultimap;
import org.assertj.core.api.Assertions;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Before;
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
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModel;
import ru.yandex.market.psku.postprocessor.service.MockUtils;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.Session.SESSION;


/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ProcessEnrichedPskusJobTest extends BaseDBTest {

    @Mock
    private YtDataService ytDataService;

    @Autowired
    private PairDao pairDao;

    @Autowired
    private PskuToModelDao pskuToModelDao;

    private List<PskuInfo> testPskuInfos;
    private Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testPskuInfos = new ArrayList<>();
        pskuInfoToUCOffer = new HashMap<>();
    }

    @Test
    public void whenNewTableThenDropCreate() {
        pairDao.createNewTable();
        pskuToModelDao.createNewTable();
        PairDao spiedPairDao = Mockito.spy(pairDao);
        PskuToModelDao spiedPskuToModelDao = Mockito.spy(pskuToModelDao);

        Mockito.when(ytDataService.getEnrichedPskusToHandle(Mockito.any()))
            .thenReturn(dataHandler -> {
            });

        ProcessEnrichedPskusJob.with(ytDataService, spiedPairDao, spiedPskuToModelDao, sessionDao, 1L)
            .processEnrichedPskus(MockUtils.TEST_SESSION_PARAM);

        Mockito.verify(spiedPairDao).dropNewTable();
        Mockito.verify(spiedPairDao).createNewTable();
        Mockito.verify(spiedPskuToModelDao).dropNewTable();
        Mockito.verify(spiedPskuToModelDao).createNewTable();
    }

    @Test
    public void whenProcessEnrichedPskusOk() {
        ModelStorage.Model stubModel = ModelStorage.Model.newBuilder().setSupplierId(1).build();
        testPskuInfos.addAll(Arrays.asList(
            PskuInfo.builder().setId(1L).setTitle("psku_1").setVendorId(1).setModel(stubModel).setPmodelSkuCount(1L).build(),
            PskuInfo.builder().setId(2L).setTitle("psku_2").setVendorId(2).setModel(stubModel).setPmodelSkuCount(1L).build(),
            PskuInfo.builder().setId(3L).setTitle("psku_3").setVendorId(3).setModel(stubModel).setPmodelSkuCount(1L).build()));

        ImmutableMultimap.Builder<Long, Long> idsMapBuilder = ImmutableMultimap.builder();
        ImmutableMultimap.Builder<Long, Long> pskuToModelIdsMapBuilder = ImmutableMultimap.builder();

        MemorizingLongGenerator idsGenerator = new MemorizingLongGenerator();

        testPskuInfos.forEach(pskuInfo -> {
            UltraController.EnrichedOffer enrichedOffer = TestDataGenerator.generateUCOffer(pskuInfo, idsGenerator);
            pskuInfoToUCOffer.put(pskuInfo, enrichedOffer);
            idsGenerator.getGenerated().forEach(id -> idsMapBuilder.put(pskuInfo.getId(), id));
            idsGenerator.clearGenerated();
        });

        List<PskuInfo> morePskuInfos = Arrays.asList(
            PskuInfo.builder().setId(5L).setTitle("psku_5").setVendorId(1).setModel(stubModel).setPmodelSkuCount(1L).build(),
            PskuInfo.builder().setId(6L).setTitle("psku_6").setVendorId(2).setModel(stubModel).setPmodelSkuCount(1L).build(),
            PskuInfo.builder().setId(7L).setTitle("psku_7").setVendorId(3).setModel(stubModel).setPmodelSkuCount(1L).build());
        morePskuInfos.forEach(pskuInfo -> {
            UltraController.EnrichedOffer enrichedOffer =
                TestDataGenerator.generateUCOfferWithoutMsku(pskuInfo);
            pskuInfoToUCOffer.put(pskuInfo, enrichedOffer);
            pskuToModelIdsMapBuilder.put(pskuInfo.getId(), (long) enrichedOffer.getModelId());
        });
        testPskuInfos.addAll(morePskuInfos);

        ImmutableMultimap pairIds = idsMapBuilder.build();
        ImmutableMultimap<Long, Long> pskuToModelIds = pskuToModelIdsMapBuilder.build();
        System.out.println(pskuToModelIds);

        MockUtils.mockYtEnrichedPskusToHandle(ytDataService, testPskuInfos, pskuInfoToUCOffer);

        String sessionName = "TEST_SESSION";
        Long sessionId = sessionDao.createNewSession(sessionName);
        SessionParam sessionParam = new SessionParam(sessionId, sessionName);
        ProcessEnrichedPskusJob.with(ytDataService, pairDao, pskuToModelDao, sessionDao, sessionId)
            .processEnrichedPskus(sessionParam);

        List<Pair> pairs = DSL.using(jooqConfiguration)
            .fetch(PairDao.PAIR_NEW)
            .into(Pair.class);

        List<PskuToModel> pskuToModels = DSL.using(jooqConfiguration)
            .fetch(PskuToModelDao.PSKU_TO_MODEL_NEW)
            .into(PskuToModel.class);

        Assertions.assertThat(pairs).hasSize(pairIds.size());
        Assertions.assertThat(pairs)
            .allMatch(pair -> pairIds.containsEntry(pair.getPskuId(), pair.getMskuId()));

        Assertions.assertThat(pskuToModels).hasSize(6);
        Record2<Integer, Integer> record = DSL.using(jooqConfiguration)
            .select(SESSION.TOTAL_PSKU, SESSION.PROBABLY_DUPLICATED_PSKU)
            .from(SESSION)
            .where(SESSION.ID.eq(sessionId))
            .fetchOne();

        Assert.assertEquals(6, (int) record.value1());
        Assert.assertEquals(3, (int) record.value2());
    }
}
