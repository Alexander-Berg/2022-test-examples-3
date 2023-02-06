package ru.yandex.direct.core.entity.banner.type.href;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotionHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotionHref.HREF;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithContentPromotionHrefAddOperationTypeSupportTest {
    @Autowired
    private Steps steps;
    @Autowired
    private BannerWithContentPromotionHrefAddOperationTypeSupport addSupport;

    private ClientInfo clientInfo;
    private BannersAddOperationContainerImpl parametersContainer;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        RbacRole operatorRole = RbacRole.CLIENT;
        parametersContainer = new BannersAddOperationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                operatorRole, clientInfo.getClientId(), clientInfo.getUid(), null,
                null, emptySet(), ModerationMode.FORCE_SAVE_DRAFT, operatorRole.isInternal(),
                false, true);
    }

    @Test
    public void onModelsValidated_shouldFillHrefWithContentPromotionUrl() {
        var content = steps.contentPromotionSteps().createContentPromotionContent(clientInfo.getClientId(),
                ContentPromotionContentType.VIDEO);

        List<BannerWithContentPromotionHref> banners = singletonList(
                new ContentPromotionBanner().withContentPromotionId(content.getId()));
        addSupport.onModelsValidated(parametersContainer, banners);

        assertThat(banners.get(0), hasProperty(HREF.name(), is(content.getUrl())));
    }
}
