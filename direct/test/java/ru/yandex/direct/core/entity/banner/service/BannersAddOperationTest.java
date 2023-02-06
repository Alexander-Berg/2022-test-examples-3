package ru.yandex.direct.core.entity.banner.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jdk.jfr.Description;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion.CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithVcard.VCARD_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.contentTypeNotMatchesAdGroupContentType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.vcardOfAnotherCampaign;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapAndFilterList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersAddOperationTest extends BannerNewAdGroupInfoAddOperationTestBase {

    @Autowired
    private BannersAddOperationFactory addOperationFactory;

    @Autowired
    private Steps steps;

    private UserInfo userInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
    }

    @Test
    public void saveOneTextBannerWithVcardAndSitelinkSetAndContentPromotionBanner() {
        var contentPromotion = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.VIDEO);
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);

        var textAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var vcard = steps.vcardSteps().createVcard(textAdGroupInfo.getCampaignInfo());
        var sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
        var textBanner = clientTextBanner()
                .withVcardId(vcard.getVcardId())
                .withSitelinksSetId(sitelinkSet.getSitelinkSetId())
                .withAdGroupId(textAdGroupInfo.getAdGroupId());


        var contentPromotionBanner = clientContentPromoBanner(contentPromotion.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        List<Long> ids = prepareAndApplyValid(asList(textBanner, contentPromotionBanner));

        TextBanner actualTextBanner = getBanner(ids.get(0));
        ContentPromotionBanner actualContentPromotionBanner = getBanner(ids.get(1));

        assertThat(actualTextBanner.getId(), equalTo(textBanner.getId()));
        assertThat(actualContentPromotionBanner.getId(), equalTo(contentPromotionBanner.getId()));
    }

    @Test
    public void twoInvalidBannersWithDifferentInterfaces() {
        var contentPromotion = steps.contentPromotionSteps()
                .createContentPromotionContent(clientInfo.getClientId(), ContentPromotionContentType.COLLECTION);
        var contentPromotionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var contentPromotionBanner = clientContentPromoBanner(contentPromotion.getId())
                .withAdGroupId(contentPromotionAdGroup.getAdGroupId());
        assumeThat(contentPromotionBanner, instanceOf(BannerWithContentPromotion.class));
        assumeThat(contentPromotionBanner, not(instanceOf(BannerWithVcard.class)));

        var vcardFromContentPromotionCampaign =
                steps.vcardSteps().createVcard(contentPromotionAdGroup.getCampaignInfo());
        var textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var textBanner = clientTextBanner()
                .withAdGroupId(textAdGroup.getAdGroupId())
                .withVcardId(vcardFromContentPromotionCampaign.getVcardId());
        assumeThat(textBanner, instanceOf(BannerWithVcard.class));
        assumeThat(textBanner, not(instanceOf(BannerWithContentPromotion.class)));

        // проверяем валидацию с разным порядком баннеров
        SoftAssertions.assertSoftly(softly -> {
            var result1 = validate(List.of(contentPromotionBanner, textBanner));
            softly.assertThat(result1).is(matchedBy(hasDefectDefinitionWith(validationError(
                    path(index(0), field(CONTENT_PROMOTION_ID)),
                    contentTypeNotMatchesAdGroupContentType()))));
            softly.assertThat(result1).is(matchedBy(hasDefectDefinitionWith(validationError(
                    path(index(1), field(VCARD_ID)),
                    vcardOfAnotherCampaign()))));

            var result2 = validate(List.of(textBanner, contentPromotionBanner));
            softly.assertThat(result2).is(matchedBy(hasDefectDefinitionWith(validationError(
                    path(index(0), field(VCARD_ID)),
                    vcardOfAnotherCampaign()))));
            softly.assertThat(result2).is(matchedBy(hasDefectDefinitionWith(validationError(
                    path(index(1), field(CONTENT_PROMOTION_ID)),
                    contentTypeNotMatchesAdGroupContentType()))));
        });
    }

    @Test
    @Description("Создаём 2 кампании и 2 группы и 3 баннера. Первый баннер не валидный, а вторые 2 валидные. " +
            "(пытаемся сломать какие-нибудь мапы в контейнерах)")
    public void addThreeBannersFirstInvalid_DifferentAdGroups() {
        String title2 = "some test title 2";
        String title3 = "some test title 3";
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        // другая группа в другой кампании у этого же клиента
        var adGroupInfo2 = steps.contentPromotionAdGroupSteps().createAdGroup(
                adGroupInfo.getClientInfo(),
                fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO));

        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner1 = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle("title in wrong banner")
                .withHref("wrong href");
        ContentPromotionBanner banner2 = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTitle(title2);
        ContentPromotionBanner banner3 = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTitle(title3);

        List<BannerWithAdGroupId> banners = new ArrayList<>();
        banners.add(banner1);
        banners.add(banner2);
        banners.add(banner3);

        MassResult<Long> result = createOperation(banners, false, Applicability.PARTIAL).prepareAndApply();

        List<Long> ids = mapAndFilterList(result.getResult(), Result::getResult, Objects::nonNull);
        assertThat(ids, hasSize(2));
        ContentPromotionBanner actualBanner2 = getBanner(ids.get(0), ContentPromotionBanner.class);
        ContentPromotionBanner actualBanner3 = getBanner(ids.get(1), ContentPromotionBanner.class);

        assertThat(actualBanner2.getTitle(), equalTo(title2));
        assertThat(actualBanner2.getAdGroupId(), equalTo(adGroupInfo2.getAdGroupId()));
        assertThat(actualBanner2.getCampaignId(), equalTo(adGroupInfo2.getCampaignId()));

        assertThat(actualBanner3.getTitle(), equalTo(title3));
        assertThat(actualBanner3.getAdGroupId(), equalTo(adGroupInfo2.getAdGroupId()));
        assertThat(actualBanner3.getCampaignId(), equalTo(adGroupInfo2.getCampaignId()));
    }

    @Test
    @Description("Добавляем 2 баннера. Первый баннер не валидный, а второй валидный (пытаемся сломать какие-нибудь " +
            "мапы в контейнерах.")
    public void addTwoBannersFirstInvalidSecondValid_SameAdgroup() {
        String title = "some test title";
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);

        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner1 = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle("title in wrong banner")
                .withHref("wrong href");
        ContentPromotionBanner banner2 = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle(title);

        List<BannerWithAdGroupId> banners = new ArrayList<>();
        banners.add(banner1);
        banners.add(banner2);

        MassResult<Long> result = createOperation(banners, false, Applicability.PARTIAL).prepareAndApply();

        List<Long> ids = mapAndFilterList(result.getResult(), Result::getResult, Objects::nonNull);
        assertThat(ids, hasSize(1));
        ContentPromotionBanner actualBanner = getBanner(ids.get(0), ContentPromotionBanner.class);

        assertThat(actualBanner.getTitle(), equalTo(title));
        assertThat(actualBanner.getAdGroupId(), equalTo(adGroupInfo.getAdGroupId()));
        assertThat(actualBanner.getCampaignId(), equalTo(adGroupInfo.getCampaignId()));
    }

    private ValidationResult<?, Defect> validate(List<BannerWithAdGroupId> banners) {
        var operation = addOperationFactory.createAddOperation(Applicability.FULL, false, banners,
                clientInfo.getShard(), clientInfo.getClientId(), clientInfo.getUid(),
                false, false, false, false, emptySet());
        return operation.prepare().get().getValidationResult();
    }
}
