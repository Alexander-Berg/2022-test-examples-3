package ru.yandex.direct.web.entity.grid.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.processing.exception.GdExceptions;
import ru.yandex.direct.grid.processing.model.api.GdApiResponse;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContext;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSuspendKeywords;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSuspendKeywordsPayload;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.grid.model.GridResponse;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignsGraphQlService.CAMPAIGNS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.client.ClientGraphQlService.CLIENT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.showcondition.KeywordsGraphQlService.SUSPEND_KEYWORDS_MUTATION;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.libs.graphql.GraphqlHelper.REQ_ID_KEY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.grid.service.GridErrorProcessingService.VALIDATION_RESULT_FIELD;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GridResponseTest {

    private static final Long NOT_EXIST_KEYWORD_ID = Long.MAX_VALUE;
    private static final Long INVALID_KEYWORD_ID = -1L;
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: {keywordIds: [%s]}) {\n"
            + "    suspendedKeywordIds,\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "         ... defect\n"
            + "      }\n"
            + "      warnings {\n"
            + "         ... defect\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}"
            + "fragment defect on GdDefect {\n"
            + "  code,\n"
            + "  path,\n"
            + "  params\n"
            + "}";
    private static final String MESSAGE_FIELD = "message";
    private static final String LOCATIONS_FIELD = "locations";
    private static final String EXTENSIONS_FIELD = "extensions";

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  %s(searchBy: {login: \"%s\"}) {\n"
            + "    %s(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdTextCampaign {\n"
            + "           contentLanguage\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private UserInfo userInfo;

    @Autowired
    private GridService gridService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private DirectWebAuthenticationSource directWebAuthenticationSource;

    @Autowired
    private Steps steps;

    @Before
    public void initTestData() {
        userInfo = testAuthHelper.createDefaultUser();
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.getAuthentication());
    }


    @Test
    public void checkSuccessResponse() {
        KeywordInfo keyword = steps.keywordSteps().createKeyword(new AdGroupInfo()
                .withClientInfo(userInfo.getClientInfo())
        );
        String query = String.format(MUTATION_TEMPLATE, SUSPEND_KEYWORDS_MUTATION, keyword.getId());

        GridResponse gridResponse = gridService.executeGraphQL(null, query, null);
        assertThat(gridResponse.getSuccess())
                .isTrue();

        Map<String, Object> expectedData = getExpectedData(singletonList(keyword.getId()), null);
        assertThat(gridResponse.getData())
                .is(matchedBy(beanDiffer(expectedData)));
    }

    @Test
    public void checkResponse_WithPreValidationResult() {
        String query = String.format(MUTATION_TEMPLATE, SUSPEND_KEYWORDS_MUTATION, INVALID_KEYWORD_ID);

        GridResponse gridResponse = gridService.executeGraphQL(null, query, null);
        assertThat(gridResponse.getSuccess())
                .isFalse();

        Path expectedPath = path(field(GdSuspendKeywords.KEYWORD_IDS), index(0));
        GdValidationResult gridValidationResult = GridValidationHelper.toGdValidationResult(expectedPath, validId());
        Map<String, Map<String, GdValidationResult>> validationResultToResponse =
                ImmutableMap.<String, Map<String, GdValidationResult>>builder()
                        .put(SUSPEND_KEYWORDS_MUTATION, ImmutableMap.of(VALIDATION_RESULT_FIELD, gridValidationResult))
                        .build();
        GridResponse expectedResponse = new GridResponse()
                .withErrors(singletonList(validationResultToResponse));
        assertThat(gridResponse)
                .is(matchedBy(beanDiffer(expectedResponse)));
    }

    @Test
    public void checkResponse_WithCoreValidationResult() {
        String query = String.format(MUTATION_TEMPLATE, SUSPEND_KEYWORDS_MUTATION, NOT_EXIST_KEYWORD_ID);

        GridResponse gridResponse = gridService.executeGraphQL(null, query, null);
        assertThat(gridResponse.getSuccess())
                .isTrue();

        Path expectedPath = path(field(GdSuspendKeywords.KEYWORD_IDS), index(0));
        GdValidationResult gdValidationResult =
                GridValidationHelper.toGdValidationResult(expectedPath, objectNotFound());
        Map<String, Object> expectedData = getExpectedData(emptyList(), gdValidationResult);
        assertThat(gridResponse.getData())
                .is(matchedBy(beanDiffer(expectedData)));
    }

    @Test
    public void checkResponse_WhenGotExceptionWhileDataFetching() {
        //Создаем нового юзера, у которого нет доступа на запись
        UserInfo operator = steps.userSteps().createDefaultUser();
        testAuthHelper.setOperator(operator.getUid());

        String query = String.format(MUTATION_TEMPLATE, SUSPEND_KEYWORDS_MUTATION, NOT_EXIST_KEYWORD_ID);

        GridResponse gridResponse = gridService.executeGraphQL(null, query, null);
        assertThat(gridResponse.getSuccess())
                .isFalse();

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(".*", EXTENSIONS_FIELD)).useMatcher(notNullValue())
                .forFields(newPath(".*", MESSAGE_FIELD)).useMatcher(startsWith("No rights"))
                .forFields(newPath(".*", LOCATIONS_FIELD)).useMatcher(notNullValue())
                .forFields(newPath(".*", REQ_ID_KEY)).useMatcher(notNullValue());
        Map<String, Object> expectedErrorData =
                ImmutableMap.of("path", singletonList(SUSPEND_KEYWORDS_MUTATION),
                        "code", GdExceptions.NO_RIGHTS.getCode());
        assertThat(gridResponse.getErrors())
                .is(matchedBy(beanDiffer(singletonList(expectedErrorData)).useCompareStrategy(strategy)));
    }

    @Test
    public void checkResponse_WhenGotGraphQLError() {
        String invalidQuery = "bla_bla";

        GridResponse gridResponse = gridService.executeGraphQL(null, invalidQuery, null);
        assertThat(gridResponse.getSuccess())
                .isFalse();

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(".*", EXTENSIONS_FIELD)).useMatcher(notNullValue())
                .forFields(newPath(".*", MESSAGE_FIELD)).useMatcher(startsWith("Invalid Syntax"))
                .forFields(newPath(".*", LOCATIONS_FIELD)).useMatcher(notNullValue())
                .forFields(newPath(".*", REQ_ID_KEY)).useMatcher(notNullValue());
        assertThat(gridResponse.getErrors())
                .is(matchedBy(beanDiffer(singletonList(emptyMap())).useCompareStrategy(strategy)));
    }

    @Test
    public void checkResponse_whenSkipGridNotPublicExceptionFromErrors() {
        userInfo = testAuthHelper.createDefaultUser();

        CampaignInfo campaign = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        GdCampaignsContainer campaignsInput = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsInput.getFilter()
                .setCampaignIdIn(Set.of(campaign.getCampaignId()));
        String query = String.format(QUERY_TEMPLATE, CLIENT_RESOLVER_NAME, userInfo.getUser().getLogin(),
                CAMPAIGNS_RESOLVER_NAME, graphQlSerialize(campaignsInput));

        GridResponse gridResponse = gridService.executeGraphQL(null, query, null);
        assertThat(gridResponse.getSuccess())
                .isTrue();

        Map<String, Object> actualData = getDataValue(gridResponse.getData(), CLIENT_RESOLVER_NAME);
        Map<String, Object> expectedData = getCampaignExpectedData(campaign.getCampaignId());
        assertThat(actualData)
                .is(matchedBy(beanDiffer(expectedData)));
    }

    private static Map<String, Object> getCampaignExpectedData(Long campaignId) {
        Map<String, Object> campaignData = new HashMap<>();
        campaignData.put(GdCampaign.ID.name(), campaignId);
        campaignData.put(GdTextCampaign.CONTENT_LANGUAGE.name(), null);
        return Map.of(CAMPAIGNS_RESOLVER_NAME, Map.of(GdCampaignsContext.ROWSET.name(), List.of(campaignData)));
    }

    private static Map<String, Object> getExpectedData(List<Long> keywordIds,
                                                       @Nullable GdValidationResult validationResult) {
        Map<String, Object> data = new HashMap<>();
        data.put(GdSuspendKeywordsPayload.SUSPENDED_KEYWORD_IDS.name(), keywordIds);
        data.put(GdApiResponse.VALIDATION_RESULT.name(),
                ifNotNull(validationResult, vr -> JsonUtils.getObjectMapper().convertValue(vr, Map.class)));

        return ImmutableMap.<String, Object>builder()
                .put(SUSPEND_KEYWORDS_MUTATION, data)
                .put(REQ_ID_KEY, String.valueOf(Trace.current().getSpanId()))
                .build();
    }

}
