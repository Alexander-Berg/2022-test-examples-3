package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannerButtonsChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_BUTTONS;
import static ru.yandex.direct.ess.router.testutils.BannerButtonsChange.createBannerButtonsEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerButtonsTest {

    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void insertNotSuitableModerationStatusTest() {
        BannerButtonsChange bannerButtonsChangeSending = new BannerButtonsChange().withBid(1L);
        bannerButtonsChangeSending.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Sending");
        BannerButtonsChange bannerButtonsChangeSent = new BannerButtonsChange().withBid(2L);
        bannerButtonsChangeSent.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Sent");

        BannerButtonsChange bannerButtonsChangeReady = new BannerButtonsChange().withBid(3L);
        bannerButtonsChangeReady.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Ready");

        BannerButtonsChange bannerButtonsChangeNew = new BannerButtonsChange().withBid(4L);
        bannerButtonsChangeNew.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "New");

        var changes = List.of(
                bannerButtonsChangeSending,
                bannerButtonsChangeSent,
                bannerButtonsChangeReady,
                bannerButtonsChangeNew);
        var binlogEvent = createBannerButtonsEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void insertSuitableModerationTest() {
        BannerButtonsChange bannerButtonsChangeYes = new BannerButtonsChange().withBid(1L);
        bannerButtonsChangeYes.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Yes");

        BannerButtonsChange bannerButtonsChangeNo = new BannerButtonsChange().withBid(2L);
        bannerButtonsChangeNo.addInsertedColumn(BANNER_BUTTONS.STATUS_MODERATE, "No");

        var binlogEvent = createBannerButtonsEvent(List.of(bannerButtonsChangeYes, bannerButtonsChangeNo),
                Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNotSuitableModerationStatusTest() {
        BannerButtonsChange bannerButtonsChangeSending = new BannerButtonsChange().withBid(1L);
        bannerButtonsChangeSending.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Ready", "Sending");
        BannerButtonsChange bannerButtonsChangeSent = new BannerButtonsChange().withBid(2L);
        bannerButtonsChangeSent.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Sending", "Sent");

        BannerButtonsChange bannerButtonsChangeReady = new BannerButtonsChange().withBid(3L);
        bannerButtonsChangeReady.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "New", "Ready");

        BannerButtonsChange bannerButtonsChangeNew = new BannerButtonsChange().withBid(4L);
        bannerButtonsChangeNew.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Yes", "New");

        var changes = List.of(
                bannerButtonsChangeSending,
                bannerButtonsChangeSent,
                bannerButtonsChangeReady,
                bannerButtonsChangeNew);
        var binlogEvent = createBannerButtonsEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void updateSuitableModerationTest() {
        BannerButtonsChange bannerButtonsChangeYes = new BannerButtonsChange().withBid(1L);
        bannerButtonsChangeYes.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Sent", "Yes");

        BannerButtonsChange bannerButtonsChangeNo = new BannerButtonsChange().withBid(2L);
        bannerButtonsChangeNo.addChangedColumn(BANNER_BUTTONS.STATUS_MODERATE, "Yes", "No");

        var binlogEvent = createBannerButtonsEvent(List.of(bannerButtonsChangeYes, bannerButtonsChangeNo),
                Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteTest() {
        BannerButtonsChange bannerButtonsChange = new BannerButtonsChange().withBid(1L);

        var binlogEvent = createBannerButtonsEvent(List.of(bannerButtonsChange), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .setDeleted(true)
                        .build()
        );
        assertThat(objects).hasSize(1);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateButtonsTest() {
        BannerButtonsChange bannerButtonsChangeHref = new BannerButtonsChange().withBid(1L);
        bannerButtonsChangeHref.addChangedColumn(BANNER_BUTTONS.HREF, "href1", "href2");

        BannerButtonsChange bannerButtonsChangeCaption = new BannerButtonsChange().withBid(2L);
        bannerButtonsChangeCaption.addChangedColumn(BANNER_BUTTONS.CAPTION, "caption1", "caption2");

        BannerButtonsChange bannerButtonsChangeKey = new BannerButtonsChange().withBid(3L);
        bannerButtonsChangeKey.addChangedColumn(BANNER_BUTTONS.KEY, "key1", "key2");

        var binlogEvent = createBannerButtonsEvent(List.of(bannerButtonsChangeHref, bannerButtonsChangeCaption,
                bannerButtonsChangeKey),
                Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(3L)
                        .setResourceType(BannerResourceType.BANNER_BUTTON)
                        .build()
        );
        assertThat(objects).hasSize(3);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
