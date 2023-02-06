package ru.yandex.market.core.moderation.request;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffMinorInfo;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingType;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "AbstractModerationFunctionalTest.csv")
class ModerationRequestEntryPointFunctionalTest extends AbstractModerationFunctionalTest {

    private static final String MESSAGE = "message";

    private static void runBeforeEachStep(Runnable separator, List<Runnable> steps) {
        for (Runnable step : steps) {
            separator.run();
            step.run();
        }
    }

    /**
     * Проверяем CPA модерацию.
     */
    @Test
    @DbUnitDataSet(before = "testCpaShopModeration.csv")
    void cpaModerationTest() {
        withinAction(actionId -> {
            ShopActionContext context = new ShopActionContext(actionId, 200L);

            moderationRequestEntryPoint.requestCPAModeration(context);
            SandboxState cpcState = sandboxRepository.load(200L, ShopProgram.CPC);
            SandboxState cpaState = sandboxRepository.load(200L, ShopProgram.CPA);

            assertThat(cpaState).isNotNull();
            assertThat(cpaState.moderationIsRequested()).isTrue();
            assertThat(cpcState).isNotNull();
            assertThat(cpcState.moderationIsRequested()).isFalse();
        });
    }


    /**
     * Проверяем CPС модерацию.
     */
    @Test
    @DbUnitDataSet(before = "testCpcShopModeration.csv")
    void cpcModerationTest() {
        withinAction(actionId -> {
            ShopActionContext context = new ShopActionContext(actionId, 200L);

            moderationRequestEntryPoint.requestRequiredModeration(context);
            SandboxState cpcState = sandboxRepository.load(200L, ShopProgram.CPC);
            SandboxState cpaState = sandboxRepository.load(200L, ShopProgram.CPA);

            assertThat(cpaState).isNotNull();
            assertThat(cpaState.moderationIsRequested()).isFalse();
            assertThat(cpcState).isNotNull();
            assertThat(cpcState.moderationIsRequested()).isTrue();
        });
    }

    /**
     * CPA модерация стартует несмотря на то, что return_delivery_address у магазина не настрое.
     */
    @Test
    @DbUnitDataSet(before = "testCpaShopModerationWithoutReturnAddress.csv")
    void cpaModerationWhenNoReturnAddressTest() {
        withinAction(actionId -> {
            ShopActionContext context = new ShopActionContext(actionId, 200L);

            moderationRequestEntryPoint.requestCPAModeration(context);
            SandboxState cpaState = sandboxRepository.load(200L, ShopProgram.CPA);

            assertThat(cpaState).isNotNull();
            assertThat(cpaState.moderationIsRequested()).isTrue();
        });
    }

    /**
     * Тест прохождения модерации GENERAL_LIGHT_CHECK, без каких-либо других проверок.
     * <p>
     * Подготовка.
     * <ul>
     * <li>Создаём СPC-only магазин
     * </ul>
     * <p>
     * Проверка COMMON_OTHER
     * <ul>
     * <li>Магазину открывает отключение COMMON_OTHER</li>
     * <li>Запрашиваем модерацию</li>
     * <li>Запускаем процесс модерации
     * <li>Смотрим, что у него на протяжении всего этого времени остаётся единственное отключение COMMON_OTHER
     * <li>Завершаем модерацию, смотрим, что в конце этого процесса COMMON_OTHER закрылся.
     * </ul>
     */
    @Test
    void loneGeneralLightCheckForCPCOnly() {
        loneGeneralLightCheck(EnumSet.of(ShopProgram.CPC));
    }

