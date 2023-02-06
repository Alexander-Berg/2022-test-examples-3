package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsCurrency;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsCurrencyconverted;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.rbac.RbacRole;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.rbac.RbacRole.LIMITED_SUPPORT;
import static ru.yandex.direct.rbac.RbacRole.SUPER;
import static ru.yandex.direct.rbac.RbacRole.SUPERREADER;
import static ru.yandex.direct.rbac.RbacRole.SUPPORT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на сервис, проверяем получение wallet.order_id
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignGraphQlServiceGetWalletOrderIdTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    wallets {\n"
            + "      orderId\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final long ORDER_ID = 555L;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;

    public static List<Object[]> roleAccessParameters() {
        Set permittedRoles = Set.of(SUPER, SUPERREADER, SUPPORT, LIMITED_SUPPORT);
        return StreamEx.of(RbacRole.values())
                .mapToEntry(permittedRoles::contains)
                .mapKeyValue((role, isReturnOrderId) -> new Object[]{
                        String.format("для %s роли %s возвращаем orderId", role, isReturnOrderId ? "" : "не"),
                        role, isReturnOrderId})
                .toList();
    }

    @Test
    @Parameters(method = "roleAccessParameters")
    @TestCaseName("{0}")
    public void checkRoleAccess(@SuppressWarnings("unused") String description,
                                RbacRole role,
                                boolean returnOrderId) {
        var userInfo = steps.clientSteps().createDefaultClientWithRole(role).getChiefUserInfo();
        GridGraphQLContext context = buildContext(userInfo.getUser());

        createWallet(userInfo, true);

        ExecutionResult result = sendRequest(context);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getErrors()).as("Нет ошибок").isEmpty();

            Long orderId = GraphQLUtils.getDataValue(result.getData(), "client/wallets/0/orderId");
            if (returnOrderId) {
                soft.assertThat(orderId).as("Получили order Id").is(matchedBy(beanDiffer(ORDER_ID)));
            } else {
                soft.assertThat(orderId).as("Order Id не вернулся").isNull();
            }
        });
    }

    @Test
    public void checkWithoutCampaignsUnderWallet_ReturnNullOrderId() {
        var userInfo = steps.clientSteps().createDefaultClientWithRole(SUPER).getChiefUserInfo();
        GridGraphQLContext context = buildContext(userInfo.getUser());

        createWallet(userInfo, false);

        ExecutionResult result = sendRequest(context);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getErrors()).as("Нет ошибок").isEmpty();

            Long orderId = GraphQLUtils.getDataValue(result.getData(), "client/wallets/0/orderId");
            soft.assertThat(orderId).as("Order Id не вернулся").isNull();
        });
    }

    @Test
    public void checkWithArchiveWalletCurrency_ReturnNullOrderId() {
        var userInfo = steps.userSteps().createUser(generateNewUser()
                .withSuperManager(true)
                .withRole(SUPER));
        GridGraphQLContext context = buildContext(userInfo.getUser());

        var walletInfo = createWallet(userInfo, true);
        makeWalletWithCurrencyConverted(userInfo.getShard(), walletInfo.getCampaignId());

        ExecutionResult result = sendRequest(context);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getErrors()).as("Нет ошибок").isEmpty();

            Long orderId = GraphQLUtils.getDataValue(result.getData(), "client/wallets/0/orderId");
            soft.assertThat(orderId).as("Order Id не вернулся").isNull();
        });
    }

    private CampaignInfo createWallet(UserInfo userInfo, boolean withCampaignUnderThisWallet) {
        var walletInfo = steps.campaignSteps()
                .createCampaign(activeWalletCampaign(userInfo.getClientId(), userInfo.getUid())
                        .withOrderId(ORDER_ID), userInfo.getClientInfo());
        if (withCampaignUnderThisWallet) {
            steps.campaignSteps().createCampaignUnderWalletByCampaignType(CampaignType.TEXT,
                    userInfo.getClientInfo(), walletInfo.getCampaignId(), BigDecimal.ZERO);
        }
        return walletInfo;
    }

    private GridGraphQLContext buildContext(User user) {
        var context = ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        return context;
    }

    private ExecutionResult sendRequest(GridGraphQLContext context) {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        return result;
    }

    private void makeWalletWithCurrencyConverted(int shard, Long walletId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.CURRENCY_CONVERTED, CampaignsCurrencyconverted.Yes)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .set(CAMPAIGNS.CURRENCY, CampaignsCurrency.YND_FIXED)
                .where(CAMPAIGNS.CID.eq(walletId))
                .execute();
    }
}
