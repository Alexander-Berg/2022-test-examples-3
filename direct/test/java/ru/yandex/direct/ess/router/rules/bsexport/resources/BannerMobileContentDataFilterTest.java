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
import ru.yandex.direct.ess.router.testutils.BannersMobileContentTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.BUY;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.DOWNLOAD;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_MOBILE_CONTENT;
import static ru.yandex.direct.ess.router.testutils.BannersMobileContentTableChange.createBannersMobileContentEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerMobileContentDataFilterTest {

    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void bannersMobileContentDataInsertTest() {
        var impressionUrlInsertion =
                new BannersMobileContentTableChange().withBid(666L);
        impressionUrlInsertion.addInsertedColumn(BANNERS_MOBILE_CONTENT.IMPRESSION_URL,
                                                 "https://view.adjust.com/impression/asdfgh");

        var binlogEvent = createBannersMobileContentEvent(List.of(impressionUrlInsertion), Operation.INSERT);
        assertChanges(binlogEvent);
    }

    @Test
    void impressionUrlChangesTest() {
        var impressionUrlChange =
                new BannersMobileContentTableChange().withBid(666L);
        impressionUrlChange.addChangedColumn(BANNERS_MOBILE_CONTENT.IMPRESSION_URL,
                                             "https://view.adjust.com/impression/asdfgh",
                                             "https://view.adjust.com/impression/asdfgh?s2s=1&ya_click_id={logid}");

        var binlogEvent = createBannersMobileContentEvent(List.of(impressionUrlChange), Operation.UPDATE);
        assertChanges(binlogEvent);
    }

    @Test
    void notImpressionUrlChangesTest() {
        var primaryActionChange =
                new BannersMobileContentTableChange().withBid(666L);
        primaryActionChange.addChangedColumn(BANNERS_MOBILE_CONTENT.PRIMARY_ACTION, BUY, DOWNLOAD);

        var binlogEvent = createBannersMobileContentEvent(List.of(primaryActionChange), Operation.UPDATE);
        assertThat(rule.mapBinlogEvent(binlogEvent)).isEqualTo(List.of());
    }

    private void assertChanges(BinlogEvent binlogEvent) {
        var objects = rule.mapBinlogEvent(binlogEvent);

        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setBid(666L)
                        .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT_DATA)
                        .build());

        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }
}
