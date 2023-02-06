package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.DOMAIN;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.HREF;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createAppliedChanges;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createModelChanges;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithHrefUpdateOperationTypeSupportTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BannerWithHrefUpdateOperationTypeSupport updateSupport;

    private BannersUpdateOperationContainer parametersContainer;

    @Before
    public void setUp() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        RbacRole operatorRole = RbacRole.CLIENT;
        parametersContainer = new BannersUpdateOperationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                operatorRole, clientInfo.getClientId(), null, null,
                null, Collections.emptySet(), ModerationMode.FORCE_SAVE_DRAFT, operatorRole.isInternal(),
                false, true);
    }

    @Test
    public void beforeExecution_shouldEncodeHrefToUnicodeIfHrefChanged() {
        var banners = List.of(
                createNewBannerWithHref("http://я.ру"),
                createNewBannerWithHref("http://я.ру"),
                createNewBannerWithHref("http://я.ру")
        );
        var modelsChanges = createModelChanges(banners);
        modelsChanges.get(0).process("https://xn--41a.xn--p1ag", HREF); // href поменялся
        modelsChanges.get(1).process(null, HREF); // href сбросили в null
        modelsChanges.get(2); // href не менялся

        updateSupport.beforeExecution(parametersContainer, createAppliedChanges(banners, modelsChanges));

        assertThat(banners, contains(
                hasProperty(HREF.name(), is("https://я.ру")),
                hasProperty(HREF.name(), nullValue()),
                hasProperty(HREF.name(), is("http://я.ру"))
        ));
    }

    @Test
    public void beforExecution_shouldEncodeDomainToUnicodeIfDomainChanged() {
        var banners = List.of(
                createNewBannerWithHref("http://ya.ru").withDomain("ya.ru"),
                createNewBannerWithHref("http://ya.ru").withDomain("ya.ru"),
                createNewBannerWithHref("http://ya.ru").withDomain("ya.ru")
        );
        var modelsChanges = createModelChanges(banners);
        modelsChanges.get(0).process("xn--41a.xn--p1ag", DOMAIN); // домен поменялся
        modelsChanges.get(1).process("https://xn--41a.xn--p1ag", HREF); // href поменялся
        modelsChanges.get(2); // домен не менялся

        updateSupport.beforeExecution(parametersContainer, createAppliedChanges(banners, modelsChanges));

        assertThat(banners, contains(
                hasProperty(DOMAIN.name(), is("я.ру")),
                hasProperty(DOMAIN.name(), is("я.ру")),
                hasProperty(DOMAIN.name(), is("ya.ru"))
        ));
    }

    private BannerWithHref createNewBannerWithHref(String href) {
        return new ContentPromotionBanner().withHref(href).withId(RandomNumberUtils.nextPositiveLong());
    }
}
