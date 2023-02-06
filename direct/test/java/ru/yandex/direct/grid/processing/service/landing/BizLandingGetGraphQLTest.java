package ru.yandex.direct.grid.processing.service.landing;

import java.util.Map;

import maps_adv.geosmb.landlord.proto.common.Common;
import maps_adv.geosmb.landlord.proto.contacts.ContactsOuterClass;
import maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass;
import maps_adv.geosmb.landlord.proto.preferences.PreferencesOuterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.landing.GdBizContactType;
import ru.yandex.direct.grid.processing.model.landing.GdBizCtaAction;
import ru.yandex.direct.grid.processing.model.landing.GdGetBizLanding;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.landlord.client.LandlordClient;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BizLandingGetGraphQLTest {

    private static final Long BIZ_ID = RandomNumberUtils.nextPositiveLong();
    private static final Long PERMALINK = RandomNumberUtils.nextPositiveLong();

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    bizLanding(input: %s) {\n" +
            "      id\n" +
            "      url\n" +
            "      permalink\n" +
            "      name\n" +
            "      description\n" +
            "      logoUrl\n" +
            "      categories\n" +
            "      contacts {\n" +
            "        type\n" +
            "        value\n" +
            "      }\n" +
            "      cta {\n" +
            "        action\n" +
            "        custom\n" +
            "        value\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private LandlordClient landlordClient;

    private GridGraphQLContext context;

    @Before
    public void setUp() {
        var clientInfo = steps.clientSteps().createDefaultClient();

        var uid = clientInfo.getUid();

        var user = userService.getUser(uid);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void test_bizLanding_by_id() {
        var name = "name";
        var description = "descr";
        var logoUrl = "https://avatars.mdst.yandex.net/get-direct/4699/Aa73ej0j/x450";
        var buttonText = "button text";
        var buttonValue = "button value";
        var email = "smth@ya.ru";

        var geo = ContactsOuterClass.Geo
                .newBuilder()
                .setPermalink(PERMALINK.toString())
                .setLon(Common.Decimal.newBuilder().setValue("1.23").build())
                .setLat(Common.Decimal.newBuilder().setValue("1.23").build())
                .setAddress("aaddress")
                .build();
        var ctaButton = PreferencesOuterClass.CTAButton
                .newBuilder()
                .setCustom(buttonText)
                .setValue(buttonValue)
                .build();
        var colorTheme = LandingDetailsOuterClass.ColorTheme
                .newBuilder()
                .setTheme(LandingDetailsOuterClass.ColorTheme.ColorTone.LIGHT)
                .setPreset("YELLOW")
                .build();
        var preferences = LandingDetailsOuterClass.Preferences
                .newBuilder()
                .setColorTheme(colorTheme)
                .setCtaButton(ctaButton)
                .build();
        var contacts = ContactsOuterClass.Contacts
                .newBuilder()
                .setEmail(email)
                .setGeo(geo)
                .build();
        var blocksOptions = LandingDetailsOuterClass.BlocksOptions.newBuilder()
                .setShowCover(true)
                .setShowLogo(true)
                .setShowSchedule(false)
                .setShowPhotos(false)
                .setShowMapAndAddress(false)
                .setShowServices(false)
                .setShowReviews(false)
                .setShowExtras(false)
                .build();
        doReturn(
                LandingDetailsOuterClass.ShowLandingDetailsOutput.newBuilder()
                        .setSlug("test-slug")
                        .setIsPublished(true)
                        .setLandingDetails(
                                LandingDetailsOuterClass.LandingDetails.newBuilder()
                                        .setName(name)
                                        .setDescription(description)
                                        .setLogo(Common.ImageTemplate.newBuilder().setTemplateUrl(logoUrl).build())
                                        .setPreferences(preferences)
                                        .setContacts(contacts)
                                        .setBlocksOptions(blocksOptions)
                                        .build()
                        )
                        .build()
        ).when(landlordClient).showLandingDetails(Mockito.anyLong());

        var input = new GdGetBizLanding()
                .withId(BIZ_ID);

        var query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        var result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = map(
                "client",
                map(
                        "bizLanding", map(
                                "id", BIZ_ID,
                                "url", "test-slug",
                                "permalink", PERMALINK,
                                "name", "name",
                                "description", "descr",
                                "logoUrl", logoUrl,
                                "categories", list(),
                                "contacts", list(
                                        map(
                                                "type", GdBizContactType.EMAIL.toString(),
                                                "value", "smth@ya.ru"
                                        )
                                ),
                                "cta", map(
                                        "action", GdBizCtaAction.LINK.name(),
                                        "custom", "button text",
                                        "value", "button value"
                                )
                        )
                )
        );
        assertThat(data).usingRecursiveComparison().isEqualTo(expected);
    }

}
