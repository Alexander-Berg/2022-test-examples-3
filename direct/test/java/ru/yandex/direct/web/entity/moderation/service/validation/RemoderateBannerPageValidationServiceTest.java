package ru.yandex.direct.web.entity.moderation.service.validation;

import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ModerateBannerPageInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestModerateBannerPages.defaultModerateBannerPage;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest.Prop.BANNER_ID_FIELD_NAME;
import static ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest.Prop.PAGE_IDS_FIELD_NAME;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoderateBannerPageValidationServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private RemoderateBannerPageValidationService validationService;

    private Long pageId;
    private Long otherClientPageId;
    private Long bannerId;
    private Long otherClientBannerId;

    private ClientId clientId;
    private RemoderateBannerPageRequest request;

    @Before
    public void before() {
        initDataOfMainClient();
        initDataOfOtherClient();
        request = new RemoderateBannerPageRequest();
    }

    private void initDataOfMainClient() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup();
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        bannerId = banner.getBannerId();
        pageId = createInitialModerateBannerPage(banner).getModerateBannerPage().getPageId();

        clientId = adGroup.getClientId();
    }

    private void initDataOfOtherClient() {
        AdGroupInfo otherClientAdGroup = steps.adGroupSteps().createDefaultAdGroup();
        TextBannerInfo otherClientBanner = steps.bannerSteps().createDefaultBanner(otherClientAdGroup);
        otherClientBannerId = otherClientBanner.getBannerId();
        otherClientPageId = createInitialModerateBannerPage(otherClientBanner).getModerateBannerPage().getPageId();
    }

    @Test
    public void validate_ValidBannerPages_Success() {
        request.setBannerId(bannerId);
        request.setPageIds(singletonList(pageId));

        ValidationResult<RemoderateBannerPageRequest, Defect> actual = validationService.validate(request, clientId);
        assertThat(actual.hasAnyErrors()).isFalse();
    }

    @Test
    public void validate_OtherClientBannerId_ObjectNotFound() {
        request.setBannerId(otherClientBannerId);
        request.setPageIds(singletonList(pageId));

        ValidationResult<RemoderateBannerPageRequest, Defect> actual = validationService.validate(request, clientId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual).is(hasObjectNotFoundDefect(field(BANNER_ID_FIELD_NAME)));
            soft.assertThat(actual.flattenErrors()).hasSize(1);
        });
    }

    @Test
    public void validate_NonexistentModerationVerdict_ObjectNotFound() {
        request.setBannerId(bannerId);
        request.setPageIds(asList(pageId, otherClientPageId));

        ValidationResult<RemoderateBannerPageRequest, Defect> actual = validationService.validate(request, clientId);


        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual).is(hasObjectNotFoundDefect(field(PAGE_IDS_FIELD_NAME), index(1)));
            soft.assertThat(actual.flattenErrors()).hasSize(1);
        });
    }

    private <T> Condition<T> hasObjectNotFoundDefect(PathNode... nodes) {
        return matchedBy(hasDefectWithDefinition(validationError(path(nodes), CommonDefects.objectNotFound())));
    }


    private ModerateBannerPageInfo createInitialModerateBannerPage(AbstractBannerInfo bannerInfo) {
        return steps.moderateBannerPageSteps().createModerateBannerPage(bannerInfo, defaultModerateBannerPage()
                .withPageId(nextLong(1, Integer.MAX_VALUE)));
    }
}
