package ru.yandex.direct.core.entity.sitelink;

import org.junit.Test;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkUtils;
import ru.yandex.direct.core.entity.sitelink.turbolanding.model.SitelinkTurboLanding;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.CloneTestUtil.fill;
import static ru.yandex.direct.core.testing.CloneTestUtil.fillIgnoring;

public class SitelinkUtilsCloneTest {

    @Test
    public void clone_WorksFine() {
        SitelinkTurboLanding sourceTurboLanding = new SitelinkTurboLanding();
        fill(sourceTurboLanding);

        Sitelink sourceSitelink = new Sitelink().withTurboLandingId(sourceTurboLanding.getId());
        fillIgnoring(sourceSitelink, Sitelink.TURBO_LANDING_ID.name());

        Sitelink clonedSitelink = SitelinkUtils.cloneSitelink(sourceSitelink);
        assertThat(clonedSitelink, beanDiffer(sourceSitelink));
    }

    @Test
    public void clone_WorksFine_ForNullTurbolanding() {
        Sitelink sourceSitelink = new Sitelink();
        fillIgnoring(sourceSitelink, Sitelink.TURBO_LANDING_ID.name());

        Sitelink clonedSitelink = SitelinkUtils.cloneSitelink(sourceSitelink);
        assertThat(clonedSitelink, beanDiffer(sourceSitelink));
    }
}
