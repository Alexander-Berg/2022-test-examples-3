package ru.yandex.direct.grid.processing.service.pricepackage;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdDeletePricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdDeletePricePackagesPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceDeletePricePackagesTest {

    private static final String MUTATION_HANDLE = "deletePricePackages";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    deletedPackageIds\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private PricePackageRepository repository;

    @Autowired
    private Steps steps;

    private User operator;
    private PricePackage pricePackage;
    private PricePackage anotherPricePackage;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        createPricePackages();
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    private void createPricePackages() {
        steps.pricePackageSteps().clearPricePackages();
        pricePackage = steps.pricePackageSteps().createNewPricePackage().getPricePackage();
        anotherPricePackage = steps.pricePackageSteps().createPricePackage(
                new PricePackageInfo().withPricePackage(anotherPricePackage())).getPricePackage();
    }

    @Test
    public void deletePricePackages_OneValidAndOneInvalidPackages() {
        List<Long> validIds = List.of(pricePackage.getId(), anotherPricePackage.getId());
        List<Long> mixedIds = List.of(pricePackage.getId() + 100, anotherPricePackage.getId());

        GdDeletePricePackagesPayload payload = deletePricePackagesGraphQl(mixedIds);

        Map<Long, PricePackage> actual = repository.getPricePackages(validIds);
        assertThat(actual).containsOnlyKeys(pricePackage.getId());
        GdDeletePricePackagesPayload expectedPayload = new GdDeletePricePackagesPayload()
                .withValidationResult(toGdValidationResult(toGdDefect(
                        path(field(GdDeletePricePackages.PACKAGE_IDS), index(0)), objectNotFound())))
                .withDeletedPackageIds(asList(null, anotherPricePackage.getId()));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdDeletePricePackagesPayload deletePricePackagesGraphQl(List<Long> ids) {
        GdDeletePricePackages request = new GdDeletePricePackages().withPackageIds(ids);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdDeletePricePackagesPayload.class);
    }

}
