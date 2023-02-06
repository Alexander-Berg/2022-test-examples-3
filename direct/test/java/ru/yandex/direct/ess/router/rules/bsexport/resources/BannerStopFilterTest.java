package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.rules.bsexport.resources.filter.BannerStopFilter;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

class BannerStopFilterTest {

    private final BsExportBannerResourcesRule rule = new BsExportBannerResourcesRule(List.of(new BannerStopFilter()));

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerChangeStatusModerateToYesTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.Yes.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerInsertWithStatusModerateYesTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Yes.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerInsertWithStatusModerateNoTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.No.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerChangeStatusModerateToNoTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.No.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerChangeStatusShowTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_SHOW, BannersStatusshow.No, BannersStatusshow.Yes);

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = BannersBannerType.class)
    void bannerChangeStatusPostModerateTest(BannersBannerType bannerType) {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(bannerType);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_POST_MODERATE,
                BannersStatuspostmoderate.Yes, BannersStatuspostmoderate.Rejected);

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }
}
