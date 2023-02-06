package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Optional;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdAgeFlags;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateAdAgeFlagsTest {
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_ADS_MUTATION_NAME = "updateAdAgeFlags";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private long bannerId;
    private User operator;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        AdGroupInfo defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo defaultBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        bannerId = defaultBanner.getBannerId();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void updateFlags_invalidRights() {
        GdUpdateAdAgeFlags gdUpdateAdAgeFlags = new GdUpdateAdAgeFlags()
                .withAdIds(List.of(bannerId))
                .withAdAgeValue(Optional.empty());

        String query = createQuery(gdUpdateAdAgeFlags);
        List<GraphQLError> errorList = processQueryAndGetResult(query);
        GdValidationResult error = ((GridValidationException) ((ExceptionWhileDataFetching)
                errorList.get(0)).getException()).getValidationResult();
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateAdAgeFlags.AD_AGE_VALUE.name())),
                BannerDefects.insufficientRights())
                .withWarnings(emptyList());

        assertThat(error).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private List<GraphQLError> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        return result.getErrors();
    }

    private String createQuery(GdUpdateAdAgeFlags gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, UPDATE_ADS_MUTATION_NAME, graphQlSerialize(gdUpdateAds));
    }

}
