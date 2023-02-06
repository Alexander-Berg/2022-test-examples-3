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
import ru.yandex.direct.ess.router.testutils.BannerLogosChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_LOGOS;
import static ru.yandex.direct.ess.router.testutils.BannerLogosChange.createBannerLogosEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerLogosTest {

    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void insertNotSuitableModerationStatusTest() {
        BannerLogosChange bannerLogosChangeSending = new BannerLogosChange().withBid(1L);
        bannerLogosChangeSending.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "Sending");
        BannerLogosChange bannerLogosChangeSent = new BannerLogosChange().withBid(2L);
        bannerLogosChangeSent.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "Sent");

        BannerLogosChange bannerLogosChangeReady = new BannerLogosChange().withBid(3L);
        bannerLogosChangeReady.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "Ready");

        BannerLogosChange bannerLogosChangeNew = new BannerLogosChange().withBid(4L);
        bannerLogosChangeNew.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "New");

        var changes = List.of(
                bannerLogosChangeSending,
                bannerLogosChangeSent,
                bannerLogosChangeReady,
                bannerLogosChangeNew);
        var binlogEvent = createBannerLogosEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void insertSuitableModerationTest() {
        BannerLogosChange bannerLogosChangeYes = new BannerLogosChange().withBid(1L);
        bannerLogosChangeYes.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "Yes");

        BannerLogosChange bannerLogosChangeNo = new BannerLogosChange().withBid(2L);
        bannerLogosChangeNo.addInsertedColumn(BANNER_LOGOS.STATUS_MODERATE, "No");

        var binlogEvent = createBannerLogosEvent(List.of(bannerLogosChangeYes, bannerLogosChangeNo), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNotSuitableModerationStatusTest() {
        BannerLogosChange bannerLogosChangeSending = new BannerLogosChange().withBid(1L);
        bannerLogosChangeSending.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "Ready", "Sending");
        BannerLogosChange bannerLogosChangeSent = new BannerLogosChange().withBid(2L);
        bannerLogosChangeSent.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "Sending", "Sent");

        BannerLogosChange bannerLogosChangeReady = new BannerLogosChange().withBid(3L);
        bannerLogosChangeReady.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "New", "Ready");

        BannerLogosChange bannerLogosChangeNew = new BannerLogosChange().withBid(4L);
        bannerLogosChangeNew.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "Yes", "New");

        var changes = List.of(
                bannerLogosChangeSending,
                bannerLogosChangeSent,
                bannerLogosChangeReady,
                bannerLogosChangeNew);
        var binlogEvent = createBannerLogosEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void updateSuitableModerationTest() {
        BannerLogosChange bannerLogosChangeYes = new BannerLogosChange().withBid(1L);
        bannerLogosChangeYes.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "Sent", "Yes");

        BannerLogosChange bannerLogosChangeNo = new BannerLogosChange().withBid(2L);
        bannerLogosChangeNo.addChangedColumn(BANNER_LOGOS.STATUS_MODERATE, "Yes", "No");

        var binlogEvent = createBannerLogosEvent(List.of(bannerLogosChangeYes, bannerLogosChangeNo), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build()
        );
        assertThat(objects).hasSize(2);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteTest() {
        BannerLogosChange bannerLogosChange = new BannerLogosChange().withBid(1L);

        var binlogEvent = createBannerLogosEvent(List.of(bannerLogosChange), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .setDeleted(true)
                        .build()
        );
        assertThat(objects).hasSize(1);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateLogoTest() {
        BannerLogosChange bannerLogosChange = new BannerLogosChange().withBid(1L);
        bannerLogosChange.addChangedColumn(BANNER_LOGOS.IMAGE_HASH, "hash1", "hash2");

        var binlogEvent = createBannerLogosEvent(List.of(bannerLogosChange),
                Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_LOGO)
                        .build()
        );
        assertThat(objects).hasSize(1);
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
