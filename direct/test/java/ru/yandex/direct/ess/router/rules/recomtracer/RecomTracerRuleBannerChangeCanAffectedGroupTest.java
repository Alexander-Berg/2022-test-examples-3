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
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
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
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.TestUtils.getRecommendationsTypes;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class RecomTracerRuleBannerChangeCanAffectedGroupTest {

    private static final RecommendationType[] BANNER_DELETE_TYPES = new RecommendationType[]{
            addAdditionItemCallouts, addBannerDisplayHrefs, addImageToBanner, addSiteLinks,
            sendLicensesBanks, addTitleExtension, sendLicensesMedServices, sendLicensesPharmacy,
            switchOnAutotargeting, changeAdGroupWithLowStat
    };

    private static final RecommendationType[] BANNER_STATUS_ARCH_CHANGE_TYPES = new RecommendationType[]{
            addAdditionItemCallouts, addBannerDisplayHrefs, addImageToBanner, addSiteLinks,
            sendLicensesBanks, addTitleExtension, sendLicensesMedServices, sendLicensesPharmacy,
            switchOnAutotargeting, changeAdGroupWithLowStat
    };

    private static final Long BID_VALUE = 50L;
    private static final Long PID_VALUE = 30L;
    private static final Long CID_VALUE = 10L;

    @Autowired
    private RecomTracerRule rule;

    @Test
    void bannerDeleteTest() {
        BannersTableChange bannersTableChange =
                new BannersTableChange().withBid(BID_VALUE).withCid(CID_VALUE).withPid(PID_VALUE);

        BinlogEvent binlogEvent = createBannersEvent(singletonList(bannersTableChange), DELETE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNER_DELETE_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNER_DELETE_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void bannerStatusArchBecameYesTest() {
        BannersTableChange bannersTableChange =
                new BannersTableChange().withBid(BID_VALUE).withCid(CID_VALUE).withPid(PID_VALUE);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_ARCH, "No", "Yes");
        BinlogEvent binlogEvent = createBannersEvent(singletonList(bannersTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNER_STATUS_ARCH_CHANGE_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNER_STATUS_ARCH_CHANGE_TYPES);
        checkLogicObjectValues(got);
    }

    public void bannerStatusArchBecameNoTest() {
        BannersTableChange bannersTableChange =
                new BannersTableChange().withBid(BID_VALUE).withCid(CID_VALUE).withPid(PID_VALUE);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_ARCH, "Yes", "No");
        BinlogEvent binlogEvent = createBannersEvent(singletonList(bannersTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        assertThat(got).hasSize(0);
    }

    private void checkLogicObjectValues(List<RecomTracerLogicObject> got) {
        long suitableObjectsCount = got.stream()
                .filter(logicObject -> Objects.equals(logicObject.getTableToLoad(), TablesEnum.PHRASES))
                .filter(logicObject -> logicObject.getPrimaryKey().equals(PID_VALUE))
                .filter(logicObject ->
                        logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.BID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID).equals(BID_VALUE)
                                && logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.PID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID).equals(PID_VALUE)
                )
                .count();
        assertThat(suitableObjectsCount).isEqualTo(got.size());
    }
}
