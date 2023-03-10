package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import graphql.validation.ValidationError;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(Parameterized.class)
public class AdGraphQlServiceValidateAdPriceTest {
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();

    private static final String QUERY = "" +
            "query {\n" +
            "  validateAdPrice (input:[\n" +
            "    %s \n" +
            "  ]) {\n" +
            "    errors {\n" +
            "      code, path\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public String input;

    @Parameterized.Parameter(2)
    public String expectedErrorCode;

    @Parameterized.Parameter(3)
    public String expectedErrorPath;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // ???????????????????? ????????????????
                {"???????????????????? ????????", "{price:\"10.01\", currency:BYN}", null, null},
                {"???????????????????? ???????? ?? ???????????????????? ??????????????", "{price:\"10,01\", currency:BYN}", null, null},
                {"???????????????????? ???????? ?? ??????????????????", "{price:\"10.01\", priceOld: \"10 000\", currency:BYN}", null, null},
                {"???????????? ???????? ???????????? ??????????", "{price:\"1\", priceOld:\"10\", currency:RUB}", null, null},
                // ???????????????????? ????????????????
                {"???????? ???????????? ????????", "{price:\"-1\", currency:RUB}",
                        "NumberDefectIds.MUST_BE_GREATER_THAN_MIN",
                        path(index(0), field("price")).toString()},
                {"???????????? ???????? ???????????? ????????", "{price:\"1\", priceOld:\"-10\", currency:RUB}",
                        "NumberDefectIds.MUST_BE_GREATER_THAN_MIN",
                        path(index(0), field("priceOld")).toString()},
                {"???????????? ???????? ???????????? ??????????", "{price:\"10\", priceOld:\"1\", currency:RUB}",
                        "BannerDefectIds.Gen.BANNER_PRICE_GREATER_THAN_OLD",
                        path(index(0), field("priceOld")).toString()},
                {"???????????????????????? ????????", "{price:\"test\", priceOld:\"1\", currency:RUB}",
                        "DefectIds.INVALID_VALUE",
                        path(index(0), field("price")).toString()},
                {"???????????????????????? ???????????? ????????", "{price:\"10\", priceOld:\"test\", currency:RUB}",
                        "DefectIds.INVALID_VALUE",
                        path(index(0), field("priceOld")).toString()},
                // ???????????????????????? ????????????
                {"???????????? ???? ????????????", "{price: \"10\"}", "missing required fields '[currency]'", null},
                {"???????? ???? ????????????", "{currency: RUB}", "missing required fields '[price]'", null},
        });
    }

    private GridGraphQLContext context;

    @Test
    public void testService() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        context = ContextHelper.buildContext(userInfo.getUser()).withFetchedFieldsReslover(null);
        ExecutionResult result = processor.processQuery(null, String.format(QUERY, input), null, context);
        if (result.getData() != null) {
            validatePartialError(result);
        } else {
            validateFullError(result);
        }
    }

    private void validateFullError(ExecutionResult result) {
        assertThat(result.getErrors(), hasSize(1));
        ValidationError error = (ValidationError) result.getErrors().get(0);
        assertThat(error.getDescription(), containsString(expectedErrorCode));
    }

    private void validatePartialError(ExecutionResult result) {
        Map<String, List> validationResult = (Map<String, List>) ((Map) result.getData()).get("validateAdPrice");
        if (expectedErrorCode == null) {
            assertThat(validationResult, nullValue());
        } else {
            assertThat(validationResult.size(), is(1));
            List<Map<String, String>> errors = validationResult.get("errors");
            assertThat(errors, hasSize(1));
            assertThat(errors.get(0).get("code"), equalTo(expectedErrorCode));
            assertThat(errors.get(0).get("path"), equalTo(expectedErrorPath));
        }
    }
}
