package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.toSource;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_PERMALINKS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGN_PERMALINKS;
import static ru.yandex.direct.dbschema.ppc.enums.BannerPermalinksPermalinkAssignType.auto;

@Repository
public class TestOrganizationRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void changePermalinkAssignType(int shard, Long bannerId, Long permalinkId,
                                          PermalinkAssignType permalinkAssignType) {
        dslContextProvider.ppc(shard)
                .update(BANNER_PERMALINKS)
                .set(BANNER_PERMALINKS.PERMALINK_ASSIGN_TYPE, toSource(permalinkAssignType))
                .where(BANNER_PERMALINKS.PERMALINK.eq(permalinkId))
                .and(BANNER_PERMALINKS.BID.eq(bannerId))
                .execute();
    }

    public void addAutoPermalink(int shard, Long bannerId, Long permalinkId) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_PERMALINKS)
                .columns(BANNER_PERMALINKS.BID, BANNER_PERMALINKS.PERMALINK, BANNER_PERMALINKS.PERMALINK_ASSIGN_TYPE)
                .values(bannerId, permalinkId, auto)
                .execute();
    }

    public void linkDefaultOrganizationToCampaign(int shard, Long permalinkId, Long campaignId) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMPAIGN_PERMALINKS)
                .columns(CAMPAIGN_PERMALINKS.CID, CAMPAIGN_PERMALINKS.PERMALINK_ID)
                .values(campaignId, permalinkId)
                .execute();
    }
}
