package ru.yandex.direct.ess.router.rules.metrika.bannerupdate;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.metrika.bannerupdate.MetrikaBannerUpdateObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannerImagesTableChange;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.ess.router.testutils.BannerImagesTableChange.createBannerImagesEvent;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class MetrikaBannerUpdateRuleTest {

    @Autowired
    private MetrikaBannerUpdateRule rule;

    @Test
    void mapBinlogEventTest_UpdateBannerTitle() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        var bannersTableChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChange.addChangedColumn(BANNERS.TITLE, "title1", "title2");
        bannersTableChanges.add(bannersTableChange);


        /* Поле title есть в списке изменившихся полей, но его значение не изменилось */
        var bannersTableChangeTitlesEquals =
                new BannersTableChange().withBid(2L).withCid(4L).withPid(4L);
        bannersTableChangeTitlesEquals.addChangedColumn(BANNERS.TITLE, "title1", "title1");
        bannersTableChanges.add(bannersTableChangeTitlesEquals);

        var binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        var resultObjects = rule.mapBinlogEvent(binlogEvent);
        var expected = new MetrikaBannerUpdateObject[]{
                new MetrikaBannerUpdateObject(TablesEnum.BANNERS, 1L)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBannerBody() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        var bannersTableChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChange.addChangedColumn(BANNERS.BODY, "body1", "body2");
        bannersTableChanges.add(bannersTableChange);


        /* Поле body есть в списке изменившихся полей, но его значение не изменилось */
        BannersTableChange bannersTableChangeBodiesEquals =
                new BannersTableChange().withBid(2L).withCid(4L).withPid(4L);
        bannersTableChangeBodiesEquals.addChangedColumn(BANNERS.BODY, "body1", "body1");
        bannersTableChanges.add(bannersTableChangeBodiesEquals);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        List<MetrikaBannerUpdateObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        MetrikaBannerUpdateObject[] expected = new MetrikaBannerUpdateObject[]{
                new MetrikaBannerUpdateObject(TablesEnum.BANNERS, 1L)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBannerBannerId() {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        BannersTableChange bannersTableChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L);
        bannersTableChange.addChangedColumn(BANNERS.BANNER_ID, 0L, 1L);
        bannersTableChanges.add(bannersTableChange);


        /* Поле BannerId есть в списке изменившихся полей, но его значение не изменилось */
        BannersTableChange bannersTableChangeBannerIdsEquals =
                new BannersTableChange().withBid(2L).withCid(4L).withPid(4L);
        bannersTableChangeBannerIdsEquals.addChangedColumn(BANNERS.BANNER_ID, 0L, 0L);
        bannersTableChanges.add(bannersTableChangeBannerIdsEquals);

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        List<MetrikaBannerUpdateObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        MetrikaBannerUpdateObject[] expected = new MetrikaBannerUpdateObject[]{
                new MetrikaBannerUpdateObject(TablesEnum.BANNERS, 1L)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @Test
    void mapBinlogEventTest_UpdateBannerImageBannerId() {
        List<BannerImagesTableChange> bannerImagesTableChanges = new ArrayList<>();

        BannerImagesTableChange bannerImagesTableChange =
                new BannerImagesTableChange().withImageId(1L);
        bannerImagesTableChange.addChangedColumn(BANNER_IMAGES.BANNER_ID, 0L, 1L);
        bannerImagesTableChanges.add(bannerImagesTableChange);


        /* Поле BannerId есть в списке изменившихся полей, но его значение не изменилось */
        BannerImagesTableChange bannerImagesTableChangeBannerIdsEquals =
                new BannerImagesTableChange().withImageId(2L);
        bannerImagesTableChangeBannerIdsEquals.addChangedColumn(BANNERS.BANNER_ID, 0L, 0L);
        bannerImagesTableChanges.add(bannerImagesTableChangeBannerIdsEquals);

        BinlogEvent binlogEvent = createBannerImagesEvent(bannerImagesTableChanges, UPDATE);
        List<MetrikaBannerUpdateObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        MetrikaBannerUpdateObject[] expected = new MetrikaBannerUpdateObject[]{
                new MetrikaBannerUpdateObject(TablesEnum.BANNER_IMAGES, 1L)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }
}
