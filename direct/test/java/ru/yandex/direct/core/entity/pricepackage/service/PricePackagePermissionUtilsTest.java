package ru.yandex.direct.core.entity.pricepackage.service;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_25_34;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_GEO_EXPANDED;
import static ru.yandex.direct.core.testing.data.TestPricePackages.FEMALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@RunWith(SpringRunner.class)
@CoreTest
public class PricePackagePermissionUtilsTest {

    @Test
    public void canViewPricePackages_mustReturnTrueIfCanManagePricePackages() {
        var user = generateNewUser()
                .withCanManagePricePackages(true);
        Assertions.assertThat(PricePackagePermissionUtils.canViewPricePackages(user)).isTrue();
    }

    @Test
    public void equalsIgnoringGeoExpanded_targetingsFixedGeoExpandedIsIgnored() {
        var targetings = new TargetingsFixed()
                .withGeo(DEFAULT_GEO)
                .withGeoExpanded(DEFAULT_GEO_EXPANDED);
        var secondTargetings = new TargetingsFixed()
                .withGeo(DEFAULT_GEO);
        Assertions.assertThat(PricePackagePermissionUtils.equalsIgnoringGeoExpanded(targetings, secondTargetings))
                .isTrue();
    }

    @Test
    public void equalsIgnoringGeoExpanded_targetingsFixedUnorderedListsAreEqual() {
        var targetings = new TargetingsFixed()
                .withGeo(List.of(RUSSIA, -VOLGA_DISTRICT))
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID, AGE_25_34));
        var secondTargetings = new TargetingsFixed()
                .withGeo(List.of(-VOLGA_DISTRICT, RUSSIA))
                .withViewTypes(List.of(ViewType.MOBILE, ViewType.DESKTOP, ViewType.NEW_TAB))
                .withCryptaSegments(List.of(AGE_25_34, FEMALE_CRYPTA_GOAL_ID));
        Assertions.assertThat(PricePackagePermissionUtils.equalsIgnoringGeoExpanded(targetings, secondTargetings))
                .isTrue();
    }

    @Test
    public void equalsIgnoringGeoExpanded_targetingsCustomGeoExpandedIsIgnored() {
        var targetings = new TargetingsCustom()
                .withGeo(DEFAULT_GEO)
                .withGeoExpanded(DEFAULT_GEO_EXPANDED);
        var secondTargetings = new TargetingsCustom()
                .withGeo(DEFAULT_GEO);
        Assertions.assertThat(PricePackagePermissionUtils.equalsIgnoringGeoExpanded(targetings, secondTargetings))
                .isTrue();
    }

    @Test
    public void equalsIgnoringGeoExpanded_targetingsCustomUnorderedListsAreEqual() {
        var targetings = new TargetingsCustom()
                .withGeo(List.of(RUSSIA, -VOLGA_DISTRICT));
        var secondTargetings = new TargetingsCustom()
                .withGeo(List.of(-VOLGA_DISTRICT, RUSSIA));
        Assertions.assertThat(PricePackagePermissionUtils.equalsIgnoringGeoExpanded(targetings, secondTargetings))
                .isTrue();
    }

}
