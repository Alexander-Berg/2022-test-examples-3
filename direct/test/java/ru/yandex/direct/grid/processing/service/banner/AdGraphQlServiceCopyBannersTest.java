package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.util.StringUtils;

import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassActionPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdCopyAdsInput;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceCopyBannersTest
        extends BannersMassActionsServiceBaseTest {

    private static final String MUTATION_NAME = "copyAds";

    @Autowired
    private InternalAdsProductService internalAdsProductService;

    @Autowired
    private GraphQlTestExecutor testExecutor;

    private static final GraphQlTestExecutor.TemplateMutation<GdCopyAdsInput, GdAdsMassActionPayload>
            COPY_ADS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, BANNERS_MASS_ACTIONS_MUTATION_TEMPLATE,
                    GdCopyAdsInput.class, GdAdsMassActionPayload.class);

    @Test
    public void doNothingOnEmptyList() {
        TestAuthHelper.setDirectAuthentication(operator);

        var input = new GdCopyAdsInput()
                .withAdIds(Collections.emptyList());

        var result = testExecutor.doMutationAndGetPayload(COPY_ADS_MUTATION, input, operator);

        AdMassActionsServiceTest.checkEmpty(result);
    }

    @Test
    public void copyBanners_success() {
        TestAuthHelper.setDirectAuthentication(operator);
        var totalBanners = 3;
        var groupInfo = adGroupInfo();

        var banners = StreamEx.generate(() -> createInternalBanner(groupInfo))
                .limit(totalBanners)
                .collect(Collectors.toList());

        var input = new GdCopyAdsInput()
                .withAdIds(mapList(banners, NewBannerInfo::getBannerId))
                .withDestinationAdGroupId(Optional.empty())
                .withDestinationClientId(Optional.empty());

        var result = testExecutor.doMutationAndGetPayload(COPY_ADS_MUTATION, input, operator);

        var expectedResult = new GdAdsMassActionPayload()
                .withValidationResult(null)
                .withSuccessCount(totalBanners)
                .withProcessedAdIds(mapList(banners, NewBannerInfo::getBannerId))
                .withSkippedAdIds(Collections.emptyList())
                .withTotalCount(totalBanners);

        AdMassActionsServiceTest.checkResult(result, expectedResult);
    }

    @Test
    public void hasNoRightToMutation_failure() {
        var agencyInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        var clientInfo = steps.clientSteps().createClientUnderAgency(agencyInfo, new ClientInfo());
        var readOnlyOperator = clientInfo.getChiefUserInfo().getUser();
        var groupInfo = adGroupInfo(clientInfo);
        TestAuthHelper.setDirectAuthentication(readOnlyOperator);

        var banner = createInternalBanner(groupInfo);

        var input = new GdCopyAdsInput()
                .withAdIds(List.of(banner.getBanner().getId()))
                .withDestinationAdGroupId(Optional.empty())
                .withDestinationClientId(Optional.empty());

        var result = testExecutor.doMutation(COPY_ADS_MUTATION, input, readOnlyOperator);

        assertThat(result.getErrors()).isNotEmpty();
    }

    private AdGroupInfo adGroupInfo() {
        return adGroupInfo(clientInfo);
    }

    private AdGroupInfo adGroupInfo(ClientInfo info) {
        var createdProduct = new InternalAdsProduct()
                .withClientId(info.getClientId())
                .withName("product name" + StringUtils.randomAlphanumeric(20))
                .withDescription("product description")
                .withOptions(Collections.emptySet());
        internalAdsProductService.createProduct(createdProduct);

        var campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(info);
        return steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
    }

}
