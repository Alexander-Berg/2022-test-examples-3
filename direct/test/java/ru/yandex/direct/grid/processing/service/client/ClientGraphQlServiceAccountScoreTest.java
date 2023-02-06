package ru.yandex.direct.grid.processing.service.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.ListDiffer;
import ru.yandex.direct.core.entity.account.score.model.AccountScore;
import ru.yandex.direct.core.entity.account.score.model.AccountScoreFactors;
import ru.yandex.direct.core.entity.account.score.repository.AccountScoreRepository;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbschema.ppc.enums.AccountScoreType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdAccountScoreInfo;
import ru.yandex.direct.grid.processing.model.client.GdVerdicts;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CONTENT_PROMOTION;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(Parameterized.class)
public class ClientGraphQlServiceAccountScoreTest {

    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    info {\n"
            + "      accountScoreInfo {\n"
            + "        score,\n"
            + "        prevScore,\n"
            + "        progress,\n"
            + "        verdicts\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AccountScoreRepository accountScoreRepository;

    @Autowired
    private UserSteps userSteps;

    @Parameterized.Parameter()
    public String testName;

    @Parameterized.Parameter(1)
    public AccountScore accountScore;

    @Parameterized.Parameter(2)
    public AccountScore prevAccountScore;

    @Parameterized.Parameter(3)
    public GdAccountScoreInfo expectedInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "В базе две записи",
                        new AccountScore()
                                .withDate(LocalDate.now())
                                .withScore(BigDecimal.ONE)
                                .withType(AccountScoreType.client)
                                .withFactors(new AccountScoreFactors()
                                .withAvgBannersCount(2d)
                                .withVcardsCount(1d)
                                .withSeparatePlaceCount(2d)),
                        new AccountScore()
                                .withDate(LocalDate.now().minusDays(1))
                                .withScore(BigDecimal.ONE)
                                .withType(AccountScoreType.client)
                                .withFactors(new AccountScoreFactors()),
                        new GdAccountScoreInfo()
                                .withScore(BigDecimal.ONE)
                                .withProgress(0)
                                .withPrevScore(BigDecimal.ONE)
                                .withVerdicts(Set.of(GdVerdicts.AVG_BANNERS))
                },
                {
                        "В базе 1 запись",
                        new AccountScore()
                                .withDate(LocalDate.now())
                                .withScore(BigDecimal.TEN)
                                .withType(AccountScoreType.client)
                                .withFactors(new AccountScoreFactors()
                                .withCtxPriceCoefPercent(1d)
                                .withVcardsCount(1d)
                                .withSeparatePlaceCount(2d)),
                        null,
                        new GdAccountScoreInfo()
                                .withScore(BigDecimal.TEN)
                                .withProgress(1)
                                .withPrevScore(BigDecimal.ZERO)
                                .withVerdicts(Set.of(GdVerdicts.CTX_PRICE_COEF))
                },
                {
                        "Прогресс отрицательный",
                        new AccountScore()
                                .withDate(LocalDate.now())
                                .withScore(BigDecimal.ONE)
                                .withType(AccountScoreType.client)
                                .withFactors(new AccountScoreFactors()
                                .withImagesPercent(40d)
                                .withShowsDaysCount(20d)
                                .withVcardsCount(1d)
                                .withSeparatePlaceCount(2d)),
                        new AccountScore()
                                .withDate(LocalDate.now().minusDays(1))
                                .withScore(BigDecimal.TEN)
                                .withType(AccountScoreType.client)
                                .withFactors(new AccountScoreFactors()),
                        new GdAccountScoreInfo()
                                .withScore(BigDecimal.ONE)
                                .withProgress(-1)
                                .withPrevScore(BigDecimal.TEN)
                                .withVerdicts(Set.of(GdVerdicts.STOP_DAY))
                }
        });
    }

    @Test
    public void testService() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        Long clientId = userInfo.getClientId().asLong();
        accountScore.setClientId(clientId);
        List<AccountScore> accountScoresToAdd = List.of(accountScore);
        if (prevAccountScore != null) {
            prevAccountScore.setClientId(clientId);
            accountScoresToAdd = List.of(accountScore, prevAccountScore);
        }

        accountScoreRepository.addAccountScores(userInfo.getShard(), accountScoresToAdd);
        GridGraphQLContext operatorContext = new GridGraphQLContext(userInfo.getUser());
        String query = String.format(QUERY_TEMPLATE, userInfo.getUser().getLogin(), asList(CONTENT_PROMOTION));

        ExecutionResult result = processor.processQuery(null, query, null, operatorContext);

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();

        List<String> expectedVerdicts = mapList(expectedInfo.getVerdicts(), GdVerdicts::toString);

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "info", ImmutableMap.of(
                                "accountScoreInfo", ImmutableMap.of(
                                        "score", expectedInfo.getScore(),
                                        "prevScore", expectedInfo.getPrevScore(),
                                        "progress", expectedInfo.getProgress(),
                                        "verdicts", expectedVerdicts
                                )
                        )
                )
        );

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("client", "info", "accountScoreInfo", "score"))
                .useDiffer(new BigDecimalDiffer())
                .forFields(newPath("client", "info", "accountScoreInfo", "prevScore"))
                .useDiffer(new BigDecimalDiffer())
                .forFields(newPath("client", "info", "accountScoreInfo", "verdicts"))
                .useDiffer(new ListDiffer());

        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }
}
