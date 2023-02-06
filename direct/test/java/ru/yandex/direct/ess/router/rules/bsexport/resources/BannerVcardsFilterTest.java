package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.rules.bsexport.resources.filter.BannerVcardFilter;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

class BannerVcardsFilterTest {

    private final BsExportBannerResourcesRule rule = new BsExportBannerResourcesRule(List.of(new BannerVcardFilter()));

    @Test
    void insertNotVcardModerationStatusTest() {
        var changeModerateToSending =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToSending.addInsertedColumn(BANNERS.PHONEFLAG, "Sending");
        var changeModerateToSent =
                new BannersTableChange().withBid(2L).withPid(11L).withCid(21L).withBannerType(BannersBannerType.text);
        changeModerateToSent.addInsertedColumn(BANNERS.PHONEFLAG, "Sent");

        var changeModerateToReady =
                new BannersTableChange().withBid(3L).withPid(12L).withCid(22L).withBannerType(BannersBannerType.text);
        changeModerateToReady.addInsertedColumn(BANNERS.PHONEFLAG, "Ready");

        var changeModerateToNew =
                new BannersTableChange().withBid(4L).withPid(13L).withCid(23L).withBannerType(BannersBannerType.text);
        changeModerateToNew.addInsertedColumn(BANNERS.PHONEFLAG, "New");

        var changes = List.of(
                changeModerateToSending,
                changeModerateToSent,
                changeModerateToReady,
                changeModerateToNew);
        var binlogEvent = createBannersEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_VCARD);
    }

    @Test
    void insertSuitableVcardModerationStatusTest() {
        var changeModerateToYes =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToYes.addInsertedColumn(BANNERS.PHONEFLAG, "Yes");

        var changeModerateToNo =
                new BannersTableChange().withBid(2L).withPid(12L).withCid(22L).withBannerType(BannersBannerType.text);
        changeModerateToYes.addInsertedColumn(BANNERS.PHONEFLAG, "No");


        var binlogEvent = createBannersEvent(List.of(changeModerateToYes, changeModerateToNo), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setPid(10L)
                        .setCid(20L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setPid(12L)
                        .setCid(22L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNotSuitableVcardModerationStatusTest() {
        var changeModerateToSending =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToSending.addChangedColumn(BANNERS.PHONEFLAG, "Ready", "Sending");
        var changeModerateToSent =
                new BannersTableChange().withBid(2L).withPid(11L).withCid(21L).withBannerType(BannersBannerType.text);
        changeModerateToSent.addChangedColumn(BANNERS.PHONEFLAG, "Sending", "Sent");

        var changeModerateToReady =
                new BannersTableChange().withBid(3L).withPid(12L).withCid(22L).withBannerType(BannersBannerType.text);
        changeModerateToReady.addChangedColumn(BANNERS.PHONEFLAG, "Sent", "Ready");
        var changeModerateToNew =
                new BannersTableChange().withBid(4L).withPid(13L).withCid(23L).withBannerType(BannersBannerType.text);
        changeModerateToNew.addChangedColumn(BANNERS.PHONEFLAG, "Yes", "New");

        var changes = List.of(
                changeModerateToSending,
                changeModerateToSent,
                changeModerateToReady,
                changeModerateToNew);
        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_VCARD);
    }

    @Test
    void updateNotSuitableVcardModerationStatus_AndVcardIdNullTest() {
        var change =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        change.addChangedColumn(BANNERS.PHONEFLAG, "Ready", "New");
        change.addChangedColumn(BANNERS.VCARD_ID, 123, null);

        var changes = List.of(change);
        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject =
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setPid(10L)
                        .setCid(20L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build();
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObject);
    }

    @Test
    void updateSuitableVcardModerationStatusTest() {
        var changeModerateToYes =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToYes.addChangedColumn(BANNERS.PHONEFLAG, "Sent", "Yes");

        var changeModerateToNo =
                new BannersTableChange().withBid(2L).withPid(12L).withCid(22L).withBannerType(BannersBannerType.text);
        changeModerateToNo.addChangedColumn(BANNERS.PHONEFLAG, "Sent", "No");


        var binlogEvent = createBannersEvent(List.of(changeModerateToYes, changeModerateToNo), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setPid(10L)
                        .setCid(20L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setPid(12L)
                        .setCid(22L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateVcardIdTest() {
        var changeVcardId = new BannersTableChange()
                .withBid(1L)
                .withCid(10L)
                .withPid(20L)
                .withBannerType(BannersBannerType.text);
        changeVcardId.addChangedColumn(BANNERS.VCARD_ID, 123L, 345L);

        var changeVcardIdToNull = new BannersTableChange()
                .withBid(2L)
                .withCid(12L)
                .withPid(22L)
                .withBannerType(BannersBannerType.text);
        changeVcardIdToNull.addChangedColumn(BANNERS.VCARD_ID, 123L, null);

        var binlogEvent = createBannersEvent(
                List.of(changeVcardId, changeVcardIdToNull), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setPid(20L)
                        .setCid(10L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setPid(22L)
                        .setCid(12L)
                        .setResourceType(BannerResourceType.BANNER_VCARD)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    private void assertThatNotContainsResourceType(List<BsExportBannerResourcesObject> objects,
                                                   BannerResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();
    }
}
