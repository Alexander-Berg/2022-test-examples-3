package ru.yandex.direct.core.entity.banner.type.adgroupid;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBannerValidationContainers;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.addToArchivedCampaignNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdGroupIdAddValidationTypeSupportTest {

    @Autowired
    private BannerWithAdGroupIdAddValidationTypeSupport typeSupport;

    @Autowired
    private Steps steps;

    private NewTextBannerInfo bannerInfo;
    private TestBannerValidationContainers.Builder containerBuilder;

    @Before
    public void setUp() {
        bannerInfo = steps.textBannerSteps().createDefaultTextBanner();
        containerBuilder = newBannerValidationContainer()
                .withClientInfo(bannerInfo.getClientInfo())
                .withIndexToAdGroupForOperationMap(Map.of(0, bannerInfo.getAdGroupInfo().getAdGroup()));
    }

    @Test
    public void validBanner() {
        var bannerToAdd = fullTextBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId());
        var validationResult = validate(bannerToAdd);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void testAdGroupConnections_AdGroupNotExist() {
        var bannerToAdd = fullTextBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId() + 1);
        var validationResult = validate(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithAdGroupId.AD_GROUP_ID)), adGroupNotFound())));
    }

    @Test
    public void testAdGroupConnections_AdGroupNotVisible() {
        var anotherClientInfo = steps.clientSteps().createDefaultClient();
        containerBuilder.withOperatorUid(anotherClientInfo.getUid());
        var bannerToAdd = fullTextBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId());
        var validationResult = validate(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithAdGroupId.AD_GROUP_ID)), adGroupNotFound())));
    }

    @Test
    public void testAdGroupConnections_AdGroupNotWritable() {
        var superReaderClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);
        containerBuilder.withOperatorUid(superReaderClientInfo.getUid());
        var bannerToAdd = fullTextBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId());
        var validationResult = validate(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithAdGroupId.AD_GROUP_ID)), noRights())));
    }

    @Test
    public void testAdGroupConnections_CampaignArchived() {
        steps.campaignSteps().archiveCampaign(bannerInfo.getShard(), bannerInfo.getCampaignId());
        var bannerToAdd = fullTextBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId());
        var validationResult = validate(bannerToAdd);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(index(0), field(BannerWithAdGroupId.AD_GROUP_ID)), addToArchivedCampaignNotAllowed())));
    }

    @Test
    public void testAdGroupConnections_bannerTypeNotAllowed() {
        var bannerToAdd = new ContentPromotionBanner()
                .withCampaignId(bannerInfo.getCampaignId())
                .withAdGroupId(bannerInfo.getAdGroupId());
        var validationResult = validate(Collections.singletonList(bannerToAdd));
        assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(index(0)), inconsistentStateBannerTypeAndAdgroupType())));
    }

    private ValidationResult<List<BannerWithAdGroupId>, Defect> validate(BannerWithAdGroupId bannerToAdd) {
        return validate(Collections.singletonList(bannerToAdd));
    }

    private ValidationResult<List<BannerWithAdGroupId>, Defect> validate(List<BannerWithAdGroupId> bannersToAdd) {
        ValidationResult<List<BannerWithAdGroupId>, Defect> vr = new ValidationResult<>(bannersToAdd);
        return typeSupport.validate(containerBuilder.build(), vr);
    }
}
