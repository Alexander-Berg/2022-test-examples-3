package ru.yandex.direct.autotests.gridtest.scheme;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.autotests.gridtest.scheme.util.GraphQLSchemaB2BProperties;
import ru.yandex.direct.autotests.gridtest.scheme.util.GridFeatures;
import ru.yandex.direct.autotests.gridtest.scheme.util.WebApiAdminClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Features(GridFeatures.SCHEMA_B2B)
@Description("B2B тест на graphQL схему для нового интерфейса")
@Issue("https://st.yandex-team.ru/DIRECT-77188")
public class DirectGridGraphQLSchemaB2BTest {

    private static final String VERSION_NAME = "version";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final GraphQLSchemaB2BProperties B2B_PROPERTIES = GraphQLSchemaB2BProperties.get();

    private static DirectTestRunProperties stableProperties;
    private static DirectTestRunProperties prestableProperties;

    private static WebApiAdminClient stable;
    private static WebApiAdminClient prestable;

    /**
     * Игнорируемые поля
     */
    private static List<BeanFieldPath> ignoredFields = new ArrayList<>();

    @BeforeClass
    public static void init() {
        stableProperties = DirectTestRunProperties.newInstance().withDirectStage(B2B_PROPERTIES.getStableStage());
        prestableProperties = DirectTestRunProperties.newInstance().withDirectStage(B2B_PROPERTIES.getPrestableStage());
        stable = new WebApiAdminClient(stableProperties.getDirectHost());
        prestable = new WebApiAdminClient(prestableProperties.getDirectHost());


    }

    @Before
    public void before() {
        String stableVersion = stable.getVersion().get(VERSION_NAME).getAsString();
        String prestableVersion = prestable.getVersion().get(VERSION_NAME).getAsString();
        assumeThat("сравниваем схемы разных версий приложения", prestableVersion,
                not(is(stableVersion)));
        initIgnoredFields();
    }

    @Test
    public void testGraphQLSchema() throws IOException {
        String message = "схемы на стендах совпадают.";
        JsonObject stableSchema = stable.getSchema();
        JsonObject prestableSchema = prestable.getSchema();

        JsonNode stableSchemaTree = OBJECT_MAPPER.readTree(stableSchema.toString());
        JsonNode prestableSchemaTree = OBJECT_MAPPER.readTree(prestableSchema.toString());

        assumeThat("удалось прочитать схему со стенда " + stableProperties.getDirectHost(), stableSchema,
                notNullValue());
        assumeThat("удалось прочитать схему со стенда " + prestableProperties.getDirectHost(), prestableSchema,
                notNullValue());

        BeanDifferMatcher matcher = beanDiffer(stableSchemaTree);
        if (B2B_PROPERTIES.isIgnoreNewFields()) {
            matcher = matcher.useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()
                    .forFields(ignoredFields.toArray(new BeanFieldPath[]{})).useMatcher(anything()));
            message += " Новые поля не учитываются.";
        } else {
            matcher = matcher.useCompareStrategy(
                    DefaultCompareStrategies.allFieldsExcept(ignoredFields.toArray(new BeanFieldPath[]{})));
        }

        if (B2B_PROPERTIES.getFieldsToIgnore() != null && !B2B_PROPERTIES.getFieldsToIgnore().isEmpty()) {
            message += " Не учитываются поля: " + B2B_PROPERTIES.getFieldsToIgnore();
        }
        assertThat(message, prestableSchemaTree, matcher);

    }

    private static void initIgnoredFields() {
        String ignoreList = B2B_PROPERTIES.getFieldsToIgnore();
        if (ignoreList != null && !ignoreList.isEmpty()) {
            for (String ignoreItem : ignoreList.replaceAll(" ", "").split(",")) {
                ignoredFields.add(BeanFieldPath.newPath(ignoreItem.split("/")));
            }
        }
    }
}