    /**
     * Тест прохождения модерации GENERAL_LIGHT_CHECK одновременно с другой проверкой.
     * <p>
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * </ul>
     * <p>
     * Проверка COMMON_OTHER
     * <ul>
     * <li>Магазину открывает отключение COMMON_OTHER
     * <li>Магазину открывает отключение QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Запускаем процесс модерации
     * <li>Завершаем сначала GENERAL_LIGHT_CHECK
     * <li>Смотрим, что CPC не включился
     * <li>Завершаем теперь CPC-проверку
     * <li>Смотрим, что тепер CPC включился
     * </ul>
     */
    @Test
    void generalAndProgramSpecificCheckForCPCOnlyGeneralCheckFinishesFirst() {
        generalAndProgramSpecificCheckSimultaneously(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER),
                true);
    }

    /**
     * Тест прохождения модерации GENERAL_LIGHT_CHECK одновременно с другой проверкой.
     * <p>
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * </ul>
     * <p>
     * Проверка COMMON_OTHER
     * <ul>
     * <li>Магазину открывает отключение COMMON_OTHER
     * <li>Магазину открывает отключение QMANAGER_OTHER
     * <li>Запрашиваем модерацию
     * <li>Запускаем процесс модерации
     * <li>Завершаем сначала CPC-проверку
     * <li>Смотрим, что CPC не включился
     * <li>Завершаем теперь GENERAL_LIGHT_CHECK
     * <li>Смотрим, что тепер CPC включился
     * </ul>
     */
    @Test
    void generalAndProgramSpecificCheckForCPCOnlyGeneralCheckFinishesLast() {
        generalAndProgramSpecificCheckSimultaneously(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER),
                false);
    }

    @Test
    void dsbsLiteCheckTest() {
        long datasourceID = 102;

        assertThat(getAllFeatureCutoffs(datasourceID, FeatureType.MARKETPLACE_SELF_DELIVERY))
                .doesNotContain(DSBSCutoffs.QUALITY_OTHER);

        withinAction(actionId -> {
            featureCutoffService.openCutoff(
                    actionId, datasourceID, FeatureType.MARKETPLACE_SELF_DELIVERY, FeatureCutoffMinorInfo.builder()
                            .setUserId(USER_ID)
                            .setFeatureCutoffType(DSBSCutoffs.QUALITY_OTHER)
                            .setMessage(MESSAGE)
                            .build());
            Optional<FeatureCutoffInfo> cutoff = featureService.getCutoff(
                    datasourceID,
                    FeatureType.MARKETPLACE_SELF_DELIVERY,
                    DSBSCutoffs.QUALITY_OTHER
            );

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            moderationRequestEntryPoint.requestCPAModeration(context);
            confirmModerationSandboxFeedLoadForced(datasourceID, ShopProgram.CPA);
            TestingState state = testingService.getTestingStatus(datasourceID, ShopProgram.CPA);

            // Check what datasource_in_testing record linked to correct cutoff
            assertThat(state.getCutoffId())
                    .isEqualTo(cutoff.orElseThrow().getId());

            List<ShopForLiteCheck> shopsForLiteCheck = new ArrayList<>();
            shopsModeratedSupplier.getLightCheckReadyShops((shopId, type, subject, message) -> {
                shopsForLiteCheck.add(new ShopForLiteCheck(shopId, type));
            });

            // Check what what API for ABO return correct values
            assertThat(shopsForLiteCheck)
                    .usingFieldByFieldElementComparator()
                    .contains(
                            new ShopForLiteCheck(datasourceID, TestingType.DSBS_LITE_CHECK)
                    );
        });
    }

    @Test
    void cpcLiteCheckTest() {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, Set.of(ShopProgram.CPC));

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffNotificationService.openCutoffWithNotification(
                    datasourceID, CutoffType.QMANAGER_OTHER,
                    context.getActionId(), 0, List.of(), MESSAGE
            );
            CutoffInfo cutoff = cutoffService.getCutoff(datasourceID, CutoffType.QMANAGER_OTHER);

            moderationRequestEntryPoint.requestRequiredModeration(context);
            skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.CPC);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.CPC);
            confirmModerationSandboxFeedLoad(datasourceID, ShopProgram.CPC);

            TestingState state = testingService.getTestingStatus(datasourceID, ShopProgram.CPC);

            // Check what datasource_in_testing record linked to correct cutoff
            assertThat(state.getCutoffId())
                    .isEqualTo(cutoff.getId());

            List<ShopForLiteCheck> shopsForLiteCheck = new ArrayList<>();
            shopsModeratedSupplier.getLightCheckReadyShops((shopId, type, subject, message) ->
                    shopsForLiteCheck.add(new ShopForLiteCheck(shopId, type))
            );

            // Check what what API for ABO return correct values
            assertThat(shopsForLiteCheck)
                    .usingFieldByFieldElementComparator()
                    .contains(
                            new ShopForLiteCheck(datasourceID, TestingType.CPC_LITE_CHECK)
                    );
        });
    }

    /**
     * Тест на открытие COMMON_OTHER во время прохождения другой модерации.
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * <li>Открываем COMMON_OTHER, запускаем проверку
     * <li>Открываем QMANAGER_OTHER, запускам проверку
     * <li>Завершаем GENERAL-проверку
     * <li>Завершаем CPC-проверку
     * </ul>
     * Проверяем, что CPC остаётся выключенным, пока не пройдут обе проверки
     */
    @Test
    void generalAndProgramSpecificUnsynchronized_G_P_G_P_CPCOnly() {
        generalAndProgramSpecificUnsynchronized_G_P_G_P(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER));
    }

    /**
     * Тест на открытие COMMON_OTHER во время прохождения другой модерации.
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * <li>Открываем COMMON_OTHER, запускаем проверку
     * <li>Открываем QMANAGER_OTHER, запускам проверку
     * <li>Завершаем CPC-проверку
     * <li>Завершаем GENERAL-проверку
     * </ul>
     * Проверяем, что CPC остаётся выключенным, пока не пройдут обе проверки
     */
    @Test
    void generalAndProgramSpecificUnsynchronized_G_P_P_G_CPCOnly() {
        generalAndProgramSpecificUnsynchronized_G_P_P_G(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER));
    }

    /**
     * Тест на открытие COMMON_OTHER во время прохождения другой модерации.
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * <li>Открываем QMANAGER_OTHER, запускам проверку
     * <li>Открываем COMMON_OTHER, запускаем проверку
     * <li>Завершаем CPC-проверку
     * <li>Завершаем GENERAL-проверку
     * </ul>
     * Проверяем, что CPC остаётся выключенным, пока не пройдут обе проверки
     */
    @Test
    void generalAndProgramSpecificUnsynchronized_P_G_P_G_CPCOnly() {
        generalAndProgramSpecificUnsynchronized_P_G_P_G(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER));
    }

    /**
     * Тест на открытие COMMON_OTHER во время прохождения другой модерации.
     * Подготовка.
     * <ul>
     * <li>Создаём CPC-only магазин
     * <li>Открываем QMANAGER_OTHER, запускам проверку
     * <li>Открываем COMMON_OTHER, запускаем проверку
     * <li>Завершаем GENERAL-проверку
     * <li>Завершаем CPC-проверку
     * </ul>
     * Проверяем, что CPC остаётся выключенным, пока не пройдут обе проверки
     */
    @Test
    void generalAndProgramSpecificUnsynchronized_P_G_G_P_CPCOnly() {
        generalAndProgramSpecificUnsynchronized_P_G_G_P(
                Set.of(ShopProgram.CPC),
                Set.of(CutoffType.QMANAGER_OTHER));
    }

    /**
     * Начинаем GENERAL-проверку,
     * Потом начинаем CPC/CPA-проверку
     * Потом заканчиваем GENERAL-проверку
     * Потом заканчиваем CPC/CPA-проверку
     */
    private void generalAndProgramSpecificUnsynchronized_G_P_G_P(
            Set<ShopProgram> programs,
            Set<CutoffType> programSpecificCutoffs
    ) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            openCutoffsAndStartModeration(context,
                    Set.of(CutoffType.COMMON_OTHER),
                    Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            openCutoffsAndStartModeration(context, programSpecificCutoffs, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    /**
     * Начинаем GENERAL-проверку,
     * Потом начинаем CPC/CPA-проверку
     * Потом заканчиваем CPC/CPA-проверку
     * Потом заканчиваем GENERAL-проверку
     */
    private void generalAndProgramSpecificUnsynchronized_G_P_P_G(
            Set<ShopProgram> programs,
            Set<CutoffType> programSpecificCutoffs
    ) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            openCutoffsAndStartModeration(context,
                    Set.of(CutoffType.COMMON_OTHER),
                    Set.of(ShopProgram.GENERAL)
            );
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            openCutoffsAndStartModeration(context, programSpecificCutoffs, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    /**
     * Начинаем CPC/CPA-проверку
     * Потом начинаем GENERAL-проверку,
     * Потом заканчиваем CPC/CPA-проверку
     * Потом заканчиваем GENERAL-проверку
     */
    private void generalAndProgramSpecificUnsynchronized_P_G_P_G(
            Set<ShopProgram> programs,
            Set<CutoffType> programSpecificCutoffs
    ) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            openCutoffsAndStartModeration(context, programSpecificCutoffs, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            openCutoffsAndStartModeration(context,
                    Set.of(CutoffType.COMMON_OTHER),
                    Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    /**
     * Начинаем CPC/CPA-проверку
     * Потом начинаем GENERAL-проверку,
     * Потом заканчиваем GENERAL-проверку
     * Потом заканчиваем CPC/CPA-проверку
     */
    private void generalAndProgramSpecificUnsynchronized_P_G_G_P(
            Set<ShopProgram> programs,
            Set<CutoffType> programSpecificCutoffs
    ) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            openCutoffsAndStartModeration(context, programSpecificCutoffs, programs);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            openCutoffsAndStartModeration(context,
                    Set.of(CutoffType.COMMON_OTHER),
                    Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, Set.of(ShopProgram.GENERAL));
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            finishStartedModeration(context, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    private void loneGeneralLightCheck(Set<ShopProgram> programs) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.COMMON_OTHER);
            assertThatAllProgramsAreCuttedOff(datasourceID, programs);

            runBeforeEachStep(
                    () -> assertThatShopHasSingleCutoffForEachProgram(datasourceID, CutoffType.COMMON_OTHER, programs),
                    List.of(
                            () -> moderationRequestEntryPoint.requestRequiredModeration(context),
                            () -> skipModerationDelayAndConfirmModerationRequest(datasourceID, ShopProgram.GENERAL),
                            () -> skipModerationDelayAndStartMainModerationProcess(datasourceID, ShopProgram.GENERAL),
                            () -> confirmModerationSandboxFeedLoad(datasourceID, ShopProgram.GENERAL),
                            () -> submitModerationResult(datasourceID, ShopProgram.GENERAL, ModerationResult.PASSED),
                            () -> confirmModerationSandboxFeedLoad(datasourceID, ShopProgram.GENERAL),
                            () -> finishPassedModeration(datasourceID, ShopProgram.GENERAL)
                    ));
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    private void generalAndProgramSpecificCheckSimultaneously(
            Set<ShopProgram> programs,
            Set<CutoffType> programSpecificCutoffs,
            boolean generalFirst
    ) {
        withinAction(actionId -> {
            long datasourceID = createDatasource(actionId, programs);
            assertThatAllProgramsAreOn(datasourceID, programs);

            ShopActionContext context = new ShopActionContext(actionId, datasourceID);
            cutoffService.openCutoff(context, CutoffType.COMMON_OTHER);
            for (CutoffType cutoffType : programSpecificCutoffs) {
                cutoffService.openCutoff(context, cutoffType);
            }

            assertThatAllProgramsAreCuttedOff(datasourceID, programs);
            moderationRequestEntryPoint.requestRequiredModeration(context);
            List<Set<ShopProgram>> checkOrder =
                    generalFirst
                            ? List.of(Set.of(ShopProgram.GENERAL), programs)
                            : List.of(programs, Set.of(ShopProgram.GENERAL));
            for (Set<ShopProgram> programsToCheck : checkOrder) {
                assertThatAllProgramsAreCuttedOff(datasourceID, programs);
                passAlreadyStartedModeration(context, programsToCheck);
            }
            assertThatAllProgramsAreOn(datasourceID, programs);
        });
    }

    private void openCutoffsAndStartModeration(ShopActionContext context,
                                               Set<CutoffType> cutoffTypes,
                                               Set<ShopProgram> programs) {
        long datasourceID = context.getShopId();
        for (CutoffType cutoffType : cutoffTypes) {
            cutoffService.openCutoff(context, cutoffType);
        }

        moderationRequestEntryPoint.requestRequiredModeration(context);
        for (ShopProgram program : programs) {
            skipModerationDelayAndConfirmModerationRequest(datasourceID, program);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, program);
            confirmModerationSandboxFeedLoad(datasourceID, program);
        }
    }

    private void finishStartedModeration(ShopActionContext context, Set<ShopProgram> programs) {
        long datasourceID = context.getShopId();
        for (ShopProgram program : programs) {
            submitModerationResult(datasourceID, program, ModerationResult.PASSED);
            confirmModerationSandboxFeedLoad(datasourceID, program);
            finishPassedModeration(datasourceID, program);
        }
    }

    private void assertThatShopHasSingleCutoffForEachProgram(
            long datasourceID,
            CutoffType cutoffType,
            Set<ShopProgram> programs
    ) {
        for (ShopProgram program : programs) {
            Set<CutoffType> allCutoffs = getCutoffs(datasourceID, program);
            assertThat(allCutoffs).singleElement().isEqualTo(cutoffType);
        }
    }

    @Nonnull
    private Set<CutoffType> getCutoffs(long datasourceID, ShopProgram program) {
        return getAllCutoffs(datasourceID).stream()
                .filter(c -> c != CutoffType.CPA_GENERAL)
                .filter(c -> c.cutsOff(program))
                .collect(Collectors.toSet());
    }

    private static class ShopForLiteCheck {
        private long shopId;
        private TestingType type;

        public ShopForLiteCheck(long shopId, TestingType type) {
            this.shopId = shopId;
            this.type = type;
        }

        public long getShopId() {
            return shopId;
        }

        public TestingType getType() {
            return type;
        }
    }
}
