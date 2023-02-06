package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOAL_TIME;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOAL_TIME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ALLOWED_CAMPAIGN_TYPES_FOR_SHORTCUTS;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_LAL_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_ABANDONED_CART_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_PURCHASE_LAL_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_NAME;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.SHORTCUT_DEFAULT_ID_BY_NAME;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionShortcutServiceTest {
    @Autowired
    protected RetargetingConditionShortcutService retargetingConditionShortcutService;

    private final ClientId clientId = ClientId.fromLong(1L);

    private final List<Goal> goals = List.of((Goal) new Goal()
            .withId(5L)
            .withTime(540));

    @Test
    public void checkConstantsTest() {
        assertThat("Шорткаты разрешены только для ожидаемых типов кампаний",
                List.of(CampaignType.TEXT),
                containsInAnyOrder(ALLOWED_CAMPAIGN_TYPES_FOR_SHORTCUTS.toArray()));

        // DIRECT-150327
        assertThat("Дефолтные id шорткатов находятся в ожидаемом интервале",
                RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS,
                everyItem(allOf(
                        greaterThan(11746L),
                        lessThan(41743L))));

        assertThat("Для всех шорткатов прописано соответствие дефолтного названия и id",
                RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS,
                hasSize(SHORTCUT_DEFAULT_ID_BY_NAME.size()));
    }

    @Test
    public void checkShortcutNamesTest() {
        var allShortcutCurrentNames = List.of(
                NOT_BOUNCE_SHORTCUT_NAME,
                NOT_BOUNCE_LAL_SHORTCUT_NAME,
                CAMPAIGN_GOALS_SHORTCUT_NAME,
                CAMPAIGN_GOALS_LAL_SHORTCUT_NAME,
                ECOM_PURCHASE_LAL_SHORTCUT_NAME,
                ECOM_ABANDONED_CART_SHORTCUT_NAME,
                ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_NAME);

        assertThat("Проверяются имена всех используемых шорткатов",
                allShortcutCurrentNames,
                hasSize(RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS.size()));

        assertThat("Шорткаты имеют ожидаемые названия",
                allShortcutCurrentNames,
                equalTo(List.of("Посетители",
                        "Похожие на посетителей",
                        "Достигли целей кампании",
                        "Похожие на достигнувших целей кампании",
                        "Похожие на покупателей",
                        "Брошенные корзины",
                        "Смотрели товары, но не купили")));

        // Если этот тест упал, значит, у шортката было изменено название.
        // Нужно убедиться, что устаревшее название добавлено в OBSOLETE_SHORTCUT_NAME_TO_CURRENT_NAME и обновить тест.
    }

    @Test
    public void checkCalculateGoalTime() {
        Integer time = retargetingConditionShortcutService.calculateGoalTime(CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID);
        assertThat("Период нацеливания цели условия ретаргетинга всегда в интервале от " + MIN_GOAL_TIME +
                        " до " + MAX_GOAL_TIME + " дней",
                time,
                allOf(
                        greaterThanOrEqualTo(MIN_GOAL_TIME),
                        lessThanOrEqualTo(MAX_GOAL_TIME)));
    }

    @Test
    public void testGetAvailableTruncatedRetargetingConditionShortcuts_Empty() {
        var shortcuts = retargetingConditionShortcutService.getAvailableTruncatedRetargetingConditionShortcuts(clientId,
                false, false, false, false, false);

        assertThat("При отсутствии подходящих целей/сегментов возвращается пустой список шорткатов",
                shortcuts, hasSize(0));
    }

    @Test
    public void testGetAvailableTruncatedRetargetingConditionShortcuts_NotBounceSegmentsAvailable() {
        testGetAvailableTruncatedRetargetingConditionShortcuts("Доступны только шорткаты по сегментам Неотказы",
                true, false, false, false, false, 2,
                List.of(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID));
    }

    @Test
    public void testGetAvailableTruncatedRetargetingConditionShortcuts_CampaignGoalsAvailable() {
        testGetAvailableTruncatedRetargetingConditionShortcuts("Доступны только шорткаты по целям Кампании",
                false, true, false, false, false, 2,
                List.of(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID, CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID));
    }

    @Test
    public void testGetAvailableTruncatedRetargetingConditionShortcuts_EcommerceCountersAvailable() {
        testGetAvailableTruncatedRetargetingConditionShortcuts("Доступны только ecom-шорткаты",
                false, false, true, true, true, 3,
                List.of(ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID, ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID,
                        ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID));
    }

    @Test
    public void testGetAvailableTruncatedRetargetingConditionShortcuts_AllGoalsAvailable() {
        testGetAvailableTruncatedRetargetingConditionShortcuts("Доступны все шорткаты",
                true, true, true, true, true, 7,
                List.of(ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID,
                        ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID,
                        ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID,
                        NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID,
                        CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID, CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID));
    }

    private void testGetAvailableTruncatedRetargetingConditionShortcuts(String testName,
                                                                        Boolean isNotBounceShortcutsAvailable,
                                                                        Boolean isCampaignGoalsAvailable,
                                                                        Boolean isEcomPurchaseSegmentsAvailable,
                                                                        Boolean isEcomAbandonedCartSegmentsAvailable,
                                                                        Boolean isEcomViewedWithoutPurchaseSegmentsAvailable,
                                                                        int expectedShortcutsSize,
                                                                        List<Long> expectedIds) {
        var shortcuts = retargetingConditionShortcutService.getAvailableTruncatedRetargetingConditionShortcuts(clientId,
                isNotBounceShortcutsAvailable, isCampaignGoalsAvailable, isEcomPurchaseSegmentsAvailable,
                isEcomAbandonedCartSegmentsAvailable, isEcomViewedWithoutPurchaseSegmentsAvailable);

        assertThat(testName,
                mapList(shortcuts, RetargetingConditionBase::getId),
                contains(expectedIds.toArray()));
        testRetargetingConditionShortcuts(testName, shortcuts, expectedShortcutsSize, 0);
    }

    @Test
    public void testGetRetargetingConditionShortcuts_Empty() {
        testGetRetargetingConditionShortcuts(
                "При отсутствии подходящих целей возвращается пустой список шорткатов",
                null, null, null, null, null, null, null, 0, List.of());
    }

    @Test
    public void testGetRetargetingConditionShortcuts_SomeShortcuts() {
        testGetRetargetingConditionShortcuts(
                "Возвращается часть шорткатов",
                goals, null, goals, null, null, null, null, 2,
                List.of(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID));
    }

    @Test
    public void testGetRetargetingConditionShortcuts_AllShortcuts() {
        testGetRetargetingConditionShortcuts(
                "Возвращаются все шорткаты",
                goals, goals, goals, goals, goals, goals, goals, 7,
                List.of(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID,
                        CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID, CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID,
                        ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID, ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID,
                        ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID));
    }

    private void testGetRetargetingConditionShortcuts(String testName,
                                                      @Nullable List<Goal> notBounceSegments,
                                                      @Nullable List<Goal> notBounceLalSegments,
                                                      @Nullable List<Goal> campaignGoals,
                                                      @Nullable List<Goal> campaignLalGoals,
                                                      @Nullable List<Goal> ecomPurchaseLalSegments,
                                                      @Nullable List<Goal> ecomAbandonedCartSegments,
                                                      @Nullable List<Goal> ecomViewedWithoutPurchaseSegments,
                                                      int expectedShortcutsSize,
                                                      List<Long> expectedIds) {
        var shortcutsById = retargetingConditionShortcutService.getRetargetingConditionShortcuts(clientId,
                notBounceSegments, notBounceLalSegments, campaignGoals, campaignLalGoals,
                ecomPurchaseLalSegments, ecomAbandonedCartSegments, ecomViewedWithoutPurchaseSegments);

        assertThat(testName,
                shortcutsById.keySet(),
                containsInAnyOrder(expectedIds.toArray()));
        testRetargetingConditionShortcuts(testName, shortcutsById.values(), expectedShortcutsSize, 1);
    }

    private void testRetargetingConditionShortcuts(String testName,
                                                   Collection<RetargetingCondition> shortcuts,
                                                   int expectedShortcutsSize,
                                                   int expectedRulesSize) {
        assertThat(testName, shortcuts, hasSize(expectedShortcutsSize));
        assertThat(testName,
                shortcuts,
                everyItem(
                        allOf(
                                hasProperty("clientId", equalTo(clientId.asLong())),
                                hasProperty("type", equalTo(ConditionType.shortcuts)),
                                hasProperty("available", equalTo(Boolean.TRUE)),
                                hasProperty("interest", equalTo(Boolean.FALSE)),
                                hasProperty("deleted", equalTo(Boolean.FALSE)),
                                hasProperty("autoRetargeting", equalTo(Boolean.FALSE)),
                                hasProperty("rules", hasSize(expectedRulesSize)))));
    }

    @Test
    public void testGetRetargetingConditionShortcuts_MaxGoalsPerRule() {
        var goalsOverLimit = Collections.nCopies(MAX_GOALS_PER_RULE + 50, (Goal) new Goal()
                .withId(10L)
                .withTime(540));
        var shortcutsById = retargetingConditionShortcutService.getRetargetingConditionShortcuts(clientId,
                null, null, goalsOverLimit, null, null, null, null);
        shortcutsById.get(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID).collectGoals();
        assertThat("Число целей в правиле шортката не превышает лимит MAX_GOALS_PER_RULE",
                shortcutsById.get(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID).collectGoals(), hasSize(MAX_GOALS_PER_RULE));
    }
}
