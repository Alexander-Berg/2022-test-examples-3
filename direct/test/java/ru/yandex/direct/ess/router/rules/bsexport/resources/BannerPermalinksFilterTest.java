package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.rules.bsexport.resources.filter.BannerPermalinksFilter;
import ru.yandex.direct.ess.router.testutils.BannerPermalinksChange;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.OrganizationsChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_PERMALINKS;
import static ru.yandex.direct.dbschema.ppc.Tables.ORGANIZATIONS;
import static ru.yandex.direct.ess.router.testutils.BannerPermalinksChange.createBannerPermalinksEvent;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.OrganizationsChange.createOrganizationsEvent;


class BannerPermalinksFilterTest {

    private final BsExportBannerResourcesRule rule =
            new BsExportBannerResourcesRule(List.of(new BannerPermalinksFilter()));

    @Test
    void updateNotSuitableModerationStatusTest() {
        var changeModerateToSending =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToSending.addChangedColumn(BANNERS.STATUS_MODERATE, "Ready", "Sending");
        var changeModerateToSent =
                new BannersTableChange().withBid(2L).withPid(11L).withCid(21L).withBannerType(BannersBannerType.text);
        changeModerateToSent.addChangedColumn(BANNERS.STATUS_MODERATE, "Sending", "Sent");

        var changeModerateToReady =
                new BannersTableChange().withBid(3L).withPid(12L).withCid(22L).withBannerType(BannersBannerType.text);
        changeModerateToReady.addChangedColumn(BANNERS.STATUS_MODERATE, "Sent", "Ready");

        var changeModerateToNo =
                new BannersTableChange().withBid(5L).withPid(14L).withCid(24L).withBannerType(BannersBannerType.text);
        changeModerateToReady.addChangedColumn(BANNERS.STATUS_MODERATE, "Sent", "No");

        var changeModerateToNew =
                new BannersTableChange().withBid(4L).withPid(13L).withCid(23L).withBannerType(BannersBannerType.text);
        changeModerateToNew.addChangedColumn(BANNERS.STATUS_MODERATE, "Yes", "New");

        var changes = List.of(
                changeModerateToSending,
                changeModerateToSent,
                changeModerateToNo,
                changeModerateToReady,
                changeModerateToNew);
        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_PERMALINKS);
    }

    @Test
    void updateSuitableModerationTest() {
        var changeModerateToYes =
                new BannersTableChange().withBid(1L).withPid(10L).withCid(20L).withBannerType(BannersBannerType.text);
        changeModerateToYes.addChangedColumn(BANNERS.STATUS_MODERATE, "Sent", "Yes");


        var binlogEvent = createBannersEvent(List.of(changeModerateToYes), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setPid(10L)
                        .setCid(20L)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateOrganizationStatusPublishTest() {
        var statusPublishChangeToUnpublished = new OrganizationsChange().withPermalinkId(14L);
        statusPublishChangeToUnpublished.addChangedColumn(ORGANIZATIONS.STATUS_PUBLISH, "published", "unpublished");

        var statusPublishChangeToPublished = new OrganizationsChange().withPermalinkId(18L);
        statusPublishChangeToPublished.addChangedColumn(ORGANIZATIONS.STATUS_PUBLISH, "unpublished", "published");

        var binlogEvent = createOrganizationsEvent(
                List.of(statusPublishChangeToUnpublished, statusPublishChangeToPublished), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(14L)
                        .setAdditionalTable(TablesEnum.ORGANIZATIONS)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(18L)
                        .setAdditionalTable(TablesEnum.ORGANIZATIONS)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteFromBannerPermalinksTest() {
        var change = new BannerPermalinksChange().withBid(1L).withPermalinkId(123L);

        var binlogEvent = createBannerPermalinksEvent(List.of(change), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .setDeleted(false)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateBannerPermalinksTest() {
        var change = new BannerPermalinksChange().withBid(1L).withPermalinkId(1234L);
        change.addChangedColumn(BANNER_PERMALINKS.PREFER_VCARD_OVER_PERMALINK, 0, 1);

        var binlogEvent = createBannerPermalinksEvent(List.of(change), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void insertBannerPermalinksTest() {
        var change = new BannerPermalinksChange().withBid(1L).withPermalinkId(123L);

        var binlogEvent = createBannerPermalinksEvent(List.of(change), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_PERMALINKS)
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
