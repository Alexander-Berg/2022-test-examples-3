package ru.yandex.direct.web.entity.placements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.placements.model1.GeoBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.entity.placements.model1.PlacementsFilter;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.entity.placement.model.PlacementsResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.placements.model1.GeoBlock.ADDRESS_TRANSLATIONS_PROPERTY;
import static ru.yandex.direct.core.entity.placements.model1.Placement.BLOCKS_PROPERTY;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithAddressTranslations;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringRunner.class)
public class PlacementControllerTest {

    private static final CompareStrategy PLACEMENT_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFields()
            .forFields(newPath(BLOCKS_PROPERTY, "0", ADDRESS_TRANSLATIONS_PROPERTY)).useMatcher(nullValue());

    private static final CompareStrategy BLOCK_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFields()
            .forFields(newPath(ADDRESS_TRANSLATIONS_PROPERTY)).useMatcher(nullValue());

    @Autowired
    private DirectWebAuthenticationSource authenticationSource;
    @Autowired
    private PlacementController placementController;
    @Autowired
    private Steps steps;

    private Placement notTestingOutdoorPlacement;
    private Placement testingPlacement;

    @Before
    public void before() {
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addOutdoorOperator("rusoutdoor-login-5", "RusOutdoor");
        steps.placementSteps().addOutdoorOperator("anoutdoor-login-5", "AnOutdoor");
        notTestingOutdoorPlacement = new OutdoorPlacement(7L, "rusoutdoor5.ru", "RusOutdoor5", "rusoutdoor-login-5",
                "RusOutdoor", true, false, false,
                singletonList(outdoorBlockWithOneSize(7L, 12L)), List.of());
        testingPlacement = new OutdoorPlacement(25L, "anoutdoor5.ru", "AnOutdoor5", "anoutdoor-login-5",
                "AnOutdoor", true, false, true,
                singletonList(outdoorBlockWithOneSize(25L, 143L)), List.of());
        steps.placementSteps().addPlacements(notTestingOutdoorPlacement, testingPlacement);
        setRequestLocale(Locale.forLanguageTag("ru"));
    }

    @After
    public void after() {
        setRequestLocale(null);
    }

    @Test
    public void testingPagesFilteredIfClientWithoutFeature() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        setAuthInfo(clientInfo);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse result = placementController.getPlacements(placementsFilter, null);
        assertThat("должен был вернуться один placement", result.getPlacements(), hasSize(1));
        assertThat("среди них только не тестовые placement'ы", result.getPlacements(),
                contains(beanDiffer(notTestingOutdoorPlacement).useCompareStrategy(PLACEMENT_COMPARE_STRATEGY)));
    }

    @Test
    public void testingPagesNotFilteredIfClientWithFeature() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        setAuthInfo(clientInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.OUTDOOR_INDOOR_TESTING_PAGES, true);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse result = placementController.getPlacements(placementsFilter, null);
        assertThat("должно было вернуться два placement'а", result.getPlacements(), hasSize(2));
        assertThat("вернулись все placement'ы", result.getPlacements(),
                containsInAnyOrder(beanDiffer(notTestingOutdoorPlacement).useCompareStrategy(PLACEMENT_COMPARE_STRATEGY),
                        beanDiffer(testingPlacement).useCompareStrategy(PLACEMENT_COMPARE_STRATEGY)));
    }

    @Test
    public void blocksOfTestingPagesFiltered() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        setAuthInfo(clientInfo);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse result = placementController.getPlacements(placementsFilter, null);
        assertThat("только не тестовые блоки вернулись", result.getPlacementBlocks(),
                contains(mapList(notTestingOutdoorPlacement.getBlocks(), block -> beanDiffer(block).useCompareStrategy(BLOCK_COMPARE_STRATEGY))));
    }

    @Test
    public void blocksOfTestingPagesNotFiltered() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        setAuthInfo(clientInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.OUTDOOR_INDOOR_TESTING_PAGES, true);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse result = placementController.getPlacements(placementsFilter, null);
        List<PlacementBlock> allBlocks = new ArrayList<>();
        allBlocks.addAll(notTestingOutdoorPlacement.getBlocks());
        allBlocks.addAll(testingPlacement.getBlocks());
        assertThat("все блоки вернулись", result.getPlacementBlocks(),
                contains(mapList(allBlocks, block -> beanDiffer(block).useCompareStrategy(BLOCK_COMPARE_STRATEGY))));
    }

    @Test
    public void blocksAddressTranslated() {
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addOutdoorOperator("rusoutdoor-login-5", "RusOutdoor");
        Placement placement = new OutdoorPlacement(7L, "rusoutdoor5.ru", "RusOutdoor5", "rusoutdoor-login-5",
                "RusOutdoor", true, false, false,
                singletonList(outdoorBlockWithAddressTranslations(7L, 12L, Map.of(
                        Language.EN, "address en"
                ))), List.of());
        steps.placementSteps().addPlacements(placement);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        setAuthInfo(clientInfo);
        setRequestLocale(Locale.ENGLISH);

        PlacementsFilter placementsFilter = new PlacementsFilter();
        placementsFilter.setPlacementType(PlacementType.OUTDOOR);
        PlacementsResponse result = placementController.getPlacements(placementsFilter, null);
        GeoBlock block = (GeoBlock) result.getPlacementBlocks().get(0);
        GeoBlock blockInPlacement = (GeoBlock) result.getPlacements().get(0).getBlocks().get(0);

        assertEquals("вернулся переведенный адрес", block.getAddress(), "address en");
        assertEquals("вернулся переведенный адрес", blockInPlacement.getAddress(), "address en");
        assertNull("переводы занулены (чтобы их не использовал фронт)", block.getAddressTranslations());
        assertNull("переводы занулены (чтобы их не использовал фронт)", blockInPlacement.getAddressTranslations());
    }

    private void setAuthInfo(ClientInfo clientInfo) {
        UserInfo user = clientInfo.getChiefUserInfo();

        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(user.getUser());
        authSource.withSubjectUser(user.getUser());

        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user.getUser(), user.getUser()));
    }

    private void setRequestLocale(Locale locale) {
        LocaleContextHolder.setLocale(locale);
    }
}
