package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdAction;
import ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.utils.CommonUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.DOWNLOAD;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.ICON;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.PRICE;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.RATING;
import static ru.yandex.direct.grid.processing.service.banner.converter.AdMutationDataConverter.toNewMobileContentPrimaryAction;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceGetMobileContentAdsTest {
    private static final String TRACKING_URL = "http://app.adjust.com/newnewnew?aaa=555";
    private static final String IMPRESSION_URL = "http://view.adjust.com/impression/newnewnew?aaa=555";
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdMobileContentAd {\n"
            + "          primaryAction\n"
            + "          href\n"
            + "          impressionUrl\n"
            + "          reflectedAttrs\n"
            + "          bannerImage {\n"
            + "              imageHash\n"
            + "          }\n"
            + "          creative {\n"
            + "              creativeId\n"
            + "          }\n"
            + "          modFlags {\n"
            + "              age\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final GdAdOrderBy ORDER_BY_ID = new GdAdOrderBy()
            .withField(GdAdOrderByField.ID)
            .withOrder(Order.ASC);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private BannersAddOperationFactory bannersAddOperationFactory;
    @Autowired
    private TrustedRedirectsRepository trustedRedirectsRepository;

    private Long bannerId;
    private AdGroupInfo adGroupInfo;
    private String imageHash;
    private GdAdsContainer adsContainer;
    private GridGraphQLContext context;
    private Long creativeCanvasId;
    private User operator;
    private ClientId clientId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operator = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);

        imageHash = steps.bannerSteps().createWideImageFormat(clientInfo).getImageHash();

        creativeCanvasId = steps.creativeSteps().getNextCreativeId();
        creativeCanvasId = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(adGroupInfo.getClientInfo(), creativeCanvasId).getCreativeId();

        Long uid = adGroupInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(adGroupInfo.getShard(), singletonList(uid)).get(0);
        context = ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        trustedRedirectsRepository.addTrustedDomain(
                new TrustedRedirects().withDomain("app.adjust.com").withRedirectType(RedirectType.MOBILE_APP_COUNTER));
        trustedRedirectsRepository.addTrustedDomain(
                new TrustedRedirects().withDomain("view.adjust.com").withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER));
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void testService() {
        MobileAppBanner banner = createBanner(Age.AGE_12, DOWNLOAD,
                Map.of(NewReflectedAttribute.RATING_VOTES, true), IMPRESSION_URL);

        ExecutionResult result = processQuery();

        Long id = getDataValue(result.getData(), "client/ads/rowset/0/id");
        Object primaryAction = getDataValue(result.getData(), "client/ads/rowset/0/primaryAction");
        String href = getDataValue(result.getData(), "client/ads/rowset/0/href").toString();
        String impressionUrl = getDataValue(result.getData(), "client/ads/rowset/0/impressionUrl").toString();
        Object modFlagsAge = getDataValue(result.getData(), "client/ads/rowset/0/modFlags/age");
        Map<Object, Object> reflectedAttrs = getDataValue(result.getData(), "client/ads/rowset/0/reflectedAttrs");
        String imageHash = getDataValue(result.getData(), "client/ads/rowset/0/bannerImage/imageHash");
        Long creativeId = getDataValue(result.getData(), "client/ads/rowset/0/creative/creativeId");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).as("id баннера")
                    .isEqualTo(bannerId);
            softly.assertThat(primaryAction).as("действие на баннере")
                    .isEqualTo(GdMobileContentAdAction.DOWNLOAD.toString());
            softly.assertThat(href).as("трекинговая ссылка")
                    .isEqualTo(banner.getHref());
            softly.assertThat(impressionUrl).as("трекинговая ссылка для показа")
                    .isEqualTo(banner.getImpressionUrl());
            softly.assertThat(modFlagsAge).as("возрастные ограничения")
                    .isEqualTo(Age.AGE_12.name());
            softly.assertThat(reflectedAttrs).as("показ в объявлении")
                    .isEqualTo(BannerDataConverter.toGdMobileContentAdFeature(Set.of(NewReflectedAttribute.RATING_VOTES)));
            softly.assertThat(imageHash).as("изображение")
                    .isEqualTo(banner.getImageHash());
            softly.assertThat(creativeId).as("креатив")
                    .isEqualTo(banner.getCreativeId());
        });
    }

    @Test
    public void testService_NullImpressionUrl() {
        MobileAppBanner banner = createBanner(Age.AGE_12, DOWNLOAD,
                Map.of(NewReflectedAttribute.RATING_VOTES, true), null);

        ExecutionResult result = processQuery();

        Long id = getDataValue(result.getData(), "client/ads/rowset/0/id");
        Object primaryAction = getDataValue(result.getData(), "client/ads/rowset/0/primaryAction");
        String href = getDataValue(result.getData(), "client/ads/rowset/0/href").toString();
        Object impressionUrl = getDataValue(result.getData(), "client/ads/rowset/0/impressionUrl");
        Object modFlagsAge = getDataValue(result.getData(), "client/ads/rowset/0/modFlags/age");
        Map<Object, Object> reflectedAttrs = getDataValue(result.getData(), "client/ads/rowset/0/reflectedAttrs");
        String imageHash = getDataValue(result.getData(), "client/ads/rowset/0/bannerImage/imageHash");
        Long creativeId = getDataValue(result.getData(), "client/ads/rowset/0/creative/creativeId");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).as("id баннера")
                    .isEqualTo(bannerId);
            softly.assertThat(primaryAction).as("действие на баннере")
                    .isEqualTo(GdMobileContentAdAction.DOWNLOAD.toString());
            softly.assertThat(href).as("трекинговая ссылка")
                    .isEqualTo(banner.getHref());
            softly.assertThat(impressionUrl).as("трекинговая ссылка для показа")
                    .isNull();
            softly.assertThat(modFlagsAge).as("возрастные ограничения")
                    .isEqualTo(Age.AGE_12.name());
            softly.assertThat(reflectedAttrs).as("показ в объявлении")
                    .isEqualTo(BannerDataConverter.toGdMobileContentAdFeature(Set.of(NewReflectedAttribute.RATING_VOTES)));
            softly.assertThat(imageHash).as("изображение")
                    .isEqualTo(banner.getImageHash());
            softly.assertThat(creativeId).as("креатив")
                    .isEqualTo(banner.getCreativeId());
        });
    }

    public static Object[] ageLabelParameters() {
        return StreamEx.of(Age.values(), new Age[]{null})
                .flatMap(Stream::of)
                .map(age -> new Object[]{age, CommonUtils.nvl(age, Age.AGE_18)})
                .toArray();
    }

    @Test
    @Parameters(method = "ageLabelParameters")
    @TestCaseName("[{index}]: в базе age = {0}, ожидаем что получим {1}")
    public void getAd_WithDifferentAges(Age age, Age expectedAge) {
        createBanner(age, DOWNLOAD, Map.of(NewReflectedAttribute.RATING_VOTES, true), IMPRESSION_URL);

        ExecutionResult result = processQuery();

        Object modFlagsAge = getDataValue(result.getData(), "client/ads/rowset/0/modFlags/age");

        assertThat(modFlagsAge).as("возрастные ограничения")
                .isEqualTo(expectedAge.toString());
    }

    public static Object[] actionParameters() {
        return StreamEx.of(GdMobileContentAdAction.values())
                .map(action -> new Object[]{toNewMobileContentPrimaryAction(action), action})
                .toArray();
    }

    @Test
    @Parameters(method = "actionParameters")
    @TestCaseName("[{index}]: в базе primaryAction = {0}, ожидаем что получим {1}")
    public void getAd_WithDifferentActions(NewMobileContentPrimaryAction primaryAction,
                                           GdMobileContentAdAction expectedGdAction) {
        createBanner(Age.AGE_12, primaryAction, Map.of(NewReflectedAttribute.RATING_VOTES, true), IMPRESSION_URL);

        ExecutionResult result = processQuery();

        Object resultPrimaryAction = getDataValue(result.getData(), "client/ads/rowset/0/primaryAction");
        assertThat(resultPrimaryAction).as("действие на баннере")
                .isEqualTo(expectedGdAction.toString());
    }

    public static Object[][] featureParameters() {
        return new Object[][]{
                {"[ICON, RATING_VOTES, PRICE, RATING]",
                        Map.of(NewReflectedAttribute.ICON, true, NewReflectedAttribute.RATING_VOTES, true,
                                NewReflectedAttribute.PRICE, true, NewReflectedAttribute.RATING, true),
                        Map.of(ICON, true, GdMobileContentAdFeature.RATING_VOTES, true, PRICE, true, RATING, true)},
                {"[ICON]",
                        Map.of(NewReflectedAttribute.ICON, true, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false),
                        Map.of(ICON, true, GdMobileContentAdFeature.RATING_VOTES, false, PRICE, false, RATING, false)},
                {"[RATING_VOTES]",
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, true,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false),
                        Map.of(ICON, false, GdMobileContentAdFeature.RATING_VOTES, true, PRICE, false, RATING, false)},
                {"[PRICE]",
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, true, NewReflectedAttribute.RATING, false),
                        Map.of(ICON, false, GdMobileContentAdFeature.RATING_VOTES, false, PRICE, true, RATING, false)},
                {"[RATING]",
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, true),
                        Map.of(ICON, false, GdMobileContentAdFeature.RATING_VOTES, false, PRICE, false, RATING, true)},
                {"[]",
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false),
                        Map.of(ICON, false, GdMobileContentAdFeature.RATING_VOTES, false, PRICE, false, RATING, false)}
        };
    }

    @Test
    @Parameters(method = "featureParameters")
    @TestCaseName("[{index}]: разрешенные к показу атрибуты {0}")
    public void getAd_WithDifferentFeatures(@SuppressWarnings("unused") String description,
                                            Map<NewReflectedAttribute, Boolean> reflectedAttributes,
                                            Map<GdMobileContentAdFeature, Boolean> expectedReflectedAttributes) {
        createBanner(Age.AGE_12, DOWNLOAD, reflectedAttributes, IMPRESSION_URL);

        ExecutionResult result = processQuery();

        Map<Object, Object> reflectedAttrs = getDataValue(result.getData(), "client/ads/rowset/0/reflectedAttrs");
        assertThat(reflectedAttrs).as("показ в объявлении")
                .isEqualTo(expectedReflectedAttributes);
    }

    @Test
    public void getAd_WithImpressionUrl() {
        createBanner(Age.AGE_12, DOWNLOAD, Map.of(NewReflectedAttribute.RATING_VOTES, true), IMPRESSION_URL);

        ExecutionResult result = processQuery();

        String resultImpressionUrl = getDataValue(result.getData(), "client/ads/rowset/0/impressionUrl");
        assertThat(resultImpressionUrl).as("трекинговая ссылка для показа").isEqualTo(IMPRESSION_URL);
    }

    @Test
    public void getAd_WithoutImpressionUrl() {
        createBanner(Age.AGE_12, DOWNLOAD, Map.of(NewReflectedAttribute.RATING_VOTES, true), null);

        ExecutionResult result = processQuery();

        String resultImpressionUrl = getDataValue(result.getData(), "client/ads/rowset/0/impressionUrl");
        assertThat(resultImpressionUrl).as("трекинговая ссылка для показа").isNull();
    }

    private ExecutionResult processQuery() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result;
    }

    private UnversionedRowset convertToBannerRowset(MobileAppBanner banner) {
        RowsetBuilder builder = rowsetBuilder().add(rowBuilder()
                .withColValue(BANNERSTABLE_DIRECT.BID.getName(), banner.getId())
                .withColValue(BANNERSTABLE_DIRECT.PID.getName(), banner.getAdGroupId())
                .withColValue(BANNERSTABLE_DIRECT.CID.getName(), banner.getCampaignId())
                .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(), GdAdType.MOBILE_CONTENT.name())
                .withColValue(BANNERSTABLE_DIRECT.FLAGS.getName(), BannerFlags.toSource(banner.getFlags()))
                .withColValue(BANNERSTABLE_DIRECT.HREF.getName(), banner.getHref())
                .withColValue(BANNERSTABLE_DIRECT.TITLE.getName(), banner.getTitle())
                .withColValue(BANNERSTABLE_DIRECT.BODY.getName(), banner.getBody())
                .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes"));
        return builder.build();
    }

    private MobileAppBanner createBanner(@Nullable Age age,
                                         NewMobileContentPrimaryAction primaryAction,
                                         Map<NewReflectedAttribute, Boolean> reflectedAttrs,
                                         @Nullable String impressionUrl) {
        MobileAppBanner banner = new MobileAppBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBody("TEXT")
                .withTitle("TITLE")
                .withPrimaryAction(primaryAction)
                .withFlags(age == null ? new BannerFlags() : new BannerFlags().with(BannerFlags.AGE, age))
                .withHref(TRACKING_URL)
                .withImpressionUrl(impressionUrl)
                .withReflectedAttributes(reflectedAttrs)
                .withCreativeId(creativeCanvasId)
                .withImageHash(imageHash);

        MassResult<Long> massResult = bannersAddOperationFactory
                .createPartialAddOperation(List.of(banner), clientId, operator.getUid(), true)
                .prepareAndApply();

        assumeThat(massResult, isFullySuccessful());
        bannerId = mapList(massResult.getResult(), Result::getResult).get(0);

        adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(singleton(bannerId))
                        .withCampaignIdIn(ImmutableSet.of(adGroupInfo.getCampaignId())))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));

        doAnswer(invocation -> convertToBannerRowset(banner))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());

        return banner;
    }
}
