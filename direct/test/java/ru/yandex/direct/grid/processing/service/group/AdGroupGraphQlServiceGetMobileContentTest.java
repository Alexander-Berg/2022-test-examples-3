package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Select;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppcdict.enums.MediaFilesAvatarsHost;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdStoreActionForPrices;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQlServiceGetMobileContentTest {
    private static final String MINIMAL_OPERATING_SYSTEM_VERSION = "8.1";
    private static final String GROUP_NAME = "Mobile Content Group";
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";
    private static final BigDecimal RATING = BigDecimal.valueOf(4.55);
    private static final Long RATING_VOTES = 55L;
    private static final String MOBILE_ICON_HASH = "BFPewVwf";
    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        pageGroupTags\n"
            + "        targetTags\n"
            + "        ... on GdMobileContentAdGroup {"
            + "          name\n"
            + "          currentMinimalOsVersion\n"
            + "          storeHref\n"
            + "          deviceTypeTargeting\n"
            + "          networkTargeting\n"
            + "          rating\n"
            + "          ratingVotes\n"
            + "          iconUrl\n"
            + "          mobileContentPrices {\n"
            + "              country\n"
            + "              price\n"
            + "              priceCurrency\n"
            + "              storeActionForPrices\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);
    private static final Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>> TEST_PRICES =
            ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                    .put(StoreCountry.RU.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.update, getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                    .build())
                    .put(StoreCountry.BY.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.open, getExternalWorldMoney("0", CurrencyCode.USD))
                                    .build())
                    .put(StoreCountry.TR.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.more, getExternalWorldMoney("0.15", "CAD"))
                                    .build())
                    .put(StoreCountry.US.toString(),
                            ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                    .put(StoreActionForPrices.more, new MobileContentExternalWorldMoney())
                                    .build())
                    .build();
    public static final List<String> PAGE_GROUP_TAGS = List.of("aaa");
    public static final List<String> TARGET_TAGS = List.of("bbb");

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private GridGraphQLContext context;
    private Integer shard;
    private CampaignInfo campaignInfo;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();

        campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        context = ContextHelper.buildContext(userInfo.getUser()).withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testGetAdGroup() {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);

        Long adGroupId = getDataValue(rowset, "0/id");
        String currentMinimalOsVersion = getDataValue(rowset, "0/currentMinimalOsVersion");
        String mobileContentName = getDataValue(rowset, "0/name");
        String storeHref = getDataValue(rowset, "0/storeHref");
        List<String> deviceTypeTargeting = getDataValue(rowset, "0/deviceTypeTargeting");
        List<String> networkTargeting = getDataValue(rowset, "0/networkTargeting");
        Double rating = getDataValue(rowset, "0/rating");
        Long ratingVotes = getDataValue(rowset, "0/ratingVotes");
        List<Map<String, Object>> mobileContentPrices = getDataValue(rowset, "0/mobileContentPrices");

        List<Map<String, Object>> expectMobileContentPrices = new ArrayList<>();
        expectMobileContentPrices.add(Map.of(
                "country", StoreCountry.RU.toString(), "price", BigDecimal.valueOf(1.23),
                "priceCurrency", CurrencyCode.RUB.toString(),
                "storeActionForPrices", GdStoreActionForPrices.UPDATE.name()));
        expectMobileContentPrices.add(Map.of(
                "country", StoreCountry.BY.toString(), "price", BigDecimal.ZERO,
                "priceCurrency", CurrencyCode.USD.toString(),
                "storeActionForPrices", GdStoreActionForPrices.OPEN.name()));
        expectMobileContentPrices.add(Map.of(
                "country", StoreCountry.TR.toString(),
                "price", BigDecimal.valueOf(0.15),
                "priceCurrency", "CAD",
                "storeActionForPrices", GdStoreActionForPrices.MORE.name()));
        Map<String, Object> expectPriceWithoutCurrency = new HashMap<>();
        expectPriceWithoutCurrency.put("country", StoreCountry.US.toString());
        expectPriceWithoutCurrency.put("price", BigDecimal.ZERO);
        expectPriceWithoutCurrency.put("priceCurrency", null);
        expectPriceWithoutCurrency.put("storeActionForPrices", GdStoreActionForPrices.MORE.name());
        expectMobileContentPrices.add(expectPriceWithoutCurrency);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(adGroupId).as("id группы")
                    .isEqualTo(adGroupInfo.getAdGroupId());
            soft.assertThat(currentMinimalOsVersion).as("минимальная версия ОС")
                    .isEqualTo(MINIMAL_OPERATING_SYSTEM_VERSION);
            soft.assertThat(mobileContentName).as("название группы")
                    .isEqualTo(GROUP_NAME);
            soft.assertThat(storeHref).as("URL приложения")
                    .isEqualTo(STORE_URL);
            soft.assertThat(networkTargeting).as("таргетинг на тип подключения к сети")
                    .containsExactly(WI_FI.name());
            soft.assertThat(deviceTypeTargeting).as("таргетинг на мобильное устройство")
                    .containsExactly(PHONE.name());
            soft.assertThat(rating).as("рейтинг в магазине")
                    .isEqualTo(RATING.doubleValue());
            soft.assertThat(ratingVotes).as("количество оценок внутри магазина")
                    .isEqualTo(RATING_VOTES);
            soft.assertThat(mobileContentPrices).as("цены, валюты и действия в приложении для разных стран")
                    .is(matchedBy(beanDiffer(expectMobileContentPrices)));
        });
    }

    public static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI", Set.of(WI_FI)},
                {"Передан список из CELLULAR", Set.of(CELLULAR)},
                {"Передан список из WI_FI, CELLULAR", Set.of(WI_FI, CELLULAR)},
        };
    }

    /**
     * Проверка получения разных значений таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void getAdGroup_CheckNetworkTargeting(@SuppressWarnings("unused") String description,
                                                 Set<MobileContentAdGroupNetworkTargeting> networkTargeting) {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(networkTargeting, Set.of(PHONE));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);
        List<String> networkTargetingFromRequest = getDataValue(rowset, "0/networkTargeting");

        assertThat(networkTargetingFromRequest).as("таргетинг на тип подключения к сети")
                .containsExactlyInAnyOrder(StreamEx.of(networkTargeting).map(Enum::name).toArray(String.class));
    }

    public static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE", Set.of(PHONE)},
                {"Передан список из TABLET", Set.of(TABLET)},
                {"Передан список из PHONE, TABLET", Set.of(PHONE, TABLET)}
        };
    }

    /**
     * Проверка получения разных значений таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void getAdGroup_CheckDeviceTypeTargeting(@SuppressWarnings("unused") String description,
                                                    Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), deviceTypeTargetings);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);
        List<String> deviceTypeTargetingFromRequest = getDataValue(rowset, "0/deviceTypeTargeting");

        assertThat(deviceTypeTargetingFromRequest).as("таргетинг на мобильное устройство")
                .containsExactlyInAnyOrder(StreamEx.of(deviceTypeTargetings).map(Enum::name).toArray(String.class));
    }

    @Test
    public void testGetAdGroup_withoutRatingAndPrices() {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE), null);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);
        Double rating = getDataValue(rowset, "0/rating");
        Long ratingVotes = getDataValue(rowset, "0/ratingVotes");
        List<Map<String, Object>> mobileContentPrices = getDataValue(rowset, "0/mobileContentPrices");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(rating).as("рейтинг в магазине")
                    .isNull();
            soft.assertThat(ratingVotes).as("количество оценок внутри магазина")
                    .isNull();
            soft.assertThat(mobileContentPrices).as("цены, валюты и действия в приложении для разных стран")
                    .isEmpty();
        });
    }

    /**
     * Проверка что для РМП группы url иконки приходит для mds аватарницы
     */
    @Test
    public void testGetAdGroup_CheckIconUrl() {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);

        String iconUrl = getDataValue(rowset, "0/iconUrl");
        assertThat(iconUrl)
                .as("url иконки")
                .isEqualTo("//" + MediaFilesAvatarsHost.avatars_mds_yandex_net.getLiteral() +
                        "/get-google-play-app-icon/" + MOBILE_ICON_HASH + "/icon");
    }

    /**
     * Если у РМП группы нет hash иконки -> iconUrl не приходит
     */
    @Test
    public void testGetAdGroup_CheckWithoutIconUrl() {
        MobileContent mobileContent = mobileContentFromStoreUrl(STORE_URL)
                .withIconHash(null);
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE), mobileContent);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(adGroupInfo);

        String iconUrl = getDataValue(rowset, "0/iconUrl");
        assertThat(iconUrl)
                .as("url иконки")
                .isNull();
    }


    @Test
    public void testGetAdGroup_CheckPiTags() {
        AdGroupInfo adGroupInfo = createTestMobileContentAdGroup(Set.of(WI_FI), Set.of(PHONE));

        var rowset = sendRequestAndGetRowset(adGroupInfo);

        var pageGroupTags = getDataValue(rowset, "0/pageGroupTags");
        var targetTags = getDataValue(rowset, "0/targetTags");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(pageGroupTags).as("pageGroupTags")
                    .isEqualTo(PAGE_GROUP_TAGS);
            soft.assertThat(targetTags).as("targetTags")
                    .isEqualTo(TARGET_TAGS);
        });
    }


    private AdGroupInfo createTestMobileContentAdGroup(Set<MobileContentAdGroupNetworkTargeting> networkTargetings,
                                                       Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        MobileContent mobileContent = mobileContentFromStoreUrl(STORE_URL)
                .withRating(RATING)
                .withRatingVotes(RATING_VOTES)
                .withIconHash(MOBILE_ICON_HASH)
                .withPrices(TEST_PRICES);
        return createTestMobileContentAdGroup(networkTargetings, deviceTypeTargetings, mobileContent);
    }

    private AdGroupInfo createTestMobileContentAdGroup(Set<MobileContentAdGroupNetworkTargeting> networkTargetings,
                                                       Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings,
                                                       @Nullable MobileContent mobileContent) {
        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(mobileContent));

        MobileContentAdGroup mobileContentAdGroup = TestGroups.activeMobileAppAdGroup(campaignInfo.getCampaignId())
                .withStoreUrl(STORE_URL)
                .withNetworkTargeting(networkTargetings)
                .withDeviceTypeTargeting(deviceTypeTargetings)
                .withMinimalOperatingSystemVersion(MINIMAL_OPERATING_SYSTEM_VERSION)
                .withMobileContentId(mobileContentInfo.getMobileContentId())
                .withName(GROUP_NAME)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withTargetTags(TARGET_TAGS);
        return steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withAdGroup(mobileContentAdGroup)
                .withCampaignInfo(campaignInfo));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sendRequestAndGetRowset(AdGroupInfo adGroupInfo) {
        UnversionedRowset ytRowset = convertToGroupsRowset(singletonList(adGroupInfo));
        doReturn(ytRowset).when(gridYtSupport).selectRows(eq(shard), any(Select.class), anyBoolean());

        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId()))
                        .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));


        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        Map<String, Object> adGroupsData = (Map<String, Object>) clientData.get("adGroups");
        return (List<Map<String, Object>>) adGroupsData.get("rowset");
    }

    private UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.GROUP_NAME.getName(), GROUP_NAME)
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
        ));

        return builder.build();
    }
}

