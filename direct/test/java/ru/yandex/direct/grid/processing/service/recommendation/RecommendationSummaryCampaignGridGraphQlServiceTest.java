package ru.yandex.direct.grid.processing.service.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anything;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOPAY_SETTINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_DAY_BUDGET_STOP_HISTORY;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_PAYMENT_TRANSACTIONS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RecommendationSummaryCampaignGridGraphQlServiceTest {
    private static final String QUERY_TEMPLATE = ""
        + "{\n"
        + "  client(searchBy: {login: \"%s\"}) {\n"
        + "    recommendationsSummaryCampaignGrid {\n"
        + "      rowset {\n"
        + "        type\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}\n";

    private GdCampaignsContainer campaignsContainer;
    private CampaignInfo campaignInfoOne;
    private CampaignInfo campaignInfoTwo;
    private CampaignInfo campaignInfoWallet;
    private GridGraphQLContext context;
    private UserInfo userInfo;
    private ClientId clientId;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void initTestData() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientId = userInfo.getClientId();
        campaignInfoWallet = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(activeWalletCampaign(clientId, userInfo.getUid())));

        Campaign camp = activeTextCampaign(clientId, userInfo.getUid());
        camp.getBalanceInfo()
            .withWalletCid(campaignInfoWallet.getCampaignId());
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(camp));

        Campaign campTwo = activeTextCampaign(clientId, userInfo.getUid());
        campaignInfoTwo = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(campTwo));

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        context = ContextHelper.buildContext(userInfo.getUser())
            .withFetchedFieldsReslover(null);
        TestAuthHelper.setDirectAuthentication(context.getOperator());
        steps.featureSteps().addClientFeature(userInfo.getClientInfo().getClientId(),
            FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
    }

    @Test
    public void testService() {
        campaignsContainer.getFilter().setCampaignIdIn(
            ImmutableSet.of(campaignInfoOne.getCampaignId(), campaignInfoTwo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
            graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
            .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
            "client",
            ImmutableMap.of(
                "recommendationsSummaryCampaignGrid", ImmutableMap.of(
                    "rowset", emptyList()
                )
            )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
            .forFields(newPath("client", "campaigns", "recommendationsSummaryCampaignGrid", "\\d+", "type"))
            .useMatcher(anything())
            .forFields(newPath("client", "campaigns", "rowset", "type"))
            .useMatcher(anything());

        assertThat(data)
            .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void autopayStoppedRecommendationTest() {
        Campaign camp = activeTextCampaign(clientId, userInfo.getUid());
        camp.getBalanceInfo()
            .withWalletCid(campaignInfoWallet.getCampaignId());
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(camp));
        setAutopayTriesNum(userInfo.getShard(), campaignInfoWallet.getCampaignId(), 3L);
        setLastBalanceStatusCode(userInfo.getShard(), campaignInfoWallet.getCampaignId(), "Error");

        campaignsContainer.getFilter().setCampaignIdIn(
            ImmutableSet.of(campaignInfoOne.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
            graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
            .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
            "client",
            ImmutableMap.of(
                "recommendationsSummaryCampaignGrid", ImmutableMap.of(
                    "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                        .put("type", GdiRecommendationType.autopayStopped.name())
                        .build())
                )
            )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
            .forFields(newPath("client", "campaigns", "recommendationsSummaryCampaignGrid", "\\d+", "type"))
            .useMatcher(anything());

        assertThat(data)
            .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void overdraftDebtRecommendation_WithCommonWallet() {
        Campaign camp = activeTextCampaign(clientId, userInfo.getUid());
        camp.getBalanceInfo()
            .withWalletCid(campaignInfoWallet.getCampaignId());
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(camp));
        updateClientsOptions(userInfo.getShard(), userInfo.getClientId().asLong(), BigDecimal.TEN);

        campaignsContainer.getFilter().setCampaignIdIn(
            ImmutableSet.of(campaignInfoOne.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
            graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
            .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
            "client",
            ImmutableMap.of(
                "recommendationsSummaryCampaignGrid", ImmutableMap.of(
                    "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                        .put("type", GdiRecommendationType.overdraftDebt.name())
                        .build())
                )
            )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
            .forFields(newPath("client", "campaigns", "recommendationsSummaryCampaignGrid", "\\d+", "type"))
            .useMatcher(anything());

        assertThat(data)
            .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void overdraftDebtRecommendationTest_WithoutCommonWallet() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientId = userInfo.getClientId();
        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        TestAuthHelper.setDirectAuthentication(context.getOperator());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(userInfo.getClientInfo())
                .withCampaign(activeTextCampaign(clientId, userInfo.getUid())));
        updateClientsOptions(userInfo.getShard(), userInfo.getClientId().asLong(), BigDecimal.TEN);

        campaignsContainer.getFilter().setCampaignIdIn(
                ImmutableSet.of(campaignInfo.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "recommendationsSummaryCampaignGrid", ImmutableMap.of(
                                "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                                        .put("type", GdiRecommendationType.overdraftDebt.name())
                                        .build())
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "campaigns", "recommendationsSummaryCampaignGrid", "\\d+", "type"))
                .useMatcher(anything());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void mainInvoiceRecommendationTest() {
        Campaign camp = activeTextCampaign(clientId, userInfo.getUid());
        camp.getBalanceInfo()
            .withWalletCid(campaignInfoWallet.getCampaignId());
        campaignInfoOne = steps.campaignSteps().createCampaign(new CampaignInfo()
            .withClientInfo(userInfo.getClientInfo())
            .withCampaign(camp));
        setMainInvoice(userInfo.getShard(), campaignInfoWallet.getCampaignId());

        campaignsContainer.getFilter().setCampaignIdIn(
            ImmutableSet.of(campaignInfoOne.getCampaignId()));
        campaignsContainer.getLimitOffset().setOffset(1);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
            graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
            .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
            "client",
            ImmutableMap.of(
                "recommendationsSummaryCampaignGrid", ImmutableMap.of(
                    "rowset", Collections.singletonList(ImmutableMap.<String, Object>builder()
                        .put("type", GdiRecommendationType.mainInvoice.name())
                        .build())
                )
            )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
            .forFields(newPath("client", "campaigns", "recommendationsSummaryCampaignGrid", "\\d+", "type"))
            .useMatcher(anything());

        assertThat(data)
            .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    private void setMainInvoice(int shard, Long cid) {
        dslContextProvider.ppc(shard)
            .insertInto(CAMP_DAY_BUDGET_STOP_HISTORY, CAMP_DAY_BUDGET_STOP_HISTORY.CID,
                CAMP_DAY_BUDGET_STOP_HISTORY.STOP_TIME)
            .values(cid, LocalDateTime.of(LocalDate.now(), LocalTime.MIN))
            .execute();

        dslContextProvider.ppc(shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.DAY_BUDGET, BigDecimal.TEN)
            .where(CAMPAIGNS.CID.eq(cid))
            .execute();

        dslContextProvider.ppc(shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.DAY_BUDGET_STOP_TIME, LocalDateTime.now())
            .where(CAMP_OPTIONS.CID.eq(cid))
            .execute();
    }

    private void updateClientsOptions(int shard, Long clientID, BigDecimal debt) {
        dslContextProvider.ppc(shard)
            .update(CLIENTS_OPTIONS)
            .set(CLIENTS_OPTIONS.DEBT, debt)
            .set(CLIENTS_OPTIONS.NEXT_PAY_DATE, LocalDate.now())
            .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientID))
            .execute();
    }

    private void setAutopayTriesNum(int shard, Long walletCid, Long triesNum) {
        dslContextProvider.ppc(shard)
            .insertInto(AUTOPAY_SETTINGS, AUTOPAY_SETTINGS.TRIES_NUM, AUTOPAY_SETTINGS.WALLET_CID,
                AUTOPAY_SETTINGS.PAYER_UID, AUTOPAY_SETTINGS.PAYMETHOD_ID, AUTOPAY_SETTINGS.REMAINING_SUM,
                AUTOPAY_SETTINGS.PAYMENT_SUM)
            .values(triesNum, walletCid, userInfo.getUid(), "", BigDecimal.ZERO, BigDecimal.ZERO)
            .execute();

        dslContextProvider.ppc(shard)
            .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID, WALLET_CAMPAIGNS.AUTOPAY_MODE)
            .values(walletCid, WalletCampaignsAutopayMode.min_balance)
            .execute();
    }

    private void setLastBalanceStatusCode(int shard, Long walletCid, String balanceStatusCode) {
        dslContextProvider.ppc(shard)
            .insertInto(WALLET_PAYMENT_TRANSACTIONS,
                WALLET_PAYMENT_TRANSACTIONS.BALANCE_STATUS_CODE,
                WALLET_PAYMENT_TRANSACTIONS.WALLET_CID,
                WALLET_PAYMENT_TRANSACTIONS.PAYER_UID)
            .values(balanceStatusCode, walletCid, userInfo.getUid())
            .execute();
    }
}
