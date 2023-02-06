package ru.yandex.direct.core.entity.banner.type.moderationstatuses;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.model.ModelChanges;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithModerationStatusesMinusGeoUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Test
    public void textBannerWithChangedStatusModerateToReady_MinusGeoDeleted() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        testBannerRepository.addMinusGeo(bannerInfo.getShard(), bannerId, BannersMinusGeoType.current);

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process("http://ya.ru/" + randomAlphabetic(5), TextBanner.HREF);

        prepareAndApplyValid(modelChanges);
        Long actualMinusGeo = testBannerRepository.getMinusGeoBannerId(bannerInfo.getShard(), bannerId);
        assertThat(actualMinusGeo, nullValue());
    }

}
