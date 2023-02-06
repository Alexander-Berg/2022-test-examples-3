package ru.yandex.market.core.feature.impl;

import java.time.Clock;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureCutoffService;
import ru.yandex.market.core.feature.model.FeatureCutoffMinorInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;
import static ru.yandex.market.core.feature.model.cutoff.UtilityCutoffs.NEED_TESTING;

/**
 * Тесты на {@link FeatureCutoffService}, {@link FeatureCutoffServiceImpl}
 */
@DbUnitDataSet(before = "cutoff/datasource.csv")
class FeatureCutoffServiceTest extends FunctionalTest {

    private static final long SHOP_ID = 1;

    @Autowired
    private FeatureCutoffService featureCutoffService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(
            before = "cutoff/testOpenFeatureCutoffWithCancelModeration.before.csv",
            after = "cutoff/testOpenFeatureCutoffWithCancelModeration.after.csv"
    )
    @DisplayName("Активная модерация должна отмениться, если открывается катоф, требующий модерацию")
    void testOpenFeatureCutoffWithCancelModeration() {
        openDsbsCutoff(SHOP_ID, DSBSCutoffs.QUALITY_OTHER);
    }

    @Test
    @DbUnitDataSet(
            before = "cutoff/testOpenFeatureCutoffChangeTestingType.before.csv",
            after = "cutoff/testOpenFeatureCutoffChangeTestingType.after.csv"
    )
    @DisplayName("При открытии катофа, требующего CPA модерацию, активная лайт-модерация должна отмениться, " +
            "тип проверки должен смениться")
    void testOpenFeatureCutoffChangeTestingType() {
        openDsbsCutoff(SHOP_ID, DSBSCutoffs.QUALITY_SERIOUS);
    }

    @Test
    @DbUnitDataSet(
            before = "cutoff/testOpenFeatureCutoffDontChangeTestingType.before.csv",
            after = "cutoff/testOpenFeatureCutoffDontChangeTestingType.after.csv"
    )
    @DisplayName("При открытии катофа, требующего CPA-лайт модерацию, активная CPA-модерация должна отмениться, " +
            "тип проверки не должен смениться")
    void testOpenFeatureCutoffDontChangeTestingType() {
        openDsbsCutoff(SHOP_ID, DSBSCutoffs.QUALITY_OTHER);
    }

    @Test
    @DbUnitDataSet(
            before = "cutoff/testOpenFeatureCutoffWithDisableModerationForSuccessFeature.before.csv",
            after = "cutoff/testOpenFeatureCutoffWithDisableModerationForSuccessFeature.after.csv"
    )
    @DisplayName("У магазина в проде блокируется программа, после открытия катофа с серьезным нарушением " +
            "качества")
    void testOpenFeatureCutoffWithDisableModerationForSuccessFeature() {
        openDsbsCutoff(SHOP_ID, DSBSCutoffs.QUALITY_SERIOUS);
    }

    @Test
    @DbUnitDataSet(
            after = "cutoff/testOpenFeatureCutoffWithDisableModerationForDontWantFeature.after.csv"
    )
    @DisplayName("У магазина DSBS не настроен, но пришел сквозной катоф за мошеничество, программа должна перейти в " +
            "REVOKE")
    void testOpenFeatureCutoffWithDisableModerationForDontWantFeature() {
        openDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
    }

    @Test
    @DbUnitDataSet(
            after = "cutoff/testCloseFeatureCutoffWithDisableModerationForRevokeFeature.after.csv"
    )
    @DisplayName("У магазина DSBS не настроен, после закрытия катофа за мошеничество, программа должна остаться в " +
            "REVOKE")
    void testCloseFeatureCutoffWithDisableModerationForRevokeFeature() {
        openDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
        closeDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
    }

    @Test
    @DbUnitDataSet(before = "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv")
    @DisplayName("После закрытия катофа за мошенничество, магазин ранее не подключенный к ДСБС должен успешно " +
            "перейти к модерации")
    void testSwitchFromRevokeToNewForNewShop() {
        openDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
        assertFeatureState(SHOP_ID, ParamCheckStatus.REVOKE,
                NEED_TESTING, FeatureCutoffType.PARTNER, CommonCutoffs.FRAUD);
        // предполагаем, что из АБО закрыли AboCutoff.SHOP_FRAUD
        // далее магазин выбирает размещение по DSBS
        featureCutoffService.changeStatus(1, SHOP_ID, DROPSHIP_BY_SELLER, ParamCheckStatus.SUCCESS);
        assertFeatureState(SHOP_ID, ParamCheckStatus.REVOKE,
                NEED_TESTING, FeatureCutoffType.PARTNER, CommonCutoffs.FRAUD);
        // далее магазин отправляется на модерацию
        featureCutoffService.changeStatus(1, SHOP_ID, MARKETPLACE_SELF_DELIVERY, ParamCheckStatus.NEW);
        assertFeatureState(SHOP_ID, ParamCheckStatus.NEW, FeatureCutoffType.TESTING);
    }

    @Test
    @DbUnitDataSet(
            before = "cutoff/testCloseNotLastRevokeCutoff.before.csv",
            after = "cutoff/testCloseNotLastRevokeCutoff.after.csv"
    )
    @DisplayName("Закрытие не последнего катофа в REVOKE не делжно разблокировать прохождение модерации")
    void testCloseNotLastRevokeCutoff() {
        openDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
        openDsbsCutoff(SHOP_ID, DSBSCutoffs.QUALITY_SERIOUS);
        closeDsbsCutoff(SHOP_ID, CommonCutoffs.FRAUD);
    }

    private void openDsbsCutoff(long shopId, FeatureCustomCutoffType cutoffType) {
        featureCutoffService.openCutoff(1, shopId, MARKETPLACE_SELF_DELIVERY, FeatureCutoffMinorInfo.builder()
                .setFeatureCutoffType(cutoffType)
                .build());
    }

    private void closeDsbsCutoff(long shopId, FeatureCustomCutoffType cutoffType) {
        featureCutoffService.closeCutoff(1, shopId, MARKETPLACE_SELF_DELIVERY, cutoffType);
    }

    private void assertFeatureState(long shopId, ParamCheckStatus expectedStatus,
                                    FeatureCustomCutoffType... expectedOpenCutoffs) {
        ShopFeature feature = featureCutoffService.lockFeature(1, shopId, MARKETPLACE_SELF_DELIVERY);
        assertThat(feature.getStatus()).isEqualTo(expectedStatus);

        Set<FeatureCustomCutoffType> openCutoffs = featureCutoffService.getOpenCutoffs(shopId,
                MARKETPLACE_SELF_DELIVERY);
        assertThat(openCutoffs).containsExactlyInAnyOrder(expectedOpenCutoffs);
    }
}
