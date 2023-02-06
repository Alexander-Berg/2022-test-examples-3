package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.rules.bsexport.resources.filter.BannerTitleExtensionFilter;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

class BannerTitleExtensionFilterTest {
    private final BsExportBannerResourcesRule rule =
            new BsExportBannerResourcesRule(List.of(new BannerTitleExtensionFilter()));

    @Test
    void bannerChangeStatusModerateToYesTest() {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(BannersBannerType.text);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.Yes.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @Test
    void bannerInsertWithStatusModerateYesTest() {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(BannersBannerType.text);
        bannersTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Yes.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @Test
    void bannerInsertWithStatusModerateNoTest() {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(BannersBannerType.text);
        bannersTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.No.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_TITLE_EXTENSION);
    }

    @Test
    void bannerChangeStatusModerateToNoTest() {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(BannersBannerType.text);
        bannersTableChange.addChangedColumn(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.No.getLiteral());

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_TITLE_EXTENSION);
    }

    @Test
    void bannerChangeTitleExtensionTest() {
        var bannersTableChange =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(BannersBannerType.text);
        bannersTableChange.addChangedColumn(BANNERS.TITLE_EXTENSION, "Расширение_До", "Расширение_После");

        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_TITLE_EXTENSION)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    private void assertThatNotContainsResourceType(List<BsExportBannerResourcesObject> objects,
                                                   BannerResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();
    }
}
