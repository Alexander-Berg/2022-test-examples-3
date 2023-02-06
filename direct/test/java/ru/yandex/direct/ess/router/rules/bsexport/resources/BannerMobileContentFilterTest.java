package ru.yandex.direct.ess.router.rules.bsexport.resources;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;
import ru.yandex.direct.ess.router.testutils.CampaignMobileContentTableChange;
import ru.yandex.direct.ess.router.testutils.MobileContentTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MOBILE_CONTENT;
import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT;
import static ru.yandex.direct.dbschema.ppc.enums.BannersBannerType.cpc_video;
import static ru.yandex.direct.dbschema.ppc.enums.BannersBannerType.image_ad;
import static ru.yandex.direct.dbschema.ppc.enums.BannersBannerType.mobile_content;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;
import static ru.yandex.direct.ess.router.testutils.CampaignMobileContentTableChange.createCampaignsMobileContentEvent;
import static ru.yandex.direct.ess.router.testutils.MobileContentTableChange.createMobileContentEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BannerMobileContentFilterTest {

    private static final List<BannersBannerType> BANNER_TYPES_WITH_MOBILE_CONTENT =
            List.of(image_ad, cpc_video, mobile_content);
    @Autowired
    private BsExportBannerResourcesRule rule;

    @ParameterizedTest
    @MethodSource("allowedBannerTypes")
    void bannerChangeStatusModerate_BannersWithMobileContentTest(BannersBannerType type) {
        var bannersTableChangeStatusModerateYes =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(type);
        bannersTableChangeStatusModerateYes.addChangedColumn(BANNERS.STATUS_MODERATE,
                BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.Yes.getLiteral());

        var bannersTableChangeStatusModerateSent =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(type);
        bannersTableChangeStatusModerateSent.addChangedColumn(BANNERS.STATUS_MODERATE,
                BannersStatusmoderate.Yes.getLiteral(),
                BannersStatusmoderate.Sent.getLiteral());

        var changes = List.of(bannersTableChangeStatusModerateYes, bannersTableChangeStatusModerateSent);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @MethodSource("disallowedBannerTypes")
    void bannerChangeStatusModeateYes_BannersWithoutMobileContentTest(BannersBannerType type) {
        var bannersTableChangeStatusModerateYes =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(type);
        bannersTableChangeStatusModerateYes.addChangedColumn(BANNERS.STATUS_MODERATE,
                BannersStatusmoderate.Sent.getLiteral(),
                BannersStatusmoderate.Yes.getLiteral());

        var changes = List.of(bannersTableChangeStatusModerateYes);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);


        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_MOBILE_CONTENT);
    }

    @ParameterizedTest
    @MethodSource("allowedBannerTypes")
    void bannerChangeHref_BannersWithMobileContentTest(BannersBannerType type) {
        var change =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(type);
        change.addChangedColumn(BANNERS.HREF, "google.com", "yandex.ru");

        var changes = List.of(change);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = new BsExportBannerResourcesObject.Builder()
                .setBid(6L)
                .setCid(2L)
                .setPid(4L)
                .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects);
    }

    @ParameterizedTest
    @MethodSource("disallowedBannerTypes")
    void bannerChangeHref_BannersWithoutMobileContentTest(BannersBannerType type) {
        var change =
                new BannersTableChange().withBid(6L).withCid(2L).withPid(4L).withBannerType(type);
        change.addChangedColumn(BANNERS.HREF, "google.com", "yandex.ru");

        var changes = List.of(change);

        var binlogEvent = createBannersEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, BannerResourceType.BANNER_MOBILE_CONTENT);
    }


    @Test
    void mobileContentChangesTest() {
        var mobileContentPublisherDomainChange =
                new MobileContentTableChange().withMobileContentId(BigInteger.valueOf(345L));
        mobileContentPublisherDomainChange.addChangedColumn(MOBILE_CONTENT.PUBLISHER_DOMAIN_ID, 29170L, 29179L);


        var mobileContentStoreContentIdChange =
                new MobileContentTableChange().withMobileContentId(BigInteger.valueOf(347L));
        mobileContentStoreContentIdChange
                .addChangedColumn(MOBILE_CONTENT.STORE_CONTENT_ID,
                        "com.appbuilder.u1346460p1721837", "com.yandex.browser");

        var mobileContentBundleIdChange = new MobileContentTableChange().withMobileContentId(BigInteger.valueOf(349L));
        mobileContentBundleIdChange
                .addChangedColumn(MOBILE_CONTENT.BUNDLE_ID,
                        "ru.yandex.mobile.search", "com.mobstudio.galaxy");


        var changes = List.of(mobileContentPublisherDomainChange, mobileContentStoreContentIdChange,
                mobileContentBundleIdChange);
        var binlogEvent = createMobileContentEvent(changes, Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObjects = List.of(
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(345L)
                        .setAdditionalTable(TablesEnum.MOBILE_CONTENT)
                        .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(347L)
                        .setAdditionalTable(TablesEnum.MOBILE_CONTENT)
                        .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                        .build(),
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(349L)
                        .setAdditionalTable(TablesEnum.MOBILE_CONTENT)
                        .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                        .build());

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObjects.toArray(BsExportBannerResourcesObject[]::new));
    }

    @Test
    void updateCampaignsMobileContentMobileAppIdTest() {
        var change = new CampaignMobileContentTableChange().withCid(123L);
        change.addChangedColumn(CAMPAIGNS_MOBILE_CONTENT.MOBILE_APP_ID, 12L, 13L);
        var binlogEvent = createCampaignsMobileContentEvent(List.of(change), Operation.UPDATE);

        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject =
                new BsExportBannerResourcesObject.Builder()
                        .setAdditionalId(123L)
                        .setAdditionalTable(TablesEnum.CAMPAIGNS_MOBILE_CONTENT)
                        .setResourceType(BannerResourceType.BANNER_MOBILE_CONTENT)
                        .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    static Stream<Arguments> allowedBannerTypes() {
        return BANNER_TYPES_WITH_MOBILE_CONTENT.stream()
                .map(Arguments::arguments);
    }

    static Stream<Arguments> disallowedBannerTypes() {
        return Arrays.stream(BannersBannerType.values())
                .filter(type -> !BANNER_TYPES_WITH_MOBILE_CONTENT.contains(type))
                .map(Arguments::arguments);
    }

    private void assertThatNotContainsResourceType(List<BsExportBannerResourcesObject> objects,
                                                   BannerResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();
    }
}
