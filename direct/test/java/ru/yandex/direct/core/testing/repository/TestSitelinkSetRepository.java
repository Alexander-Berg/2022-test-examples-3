package ru.yandex.direct.core.testing.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.Result;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.SITELINKS_LINKS;
import static ru.yandex.direct.dbschema.ppc.Tables.SITELINKS_SETS;
import static ru.yandex.direct.dbschema.ppc.Tables.SITELINKS_SET_TO_LINK;
import static ru.yandex.direct.dbschema.ppc.enums.BannersStatussitelinksmoderate.Yes;

public class TestSitelinkSetRepository {

    private final DslContextProvider dslContextProvider;

    public TestSitelinkSetRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public List<SitelinkSet> getSitelinkSetsByClientId(int shard, ClientId clientId) {
        Result<Record> result = dslContextProvider.ppc(shard)
                .select(SITELINKS_SET_TO_LINK.ORDER_NUM)
                .select(asList(SITELINKS_SETS.CLIENT_ID, SITELINKS_SETS.SITELINKS_SET_ID,
                        SITELINKS_SETS.LINKS_HASH))
                .select(asList(SITELINKS_LINKS.SL_ID, SITELINKS_LINKS.HREF, SITELINKS_LINKS.HASH,
                        SITELINKS_LINKS.TITLE, SITELINKS_LINKS.DESCRIPTION))
                .from(SITELINKS_SET_TO_LINK)
                .join(SITELINKS_SETS)
                .on(SITELINKS_SETS.SITELINKS_SET_ID.eq(SITELINKS_SET_TO_LINK.SITELINKS_SET_ID))
                .join(SITELINKS_LINKS)
                .on(SITELINKS_LINKS.SL_ID.eq(SITELINKS_SET_TO_LINK.SL_ID))
                .where(SITELINKS_SETS.CLIENT_ID.eq(clientId.asLong()))
                .fetch();

        Map<Long, SitelinkSet> sitelinkSetsByIds = new HashMap<>();

        for (Record record : result) {
            Long sitelinkSetId = record.get(SITELINKS_SETS.SITELINKS_SET_ID);

            SitelinkSet sitelinkSet = sitelinkSetsByIds.get(sitelinkSetId);

            if (sitelinkSet == null) {
                sitelinkSet = buildSitelinkSetFromRecord(record);
                sitelinkSetsByIds.put(sitelinkSetId, sitelinkSet);
            } else {
                Sitelink sitelink = buildSitelinkFromRecord(record);
                sitelinkSet.getSitelinks().add(sitelink);
            }
        }

        List<SitelinkSet> sitelinkSets = new ArrayList<>(sitelinkSetsByIds.values());
        for (SitelinkSet sitelinkSet : sitelinkSets) {
            sitelinkSet.getSitelinks().sort(Comparator.comparing(Sitelink::getOrderNum));
        }

        return sitelinkSets;
    }

    private Sitelink buildSitelinkFromRecord(Record record) {
        return new Sitelink()
                .withId(record.get(SITELINKS_LINKS.SL_ID))
                .withHref(record.get(SITELINKS_LINKS.HREF))
                .withTitle(record.get(SITELINKS_LINKS.TITLE))
                .withDescription(record.get(SITELINKS_LINKS.DESCRIPTION))
                .withOrderNum(record.get(SITELINKS_SET_TO_LINK.ORDER_NUM))
                .withHash(record.get(SITELINKS_LINKS.HASH).toBigInteger());
    }

    private SitelinkSet buildSitelinkSetFromRecord(Record record) {
        Sitelink sitelink = buildSitelinkFromRecord(record);
        List<Sitelink> sitelinks = new ArrayList<>();
        sitelinks.add(sitelink);

        return new SitelinkSet()
                .withId(record.get(SITELINKS_SETS.SITELINKS_SET_ID))
                .withClientId(record.get(SITELINKS_SETS.CLIENT_ID))
                .withLinksHash(record.get(SITELINKS_SETS.LINKS_HASH).toBigInteger())
                .withSitelinks(sitelinks);
    }

    public void linkBannerWithSitelinkSet(int shard, Long campaignId, Long bannerId, Long sitelinkSetId) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_SITELINKS_MODERATE, Yes)
                .set(BANNERS.SITELINKS_SET_ID, sitelinkSetId)
                .where(BANNERS.BID.eq(bannerId))
                .and(BANNERS.CID.eq(campaignId))
                .execute();
    }
}
