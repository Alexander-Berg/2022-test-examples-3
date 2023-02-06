package ru.yandex.direct.core.entity.banner.type.contentpromo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion.CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion.VISIT_URL;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.contentTypeNotMatchesAdGroupContentType;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithContentPromotionValidatorProviderTest {

    private static final Path PATH_CONTENT_ID = path(index(0), field(CONTENT_PROMOTION_ID));
    private static final Path PATH_VISIT_URL = path(index(0), field(VISIT_URL));

    @Autowired
    public BannerWithContentPromotionValidatorProvider provider;

    @Autowired
    public Steps steps;

    @Autowired
    public FeatureService featureService;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    // contentPromotionId

    @Test
    public void validVideoContentBanner() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.VIDEO);
        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId());
        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(VIDEO, banner), banner);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void invalidBannerWithUnexistentContent() {
        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(Long.MAX_VALUE);
        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(VIDEO, banner), banner);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(PATH_CONTENT_ID, objectNotFound())));
    }

    @Test
    public void invalidBannerWithUnknownAdGroup() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.VIDEO);
        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId());
        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(banner), banner);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void invalidBannerWithContentOfAnotherClient() {
        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(anotherClientInfo.getClientId(), ContentPromotionContentType.VIDEO);

        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId());

        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(VIDEO, banner), banner);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(PATH_CONTENT_ID, objectNotFound())));
    }

    @Test
    public void invalidBannerWithContentTypeIsNotMatchedToAdGroupType() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.VIDEO);

        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId());

        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(COLLECTION, banner), banner);
        assertThat(validationResult,
                hasDefectDefinitionWith(validationError(PATH_CONTENT_ID, contentTypeNotMatchesAdGroupContentType())));
    }

    // visitUrl

    @Test
    public void validBannerWithVisitUrl() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.COLLECTION);

        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId())
                .withVisitUrl("https://www.yandex.ru");

        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(COLLECTION, banner), banner);
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void invalidServiceBannerWithForbiddenVisitUrl() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.SERVICE);

        BannerWithContentPromotion banner = new ContentPromotionBanner()
                .withContentPromotionId(content.getId())
                .withVisitUrl("https://www.yandex.ru");

        ValidationResult<List<BannerWithContentPromotion>, Defect> validationResult =
                validate(container(SERVICE, banner), banner);
        assertThat(validationResult, hasDefectDefinitionWith(validationError(PATH_VISIT_URL, isNull())));
    }

    private ValidationResult<List<BannerWithContentPromotion>, Defect> validate(
            BannersAddOperationContainer container, BannerWithContentPromotion banner) {
        return ListValidationBuilder.<BannerWithContentPromotion, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithContentPromotionValidator(container, singletonList(banner)))
                .getResult();
    }

    private BannersAddOperationContainer container(ContentPromotionAdgroupType contentPromotionAdgroupType,
                                                   BannerWithContentPromotion banner) {
        BannersAddOperationContainerImpl container = container(banner);
        container.setIndexToContentPromotionAdgroupTypeMap(() -> ImmutableMap.of(0, contentPromotionAdgroupType));
        return container;
    }

    private BannersAddOperationContainerImpl container(BannerWithContentPromotion banner) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(clientInfo.getClientId());
        return newBannerValidationContainer()
                .withClientInfo(clientInfo)
                .withClientEnabledFeatures(clientEnabledFeatures)
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }
}
