package ru.yandex.direct.core.entity.banner.type.href;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerStatusModerate.READY;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefUpdatePositiveTest extends BannerNewBannerInfoUpdateOperationTestBase {
    public static final String NEW_DOMAIN = "new.domain.com";
    @Autowired
    private TextBannerSteps textBannerSteps;
    @Autowired
    private UserSteps userSteps;

    private UserInfo operatorInfo;

    @Test
    public void sentToModerationWhenDomainChanged() {
        operatorInfo = userSteps.createDefaultUserWithRole(RbacRole.SUPPORT);
        bannerInfo = textBannerSteps.createDefaultTextBanner();

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(NEW_DOMAIN, BannerWithHref.DOMAIN);

        Long id = prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner)
                .hasFieldOrPropertyWithValue(BannerWithHref.DOMAIN.name(), NEW_DOMAIN)
                .hasFieldOrPropertyWithValue(BannerWithModerationInfo.STATUS_MODERATE.name(), READY);
    }

    @Override
    protected Long getUid() {
        return operatorInfo != null ? operatorInfo.getUid() : super.getUid();
    }
}
