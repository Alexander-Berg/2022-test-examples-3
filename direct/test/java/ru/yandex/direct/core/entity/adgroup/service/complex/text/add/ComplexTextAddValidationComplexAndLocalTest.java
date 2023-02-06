package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroup.CAMPAIGN_ID;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.HREF;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.TITLE;
import static ru.yandex.direct.core.entity.sitelink.model.SitelinkSet.SITELINKS;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Взаимодействие комплексной валидации и валидации в отдельных операциях.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class ComplexTextAddValidationComplexAndLocalTest extends ComplexTextAddValidationTestBase {

    @Test
    public void onlyOneErrorWhenFullAdGroupHasNullCampaignId() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getAdGroup().withCampaignId(null);
        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);

        Path errPath = path(index(0), field(CAMPAIGN_ID.name()));
        assertThat("должна присутствовать ошибка для campaignId",
                vr, hasDefectDefinitionWith(validationError(errPath, notNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void interconnectionsAreNotValidatedWhenAdGroupHasOtherClientCampaignId() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();

        // выставляем группе чужой campaignId и делаем язык баннера запрещенным для региона группы
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner().withTitle("Київ"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withCampaignId(campaignInfo.getCampaignId());
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        assumeThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(CAMPAIGN_ID.name())),
                campaignNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("ожидается только одна ошибка на уровне группы", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void subObjectsAreNotValidatedWhenAdGroupHasOtherClientCampaignId() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();

        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getAdGroup().withCampaignId(campaignInfo.getCampaignId());

        // делаем невалидными все вложенные объекты, чтобы потом убедиться, что их валидация не вызывалась
        ComplexBanner complexBanner = complexAdGroup.getComplexBanners().get(0);
        ((TextBanner) complexBanner.getBanner()).withTitle(null);

        Vcard vcard = complexBanner.getVcard();
        vcard.withCity(null)
                .withCountry(null)
                .withCompanyName(null);

        SitelinkSet sitelinkSet = complexBanner.getSitelinkSet();
        sitelinkSet.getSitelinks().get(0).withHref(null);

        Keyword keyword = complexAdGroup.getKeywords().get(0);
        keyword.withPhrase(null);

        RelevanceMatch relevanceMatch = complexAdGroup.getRelevanceMatches().get(0);
        relevanceMatch.withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(null);

        OfferRetargeting offerRetargeting = complexAdGroup.getOfferRetargetings().get(0);
        offerRetargeting.withPrice(null)
                .withPriceContext(null);

        TargetInterest targetInterest = complexAdGroup.getTargetInterests().get(0);
        targetInterest.withRetargetingConditionId(null)
                .withInterestId(null)
                .withPriceContext(null);

        ComplexBidModifier complexBidModifier = complexAdGroup.getComplexBidModifier();
        complexBidModifier.getMobileModifier()
                .withType(null)
                .withMobileAdjustment(null);

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        assumeThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(CAMPAIGN_ID.name())),
                campaignNotFound())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("ожидается только одна ошибка на уровне группы", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void complexValidationAndLocalValidationOfSitelinksAreSuccessfullyMerged() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getComplexBanners().get(0).getSitelinkSet().getSitelinks().get(0)
                .withHref("wronghref")
                .withTitle(null);

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);

        Path errPath1 = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()),
                field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath1, invalidSitelinkHref())));

        Path errPath2 = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()),
                field(SITELINKS.name()), index(0), field(TITLE.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath2, notNull())));

        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должно присутствовать всего две ошибки", vr.flattenErrors(), hasSize(2));
    }
}
