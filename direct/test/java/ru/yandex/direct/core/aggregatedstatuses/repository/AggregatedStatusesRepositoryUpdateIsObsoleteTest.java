package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.time.LocalDateTime;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.UpdatableRecordImpl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanFromLong;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.DRAFT;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesAdgroups.AGGR_STATUSES_ADGROUPS;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesBanners.AGGR_STATUSES_BANNERS;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesCampaigns.AGGR_STATUSES_CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesKeywords.AGGR_STATUSES_KEYWORDS;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesRetargetings.AGGR_STATUSES_RETARGETINGS;

/**
 * Проверяем сброс поля is_obsolete и updated при обновлении статуса
 */
@CoreTest
@RunWith(Parameterized.class)
public class AggregatedStatusesRepositoryUpdateIsObsoleteTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Parameterized.Parameter
    public String description;
    @Parameterized.Parameter(1)
    public boolean initIsObsolete;
    @Parameterized.Parameter(2)
    public LocalDateTime updateBefore;
    @Parameterized.Parameter(3)
    public boolean expectIsObsolete;
    @Parameterized.Parameter(4)
    public boolean expectUpdatedGreaterThanOrEqualToInitTime;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] params() {
        return new Object[][]{
                {"is_obsolete:true, updateBefore:null -> ожидаем is_obsolete:false и обновленный updated",
                        true, null, false, true},
                {"is_obsolete:true и updateBefore < updated -> ожидаем что is_obsolete и updated не изменятся",
                        true, LocalDateTime.now().minusHours(5L), true, false},
                {"is_obsolete:true и updateBefore > updated -> ожидаем is_obsolete:false и обновленный updated",
                        true, LocalDateTime.now().plusHours(5L), false, true},
                {"is_obsolete:false, updateBefore:null -> ожидаем что is_obsolete и updated не изменятся",
                        false, null, false, false},
                {"is_obsolete:false и updateBefore < updated -> ожидаем что is_obsolete и updated не изменятся",
                        false, LocalDateTime.now().minusHours(5L), false, false},
                {"is_obsolete:false и updateBefore > updated -> ожидаем is_obsolete:false и обновленный updated",
                        false, LocalDateTime.now().plusHours(5L), false, true},
        };
    }

    private final AggregatedStatusCampaignData campaignStatusData = new AggregatedStatusCampaignData(
            emptyList(), null, GdSelfStatusEnum.DRAFT, DRAFT);
    private final AggregatedStatusAdGroupData adGroupStatusData = new AggregatedStatusAdGroupData(
            emptyList(), null, GdSelfStatusEnum.DRAFT, DRAFT);
    private final AggregatedStatusAdData adStatusData = new AggregatedStatusAdData(
            emptyList(), GdSelfStatusEnum.DRAFT, DRAFT);
    private final AggregatedStatusKeywordData keywordStatusData = new AggregatedStatusKeywordData(
            GdSelfStatusEnum.DRAFT, DRAFT);
    private final AggregatedStatusRetargetingData retargetingStatusData = new AggregatedStatusRetargetingData(
            emptyList(), GdSelfStatusEnum.DRAFT, DRAFT);
    private final int shard = 1;
    private LocalDateTime initialUpdatedTime;

    @Before
    public void before() {
        initialUpdatedTime = LocalDateTime.now().withNano(0);
    }

    @Test
    public void updateCampaigns() {
        long campaignId = RandomUtils.nextLong();
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, campaignStatusData));

        setCampaignIsObsolete(campaignId, initIsObsolete, LocalDateTime.now().minusHours(5L));
        aggregatedStatusesRepository.updateCampaigns(shard, updateBefore, Map.of(campaignId, campaignStatusData));

        Pair<Boolean, LocalDateTime> isObsoleteAndUpdated = getCampaignObsoleteAndUpdated(campaignId);
        checkIsObsoleteAndUpdated(isObsoleteAndUpdated.getLeft(), isObsoleteAndUpdated.getRight());
    }

    @Test
    public void updateAdGroups() {
        long adGroupId = RandomUtils.nextLong();
        aggregatedStatusesRepository.updateAdGroups(shard, null, Map.of(adGroupId, adGroupStatusData));

        setAdGroupIsObsolete(adGroupId, initIsObsolete, LocalDateTime.now().minusHours(5L));
        aggregatedStatusesRepository.updateAdGroups(shard, updateBefore, Map.of(adGroupId, adGroupStatusData));

        Pair<Boolean, LocalDateTime> isObsoleteAndUpdated = getAdGroupObsoleteAndUpdated(adGroupId);
        checkIsObsoleteAndUpdated(isObsoleteAndUpdated.getLeft(), isObsoleteAndUpdated.getRight());
    }

    @Test
    public void updateAds() {
        long bannerId = RandomUtils.nextLong();
        aggregatedStatusesRepository.updateAds(shard, null, Map.of(bannerId, adStatusData));

        setBannerIsObsolete(bannerId, initIsObsolete, LocalDateTime.now().minusHours(5L));
        aggregatedStatusesRepository.updateAds(shard, updateBefore, Map.of(bannerId, adStatusData));

        Pair<Boolean, LocalDateTime> isObsoleteAndUpdated = getBannerObsoleteAndUpdated(bannerId);
        checkIsObsoleteAndUpdated(isObsoleteAndUpdated.getLeft(), isObsoleteAndUpdated.getRight());
    }

    @Test
    public void updateKeywords() {
        long keywordId = RandomUtils.nextLong();
        aggregatedStatusesRepository.updateKeywords(shard, null, Map.of(keywordId, keywordStatusData));

        setKeywordIsObsolete(keywordId, initIsObsolete, LocalDateTime.now().minusHours(5L));
        aggregatedStatusesRepository.updateKeywords(shard, updateBefore, Map.of(keywordId, keywordStatusData));

        Pair<Boolean, LocalDateTime> isObsoleteAndUpdated = getKeywordObsoleteAndUpdated(keywordId);
        checkIsObsoleteAndUpdated(isObsoleteAndUpdated.getLeft(), isObsoleteAndUpdated.getRight());
    }

    @Test
    public void updateRetargeting() {
        long retargetingId = RandomUtils.nextInt();
        aggregatedStatusesRepository.updateRetargetings(shard, null, Map.of(retargetingId, retargetingStatusData));

        setRetargetingIsObsolete(retargetingId, initIsObsolete, LocalDateTime.now().minusHours(5L));
        aggregatedStatusesRepository.updateRetargetings(shard, updateBefore, Map.of(retargetingId,
                retargetingStatusData));

        Pair<Boolean, LocalDateTime> isObsoleteAndUpdated = getRetargetingObsoleteAndUpdated(retargetingId);
        checkIsObsoleteAndUpdated(isObsoleteAndUpdated.getLeft(), isObsoleteAndUpdated.getRight());
    }

    private void checkIsObsoleteAndUpdated(Boolean isObsolete, LocalDateTime updated) {
        assertEquals("isObsolete поле должно быть обнулено", expectIsObsolete, isObsolete);
        if (expectUpdatedGreaterThanOrEqualToInitTime) {
            assertThat("Время обновления статуса должно быть обновлено", updated,
                    greaterThanOrEqualTo(initialUpdatedTime));
        } else {
            assertThat("Время обновления статуса не должно быть обновлено", updated,
                    lessThan(initialUpdatedTime));
        }
    }

    private void setCampaignIsObsolete(Long campaignId, Boolean isObsolete, LocalDateTime updated) {
        setIsObsoleteAndUpdated(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.IS_OBSOLETE,
                AGGR_STATUSES_CAMPAIGNS.UPDATED, AGGR_STATUSES_CAMPAIGNS.CID, campaignId, isObsolete, updated);
    }

    private void setAdGroupIsObsolete(Long adGroupId, Boolean isObsolete, LocalDateTime updated) {
        setIsObsoleteAndUpdated(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE,
                AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.PID, adGroupId, isObsolete, updated);
    }

    private void setBannerIsObsolete(Long bannerId, Boolean isObsolete, LocalDateTime updated) {
        setIsObsoleteAndUpdated(AGGR_STATUSES_BANNERS, AGGR_STATUSES_BANNERS.IS_OBSOLETE,
                AGGR_STATUSES_BANNERS.UPDATED, AGGR_STATUSES_BANNERS.BID, bannerId, isObsolete, updated);
    }

    private void setKeywordIsObsolete(Long keywordId, Boolean isObsolete, LocalDateTime updated) {
        setIsObsoleteAndUpdated(AGGR_STATUSES_KEYWORDS, AGGR_STATUSES_KEYWORDS.IS_OBSOLETE,
                AGGR_STATUSES_KEYWORDS.UPDATED, AGGR_STATUSES_KEYWORDS.ID, keywordId, isObsolete, updated);
    }

    private void setRetargetingIsObsolete(Long retargetingId, Boolean isObsolete, LocalDateTime updated) {
        setIsObsoleteAndUpdated(AGGR_STATUSES_RETARGETINGS, AGGR_STATUSES_RETARGETINGS.IS_OBSOLETE,
                AGGR_STATUSES_RETARGETINGS.UPDATED, AGGR_STATUSES_RETARGETINGS.RET_ID, retargetingId, isObsolete,
                updated);
    }

    private void setIsObsoleteAndUpdated(Table table,
                                         TableField<? extends UpdatableRecordImpl, Long> isObsoleteField,
                                         TableField<? extends UpdatableRecordImpl, LocalDateTime> updatedField,
                                         TableField<? extends UpdatableRecordImpl, Long> idField,
                                         Long id,
                                         boolean isObsolete,
                                         LocalDateTime updated) {
        dslContextProvider.ppc(shard)
                .update(table)
                .set(isObsoleteField, booleanToLong(isObsolete))
                .set(updatedField, updated)
                .where(idField.eq(id))
                .execute();
    }

    private Pair<Boolean, LocalDateTime> getCampaignObsoleteAndUpdated(Long campaignId) {
        return getObsoleteAndUpdated(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.IS_OBSOLETE,
                AGGR_STATUSES_CAMPAIGNS.UPDATED, AGGR_STATUSES_CAMPAIGNS.CID, campaignId);
    }


    private Pair<Boolean, LocalDateTime> getAdGroupObsoleteAndUpdated(Long adGroupId) {
        return getObsoleteAndUpdated(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE,
                AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.PID, adGroupId);
    }

    private Pair<Boolean, LocalDateTime> getBannerObsoleteAndUpdated(Long bannerId) {
        return getObsoleteAndUpdated(AGGR_STATUSES_BANNERS, AGGR_STATUSES_BANNERS.IS_OBSOLETE,
                AGGR_STATUSES_BANNERS.UPDATED, AGGR_STATUSES_BANNERS.BID, bannerId);
    }

    private Pair<Boolean, LocalDateTime> getKeywordObsoleteAndUpdated(Long keywordId) {
        return getObsoleteAndUpdated(AGGR_STATUSES_KEYWORDS, AGGR_STATUSES_KEYWORDS.IS_OBSOLETE,
                AGGR_STATUSES_KEYWORDS.UPDATED, AGGR_STATUSES_KEYWORDS.ID, keywordId);
    }

    private Pair<Boolean, LocalDateTime> getRetargetingObsoleteAndUpdated(Long retargetingId) {
        return getObsoleteAndUpdated(AGGR_STATUSES_RETARGETINGS, AGGR_STATUSES_RETARGETINGS.IS_OBSOLETE,
                AGGR_STATUSES_RETARGETINGS.UPDATED, AGGR_STATUSES_RETARGETINGS.RET_ID, retargetingId);
    }

    private Pair<Boolean, LocalDateTime> getObsoleteAndUpdated(Table table,
                                                               TableField<? extends UpdatableRecordImpl, Long> isObsoleteField,
                                                               TableField<? extends UpdatableRecordImpl, LocalDateTime>
                                                                       updateField,
                                                               TableField<? extends UpdatableRecordImpl, Long> idField,
                                                               Long id) {
        return dslContextProvider.ppc(shard)
                .select(isObsoleteField, updateField)
                .from(table)
                .where(idField.eq(id))
                .fetchOne(r -> Pair.of(booleanFromLong(r.get(isObsoleteField)), r.get(updateField)));
    }
}
