package ru.yandex.direct.ess.router.rules.recomtracer;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.dbschema.ppc.enums.BidsBaseBidType;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BidsBaseTableChange;
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addAdditionItemCallouts;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addBannerDisplayHrefs;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addImageToBanner;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addSiteLinks;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addTitleExtension;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.changeAdGroupWithLowStat;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesBanks;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesMedServices;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesPharmacy;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.switchOnAutotargeting;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;
import static ru.yandex.direct.ess.router.testutils.BidsBaseTableChange.createBidsBaseEvent;
import static ru.yandex.direct.ess.router.testutils.PhrasesTableChange.createPhrasesEvent;
import static ru.yandex.direct.ess.router.testutils.TestUtils.getRecommendationsTypes;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class RecomTracerRulePhrasesChangeTest {
    private static final RecommendationType[] PHRASE_DELETE_TYPES = new RecommendationType[]{
            addAdditionItemCallouts, addBannerDisplayHrefs, addImageToBanner, addSiteLinks, addTitleExtension,
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] PHRASE_GEO_CHANGED_TYPES = new RecommendationType[]{
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] PHRASE_STATUS_MODERATE_CHANGED_TYPES = new RecommendationType[]{
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] PHRASE_IS_BS_RARELY_LOADED_CHANGED_TYPES = new RecommendationType[]{
            changeAdGroupWithLowStat
    };
    private static final RecommendationType[] BIDS_BASE_INSERT_TYPES = new RecommendationType[]{
            switchOnAutotargeting
    };
    private static final Long PID_VALUE = 30L;
    private static final Long CID_VALUE = 10L;
    @Autowired
    private RecomTracerRule rule;

    @Test
    void phraseDeleteTest() {
        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(PID_VALUE).withCid(CID_VALUE);

        BinlogEvent binlogEvent = createPhrasesEvent(singletonList(phrasesTableChange), DELETE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(PHRASE_DELETE_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(PHRASE_DELETE_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void phraseGeoChangedTest() {
        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(PID_VALUE).withCid(CID_VALUE);
        // geo - колонка с типом text, а бинлоги у нас включены в режиме NOBLOB, для теста не иммет значение пол
        phrasesTableChange.addChangedColumn(PHRASES.GEO, "No matter1", "No matter2");
        BinlogEvent binlogEvent = createPhrasesEvent(singletonList(phrasesTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(PHRASE_GEO_CHANGED_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(PHRASE_GEO_CHANGED_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void phraseStatusModerateChangedTest() {
        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(PID_VALUE).withCid(CID_VALUE);
        phrasesTableChange.addChangedColumn(PHRASES.STATUS_MODERATE, "No", "Yes");
        BinlogEvent binlogEvent = createPhrasesEvent(singletonList(phrasesTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(PHRASE_STATUS_MODERATE_CHANGED_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(PHRASE_STATUS_MODERATE_CHANGED_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void phraseIsBsRarelyLoadedModerateChangedToZeroTest() {
        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(PID_VALUE).withCid(CID_VALUE);
        phrasesTableChange.addChangedColumn(PHRASES.IS_BS_RARELY_LOADED, 1L, 0L);
        BinlogEvent binlogEvent = createPhrasesEvent(singletonList(phrasesTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(PHRASE_IS_BS_RARELY_LOADED_CHANGED_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(PHRASE_IS_BS_RARELY_LOADED_CHANGED_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void phraseIsBsRarelyLoadedModerateChangedToOneTest() {
        PhrasesTableChange phrasesTableChange = new PhrasesTableChange().withPid(PID_VALUE).withCid(CID_VALUE);
        phrasesTableChange.addChangedColumn(PHRASES.IS_BS_RARELY_LOADED, 0L, 1L);
        BinlogEvent binlogEvent = createPhrasesEvent(singletonList(phrasesTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        assertThat(got).hasSize(0);
    }

    @Test
    void bidsBaseInsertTest() {
        BidsBaseTableChange bidsBaseTableChange =
                new BidsBaseTableChange().withBidId(1L);
        bidsBaseTableChange.addInsertedColumn(BIDS_BASE.PID, PID_VALUE);
        bidsBaseTableChange.addInsertedColumn(BIDS_BASE.CID, CID_VALUE);
        bidsBaseTableChange.addInsertedColumn(BIDS_BASE.BID_TYPE, BidsBaseBidType.relevance_match.getLiteral());

        BinlogEvent binlogEvent = createBidsBaseEvent(singletonList(bidsBaseTableChange), INSERT);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BIDS_BASE_INSERT_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BIDS_BASE_INSERT_TYPES);
        checkLogicObjectValues(got);
    }

    private void checkLogicObjectValues(List<RecomTracerLogicObject> got) {
        long suitableObjectsCount = got.stream()
                .filter(logicObject -> Objects.equals(logicObject.getTableToLoad(), TablesEnum.CAMPAIGNS))
                .filter(logicObject -> logicObject.getPrimaryKey().equals(CID_VALUE))
                .filter(logicObject ->
                        logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.PID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID).equals(PID_VALUE)
                                && logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.CID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID).equals(CID_VALUE)
                )
                .count();
        assertThat(suitableObjectsCount).isEqualTo(got.size());
    }
}
