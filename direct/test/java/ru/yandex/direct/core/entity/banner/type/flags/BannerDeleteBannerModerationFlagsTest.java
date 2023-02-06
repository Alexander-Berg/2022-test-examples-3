package ru.yandex.direct.core.entity.banner.type.flags;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPM_GEOPRODUCT_AUTO_MODERATION;
import static ru.yandex.direct.core.entity.banner.model.BannerWithCreative.CREATIVE_ID;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.babyFood;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerDeleteBannerModerationFlagsTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    @Autowired
    private TestModerationRepository testModerationRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void before() {
        ppcPropertiesSupport.set(CPM_GEOPRODUCT_AUTO_MODERATION.getName(), "true");
    }

    @Test
    public void textBanner_ChangeCreativeId_ModerationFlagsDeleted() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        addModerationData();

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process("http://ya.ru/test123", TextBanner.HREF);

        prepareAndApplyValid(changes);

        assertThatModerationFlagsIs(nullValue());
    }

    @Test
    public void geoProductBanner_ChangeCreativeId_ModerationFlagsIsNotDeleted() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        bannerInfo = createGeoProductBanner(defaultClient);
        addModerationData();

        var newCreativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(defaultClient);
        var changes = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newCreativeInfo.getCreativeId(), CREATIVE_ID);

        prepareAndApplyValid(changes);

        assertThatModerationFlagsIs(notNullValue());
    }

    private void addModerationData() {
        Long bannerId = bannerInfo.getBannerId();
        int shard = bannerInfo.getShard();

        testModerationRepository.addPostModerate(shard, bannerId);
        testModerationRepository.addAutoModerate(shard, bannerId);
        testModerationRepository.addBannerModEdit(shard, bannerId);
    }

    private void assertThatModerationFlagsIs(Matcher<Object> matcher) {
        Long bannerId = bannerInfo.getBannerId();
        int shard = bannerInfo.getShard();

        Long postModerateId = testModerationRepository.getPostModerateId(shard, bannerId);
        assertThat(postModerateId, matcher);

        Long autoModerateId = testModerationRepository.getAutoModerateId(shard, bannerId);
        assertThat(autoModerateId, matcher);

        Long bannerModEditId = testModerationRepository.getBannerModEditId(shard, bannerId);
        assertThat(bannerModEditId, matcher);
    }

    private CpmBannerInfo createGeoProductBanner(ClientInfo defaultClient) {
        var creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(defaultClient);
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(defaultClient.getClientId());
        OldCpmBanner banner = activeCpmBanner(null, null, creativeInfo.getCreativeId())
                .withTurboLandingId(turboLanding.getId())
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)
                .withHref(null)
                .withFlags(babyFood(0));
        return steps.bannerSteps().createActiveCpmGeoproductBanner(banner, defaultClient);
    }
}
