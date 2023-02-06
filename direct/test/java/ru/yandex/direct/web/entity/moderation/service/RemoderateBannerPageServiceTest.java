package ru.yandex.direct.web.entity.moderation.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ModerateBannerPageInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestModerateBannerPagesRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.StatusBsSynced.YES;
import static ru.yandex.direct.core.testing.data.TestModerateBannerPages.defaultModerateBannerPage;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest.Prop.BANNER_ID_FIELD_NAME;
import static ru.yandex.direct.web.entity.moderation.model.RemoderateBannerPageRequest.Prop.PAGE_IDS_FIELD_NAME;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RemoderateBannerPageServiceTest {

    private static final BeanFieldPath CREATE_TIME = newPath("createTime");
    private static final long BANNER_INITIAL_VERSION = 1L;

    @Autowired
    private Steps steps;
    @Autowired
    private TestModerationRepository moderationRepository;
    @Autowired
    private TestModerateBannerPagesRepository moderateBannerPagesRepository;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private RemoderateBannerPageService remoderateBannerPageService;
    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    private Long pageId;
    private Long otherClientPageId;
    private Long bannerId;
    private Long otherClientBannerId;
    private ModerateBannerPage moderateBannerPage;
    private ModerateBannerPage otherClientModerateBannerPage;

    private int shard;
    private ClientInfo defaultClient;
    private RemoderateBannerPageRequest request;

    @Before
    public void before() {
        initDataOfMainClient();
        initDataOfOtherClient();

        request = new RemoderateBannerPageRequest();
        setAuthData();
    }

    private void initDataOfMainClient() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup();
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        ModerateBannerPageInfo moderateBannerPageInfo = createInitialModerateBannerPage(banner);
        steps.moderationReasonSteps().insertRejectReasonForModerateBannerPage(moderateBannerPageInfo);

        bannerId = banner.getBannerId();
        moderateBannerPage = moderateBannerPageInfo.getModerateBannerPage();
        pageId = moderateBannerPageInfo.getModerateBannerPage().getPageId();
        defaultClient = adGroup.getClientInfo();
        shard = adGroup.getShard();
    }

    private void initDataOfOtherClient() {
        AdGroupInfo otherClientAdGroup = steps.adGroupSteps().createDefaultAdGroup();
        TextBannerInfo otherClientBanner = steps.bannerSteps().createDefaultBanner(otherClientAdGroup);
        ModerateBannerPageInfo moderateBannerPageInfo = createInitialModerateBannerPage(otherClientBanner);
        steps.moderationReasonSteps().insertRejectReasonForModerateBannerPage(moderateBannerPageInfo);

        otherClientBannerId = otherClientBanner.getBannerId();
        otherClientModerateBannerPage = moderateBannerPageInfo.getModerateBannerPage();
        otherClientPageId = moderateBannerPageInfo.getModerateBannerPage().getPageId();
    }

    @Test
    public void remoderate_ValidBannerPages_Success() {
        request.setBannerId(bannerId);
        request.setPageIds(singletonList(pageId));

        WebResponse response = remoderateBannerPageService.remoderate(request);

        assertThat(response.isSuccessful()).isTrue();
        assertModReasonIsDeleted(moderateBannerPage.getId());
        OldBanner actualBanner = bannerRepository.getBanners(shard, singleton(bannerId)).get(0);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(NO);

        ModerateBannerPage actualModerateBannerPage = getActualModerateBannerPage(moderateBannerPage.getId());
        ModerateBannerPage expectedModerateBannerPage = convertToRemoderated(moderateBannerPage);

        assertThat(actualModerateBannerPage)
                .is(matchedBy(beanDiffer(expectedModerateBannerPage)
                        .useCompareStrategy(allFieldsExcept(CREATE_TIME))));
    }

    @Test
    public void remoderate_BannerIdOfOtherClient_ObjectNotFound() {
        request.setBannerId(otherClientBannerId);
        request.setPageIds(singletonList(pageId));

        WebResponse response = remoderateBannerPageService.remoderate(request);

        assertThat(response.isSuccessful()).isFalse();
        assertModReasonIsNotDeleted(otherClientModerateBannerPage.getId());
        OldBanner actualBanner = bannerRepository.getBanners(shard, singleton(otherClientBannerId)).get(0);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(YES);

        WebValidationResult actualResult = ((ValidationResponse) response).validationResult();

        WebValidationResult expectedResult = new WebValidationResult().addErrors(
                webObjectNotFound(otherClientBannerId, field(BANNER_ID_FIELD_NAME)));

        assertThat(actualResult).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void remoderate_NonexistentModerationVerdict_ObjectNotFound() {
        request.setBannerId(bannerId);
        request.setPageIds(asList(pageId, otherClientPageId));

        WebResponse response = remoderateBannerPageService.remoderate(request);

        assertThat(response.isSuccessful()).isFalse();
        assertModReasonIsNotDeleted(moderateBannerPage.getId());
        OldBanner actualBanner = bannerRepository.getBanners(shard, singleton(bannerId)).get(0);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(YES);

        WebValidationResult actualResult = ((ValidationResponse) response).validationResult();

        WebValidationResult expectedResult = new WebValidationResult().addErrors(
                webObjectNotFound(otherClientPageId, field(PAGE_IDS_FIELD_NAME), index(1)));

        assertThat(actualResult).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    private ModerateBannerPage getActualModerateBannerPage(Long moderateBannerPageId) {
        return moderateBannerPagesRepository.getModerateBannerPage(shard, moderateBannerPageId);
    }

    private void assertModReasonIsDeleted(Long bannerPageId) {
        Long modReasonId = moderationRepository.getModReasonBannerPageByBannerPageId(shard, bannerPageId);
        assertThat(modReasonId).isNull();
    }

    private void assertModReasonIsNotDeleted(Long bannerPageId) {
        Long modReasonId = moderationRepository.getModReasonBannerPageByBannerPageId(shard, bannerPageId);
        assertThat(modReasonId).isNotNull();
    }

    private ModerateBannerPage convertToRemoderated(ModerateBannerPage moderateBannerPage) {
        return moderateBannerPage
                .withVersion(BANNER_INITIAL_VERSION + 1)
                .withComment(null)
                .withStatusModerate(StatusModerateBannerPage.READY);
    }

    private ModerateBannerPageInfo createInitialModerateBannerPage(AbstractBannerInfo bannerInfo) {
        return steps.moderateBannerPageSteps().createModerateBannerPage(bannerInfo, defaultModerateBannerPage()
                .withPageId(nextLong(1, Integer.MAX_VALUE))
                .withStatusModerate(StatusModerateBannerPage.NO)
                .withVersion(BANNER_INITIAL_VERSION));
    }

    private void setAuthData() {
        DirectWebAuthenticationSourceMock authSource = (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withSubjectUser(new User()
                .withClientId(defaultClient.getClientId())
                .withUid(defaultClient.getUid()));

        UserInfo userInfo = defaultClient.getChiefUserInfo();
        User user = userInfo.getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }

    private static WebDefect webObjectNotFound(Long value, PathNode... nodes) {
        return new WebDefect()
                .withPath(path(nodes).toString())
                .withCode(objectNotFound().defectId().getCode())
                .withText("Object not found")
                .withDescription("Object not found")
                .withValue(value.toString());
    }

}
