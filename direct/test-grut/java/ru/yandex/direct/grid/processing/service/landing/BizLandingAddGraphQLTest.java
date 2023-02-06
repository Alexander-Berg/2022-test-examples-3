package ru.yandex.direct.grid.processing.service.landing;

import java.util.List;
import java.util.Map;

import maps_adv.geosmb.landlord.proto.common.Common;
import maps_adv.geosmb.landlord.proto.contacts.ContactsOuterClass;
import maps_adv.geosmb.landlord.proto.generate.Generate;
import maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bvm.client.BvmClient;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.landing.GdAddBizLanding;
import ru.yandex.direct.grid.processing.model.landing.GdAddBizLandingPayload;
import ru.yandex.direct.grid.processing.model.landing.GdBizContact;
import ru.yandex.direct.grid.processing.model.landing.GdBizContactType;
import ru.yandex.direct.grid.processing.model.landing.GdBizCta;
import ru.yandex.direct.grid.processing.model.landing.GdBizCtaAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.landlord.client.LandlordClient;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.geosmb.bvm.model.FetchBizIdProtobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BizLandingAddGraphQLTest {

    private static final Long BIZ_ID = RandomNumberUtils.nextPositiveLong();
    private static final Long PERMALINK = RandomNumberUtils.nextPositiveLong();

    private static final String MUTATION_NAME = "addBizLanding";

    private static final String QUERY_TEMPLATE = "mutation {\n" +
            "  %s (input: %s) {\n" +
            "    id\n" +
            "    validationResult {\n" +
            "      errors {\n" +
            "        path\n" +
            "        code\n" +
            "        params\n" +
            "      }\n" +
            "      warnings {\n" +
            "        path\n" +
            "        code\n" +
            "        params\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private LandlordClient landlordClient;
    @Autowired
    private BvmClient bvmClient;
    @Autowired
    private OrganizationsClientStub organizationsClient;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        var clientInfo = steps.clientSteps().createDefaultClient();

        var uid = clientInfo.getUid();

        var user = userService.getUser(uid);
        TestAuthHelper.setDirectAuthentication(user);
        context = ContextHelper.buildContext(user, clientInfo.getChiefUserInfo().getUser());
    }

    @Test
    public void add_bizLanding() {
        var slug = "test-slug";

        organizationsClient.addUidsByPermalinkId(PERMALINK, List.of(context.getOperator().getUid()));

        doReturn(FetchBizIdProtobuf.FetchBizIdOutput.newBuilder().setBizId(BIZ_ID).build())
                .when(bvmClient).fetchBizId(Mockito.any());

        doReturn(Generate.GenerateDataOutput.newBuilder().setSlug(slug).build())
                .when(landlordClient).generateLandingData(Mockito.anyLong());

        var editLandingDetails = Mockito.mock(LandingDetailsOuterClass.EditLandingDetailsOutput.class);
        Mockito.when(editLandingDetails.getSlug()).thenReturn(slug);
        doReturn(editLandingDetails).when(landlordClient).editLandingDetails(Mockito.any());

        var showLandingDetails = Mockito.mock(LandingDetailsOuterClass.ShowLandingDetailsOutput.class);
        Mockito.when(showLandingDetails.getLandingDetails()).thenReturn(
                LandingDetailsOuterClass.LandingDetails.newBuilder()
                        .setName("name")
                        .setCover(Common.ImageTemplate.newBuilder().setTemplateUrl("url").build())
                        .setPreferences(LandingDetailsOuterClass.Preferences
                                .newBuilder()
                                .setColorTheme(
                                        LandingDetailsOuterClass.ColorTheme
                                                .newBuilder()
                                                .setTheme(LandingDetailsOuterClass.ColorTheme.ColorTone.DARK)
                                                .setPreset("yellow")
                                                .build()
                                ).build()
                        ).setContacts(ContactsOuterClass.Contacts.newBuilder().build())
                        .setBlocksOptions(
                                LandingDetailsOuterClass.BlocksOptions
                                        .newBuilder()
                                        .setShowCover(true)
                                        .setShowLogo(true)
                                        .setShowSchedule(true)
                                        .setShowPhotos(true)
                                        .setShowMapAndAddress(true)
                                        .setShowServices(true)
                                        .setShowReviews(true)
                                        .setShowExtras(true)
                                        .build()
                        ).build()
        );
        doReturn(showLandingDetails).when(landlordClient).showLandingDetails(Mockito.anyLong());

        var input = new GdAddBizLanding()
                .withPermalink(PERMALINK)
                .withName("name")
                .withDescription("descr")
                .withCategories(List.of("Категория"))
                .withContacts(List.of(new GdBizContact().withType(GdBizContactType.EMAIL).withValue("smth@ya.ru")))
                .withCta(new GdBizCta().withAction(GdBizCtaAction.CALL).withValue("call cta"));

        var query = String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(input));
        var result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        var actual = convertValue(data.get(MUTATION_NAME), GdAddBizLandingPayload.class);

        var addedId = actual.getId();
        assertThat(addedId).isNotNull();
    }
}
