package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.rules.bsexport.resources.filter.BannerTurboLandingsFilter;
import ru.yandex.direct.ess.router.testutils.BannerTurboLandingParamsChange;
import ru.yandex.direct.ess.router.testutils.BannerTurboLandingsChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDING_PARAMS;
import static ru.yandex.direct.ess.router.testutils.BannerTurboLandingParamsChange.createBannerTurboLandingParamsEvent;
import static ru.yandex.direct.ess.router.testutils.BannerTurboLandingsChange.createBannerTurboLandingsEvent;

class BannerTurboLandingsFilterTest {

    private final BsExportBannerResourcesRule rule =
            new BsExportBannerResourcesRule(List.of(new BannerTurboLandingsFilter()));

    @Test
    void insertNotSuitableModerationStatusTest() {
        var changeModerateToSending = new BannerTurboLandingsChange().withBid(1L);
        changeModerateToSending.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Sending");
        var changeModerateToSent = new BannerTurboLandingsChange().withBid(2L);
        changeModerateToSent.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Sent");

        var changeModerateToReady = new BannerTurboLandingsChange().withBid(3L);
        changeModerateToReady.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Ready");

        var changeModerateToNew = new BannerTurboLandingsChange().withBid(4L);
        changeModerateToNew.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "New");

        var changes = List.of(
                changeModerateToSending,
                changeModerateToSent,
                changeModerateToReady,
                changeModerateToNew);
        var binlogEvent = createBannerTurboLandingsEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void insertSuitableModerationTest() {
        var changeModerateToYes = new BannerTurboLandingsChange().withBid(1L);
        changeModerateToYes.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Yes");

        var changeModerateToNo = new BannerTurboLandingsChange().withBid(2L);
        changeModerateToNo.addInsertedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "No");

        var binlogEvent = createBannerTurboLandingsEvent(
                List.of(changeModerateToYes, changeModerateToNo), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateNotSuitableModerationStatusTest() {
        var changeModerateToSending = new BannerTurboLandingsChange().withBid(1L);
        changeModerateToSending.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Ready", "Sending");
        var changeModerateToSent = new BannerTurboLandingsChange().withBid(2L);
        changeModerateToSent.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Sending", "Sent");

        var changeModerateToReady = new BannerTurboLandingsChange().withBid(3L);
        changeModerateToReady.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "New", "Ready");

        var changeModerateToNew = new BannerTurboLandingsChange().withBid(4L);
        changeModerateToNew.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Yes", "New");

        var changes = List.of(
                changeModerateToSending,
                changeModerateToSent,
                changeModerateToReady,
                changeModerateToNew);
        var binlogEvent = createBannerTurboLandingsEvent(changes, Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects).isEmpty();
    }

    @Test
    void updateSuitableModerationTest() {
        var changeStatusModerateYes = new BannerTurboLandingsChange().withBid(1L);
        changeStatusModerateYes.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Sent", "Yes");

        var changeStatusModerateNo = new BannerTurboLandingsChange().withBid(2L);
        changeStatusModerateNo.addChangedColumn(BANNER_TURBOLANDINGS.STATUS_MODERATE, "Yes", "No");

        var binlogEvent = createBannerTurboLandingsEvent(
                List.of(changeStatusModerateYes, changeStatusModerateNo), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setBid(2L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteTest() {
        var change = new BannerTurboLandingsChange().withBid(1L);

        var binlogEvent = createBannerTurboLandingsEvent(List.of(change), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .setDeleted(true)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateBannerTurboLandingParamsTest() {
        var change = new BannerTurboLandingParamsChange().withBid(1L);
        change.addChangedColumn(BANNER_TURBOLANDING_PARAMS.HREF_PARAMS, "params1", "params2");

        var binlogEvent = createBannerTurboLandingParamsEvent(List.of(change), Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void insertBannerTurboLandingParamsTest() {
        var change = new BannerTurboLandingParamsChange().withBid(1L);
        change.addInsertedColumn(BANNER_TURBOLANDING_PARAMS.HREF_PARAMS, "params1");

        var binlogEvent = createBannerTurboLandingParamsEvent(List.of(change), Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void deleteBannerTurboLandingParamsTest() {
        var change = new BannerTurboLandingParamsChange().withBid(1L);
        change.addDeletedColumn(BANNER_TURBOLANDING_PARAMS.HREF_PARAMS, "params1");

        var binlogEvent = createBannerTurboLandingParamsEvent(List.of(change), Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(1L)
                        .setResourceType(BannerResourceType.BANNER_TURBOLANDING)
                        .build()
        );
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
