package ru.yandex.direct.core.copyentity;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CampaignContainsAdGroups;
import ru.yandex.direct.core.entity.banner.model.AdGroupContainsBanners;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.Entity;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;

@SuppressWarnings({"rawtypes", "unchecked"})
@ParametersAreNonnullByDefault
public class CopyResultTest {
    private static final long cid1 = 1L;
    private static final long cid2 = 2L;
    private static final long pid1 = 3L;
    private static final long pid2 = 4L;
    private static final long bid1 = 5L;
    private static final long bid2 = 6L;

    private List<BaseCampaign> campaigns;
    private List<AdGroup> adGroups;
    private ValidationResult<List<AdGroup>, Defect> adgroupVR;
    private List<BannerWithAdGroupId> banners;
    private ValidationResult<List<BannerWithAdGroupId>, Defect> bannerVR;
    private Map<Class<? extends Entity<Long>>, MassResult<Long>> results;
    private EntityContext entityContext;
    private ValidationResult<List<BaseCampaign>, Defect> campaignVR;
    private Map<Class, ValidationResult<?, Defect>> prefilterResults;

    @Before
    public void setUp() {
        campaigns = List.of(
                new TextCampaign().withId(cid1),
                new TextCampaign().withId(cid2));
        campaignVR = new ValidationResult<>(campaigns);
        MassResult<Long> campaignMR = new MassResult<>(List.of(Result.successful(cid1), Result.successful(cid2)),
                campaignVR, ResultState.SUCCESSFUL);

        adGroups = List.of(
                new AdGroup().withId(pid1).withCampaignId(cid2),
                new AdGroup().withId(pid2).withCampaignId(cid2));
        adgroupVR = new ValidationResult<>(adGroups);
        MassResult<Long> adgroupMR = new MassResult<>(List.of(Result.successful(pid1), Result.successful(pid2)),
                adgroupVR, ResultState.SUCCESSFUL);

        banners = List.of(
                new TextBanner().withId(bid1).withAdGroupId(pid2),
                new TextBanner().withId(bid2).withAdGroupId(pid2));
        bannerVR = new ValidationResult<>(banners);
        MassResult<Long> bannerMR = new MassResult<>(List.of(Result.successful(bid1), Result.successful(bid2)),
                bannerVR, ResultState.SUCCESSFUL);

        results = Map.of(BaseCampaign.class, campaignMR,
                AdGroup.class, adgroupMR,
                BannerWithAdGroupId.class, bannerMR);

        prefilterResults = Map.of(BaseCampaign.class, new ValidationResult<>(campaigns),
                AdGroup.class, new ValidationResult<>(adGroups),
                BannerWithAdGroupId.class, new ValidationResult<>(banners));

        Client client = new Client().withId(1000L).withWorkCurrency(CurrencyCode.RUB);
        CopyOperationContainer copyContainer = CopyEntityTestUtils.defaultCopyContainer(client);
        entityContext = new EntityContext(copyContainer);
    }

    @Test
    public void entityAndSon_HierarchicalValidationResultIsFormedCorrectly() {
        entityContext.addObjects(BaseCampaign.class, campaigns);
        entityContext.addObjects(AdGroup.class, adGroups);
        entityContext.registerIdRelationship(adGroups, new CampaignContainsAdGroups());

        adgroupVR.getOrCreateSubValidationResult(index(1), adGroups.get(1)).addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var errors = copyResult.getMassResult().getValidationResult().flattenErrors();
        var error = (DefectInfo) errors.get(0);

        assertThat(error.getPath().toString()).isEqualTo("[1].AdGroup[1]");
    }

    @Test
    public void entitySonAndGrandson_HierarchicalValidationResultIsFormedCorrectly() {
        entityContext.addObjects(BaseCampaign.class, campaigns);
        entityContext.addObjects(AdGroup.class, adGroups);
        entityContext.addObjects(BannerWithAdGroupId.class, banners);
        entityContext.registerIdRelationship(adGroups, new CampaignContainsAdGroups());
        entityContext.registerIdRelationship(banners, new AdGroupContainsBanners());

        bannerVR.getOrCreateSubValidationResult(index(1), banners.get(1)).addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var errors = copyResult.getMassResult().getValidationResult().flattenErrors();
        var error = (DefectInfo) errors.get(0);

        assertThat(error.getPath().toString()).isEqualTo("[1].AdGroup[1].BannerWithAdGroupId[1]");
    }

    @Test
    public void entitySonAndGrandsonNoErrors_HierarchicalValidationResultIsFormedCorrectly() {
        entityContext.addObjects(BaseCampaign.class, campaigns);
        entityContext.addObjects(AdGroup.class, adGroups);
        entityContext.addObjects(BannerWithAdGroupId.class, banners);
        entityContext.registerIdRelationship(adGroups, new CampaignContainsAdGroups());
        entityContext.registerIdRelationship(banners, new AdGroupContainsBanners());

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var errors = copyResult.getMassResult().getValidationResult().flattenErrors();
        assertThat(errors).isEmpty();
    }

    @Test
    public void entityWithError_HierarchicalValidationResultIsFormedCorrectly() {
        entityContext.addObjects(BaseCampaign.class, campaigns);

        campaignVR.getOrCreateSubValidationResult(index(1), campaigns.get(1)).addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var errors = copyResult.getMassResult().getValidationResult().flattenErrors();
        var error = (DefectInfo) errors.get(0);

        assertThat(error.getPath().toString()).isEqualTo("[1]");
    }

    @Test
    public void entityWithError_resultWithTopLevelErrorIsBroken() {
        entityContext.addObjects(BaseCampaign.class, campaigns);

        campaignVR.getOrCreateSubValidationResult(index(1), campaigns.get(1)).addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var resultWithErrors = copyResult.getMassResult().get(1);

        assertThat(resultWithErrors.isSuccessful()).isFalse();
    }

    @Test
    public void entityWithError_resultWithNestedErrorOnTopLevelEntityIsBroken() {
        entityContext.addObjects(BaseCampaign.class, campaigns);

        campaignVR.getOrCreateSubValidationResult(index(1), campaigns.get(1))
                .getOrCreateSubValidationResult(field("id"), campaigns.get(1).getId())
                .addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var resultWithErrors = copyResult.getMassResult().get(1);

        assertThat(resultWithErrors.isSuccessful()).isFalse();
    }

    @Test
    public void entityWithError_resultWithTopLevelErrorOnNonTopLevelIsSuccessful() {
        entityContext.addObjects(BaseCampaign.class, campaigns);

        adgroupVR.getOrCreateSubValidationResult(index(1), adGroups.get(1)).addError(new Defect(OBJECT_NOT_FOUND));

        var copyResult = new CopyResult<Long>(List.of(1L, 2L), BaseCampaign.class, (Map) results,
                prefilterResults, entityContext, ResultState.SUCCESSFUL);
        var resultWithErrors = copyResult.getMassResult().get(1);

        assertThat(resultWithErrors.isSuccessful()).isTrue();
    }

}
