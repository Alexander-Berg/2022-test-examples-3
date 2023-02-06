package ru.yandex.direct.grid.processing.service.freelancer;

import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.PUBLIC_GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildDefaultContext;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerPublicGraphQlServiceTest {

    private static final String GET_FREELANCERS = "{\n"
            + "  freelancersList {\n"
            + "    card {\n"
            + "      avatarUrl\n"
            + "      briefInfo\n"
            + "      contacts {\n"
            + "        email\n"
            + "      }\n"
            + "    }\n"
            + "    certificates {\n"
            + "      type\n"
            + "    }\n"
//todo сделать тестовый стаб для ugcdb
//            + "    feedbacks {\n"
//            + "      authorInfo {\n"
//            + "        login\n"
//            + "      }\n"
//            + "      comments {\n"
//            + "        commentId\n"
//            + "      }\n"
//            + "    }\n"
            + "    mainSkill {\n"
            + "      name\n"
            + "    }\n"
            + "    login\n"
            + "    freelancerId\n"
            + "    skills {\n"
            + "      name\n"
            + "    }\n"
            + "    status\n"
            + "    workCurrency\n"
            + "    region(lang:RU) {\n"
            + "      regionId\n"
            + "      translation {\n"
            + "        regionId\n"
            + "        countryRegionId\n"
            + "        region\n"
            + "        country\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";


    @Autowired
    @Qualifier(PUBLIC_GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;

    @Before
    public void setUp() {
        steps.freelancerSteps().addDefaultFreelancer();
    }

    @Test
    public void getFreelancers_success_onAbsentClientId() {
        ExecutionResult result = processor.processQuery(null, "{\n"
                + "  freelancersList(filter:{freelancerIds:[1]}) {\n"
                + "    freelancerId\n"
                + "  }\n"
                + "}", null, buildDefaultContext());
        Map<String, Object> map = result.getData();
        SoftAssertions assertions = new SoftAssertions();

        GraphQLUtils.logErrors(result.getErrors());
        assertions.assertThat(map).containsKey("freelancersList");
        assertions.assertThat(result.getErrors()).hasSize(0);
        assertions.assertAll();
    }

    @Test
    public void getFreelancers_success_onNull() {
        ExecutionResult result = processor.processQuery(null, "{\n"
                + "  freelancersList(filter:null) {\n"
                + "    freelancerId\n"
                + "  }\n"
                + "}", null, buildDefaultContext());
        Map<String, Object> map = result.getData();
        SoftAssertions assertions = new SoftAssertions();

        GraphQLUtils.logErrors(result.getErrors());
        assertions.assertThat(map).containsKey("freelancersList");
        assertions.assertThat(result.getErrors()).hasSize(0);
        assertions.assertAll();
    }

    @Test
    public void getFreelancers_success_all() {
        ExecutionResult result = processor.processQuery(null, GET_FREELANCERS, null, buildDefaultContext());
        Map<String, Object> map = result.getData();
        SoftAssertions assertions = new SoftAssertions();

        GraphQLUtils.logErrors(result.getErrors());
        assertions.assertThat(map).containsKey("freelancersList");
        assertions.assertThat(result.getErrors()).hasSize(0);
        assertions.assertAll();
    }
}
