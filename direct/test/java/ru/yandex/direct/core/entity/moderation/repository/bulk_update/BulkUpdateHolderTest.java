package ru.yandex.direct.core.entity.moderation.repository.bulk_update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.TableField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldAbstractBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.dbschema.ppc.tables.records.BannersRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BulkUpdateHolderTest {
    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private List<OldTextBanner> banners = new ArrayList<>();

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();

        clientInfo = campaignInfo.getClientInfo();

        shard = clientInfo.getShard();

        for (int i = 0; i < 300; i++) {
            banners.add(makeBanner());
        }
    }

    private OldTextBanner makeBanner() {
        return steps.bannerSteps()

                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withStatusModerate(OldBannerStatusModerate.READY),
                        clientInfo
                )
                .getBanner();
    }

    @Test
    public void test() {
        List<List<Pair<TableField<BannersRecord, ?>, ?>>> combinations = changesCombinations();
        int size = combinations.size();

        BulkUpdateHolder holder = new BulkUpdateHolder();

        var bannerChanges = holder.get(BANNERS.BID);

        for (var banner : banners) {
            int pos = (int) (banner.getId() % size);

            List<Pair<TableField<BannersRecord, ?>, ?>> update =
                    combinations.get(pos);

            var rowChanges = bannerChanges.forId(banner.getId());

            for (var pair : update) {
                //noinspection unchecked
                rowChanges.set((TableField<BannersRecord, Object>) pair.getKey(), pair.getValue());
            }

        }

        holder.execute(dslContextProvider.ppc(shard).configuration());

        var result = dslContextProvider.ppc(shard)
                .select(BANNERS.BID, BANNERS.STATUS_MODERATE, BANNERS.STATUS_POST_MODERATE, BANNERS.STATUS_BS_SYNCED,
                        BANNERS.STATUS_SHOW)
                .from(BANNERS)
                .where(BANNERS.BID.in(banners.stream().map(OldAbstractBanner::getId).collect(Collectors.toList())))
                .fetch();


        assertThat(result).hasSize(banners.size());

        for (var b : result) {
            Long bid = b.get(BANNERS.BID);
            int pos = (int) (bid % size);

            List<Pair<TableField<BannersRecord, ?>, ?>> update =
                    combinations.get(pos);

            for (var pair : update) {
                assertThat(b.get(pair.getLeft())).isEqualTo(pair.getRight());
            }

        }

    }

    private List<List<Pair<TableField<BannersRecord, ?>, ?>>> changesCombinations() {
        return Lists.cartesianProduct(List.of(
                List.of(
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Yes),
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.No),
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Ready),
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sending),
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.New),
                        Pair.of(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sent)
                ),
                List.of(
                        Pair.of(BANNERS.STATUS_POST_MODERATE, BannersStatuspostmoderate.Yes),
                        Pair.of(BANNERS.STATUS_POST_MODERATE, BannersStatuspostmoderate.No),
                        Pair.of(BANNERS.STATUS_POST_MODERATE, BannersStatuspostmoderate.Rejected),
                        Pair.of(BANNERS.STATUS_POST_MODERATE, BannersStatuspostmoderate.Sent)
                ),
                List.of(
                        Pair.of(BANNERS.STATUS_BS_SYNCED, BannersStatusbssynced.Yes),
                        Pair.of(BANNERS.STATUS_BS_SYNCED, BannersStatusbssynced.No),
                        Pair.of(BANNERS.STATUS_BS_SYNCED, BannersStatusbssynced.Sending)
                ),
                List.of(
                        Pair.of(BANNERS.STATUS_SHOW, BannersStatusshow.Yes),
                        Pair.of(BANNERS.STATUS_SHOW, BannersStatusshow.No)
                )
        ));
    }

}
