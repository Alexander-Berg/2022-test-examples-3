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
import ru.yandex.direct.ess.router.testutils.BannerDisplayHrefsChange;
import ru.yandex.direct.ess.router.testutils.BannerImagesTableChange;
import ru.yandex.direct.ess.router.testutils.BannersAdditionsTableChange;
import ru.yandex.direct.ess.router.testutils.BannersMinusGeoTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addAdditionItemCallouts;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addBannerDisplayHrefs;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addImageToBanner;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesBanks;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesMedServices;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesPharmacy;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_ADDITIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_MINUS_GEO;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.ess.router.testutils.BannerDisplayHrefsChange.createBannerDisplayHrefsEvent;
import static ru.yandex.direct.ess.router.testutils.BannerImagesTableChange.createBannerImagesEvent;
import static ru.yandex.direct.ess.router.testutils.BannersAdditionsTableChange.createBannersAdditionsEvent;
import static ru.yandex.direct.ess.router.testutils.BannersMinusGeoTableChange.createBannersMinusGeoEvent;
import static ru.yandex.direct.ess.router.testutils.TestUtils.getRecommendationsTypes;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class RecomTracerRuleOtherTablesChangeTest {

    private static final RecommendationType[] BANNER_IMAGES_INSERT_TYPES = new RecommendationType[]{
            addImageToBanner
    };
    private static final RecommendationType[] BANNERS_ADDITIONS_INSERT_TYPES = new RecommendationType[]{
            addAdditionItemCallouts
    };
    private static final RecommendationType[] BANNERS_MINUS_GEO_UPDATE_TYPES = new RecommendationType[]{
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] BANNER_DISPLAY_HREFS_TYPES = new RecommendationType[]{
            addBannerDisplayHrefs
    };
    private static final Long BID_VALUE = 50L;
    @Autowired
    private RecomTracerRule rule;

    @Test
    void bannerImageInsertTest() {
        BannerImagesTableChange bannerImagesTableChange = new BannerImagesTableChange().withImageId(10L);
        bannerImagesTableChange.addInsertedColumn(BANNER_IMAGES.BID, BID_VALUE);

        BinlogEvent binlogEvent = createBannerImagesEvent(singletonList(bannerImagesTableChange), INSERT);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNER_IMAGES_INSERT_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNER_IMAGES_INSERT_TYPES);
        checkBannersLogicObjectValues(got);
    }

    @Test
    void bannersAdditionsInsertTest() {
        BannersAdditionsTableChange bannersAdditionsTableChange =
                new BannersAdditionsTableChange().withAdditionsItemId(10L);
        bannersAdditionsTableChange.addInsertedColumn(BANNERS_ADDITIONS.BID, BID_VALUE);

        BinlogEvent binlogEvent = createBannersAdditionsEvent(singletonList(bannersAdditionsTableChange), INSERT);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNERS_ADDITIONS_INSERT_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNERS_ADDITIONS_INSERT_TYPES);
        checkBannersLogicObjectValues(got);
    }

    @Test
    void bannersMinusGeoUpdateTest() {
        BannersMinusGeoTableChange bannersMinusGeoTableChange =
                new BannersMinusGeoTableChange().withBid(BID_VALUE);
        bannersMinusGeoTableChange.addChangedColumn(BANNERS_MINUS_GEO.MINUS_GEO, "149,159,187,225", "149,159,187,225," +
                "977");

        BinlogEvent binlogEvent = createBannersMinusGeoEvent(singletonList(bannersMinusGeoTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNERS_MINUS_GEO_UPDATE_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNERS_MINUS_GEO_UPDATE_TYPES);
        checkBannersLogicObjectValues(got);
    }

    @Test
    void bannerDisplayHrefsTest() {
        BannerDisplayHrefsChange bannerDisplayHrefsChange =
                new BannerDisplayHrefsChange().withBid(BID_VALUE);

        BinlogEvent binlogEvent = createBannerDisplayHrefsEvent(singletonList(bannerDisplayHrefsChange), INSERT);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(BANNER_DISPLAY_HREFS_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(BANNER_DISPLAY_HREFS_TYPES);
        checkBannersLogicObjectValues(got);
    }

    private void checkBannersLogicObjectValues(List<RecomTracerLogicObject> got) {
        long suitableObjectsCount = got.stream()
                .filter(logicObject -> Objects.equals(logicObject.getTableToLoad(), TablesEnum.BANNERS))
                .filter(logicObject -> logicObject.getPrimaryKey().equals(BID_VALUE))
                .filter(logicObject -> logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.BID)
                        && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID).equals(BID_VALUE))
                .count();
        assertThat(suitableObjectsCount).isEqualTo(got.size());
    }
}
