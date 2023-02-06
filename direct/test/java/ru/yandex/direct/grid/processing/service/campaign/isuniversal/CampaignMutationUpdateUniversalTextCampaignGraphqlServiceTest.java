package ru.yandex.direct.grid.processing.service.campaign.isuniversal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddTextCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdUpdateTextCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(Parameterized.class)
public class CampaignMutationUpdateUniversalTextCampaignGraphqlServiceTest {
    private static final String ADD_MUTATION_NAME = "addCampaigns";
    private static final String UPDATE_MUTATION_NAME = "updateCampaigns";
    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_MUTATION_NAME, UPDATE_MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    @Parameterized.Parameter(0)
    public Boolean before;

    @Parameterized.Parameter(1)
    public Boolean after;

    @Parameterized.Parameter(2)
    public Boolean expected;

    @Parameterized.Parameters(name = "before = {0}, new = {1}, expected = {2}")
    public static Collection<Object[]> parameters() {
        //DIRECT-130150 значение при обновлении не меняется
//        List<Boolean> beforePossibleValues = List.of(true, false);
//        List<Pair<Boolean, Boolean>> afterAndResultPossibleValues = List.of(Pair.of(null, false),
//                Pair.of(null, false), Pair.of(true, true));
//        return cartesianProduct(beforePossibleValues, afterAndResultPossibleValues)
//                .stream()
//                .map(t -> Arrays.asList(t.get(0), ((Pair) t.get(1)).getLeft(), ((Pair) t.get(1)).getRight()))
//                .map(List::toArray)
//                .collect(Collectors.toList());
        return cartesianProduct(Arrays.asList(Optional.empty(), Optional.of(true), Optional.of(false)) /*значения, на
         которые обновляют*/,
                Arrays.asList(true, false) /*значения до обновления*/)
                .stream()
                .map(t -> Arrays.asList(t.get(1), ((Optional<Boolean>) t.get(0)).orElse(null), t.get(1)))
                .map(List::toArray)
                .collect(Collectors.toList());
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testCombinations() {
        defaultUniversalityUpdateTest(before, after, expected);
    }


    private void defaultUniversalityUpdateTest(Boolean before, Boolean newValue, Boolean expectedReturnValue) {
        CampaignAttributionModel defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
        GdAddTextCampaign gdAddTextCampaign =
                defaultGdAddTextCampaign(defaultAttributionModel).withIsUniversalCamp(before);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withTextCampaign(gdAddTextCampaign);
        GdAddCampaigns addInput = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                addInput, operator);
        assumeThat(gdAddCampaignPayload.getValidationResult(), Matchers.nullValue());
        TextCampaign addedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);

        GdUpdateTextCampaign gdUpdateTextCampaign = defaultGdUpdateTextCampaign(addedCampaign, defaultAttributionModel);
        gdUpdateTextCampaign.withIsUniversalCamp(newValue);
        GdUpdateCampaignUnion campaignUnion = new GdUpdateCampaignUnion().withTextCampaign(gdUpdateTextCampaign);
        GdUpdateCampaigns updateInput = new GdUpdateCampaigns().withCampaignUpdateItems(singletonList(campaignUnion));

        GdUpdateCampaignPayload gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                updateInput, operator);
        List<GdUpdateCampaignPayloadItem> expectedUpdatedCampaigns = singletonList(
                new GdUpdateCampaignPayloadItem().withId(addedCampaign.getId()));
        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns(),
                beanDiffer(expectedUpdatedCampaigns).useCompareStrategy(onlyExpectedFields()));
        assertThat(gdUpdateCampaignPayload.getValidationResult(), Matchers.nullValue());

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdUpdateCampaignPayload.getUpdatedCampaigns().get(0).getId())).get(0);
        Matcher isUniversalFieldMatcher = expectedReturnValue == null ? Matchers.nullValue() :
                Matchers.is(expectedReturnValue);
        assertThat(actualCampaign.getIsUniversal(), isUniversalFieldMatcher);
    }
}
