package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationCampaignBetweenClientsBidsCurrencyConversionTest {
    public static final CurrencyCode TARGET_CURRENCY = CurrencyCode.USD;
    public static final BigDecimal RATE = BigDecimal.valueOf(60);
    public static final CurrencyCode SOURCE_CURRENCY = CurrencyCode.RUB;

    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private BaseCampaignService baseCampaignService;

    @Autowired
    private RelevanceMatchService relevanceMatchService;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private CopyOperationFactory copyOperationFactory;

    private Long uid;
    private ClientId clientId;
    private ClientId clientIdTo;

    private CampaignInfo campaignInfo;
    private TextBannerInfo bannerInfo;
    private CopyOperation xerox;

    private CopyResult copyResult;

    private Set<Long> newCampaignIds;

    @Before
    public void setUp() {
        steps.currencySteps().createCurrencyRate(TARGET_CURRENCY, LocalDate.now(), RATE);

        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(SOURCE_CURRENCY));
        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);

        ClientInfo clientInfoTo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(TARGET_CURRENCY));
        clientIdTo = clientInfoTo.getClientId();

        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStartTime(LocalDate.now().plusDays(1L)),
                clientInfo);
        bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        Long campaignId = campaignInfo.getCampaignId();

        asserts.init(clientId, clientIdTo, uid);

        xerox = copyOperationFactory.build(clientInfo.getShard(), clientInfo.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                uid,
                BaseCampaign.class, List.of(campaignId),
                new CopyCampaignFlags());
    }

    @Test
    public void campaignIsCopied() {
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        newCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());

        assertThat(newCampaignIds).hasSize(1);
    }

    @Test
    public void campaignCurrencyIsChanged() {
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        newCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var newCampaign = (TextCampaign) baseCampaignService.get(clientIdTo, uid, newCampaignIds).get(0);

        assertThat(newCampaign.getCurrency()).isEqualTo(TARGET_CURRENCY);
    }

    @Test
    public void keywordPricesAreConverted() {
        var priceRub = BigDecimal.valueOf(120);
        var priceContextRub = BigDecimal.valueOf(240);

        Long keywordId = steps.keywordSteps().createKeyword(
                bannerInfo.getAdGroupInfo(),
                defaultKeyword().withPrice(priceRub).withPriceContext(priceContextRub)).getId();
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        Long newKeywordId = (Long) copyResult.getEntityMapping(Keyword.class).get(keywordId);
        assertThat(newKeywordId).isNotNull();

        Keyword keyword = keywordService.get(clientIdTo, uid, singletonList(newKeywordId)).get(0);

        assertThat(keyword.getPrice()).isEqualTo(getExpectedConvertedValue(priceRub));
        assertThat(keyword.getPriceContext()).isEqualTo(getExpectedConvertedValue(priceContextRub));
    }

    @Test
    public void relevanceMatchPricesAreConverted() {
        var priceRub = BigDecimal.valueOf(120);
        var priceContextRub = BigDecimal.valueOf(240);

        Long relevanceMatchId = steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(
                bannerInfo.getAdGroupInfo(),
                priceRub,
                priceContextRub);
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        Long newRelevanceMatchId = (Long) copyResult.getEntityMapping(RelevanceMatch.class).get(relevanceMatchId);
        assertThat(newRelevanceMatchId).isNotNull();

        RelevanceMatch relevanceMatch =
                relevanceMatchService.get(clientIdTo, uid, singletonList(newRelevanceMatchId)).get(0);

        assertThat(relevanceMatch.getPrice()).isEqualTo(getExpectedConvertedValue(priceRub));
        assertThat(relevanceMatch.getPriceContext()).isEqualTo(getExpectedConvertedValue(priceContextRub));
    }

    @Test
    public void retargetingPriceContextIsConverted() {
        var priceContextRub = BigDecimal.valueOf(240);

        Long retargetingId = steps.retargetingSteps().createRetargeting(
                defaultRetargeting()
                        .withPriceContext(priceContextRub),
                bannerInfo.getAdGroupInfo()
        ).getRetargetingId();
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        Long newRetargetingId = (Long) copyResult.getEntityMapping(Retargeting.class).get(retargetingId);
        assertThat(newRetargetingId).isNotNull();

        Retargeting retargeting = retargetingService.get(clientIdTo, uid, singletonList(newRetargetingId)).get(0);

        assertThat(retargeting.getPriceContext()).isEqualTo(getExpectedConvertedValue(priceContextRub));
    }

    @NotNull
    private BigDecimal getExpectedConvertedValue(BigDecimal dayBudgetRub) {
        return dayBudgetRub.divide(RATE, 2, RoundingMode.HALF_UP);
    }

}
