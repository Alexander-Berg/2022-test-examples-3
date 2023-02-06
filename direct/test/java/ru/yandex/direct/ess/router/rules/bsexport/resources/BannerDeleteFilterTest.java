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
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerDeleteFilterTest {
    @Autowired
    private BsExportBannerResourcesRule rule;

    @Test
    void deleteBanner() {
        var bannersTableChange = new BannersTableChange().withBid(6L).withCid(2L).withPid(4L);
        bannersTableChange.addDeletedColumn(BANNERS.BANNER_ID, 124L);


        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setBannerId(124L)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedObjects);
    }

    @Test
    void deleteBannerWithoutBannerId() {
        var bannersTableChange = new BannersTableChange().withBid(6L).withCid(2L).withPid(4L);
        bannersTableChange.addDeletedColumn(BANNERS.BANNER_ID, 0L);


        var changes = List.of(bannersTableChange);

        var binlogEvent = createBannersEvent(changes, Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setBannerId(0L)
                .setResourceType(BannerResourceType.BANNER_DELETE)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedObjects);
    }
}
