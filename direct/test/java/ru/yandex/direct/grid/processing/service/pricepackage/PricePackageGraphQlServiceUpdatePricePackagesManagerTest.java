package ru.yandex.direct.grid.processing.service.pricepackage;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import jdk.jfr.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.GdStatusApprove;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesItem;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayload;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdMutationTargetingsFixed;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceUpdatePricePackagesManagerTest {

    private static final String MUTATION_HANDLE = "updatePricePackages";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    updatedItems {\n"
            + "      id\n"
            + "    }\n"
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
    private Steps steps;

    private User priceApprover;

    @Before
    public void initTestData() {
        steps.sspPlatformsSteps().addSspPlatforms(defaultPricePackage().getAllowedSsp());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Description("Пользователь с галкой canApprovePricePackages может менять statusApprove")
    public void userCanChangeStatusApprove() {
        initUser(true);
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withStatusApprove(StatusApprove.WAITING))
                .getPricePackage();
        GdUpdatePricePackagesItem item = newApprovePricePackageRequest(pricePackage);
        ExecutionResult result = execute(List.of(item));

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);
        GdUpdatePricePackagesPayload payload = GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE),
                GdUpdatePricePackagesPayload.class);

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(singletonList((new GdUpdatePricePackagesPayloadItem()
                        .withId(pricePackage.getId()))));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Description("Пользователь без галки canApprovePricePackages не может менять statusApprove. " +
            "Поскольку нет галки - ручка недоступна.")
    public void userCanNotChangeStatusApprove() {
        initUser(false);
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withStatusApprove(StatusApprove.WAITING))
                .getPricePackage();
        GdUpdatePricePackagesItem item = newApprovePricePackageRequest(pricePackage);
        ExecutionResult result = execute(List.of(item));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("No rights for field");
    }

    private void initUser(Boolean canApprovePricePackages) {
        priceApprover = TestUsers.generateNewUser()
                .withRole(RbacRole.MANAGER)
                .withCanApprovePricePackages(canApprovePricePackages)
                .withCanManagePricePackages(false);
        steps.clientSteps().createDefaultClient(priceApprover);
        TestAuthHelper.setDirectAuthentication(priceApprover);
        steps.pricePackageSteps().clearPricePackages();
    }

    private ExecutionResult execute(List<GdUpdatePricePackagesItem> items) {
        GdUpdatePricePackages request = new GdUpdatePricePackages().withUpdateItems(items);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        return processor.processQuery(null, query, null,
                buildContext(priceApprover));
    }

    private static GdUpdatePricePackagesItem newApprovePricePackageRequest(PricePackage pricePackage) {
        return new GdUpdatePricePackagesItem()
                .withId(pricePackage.getId())
                .withStatusApprove(GdStatusApprove.YES)
                // делаем как фронт - не присылаем geoExpanded
                .withTargetingsFixed(toGdMutationTargetingsFixed(pricePackage.getTargetingsFixed(), identity())
                        .withGeoExpanded(null))
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
    }

}
