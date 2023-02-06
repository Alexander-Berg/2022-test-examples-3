package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestNewTextBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateBannerButton;
import ru.yandex.direct.grid.processing.model.constants.GdButtonAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.validation.defect.CommonDefects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateBannerButtonsTest {

    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_BUTTONS_MUTATION_NAME = "updateBannerButtons";

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("statusBsSynced"), newPath("buttonCaption"),
                    newPath("buttonStatusModerate"), newPath("buttonHref"), newPath("buttonAction"));

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    protected TextBannerSteps textBannerSteps;

    private AdGroupInfo defaultAdGroup;

    private NewTextBannerInfo defaultBanner;

    private long bannerId;

    private User operator;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Before
    public void before() {
        LocaleContextHolder.setLocale(I18NBundle.RU);

        var clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);

        var banner = TestNewTextBanners.fullTextBanner();
        banner.setButtonHref("https://yandex.ru");
        banner.setButtonAction(ButtonAction.BUY);
        banner.setButtonCaption("Купить");
        banner.setButtonStatusModerate(BannerButtonStatusModerate.YES);

        var newTextBannerInfo =
                new NewTextBannerInfo().withAdGroupInfo(defaultAdGroup).withBanner(banner);
        defaultBanner = textBannerSteps.createBanner(newTextBannerInfo);
        bannerId = defaultBanner.getBannerId();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void updateBannerButton_success() {
        LocaleContextHolder.setLocale(I18NBundle.RU);

        var expectedHref = "http://yandex.com";
        var expectedButtonAction = ButtonAction.DOWNLOAD;
        var button = new GdBannerButton().withHref(expectedHref)
                .withAction(GdButtonAction.fromSource(expectedButtonAction));
        var input = new GdUpdateBannerButton()
                .withBannerIds(List.of(bannerId))
                .withButton(button);

        var query = createQuery(input);
        var data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        var actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(defaultAdGroup.getShard(), singleton(bannerId)).get(0);
        assertEquals(expectedButtonAction, actualBanner.getButtonAction());
        assertEquals("Скачать", actualBanner.getButtonCaption());
        assertEquals(expectedHref, actualBanner.getButtonHref());

        assertThat(actualBanner, beanDiffer(defaultBanner.getBanner()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void deleteBannerButton_success() {
        var input = new GdUpdateBannerButton()
                .withBannerIds(List.of(bannerId))
                .withButton(null);

        var query = createQuery(input);
        var data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        var actualBanner = (TextBanner) bannerTypedRepository
                .getTyped(defaultAdGroup.getShard(), singleton(bannerId)).get(0);
        assertNull(actualBanner.getButtonAction());

        assertThat(actualBanner, beanDiffer(defaultBanner.getBanner()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateBannerButton_invalidHref_error() {
        LocaleContextHolder.setLocale(I18NBundle.RU);

        var newButtonHref = "htttttp://yandex.com";
        var newButtonAction = ButtonAction.DOWNLOAD;
        var button = new GdBannerButton().withHref(newButtonHref)
                .withAction(GdButtonAction.fromSource(newButtonAction));
        var input = new GdUpdateBannerButton()
                .withBannerIds(List.of(bannerId))
                .withButton(button);


        var query = createQuery(input);
        var errorList = processQueryAndGetError(query);

        GdValidationResult error = ((GridValidationException) ((ExceptionWhileDataFetching)
                errorList.get(0)).getException()).getValidationResult();
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field("button.href")),
                CommonDefects.invalidValue())
                .withWarnings(emptyList());

        Assertions.assertThat(error).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = result.getData();
        assertTrue(data.containsKey(UPDATE_BUTTONS_MUTATION_NAME));
        return data;
    }

    private void validateUpdateSuccessful(Map<String, Object> data, Long... bannerId) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList());

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_BUTTONS_MUTATION_NAME), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload, beanDiffer(expectedGdUpdateAdsPayload));
    }

    private List<GraphQLError> processQueryAndGetError(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        return result.getErrors();
    }

    private String createQuery(GdUpdateBannerButton gdUpdateBannerButton) {
        return String.format(QUERY_TEMPLATE, UPDATE_BUTTONS_MUTATION_NAME, graphQlSerialize(gdUpdateBannerButton));
    }
}
