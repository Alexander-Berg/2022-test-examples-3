package ru.yandex.direct.core.entity.keyword.service.validation;

import java.math.BigDecimal;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.container.InternalKeyword;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.internal.InternalKeywordFactory;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordWithoutInterconnectionsValidator.MAX_PARAM_LENGTH;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.currency.Money.valueOf;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordWithoutInterconnectionsValidatorTest {

    private static final BigDecimal MAX_AUTOBUDGET_BID = new BigDecimal("25000");
    private static final BigDecimal MIN_AUTOBUDGET_BID = new BigDecimal("0.3");

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private StopWordService stopWordService;
    @Autowired
    private KeywordWithLemmasFactory keywordWithLemmasFactory;
    @Autowired
    private InternalKeywordFactory internalKeywordFactory;

    private KeywordInfo keywordInfo;
    private Keyword keyword;
    private Campaign campaign;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(newTextCampaign(null, null).withStrategy(manualStrategy().withSeparateBids(false)));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), campaignInfo);
        keywordInfo = steps.keywordSteps().createKeyword(bannerInfo.getAdGroupInfo());

        campaign = campaignRepository.getCampaigns(
                bannerInfo.getShard(), singleton(bannerInfo.getCampaignId())).get(0);

        keyword = keywordInfo.getKeyword().withPrice(new BigDecimal(1));
    }

    //default
    @Test
    public void build_OneValidKeyword_NoErrors() {
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    //price
    @Test
    public void build_KeywordMaxPriceAllowed_NoErrors() {
        keyword.withPrice(MAX_AUTOBUDGET_BID);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordMinPriceAllowed_NoErrors() {
        keyword.withPrice(MIN_AUTOBUDGET_BID);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordWithoutPrice_SearchPriceIsNotSetForManualStrategyDefinition() {
        keyword.withPrice(null);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "price", new Defect<>(
                BidsDefects.Ids.SEARCH_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY));
    }

    @Test
    public void build_KeywordWithPriceGreaterThan_SearchPriceIsNotSmallerThenMaxDefinition() {
        keyword.withPrice(new BigDecimal(25001));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "price", new Defect<>(
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                new CurrencyAmountDefectParams(valueOf(MAX_AUTOBUDGET_BID, CurrencyCode.RUB))));
    }

    @Test
    public void build_KeywordWithPriceLessThan_SearchPriceIsNotGreaterThanMinDefinition() {
        keyword.withPrice(new BigDecimal(0.2));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "price", new Defect<>(
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                new CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, CurrencyCode.RUB))));
    }

    @Test
    public void build_KeywordWithoutPrice_MissingPricesAllowed_NoErrors() {
        ValidationResult<Keyword, Defect> vr = buildAndApplyAllowMissingPrices(keyword.withPrice(null));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    //priceContext
    @Test
    public void build_KeywordMaxPriceContextAllowed_NoErrors() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword.withPriceContext(MAX_AUTOBUDGET_BID));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordMinPriceContextAllowed_NoErrors() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword.withPriceContext(MIN_AUTOBUDGET_BID));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void build_KeywordWithoutPriceContext_SearchPriceIsNotSetForManualStrategyDefinition() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        keyword.withPriceContext(null);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "priceContext", new Defect<>(
                BidsDefects.Ids.CONTEXT_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY));
    }

    @Test
    public void build_KeywordWithPriceContextGreaterThan_SearchPriceIsNotSmallerThenMaxDefinition() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        keyword.withPriceContext(new BigDecimal(25001));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "priceContext", new Defect<>(
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                new CurrencyAmountDefectParams(valueOf(MAX_AUTOBUDGET_BID, CurrencyCode.RUB))));
    }

    @Test
    public void build_KeywordWithPriceContextLessThan_SearchPriceIsNotGreaterThanMinDefinition() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        keyword.withPriceContext(new BigDecimal(0.2));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "priceContext", new Defect<>(
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                new CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, CurrencyCode.RUB))));
    }

    @Test
    public void build_KeywordWithoutPriceContext_MissingPricesAllowed_NoErrors() {
        campaign.getStrategy().setPlatform(CampaignsPlatform.CONTEXT);
        ValidationResult<Keyword, Defect> vr = buildAndApplyAllowMissingPrices(keyword.withPriceContext(null));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    // keyword phrase
    @Test
    public void hasErrorOnSyntacticallyInvalidKeyword() {
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword.withPhrase("невалидная] фраза"));
        assertSingleKeywordError(vr, "phrase", PhraseDefects.invalidBrackets());
    }

    @Test
    public void hasErrorOnLogicallyInvalidKeyword() {
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword.withPhrase("на то"));
        assertSingleKeywordError(vr, "phrase", PhraseDefects.onlyStopWords());
    }

    //autobudgetPriority
    @Test
    public void build_KeywordAutobudgetStrategyWithoutAutobudgetPriority_PriorityIsNotSetForAutoStrategyDefinition() {
        keyword.withAutobudgetPriority(null);
        campaign.getStrategy().setAutobudget(CampaignsAutobudget.YES);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "autobudgetPriority", new Defect<>(
                BidsDefects.Ids.PRIORITY_IS_NOT_SET_FOR_AUTO_STRATEGY));
    }

    @Test
    public void build_KeywordAutobudgetStrategyWithPrice_Warning() {
        keyword.withAutobudgetPriority(5);
        keyword.withPrice(BigDecimal.TEN);
        keyword.withPriceContext(null);
        campaign.getStrategy().setAutobudget(CampaignsAutobudget.YES);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoErrors());
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("price")),
                        BidsDefects.Ids.BID_FOR_SEARCH_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY)));
    }

    @Test
    public void build_KeywordAutobudgetStrategyWithPriceContext_Warning() {
        keyword.withAutobudgetPriority(5);
        keyword.withPrice(null);
        keyword.withPriceContext(BigDecimal.TEN);
        campaign.getStrategy().setAutobudget(CampaignsAutobudget.YES);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoErrors());
        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field("priceContext")),
                        BidsDefects.Ids.BID_FOR_CONTEXT_WONT_BE_ACCEPTED_NOT_DIFFERENT_PLACES)));
    }

    @Test
    public void build_KeywordAutobudgetPriority_PriorityInvalid() {
        keyword.withAutobudgetPriority(7);
        campaign.getStrategy().setAutobudget(CampaignsAutobudget.YES);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "autobudgetPriority", new Defect<>(
                BidsDefects.Ids.PRIORITY_HAS_WRONG_VALUE));
    }

    @Test
    public void build_KeywordWithoutAutobudgetPriority_MissingPricesAllowed_NoErrors() {
        campaign.getStrategy().setAutobudget(CampaignsAutobudget.YES);
        ValidationResult<Keyword, Defect> vr = buildAndApplyAllowMissingPrices(keyword.withAutobudgetPriority(null));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void checkHrefParam1LettersPositive() {
        keyword.withHrefParam1(Strings.padEnd("aaa&111", MAX_PARAM_LENGTH, '-'));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void checkHrefParam2LettersPositive() {
        keyword.withHrefParam2(Strings.padEnd("aaa&111", MAX_PARAM_LENGTH, '-'));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void checkHrefParam1LettersNegative() {
        keyword.withHrefParam1("åß∂");
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "hrefParam1", new Defect<>(
                DefectIds.MUST_CONTAIN_LETTERS_OR_DIGITS_OR_PUNCTUATIONS));
    }

    @Test
    public void checkHrefParam2LettersNegative() {
        keyword.withHrefParam2("åß∂");
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "hrefParam2", new Defect<>(
                DefectIds.MUST_CONTAIN_LETTERS_OR_DIGITS_OR_PUNCTUATIONS));
    }

    @Test
    public void checkHrefParam1Length() {
        keyword.withHrefParam1(Strings.repeat("a", MAX_PARAM_LENGTH + 1));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "hrefParam1",
                CollectionDefects.maxStringLength(MAX_PARAM_LENGTH));
    }

    @Test
    public void checkHrefParam2Length() {
        keyword.withHrefParam2(Strings.repeat("a", MAX_PARAM_LENGTH + 1));
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertSingleKeywordError(vr, "hrefParam2",
                CollectionDefects.maxStringLength(MAX_PARAM_LENGTH));
    }

    @Test
    public void checkStrategyIsNotSet() {
        campaign.withStrategy(null);
        ValidationResult<Keyword, Defect> vr = buildAndApply(keyword);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(), BidsDefects.Ids.STRATEGY_IS_NOT_SET)));

    }

    private ValidationResult<Keyword, Defect> buildAndApply(Keyword keyword) {
        return buildAndApply(keyword, true);
    }

    private ValidationResult<Keyword, Defect> buildAndApplyAllowMissingPrices(Keyword keyword) {
        return buildAndApply(keyword, false);
    }

    private ValidationResult<Keyword, Defect> buildAndApply(Keyword keyword, boolean mustHavePrices) {
        InternalKeyword internalKeyword = internalKeywordFactory.createInternalKeyword(keyword);
        return KeywordWithoutInterconnectionsValidator
                .build(stopWordService, keywordWithLemmasFactory, internalKeyword,
                        campaign, keywordInfo.getAdGroupInfo().getAdGroupType(), mustHavePrices)
                .apply(keyword);
    }

    private void assertSingleKeywordError(ValidationResult<Keyword, Defect> validationResult,
                                          String field, Defect expectedDefect) {
        assertThat(validationResult, hasDefectDefinitionWith(validationError(path(field(field)), expectedDefect)));
    }
}
