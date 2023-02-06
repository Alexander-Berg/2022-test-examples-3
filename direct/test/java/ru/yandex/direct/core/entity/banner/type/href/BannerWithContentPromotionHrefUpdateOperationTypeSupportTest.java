package ru.yandex.direct.core.entity.banner.type.href;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotionHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotionHref.CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotionHref.HREF;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createAppliedChanges;
import static ru.yandex.direct.core.entity.banner.type.Helpers.createModelChanges;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithContentPromotionHrefUpdateOperationTypeSupportTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BannerWithContentPromotionHrefUpdateOperationTypeSupport updateSupport;

    private ClientInfo clientInfo;
    private BannersUpdateOperationContainer parametersContainer;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        RbacRole operatorRole = RbacRole.CLIENT;
        parametersContainer = new BannersUpdateOperationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                operatorRole, clientInfo.getClientId(), clientInfo.getUid(), null,
                null, emptySet(), ModerationMode.FORCE_SAVE_DRAFT, operatorRole.isInternal(),
                false, true);
    }

    @Test
    public void onAppliedChangesValidated_shouldFillHrefWithContentPromotionUrl() {
        Long contentPromotionId = RandomNumberUtils.nextPositiveLong();
        var banners = List.of(
                getNewBannerWithContentPromotionHref(contentPromotionId, "http://ya.ru"),
                getNewBannerWithContentPromotionHref(contentPromotionId, "http://ya.ru"),
                getNewBannerWithContentPromotionHref(contentPromotionId, "http://ya.ru")
        );
        var newContent = steps.contentPromotionSteps().createContentPromotionContent(clientInfo.getClientId(),
                ContentPromotionContentType.VIDEO);

        var modelsChanges = createModelChanges(banners);
        modelsChanges.get(0).process(newContent.getId(), CONTENT_PROMOTION_ID); // contentPromotionId поменялся
        modelsChanges.get(1).process(newContent.getId(), CONTENT_PROMOTION_ID)  // contentPromotionId поменялся
                .process(null, HREF); // и href сбросился
        modelsChanges.get(2); // contentPromotionId не поменялся

        updateSupport.onAppliedChangesValidated(parametersContainer, createAppliedChanges(banners, modelsChanges));

        assertThat(banners, contains(
                allOf(hasProperty(HREF.name(), is(newContent.getUrl())),
                        hasProperty(CONTENT_PROMOTION_ID.name(), is(newContent.getId()))),
                allOf(hasProperty(HREF.name(), is(newContent.getUrl())),
                        hasProperty(CONTENT_PROMOTION_ID.name(), is(newContent.getId()))),
                allOf(hasProperty(HREF.name(), is(banners.get(2).getHref())),
                        hasProperty(CONTENT_PROMOTION_ID.name(), is(banners.get(2).getContentPromotionId())))
        ));
    }

    private BannerWithContentPromotionHref getNewBannerWithContentPromotionHref(Long contentPromotionId,
                                                                                String href) {
        return new ContentPromotionBanner().withHref(href).withContentPromotionId(contentPromotionId);
    }
}
