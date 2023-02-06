package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.core.testing.steps.TurboLandingSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.VcardSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.cliententity.GdCalloutStatusModerate;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativeType;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageFormat;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageSize;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageSmartCenter;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdAddress;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdInstantMessenger;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdPhone;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdPointOnMap;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdVcard;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdVcardsContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCallouts.defaultCallout;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.fullCreative;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.core.testing.steps.BannerSteps.DEFAULT_IMAGE_NAME_TEMPLATE;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientDataConverter.parseFlags;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientEntityConverter.toGdCreativeType;
import static ru.yandex.direct.grid.processing.service.client.converter.TestClientEntityConverter.toGdAdImageAvatarsHost;
import static ru.yandex.direct.grid.processing.service.client.converter.TestClientEntityConverter.toGdAdImageNamespace;
import static ru.yandex.direct.grid.processing.service.client.converter.TestClientEntityConverter.toGdAdImageType;
import static ru.yandex.direct.grid.processing.service.client.converter.TestClientEntityConverter.toGdImageFormats;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientEntityGraphQlServiceTest {
    private static final String QUERY_TEMPLATE = "{\n"
            + "  callouts (input:{searchBy: {login: \"%s\"}, filter:{deleted:false}}) {\n"
            + "    id\n"
            + "    clientId\n"
            + "    flags\n"
            + "    statusModerate\n"
            + "    text\n"
            + "  }\n"
            + "  turbolandings (input:{searchBy: {login: \"%1$s\"}, filter:{turbolandingIdIn:[%s]}}) {\n"
            + "    id\n"
            + "    clientId\n"
            + "    name\n"
            + "    href\n"
            + "  }\n"
            + "  images(input: {searchBy: {login: \"%1$s\"}, filter: {imageHashIn: [\"%s\"]}, limitOffset: {limit: 1, offset: 0}, cacheKey: \"\"}) {\n"
            + "    rowset {\n"
            + "      avatarsHost\n"
            + "      type\n"
            + "      imageHash\n"
            + "      mdsGroupId\n"
            + "      name\n"
            + "      namespace\n"
            + "      imageSize {\n"
            + "        height\n"
            + "        width\n"
            + "      }\n"
            + "      formats {\n"
            + "        path\n"
            + "        imageSize {\n"
            + "          height\n"
            + "          width\n"
            + "        }\n"
            + "        smartCenters {\n"
            + "          ratio\n"
            + "          height\n"
            + "          width\n"
            + "          x\n"
            + "          y\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "    imageHashes\n"
            + "    totalCount\n"
            + "  }\n"
            + "  creatives(input: {searchBy: {login: \"%1$s\"}, filter: {creativeIdIn: [%s], creativeTypeIn: [%s]}, limitOffset: {limit: 1, offset: 0}, cacheKey: \"\"}) {\n"
            + "    rowset {\n"
            + "      creativeId\n"
            + "      creativeType\n"
            + "      height\n"
            + "      name\n"
            + "      previewUrl\n"
            + "      width\n"
            + "      ... on GdVideoAdditionCreative {\n"
            + "        livePreviewUrl\n"
            + "        duration\n"
            + "      }\n"
            + "    }\n"
            + "    creativeIds\n"
            + "    totalCount\n"
            + "  }\n"
            + " vcards(input: {searchBy: {login: \"%1$s\"}, filter: {vcardIdIn: [%s]}}) {\n"
            + "    rowset {\n"
            + "      id\n"
            + "      campaignId\n"
            + "      companyName\n"
            + "      contactPerson\n"
            + "      extraMessage\n"
            + "      ogrn\n"
            + "      instantMessenger {\n"
            + "        type\n"
            + "        login\n"
            + "      }\n"
            + "      phone {\n"
            + "        countryCode\n"
            + "        cityCode\n"
            + "        phoneNumber\n"
            + "        extension\n"
            + "      }\n"
            + "      address {\n"
            + "        geoId\n"
            + "        country\n"
            + "        city\n"
            + "        pointOnMap {\n"
            + "          x\n"
            + "          y\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }"
            + "}\n";
    private static final String QUERY_SMART_CREATIVE_TEMPLATE = "{\n"
            + "  creatives(input: {searchBy: {login: \"%1$s\"}, filter: {creativeIdIn: [%2$s], creativeTypeIn: [%3$s]}, limitOffset: {limit: 1, offset: 0}, cacheKey: \"\"}) {\n"
            + "    rowset {\n"
            + "      creativeId\n"
            + "      creativeType\n"
            + "      ... on GdSmartCreative {\n"
            + "        regionIds\n"
            + "        groupName\n"
            + "      }\n"
            + "    }\n"
            + "    creativeIds\n"
            + "    totalCount\n"
            + "  }\n"
            + "}\n";
    private static final String QUERY_HTML5_CREATIVE_TEMPLATE = "{\n"
            + "  creatives(input: {searchBy: {login: \"%1$s\"}, filter: {creativeIdIn: [%2$s], creativeTypeIn: [%3$s]}, limitOffset: {limit: 1, offset: 0}, cacheKey: \"\"}) {\n"
            + "    rowset {\n"
            + "      creativeId\n"
            + "      creativeType\n"
            + "      ... on GdHtml5Creative {\n"
            + "        originalHeight\n"
            + "        originalWidth\n"
            + "      }\n"
            + "    }\n"
            + "    creativeIds\n"
            + "    totalCount\n"
            + "  }\n"
            + "}\n";
    private static final String FLAG = "flag";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private TurboLandingSteps turboLandingSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private VcardSteps vcardSteps;

    private UserInfo userInfo;
    private ClientId clientId;
    private List<Long> calloutsIds;
    private Callout callout;
    private TurboLanding turbolanding;
    private CreativeInfo videoCreativeInfo;
    private BannerImageFormat bannerImageFormat;
    private Vcard vcard;

    @Before
    public void init() {
        userInfo = userSteps.createUser(generateNewUser());
        clientId = userInfo.getClientInfo().getClientId();

        callout = defaultCallout(clientId)
                .withFlags(FLAG);
        calloutsIds =
                calloutRepository.add(userInfo.getShard(), singletonList(callout));
        assertThat(calloutsIds).hasSize(1);

        turbolanding = turboLandingSteps.createDefaultTurboLanding(clientId);

        videoCreativeInfo =
                creativeSteps.createCreative(fullCreative(clientId, RandomNumberUtils.nextPositiveLong()).withId(null),
                        userInfo.getClientInfo());

        bannerImageFormat = bannerSteps.createBannerImageFormat(userInfo.getClientInfo());
        CampaignInfo campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());
        Vcard vcardData = TestVcards.fullVcard(userInfo.getUid(), campaignInfo.getCampaignId());
        VcardInfo vcardInfo = vcardSteps.createVcard(vcardData, campaignInfo);
        this.vcard = vcardInfo.getVcard();
    }

    @Test
    public void testService() {
        GridGraphQLContext operator = ContextHelper.buildContext(userInfo.getUser());
        String query = String.format(QUERY_TEMPLATE,
                userInfo.getUser().getLogin(),
                turbolanding.getId(),
                bannerImageFormat.getImageHash(),
                videoCreativeInfo.getCreativeId(),
                GdCreativeType.VIDEO_ADDITION.name(),
                vcard.getId()
        );
        ExecutionResult result = processor.processQuery(null, query, null, operator);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(
                "callouts",
                singletonList(
                        ImmutableMap.of(
                                "id", calloutsIds.get(0),
                                "clientId", clientId.asLong(),
                                "flags", ImmutableList.copyOf(Objects.requireNonNull(parseFlags(callout.getFlags()))),
                                "statusModerate", GdCalloutStatusModerate.READY.name(),
                                "text", callout.getText()
                        )
                ),
                "turbolandings",
                singletonList(
                        ImmutableMap.of(
                                "id", turbolanding.getId(),
                                "clientId", clientId.asLong(),
                                "name", turbolanding.getName(),
                                "href", turbolanding.getUrl()
                        )
                ),
                "images",
                ImmutableMap.of(
                        "rowset", singletonList(ImmutableMap.builder()
                                .put("avatarsHost", toGdAdImageAvatarsHost(bannerImageFormat.getAvatarHost()).name())
                                .put("imageSize", ImmutableMap.of(
                                        "height", bannerImageFormat.getHeight().intValue(),
                                        "width", bannerImageFormat.getWidth().intValue())
                                )
                                .put("name",
                                        String.format(DEFAULT_IMAGE_NAME_TEMPLATE, bannerImageFormat.getImageHash()))
                                .put("mdsGroupId", bannerImageFormat.getMdsGroupId())
                                .put("type", toGdAdImageType(bannerImageFormat.getImageType()).name())
                                .put("namespace", toGdAdImageNamespace(bannerImageFormat.getAvatarNamespace()).name())
                                .put("formats", getExpectedImageFormatsData(bannerImageFormat))
                                .put("imageHash", bannerImageFormat.getImageHash())
                                .build()),
                        "imageHashes", singletonList(bannerImageFormat.getImageHash()),
                        "totalCount", 1
                ),
                "creatives",
                ImmutableMap.of(
                        "rowset", singletonList(ImmutableMap.builder()
                                .put("creativeId", videoCreativeInfo.getCreativeId())
                                .put("creativeType", toGdCreativeType(videoCreativeInfo.getCreative().getType()).name())
                                .put("height", videoCreativeInfo.getCreative().getHeight())
                                .put("width", videoCreativeInfo.getCreative().getWidth())
                                .put("name", videoCreativeInfo.getCreative().getName())
                                .put("previewUrl", videoCreativeInfo.getCreative().getPreviewUrl())
                                .put("livePreviewUrl", videoCreativeInfo.getCreative().getLivePreviewUrl())
                                .put("duration", videoCreativeInfo.getCreative().getDuration())
                                .build()),
                        "creativeIds", singletonList(videoCreativeInfo.getCreativeId()),
                        "totalCount", 1
                ),
                "vcards",
                getExpectedVcardData(vcard)
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }


    private static List<Map<String, Object>> getExpectedImageFormatsData(BannerImageFormat bannerImageFormat) {
        List<GdImageFormat> gdImageFormats = toGdImageFormats(bannerImageFormat);
        assertThat(gdImageFormats)
                .isNotNull();

        return mapList(gdImageFormats, ClientEntityGraphQlServiceTest::gdImageFormatToMap);
    }

    private static Map<String, Object> gdImageFormatToMap(GdImageFormat format) {
        return ImmutableMap.<String, Object>builder()
                .put(GdImageFormat.PATH.name(), format.getPath())
                .put(GdImageFormat.IMAGE_SIZE.name(), ImmutableMap.<String, Integer>builder()
                        .put(GdImageSize.HEIGHT.name(), format.getImageSize().getHeight())
                        .put(GdImageSize.WIDTH.name(), format.getImageSize().getWidth())
                        .build())
                .put(GdImageFormat.SMART_CENTERS.name(),
                        mapList(format.getSmartCenters(), ClientEntityGraphQlServiceTest::gdImageSmartCenterToMap))
                .build();
    }

    private static Map<String, Object> gdImageSmartCenterToMap(GdImageSmartCenter smartCenter) {
        return ImmutableMap.<String, Object>builder()
                .put(GdImageSmartCenter.RATIO.name(), smartCenter.getRatio())
                .put(GdImageSmartCenter.HEIGHT.name(), smartCenter.getHeight())
                .put(GdImageSmartCenter.WIDTH.name(), smartCenter.getWidth())
                .put(GdImageSmartCenter.X.name(), smartCenter.getX())
                .put(GdImageSmartCenter.Y.name(), smartCenter.getY())
                .build();
    }

    private static Map<String, Object> getExpectedVcardData(Vcard vcard) {
        return Collections.singletonMap(GdVcardsContext.ROWSET.name(), singletonList(
                ImmutableMap.<String, Object>builder()
                        .put(GdVcard.ID.name(), vcard.getId())
                        .put(GdVcard.CAMPAIGN_ID.name(), vcard.getCampaignId())
                        .put(GdVcard.COMPANY_NAME.name(), vcard.getCompanyName())
                        .put(GdVcard.CONTACT_PERSON.name(), vcard.getContactPerson())
                        .put(GdVcard.EXTRA_MESSAGE.name(), vcard.getExtraMessage())
                        .put(GdVcard.OGRN.name(), vcard.getOgrn())
                        .put(GdVcard.INSTANT_MESSENGER.name(),
                                ImmutableMap.<String, String>builder()
                                        .put(GdInstantMessenger.TYPE.name(),
                                                vcard.getInstantMessenger().getType().toUpperCase())
                                        .put(GdInstantMessenger.LOGIN.name(), vcard.getInstantMessenger().getLogin())
                                        .build())
                        .put(GdVcard.PHONE.name(),
                                ImmutableMap.<String, String>builder()
                                        .put(GdPhone.COUNTRY_CODE.name(), vcard.getPhone().getCountryCode())
                                        .put(GdPhone.CITY_CODE.name(), vcard.getPhone().getCityCode())
                                        .put(GdPhone.PHONE_NUMBER.name(), vcard.getPhone().getPhoneNumber())
                                        .put(GdPhone.EXTENSION.name(), vcard.getPhone().getExtension())
                                        .build())
                        .put(GdVcard.ADDRESS.name(),
                                ImmutableMap.<String, Object>builder()
                                        .put(GdAddress.COUNTRY.name(), vcard.getCountry())
                                        .put(GdAddress.CITY.name(), vcard.getCity())
                                        .put(GdAddress.GEO_ID.name(), vcard.getGeoId().intValue())
                                        .put(GdAddress.POINT_ON_MAP.name(),
                                                ImmutableMap.of(GdPointOnMap.X.name(), vcard.getManualPoint().getX(),
                                                        GdPointOnMap.Y.name(), vcard.getManualPoint().getY()))
                                        .build())
                        .build()
        ));
    }

    @Test
    public void getCreatives_success_forSmartCreative() {
        // Ожидаем заполняемость regionIds и groupName
        String groupName = "Группа креативов";
        Creative creative = defaultPerformanceCreative(null, null)
                .withSumGeo(singletonList(RUSSIA_REGION_ID))
                .withGroupName(groupName);
        CreativeInfo creativeInfo = creativeSteps.createCreative(creative, userInfo.getClientInfo());
        Long creativeId = creativeInfo.getCreativeId();

        GridGraphQLContext operator = ContextHelper.buildContext(userInfo.getUser());
        String query = String.format(QUERY_SMART_CREATIVE_TEMPLATE,
                userInfo.getUser().getLogin(),
                creativeId,
                GdCreativeType.SMART.name()
        );
        ExecutionResult result = processor.processQuery(null, query, null, operator);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(
                "creatives",
                ImmutableMap.of(
                        "rowset", singletonList(ImmutableMap.builder()
                                .put("creativeId", creativeId)
                                .put("creativeType", GdCreativeType.SMART.name())
                                .put("groupName", creativeInfo.getCreative().getGroupName())
                                .put("regionIds", creativeInfo.getCreative().getSumGeo())
                                .build()),
                        "creativeIds", singletonList(creativeInfo.getCreativeId()),
                        "totalCount", 1
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getCreatives_success_forHtml5Creative() {
        // Ожидаем заполняемость originalHeight, originalWidth
        var originalHeight = 602;
        var originalWidth = 500;
        Creative creative = defaultHtml5(null, null)
                .withAdditionalData(
                        new AdditionalData()
                                .withOriginalHeight(originalHeight)
                                .withOriginalWidth(originalWidth)
                );
        CreativeInfo creativeInfo = creativeSteps.createCreative(creative, userInfo.getClientInfo());
        Long creativeId = creativeInfo.getCreativeId();

        GridGraphQLContext operator = ContextHelper.buildContext(userInfo.getUser());
        String query = String.format(QUERY_HTML5_CREATIVE_TEMPLATE,
                userInfo.getUser().getLogin(),
                creativeId,
                GdCreativeType.HTML5_CREATIVE.name()
        );
        ExecutionResult result = processor.processQuery(null, query, null, operator);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(
                "creatives",
                ImmutableMap.of(
                        "rowset", singletonList(ImmutableMap.builder()
                                .put("creativeId", creativeId)
                                .put("creativeType", GdCreativeType.HTML5_CREATIVE.name())
                                .put("originalHeight", originalHeight)
                                .put("originalWidth", originalWidth)
                                .build()),
                        "creativeIds", singletonList(creativeInfo.getCreativeId()),
                        "totalCount", 1
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }
}
