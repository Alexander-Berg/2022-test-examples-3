package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannerPublisherTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_PUBLISHER;
import static ru.yandex.direct.ess.router.testutils.BannerPublisherTableChange.createBannerPublisherEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerZenSubscribeFilterTest {

    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void bannerPublisherInsertTest() {
        var change = new BannerPublisherTableChange().withBid(123L);
        change.addInsertedColumn(BANNER_PUBLISHER.ZEN_PUBLISHER_ID, "123abc");

        var binlogEvent = createBannerPublisherEvent(List.of(change), Operation.INSERT);
        assertChanges(binlogEvent);
    }

    @Test
    void bannerPublisherUpdateTest() {
        var change = new BannerPublisherTableChange().withBid(123L);
        change.addChangedColumn(BANNER_PUBLISHER.ZEN_PUBLISHER_ID, "123abc", "456xyz");

        var binlogEvent = createBannerPublisherEvent(List.of(change), Operation.UPDATE);
        assertChanges(binlogEvent);
    }

    private void assertChanges(BinlogEvent binlogEvent) {
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(123L)
                        .setResourceType(BannerResourceType.BANNER_ZEN_SUBSCRIBE)
                        .build());

        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
