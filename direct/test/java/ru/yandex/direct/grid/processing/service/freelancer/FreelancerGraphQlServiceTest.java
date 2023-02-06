package ru.yandex.direct.grid.processing.service.freelancer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.geobasehelper.GeoBaseHelperStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.constants.GdLanguage;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus.CANCELLEDBYFREELANCER;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus.NEW;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildDefaultContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


/**
 * Тест на сервис, проверяем в основном то, что базовая функциональность работает.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerGraphQlServiceTest {

    private static final String GET_PROJECTS_TEMPLATE = "{\n"
            + "  client(searchBy:{id: %s}) {    \n"
            + "    freelancerProjects(filter: {isActive:%s})\n"
            + "    {\n"
            + "      id\n"
            + "      status\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String GET_PHONE_QUERY = "{freelancer{projects{id,client{phoneNumber}}}}";
    private static final String GET_FREELANCER_ID_QUERY = "{freelancer{freelancerId}}";
    private static final String GET_REGION_QUERY =
            "{freelancersList(filter:{freelancerIds:[%s]}){freelancerId,region(lang:%s){regionId,translation{regionId,countryRegionId,region,country}}}}";
    private static final String GET_ADV_QUALITY_QUERY =
            "{freelancersList(filter:{freelancerIds:[%s]}){privateData{advQuality{freelancerId,advQualityRating,advQualityRank}}}}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FreelancerRepository freelancerRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private GeoBaseHelper geoBaseHelper;

    private ClientInfo clientInfo;
    private FreelancerInfo freelancerInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        TestAuthHelper.setDirectAuthentication(userService.getUser(clientInfo.getUid()));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    private GridGraphQLContext createContext(Long uid) {
        if (uid == null) {
            return buildDefaultContext();
        }
        User user = userService.getUser(uid);
        return buildContext(user);
    }

    private FreelancerProjectInfo createProject(ClientInfo clientInfo,
                                                FreelancerInfo freelancerInfo,
                                                FreelancerProjectStatus projectStatus) {
        LocalDateTime now = LocalDateTime.now();
        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong())
                .withStatus(projectStatus)
                .withUpdatedTime(now)
                .withStartedTime(now)
                .withCreatedTime(now);
        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withProject(project)
                .withFreelancerInfo(freelancerInfo)
                .withClientInfo(clientInfo);
        steps.freelancerSteps().createProject(projectInfo);
        return projectInfo;
    }

    @Test
    public void getFreelancers_success_onAbsentClientId() {
        ExecutionResult result = processor.processQuery(null, "{\n"
                + "  freelancersList(filter:{freelancerIds:[1]}) {\n"
                + "    freelancerId\n"
                + "  }\n"
                + "}", null, createContext(null));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void getFreelancers_success_onNull() {
        ExecutionResult result = processor.processQuery(null, "{\n"
                + "  freelancersList(filter:null) {\n"
                + "    freelancerId\n"
                + "  }\n"
                + "}", null, createContext(null));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void getFreelancers_success_nullCard() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        long clientId = clientInfo.getClientId().asLong();
        Freelancer freelancer = defaultFreelancer(clientId);
        freelancer.withCard(null);
        FreelancerInfo newFreelancerInfo = new FreelancerInfo().withClientInfo(clientInfo).withFreelancer(freelancer);
        FreelancerInfo freelancerWithNullCardInfo = steps.freelancerSteps().createFreelancer(newFreelancerInfo);
        Long freelancerId = freelancerWithNullCardInfo.getFreelancerId();

        String getFreelancersList = "{\n"
                + "  freelancersList(filter:{freelancerIds:[%s]}) {\n"
                + "    freelancerId\n"
                + "    card {\n"
                + "        statusModerate\n"
                + "     }\n"
                + "  }\n"
                + "}";
        String query = String.format(getFreelancersList, freelancerId);

        ExecutionResult result = processor.processQuery(null, query, null, createContext(null));
        Map<String, Object> data = result.getData();

        Map<String, Object> freelancersList = new HashMap<>();
        freelancersList.put("freelancerId", freelancerId);
        freelancersList.put("card", null);
        Map<String, Object> expected = singletonMap(
                "freelancersList",
                ImmutableList.of(
                        freelancersList
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getClientFreelancerProjects_withFilterTrue_success() {
        FreelancerProjectInfo project = createProject(clientInfo, freelancerInfo, NEW);
        String query = String.format(GET_PROJECTS_TEMPLATE, clientInfo.getClientId(), "true");
        GridGraphQLContext context = createContext(clientInfo.getUid());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkState(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = map(
                "client", map(
                        "freelancerProjects", list(
                                map(
                                        "id", project.getProjectId(),
                                        "status", "NEW"
                                )
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getClientFreelancerProjects_withFilterFalse_success() {
        FreelancerProjectInfo project = createProject(clientInfo, freelancerInfo, CANCELLEDBYFREELANCER);
        String query = String.format(GET_PROJECTS_TEMPLATE, clientInfo.getClientId(), "false");
        GridGraphQLContext context = createContext(clientInfo.getUid());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkState(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = map(
                "client", map(
                        "freelancerProjects", list(
                                map(
                                        "id", project.getProjectId(),
                                        "status", "CANCELLED_BY_FREELANCER"
                                )
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getClientFreelancerProjects_withNullResult_success() {
        createProject(clientInfo, freelancerInfo, CANCELLEDBYFREELANCER);
        String query = String.format(GET_PROJECTS_TEMPLATE, clientInfo.getClientId(), "true");
        GridGraphQLContext context = createContext(clientInfo.getUid());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkState(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = map(
                "client", map(
                        "freelancerProjects", list()
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getClientFreelancer_success() {
        String phoneNumber = "+7 495 739-70-00";
        Long chiefUid = clientInfo.getClient().getChiefUid();
        Integer shard = clientInfo.getShard();
        setPhoneToUser(shard, chiefUid, phoneNumber);
        Long freelancerUid = freelancerInfo.getClientInfo().getUid();
        FreelancerProjectInfo project = createProject(clientInfo, freelancerInfo, FreelancerProjectStatus.INPROGRESS);
        Long projectId = project.getProjectId();
        GridGraphQLContext context = createContext(freelancerUid);
        ExecutionResult result = processor.processQuery(null, GET_PHONE_QUERY, null, context);
        checkState(result.getErrors().isEmpty());
        Map<String, Object> expected = map(
                "freelancer", map(
                        "projects", list(
                                map("id", projectId,
                                        "client", map(
                                                "phoneNumber", phoneNumber
                                        )
                                )
                        )
                )
        );
        Map<String, Object> data = result.getData();
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private void setPhoneToUser(int shard, Long uid, String phoneNumber) {
        Collection<User> users = userRepository.fetchByUids(shard, singletonList(uid));
        User user = StreamEx.of(users).findAny().orElseThrow(() -> new RuntimeException("User not found."));
        ModelChanges<User> modelChanges = new ModelChanges<>(uid, User.class);
        modelChanges.process(phoneNumber, User.PHONE);
        AppliedChanges<User> appliedChanges = modelChanges.applyTo(user);
        userRepository.update(shard, singleton(appliedChanges));
    }

    @Test
    public void freelancer_returnsNull_whenOperatorIsNotFreelancer() {
        Long clientChiefUid = clientInfo.getClient().getChiefUid();
        GridGraphQLContext context = createContext(clientChiefUid);
        ExecutionResult result = processor.processQuery(null, GET_FREELANCER_ID_QUERY, null, context);

        Map<String, Object> data = result.getData();

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors())
                    .describedAs("Response errors")
                    .isEmpty();
            softly.assertThat(data)
                    .describedAs("Response data")
                    .containsEntry("freelancer", null);
        });
    }

    @Test
    public void getFreelancerRegion_regionNotNull_success() {
        //подготавливаем данные
        Long freelancerId = freelancerInfo.getFreelancerId();
        Long regionId = freelancerInfo.getFreelancer().getRegionId();
        checkState(regionId != null);
        String trLangName = GdLanguage.TR.name();
        long russiaRegionId = 225L;
        @SuppressWarnings("SpellCheckingInspection")
        String moscowName = "Moskova";
        @SuppressWarnings("SpellCheckingInspection")
        String russiaName = "Rusya";
        GeoBaseHelperStub geoBaseHelperStub = (GeoBaseHelperStub) this.geoBaseHelper;
        geoBaseHelperStub.addRegionWithName(regionId, trLangName, moscowName);
        geoBaseHelperStub.addRegionWithName(russiaRegionId, trLangName, russiaName);

        //ожидаемый результат
        Map<String, Object> expected = map(
                "freelancersList", list(
                        map(
                                "freelancerId", freelancerId,
                                "region", map(
                                        "regionId", regionId,
                                        "translation", map(
                                                "regionId", regionId,
                                                "countryRegionId", russiaRegionId,
                                                "region", moscowName,
                                                "country", russiaName
                                        )
                                )
                        )
                )
        );

        //выполняем запрос
        Long freelancerUid = freelancerInfo.getClientInfo().getUid();
        String query = String.format(GET_REGION_QUERY, freelancerId, trLangName);
        GridGraphQLContext context = createContext(freelancerUid);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        List<GraphQLError> errors = result.getErrors();
        Map<String, Object> data = result.getData();

        //сверяем ожидания и реальность
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors).isEmpty();
            soft.assertThat(data).is(matchedBy(beanDiffer(expected)));
        });
    }


    @Test
    public void getFreelancerRegion_regionIsNull_success() {
        //подготавливаем данные
        Long freelancerId = freelancerInfo.getFreelancerId();
        Freelancer freelancer = freelancerInfo.getFreelancer();
        AppliedChanges<FreelancerBase> freelancerAppliedChanges =
                new ModelChanges<>(freelancer.getId(), FreelancerBase.class)
                        .process(null, FreelancerBase.REGION_ID)
                        .applyTo(freelancer);
        freelancerRepository.updateFreelancer(freelancerInfo.getShard(), singletonList(freelancerAppliedChanges));

        //ожидаемый результат
        Map<String, Object> expected = map(
                "freelancersList", list(
                        map(
                                "freelancerId", freelancerId,
                                "region", null
                        )
                )
        );

        //выполняем запрос
        Long freelancerUid = freelancerInfo.getClientInfo().getUid();
        String query = String.format(GET_REGION_QUERY, freelancerId, GdLanguage.TR.name());
        GridGraphQLContext context = createContext(freelancerUid);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        List<GraphQLError> errors = result.getErrors();
        Map<String, Object> data = result.getData();

        GraphQLUtils.logErrors(result.getErrors());
        //сверяем ожидания и реальность
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(errors).isEmpty();
            soft.assertThat(data).is(matchedBy(beanDiffer(expected)));
        });
    }

    @Test
    public void getFreelancerAdvQuality_success() {
        //подготавливаем данные
        BigDecimal advQualityRating = BigDecimal.valueOf(500L, 2);
        Long advQualityRank = 1L;
        Freelancer freelancer = freelancerInfo.getFreelancer();
        Long freelancerId = freelancer.getFreelancerId();
        ModelChanges<FreelancerBase> modelChanges = new ModelChanges<>(freelancerId, FreelancerBase.class);
        modelChanges.processNotNull(advQualityRating, FreelancerBase.ADV_QUALITY_RATING);
        modelChanges.processNotNull(advQualityRank, FreelancerBase.ADV_QUALITY_RANK);
        AppliedChanges<FreelancerBase> freelancerAppliedChanges = modelChanges.applyTo(freelancer);
        Integer shard = freelancerInfo.getShard();
        freelancerRepository.updateFreelancer(shard, singletonList(freelancerAppliedChanges));

        //ожидаемый результат
        Map<String, Object> expected = map(
                "freelancersList", list(
                        map(
                                "privateData", map(
                                        "advQuality", map(
                                                "freelancerId", freelancerId,
                                                "advQualityRating", advQualityRating,
                                                "advQualityRank", advQualityRank
                                        )
                                )
                        )
                )
        );

        //выполняем запрос
        Long freelancerUid = freelancerInfo.getClientInfo().getUid();
        String query = String.format(GET_ADV_QUALITY_QUERY, freelancerId);
        GridGraphQLContext context = createContext(freelancerUid);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkState(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();

        //сверяем ожидания и реальность
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

}
