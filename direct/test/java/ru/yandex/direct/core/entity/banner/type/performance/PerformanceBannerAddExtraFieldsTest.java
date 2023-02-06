package ru.yandex.direct.core.entity.banner.type.performance;

import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.DefectIds;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.clientPerformanceBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class PerformanceBannerAddExtraFieldsTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    public Steps steps;

    @Autowired
    public VcardRepository vcardRepository;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
    }

    @Test
    public void addBanner_withVcard_success() {
        var creativeId = createCreative(adGroupInfo.getClientInfo());
        var vcardId = steps.vcardSteps().createVcard(
                TestVcards.fullVcard(adGroupInfo.getUid(), adGroupInfo.getCampaignId()),
                adGroupInfo.getCampaignInfo()
        ).getVcardId();

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId)
                .withVcardId(vcardId)
                .withVcardStatusModerate(BannerVcardStatusModerate.NEW);

        Long id = prepareAndApplyValid(banner);

        PerformanceBanner actualBanner = getBanner(id);

        PerformanceBanner expectedBanner = new PerformanceBanner()
                .withCreativeId(creativeId)
                .withVcardId(vcardId)
                .withVcardStatusModerate(BannerVcardStatusModerate.READY);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addBanner_withVcard_invalid() {
        var creativeId = createCreative(adGroupInfo.getClientInfo());

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId)
                .withVcardId(-1L)
                .withVcardStatusModerate(BannerVcardStatusModerate.NEW);

        assertThat(prepareAndApplyInvalid(banner), hasDefectDefinitionWith(
                validationError(path(field("vcardId")), DefectIds.MUST_BE_VALID_ID)
        ));
    }

    @Test
    public void addBanner_withCallouts_success() {
        var creativeId = createCreative(adGroupInfo.getClientInfo());
        var calloutIds = StreamEx.of("text1", "text2")
                .map(text -> steps.calloutSteps().createCalloutWithText(adGroupInfo.getClientInfo(), text).getId())
                .collect(Collectors.toList());

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId)
                .withCalloutIds(calloutIds);

        Long id = prepareAndApplyValid(banner);

        PerformanceBanner actualBanner = getBanner(id);

        PerformanceBanner expectedBanner = new PerformanceBanner()
                .withCreativeId(creativeId)
                .withCalloutIds(calloutIds);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addBanner_withCallouts_invalid() {
        var creativeId = createCreative(adGroupInfo.getClientInfo());

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId)
                .withCalloutIds(List.of(-1L));

        assertThat(prepareAndApplyInvalid(banner), hasDefectDefinitionWith(
                validationError(path(field("calloutIds"), index(0)), BannerDefectIds.Gen.ARRAY_ELEMENT_INVALID_ID)
        ));
    }

    private Long createCreative(ClientInfo clientInfo) {
        Creative creative = defaultPerformanceCreative(null, null);
        return steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
    }

}
