package ru.yandex.direct.grid.processing.service.cashback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.cashback.GdCashbackRewardsDetailsInput;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.i18n.I18NBundle;

import static java.math.RoundingMode.DOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.steps.CashbackSteps.getTechnicalProgram;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CashbackGraphQLServiceTest {
    private static final LocalDate DEFAULT_DATE = LocalDate.now();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String CASHBACK_INFO_QUERY_TEMPLATE = "" +
            "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    cashbackInfo {\n" +
            "      cashbacksEnabled\n" +
            "      currency\n" +
            "      totalCashback\n" +
            "      totalCashbackWithoutNds\n" +
            "      awaitingCashback\n" +
            "      programs {\n" +
            "        programId\n" +
            "        name\n" +
            "        description\n" +
            "        percent\n" +
            "        enabled\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String REWARD_DETAILS_QUERY_TEMPLATE = "" +
            "query {\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    cashbackRewardsDetails(input: %s) {\n" +
            "      totalCashback\n" +
            "      totalCashbackWithoutNds\n" +
            "      totalByPrograms {\n" +
            "        programId\n" +
            "        date\n" +
            "        reward\n" +
            "        rewardWithoutNds\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private CashbackSteps cashbackSteps;

    @Autowired
    private ClientService clientService;

    @Before
    public void init() {
        LocaleContextHolder.setLocale(I18NBundle.RU);

        var clientInfo = steps.clientSteps().createDefaultClient();

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        TestAuthHelper.setDirectAuthentication(context.getOperator());

        cashbackSteps.createTechnicalEntities();
        cashbackSteps.updateConsumedCashback(clientInfo.getClientId(), BigDecimal.TEN);
        cashbackSteps.updateAwaitingCashback(clientInfo.getClientId(), BigDecimal.ONE);

        var technicalProgram = getTechnicalProgram();
        cashbackSteps.addRewardDetails(
                clientInfo.getClientId(),
                technicalProgram.getId(),
                BigDecimal.TEN,
                BigDecimal.TEN,
                DEFAULT_DATE);
        cashbackSteps.addRewardDetails(
                clientInfo.getClientId(),
                technicalProgram.getId(),
                BigDecimal.TEN,
                BigDecimal.TEN,
                DEFAULT_DATE.minusMonths(1L));
    }

    @Test
    public void testGetCashbackInfo() {
        var query = String.format(CASHBACK_INFO_QUERY_TEMPLATE, context.getOperator().getLogin());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        var currency = clientService.getWorkCurrency(context.getOperator().getClientId());
        var technicalProgram = getTechnicalProgram();
        Map<String, Object> expected = Map.of("client", Map.of(
                "cashbackInfo", Map.of(
                        "cashbacksEnabled", true,
                        "currency", currency.getCode().name(),
                        "totalCashback", BigDecimal.TEN.setScale(2, DOWN),
                        "awaitingCashback", BigDecimal.ONE.setScale(2, DOWN),
                        "programs", List.of()
                )));

        assertThat(data).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(allFieldsExcept(
                        newPath("client", "cashbackInfo", "totalCashbackWithoutNds"),
                        newPath("client", "cashbackInfo", "awaitingCashbackWithoutNds")))));
    }

    @Test
    public void testGetCashbackInfo_severalProgram() {
        var newCategory = new CashbackCategory()
                .withNameRu("Какая-то категория")
                .withNameEn("Some category")
                .withDescriptionRu("Блаблабла")
                .withDescriptionEn("Blablabla");
        cashbackSteps.createCategory(newCategory);

        var privateProgram = new CashbackProgram()
                .withCategoryId(newCategory.getId())
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(true)
                .withIsPublic(true);
        cashbackSteps.createProgram(privateProgram);

        var query = String.format(CASHBACK_INFO_QUERY_TEMPLATE, context.getOperator().getLogin());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        var currency = clientService.getWorkCurrency(context.getOperator().getClientId());
        var technicalProgram = getTechnicalProgram();
        Map<String, Object> expected = Map.of("client", Map.of(
                "cashbackInfo", Map.of(
                        "cashbacksEnabled", true,
                        "currency", currency.getCode().name(),
                        "totalCashback", BigDecimal.TEN.setScale(2, DOWN),
                        "awaitingCashback", BigDecimal.ONE.setScale(2, DOWN),
                        "programs", List.of(Map.of(
                                "programId", privateProgram.getId(),
                                "name", newCategory.getNameRu(),
                                "description", newCategory.getDescriptionRu(),
                                "percent", privateProgram.getPercent().setScale(4, DOWN),
                                "enabled", true
                            ))
                )));

        assertThat(data).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(allFieldsExcept(
                        newPath("client", "cashbackInfo", "totalCashbackWithoutNds"),
                        newPath("client", "cashbackInfo", "awaitingCashbackWithoutNds")))));
    }

    @Test
    public void testGetCashbackRewardsDetails() {
        var request = new GdCashbackRewardsDetailsInput().withPeriod(2);
        var query = String.format(REWARD_DETAILS_QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        var technicalProgram = getTechnicalProgram();
        Map<String, Object> expected = Map.of("client", Map.of(
                "cashbackRewardsDetails", Map.of(
                        "totalCashback", BigDecimal.TEN.setScale(2, DOWN),
                        "totalByPrograms", List.of(Map.of(
                                "programId", technicalProgram.getId(),
                                "date", DEFAULT_DATE.minusMonths(1L).format(DATE_FORMAT),
                                "reward", BigDecimal.TEN.setScale(6, DOWN)),
                                Map.of(
                                "programId", technicalProgram.getId(),
                                "date", DEFAULT_DATE.format(DATE_FORMAT),
                                "reward", BigDecimal.TEN.setScale(6, DOWN)
                                ))
                )));

        assertThat(data).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(allFieldsExcept(
                        newPath("client", "cashbackRewardsDetails", "totalCashbackWithoutNds"),
                        newPath("client", "cashbackRewardsDetails", "totalByPrograms", "0", "rewardWithoutNds"),
                        newPath("client", "cashbackRewardsDetails", "totalByPrograms", "1", "rewardWithoutNds")))));
    }
}
