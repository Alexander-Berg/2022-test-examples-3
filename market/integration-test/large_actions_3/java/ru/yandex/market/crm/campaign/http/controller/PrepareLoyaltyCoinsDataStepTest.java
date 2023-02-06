package ru.yandex.market.crm.campaign.http.controller;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.services.actions.steps.loyalty.PreparingCoinsDataType;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.core.domain.coins.CoinApi;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.campaign.services.actions.steps.loyalty.PreparingCoinsDataType.DELIVERY_COINS_ISSUED_30_DAYS_AGO;
import static ru.yandex.market.crm.campaign.services.actions.steps.loyalty.PreparingCoinsDataType.DELIVERY_COINS_ISSUED_60_DAYS_AGO;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.prepareLoyaltyCoinsData;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.saveNotifiedUsersCoinsStep;
import static ru.yandex.market.crm.campaign.test.utils.LoyaltyTestHelper.buildActiveAuthCoinRow;

public class PrepareLoyaltyCoinsDataStepTest extends AbstractControllerLargeTest {
    private static final long PUID_1 = 1;
    private static final long PUID_2 = 2;
    private static final long PUID_3 = 3;

    private static final long COIN_ID_1 = 1;
    private static final long COIN_ID_2 = 2;
    private static final long COIN_ID_3 = 3;
    private static final long COIN_ID_4 = 4;
    private static final long COIN_ID_5 = 5;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private CrmYtTables crmYtTables;

    @Inject
    private YtClient ytClient;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareLoyaltyActiveAuthCoinsTable();
    }

    /**
     * Проверка корректной подготовки данных по монетам для события выдачи монет
     */
    @Test
    public void testCorrectPreparingCoinsDataForCoinsIssue() throws Exception {
        ActionStep step = prepareLoyaltyCoinsData(PreparingCoinsDataType.COINS_ISSUE);
        PlainAction action = prepareAction(step);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.BB3_TAG, now, now.plusDays(10)),
                buildActiveAuthCoinRow(COIN_ID_2, PUID_2, CoinApi.BB3_TAG, now.minusDays(1), now.plusDays(10)),
                buildActiveAuthCoinRow(COIN_ID_3, PUID_3, CoinApi.BB3_TAG, now.minusDays(10), now)
        );

        List<StepOutputRow> outputRows = execute(action, step);
        Set<Long> ids = outputRows.stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(PUID_1, PUID_2), ids);
    }

    /**
     * Проверка корректной подготовки данных по монетам для события сгорания монет
     */
    @Test
    public void testCorrectPreparingCoinsDataForCoinsBurning() throws Exception {
        ActionStep step = prepareLoyaltyCoinsData(PreparingCoinsDataType.COINS_BURNING);
        PlainAction action = prepareAction(step);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.USUAL_TAG, now.minusDays(10), now.plusDays(3)),
                buildActiveAuthCoinRow(COIN_ID_2, PUID_2, CoinApi.USUAL_TAG, now.minusDays(10), now.plusDays(10))
        );

        List<StepOutputRow> outputRows = execute(action, step);
        Set<Long> ids = outputRows.stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(PUID_1), ids);
    }

    /**
     * Проверка корректной подготовки данных по монетам для напоминания по давно привязанными монетам
     */
    @Test
    public void testCorrectPreparingCoinsDataForCoinsReminder() throws Exception {
        ActionStep step = prepareLoyaltyCoinsData(PreparingCoinsDataType.COINS_LONG_ATTACHED);
        PlainAction action = prepareAction(step);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.BB3_TAG, now.minusDays(6), now.plusDays(10)),
                buildActiveAuthCoinRow(COIN_ID_2, PUID_2, CoinApi.BB3_TAG, now, now.plusDays(10))
        );

        List<StepOutputRow> outputRows = execute(action, step);
        Set<Long> ids = outputRows.stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(PUID_1), ids);
    }

    /**
     * Проверка корректной подготовки данных по нескольким монетам для одного пользователя
     */
    @Test
    public void testCorrectPreparingCoinsDataForUserWithSeveralCoins() throws Exception {
        ActionStep step = prepareLoyaltyCoinsData(PreparingCoinsDataType.COINS_BURNING);
        PlainAction action = prepareAction(step);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.USUAL_TAG, now.minusDays(10), now.plusDays(3)),
                buildActiveAuthCoinRow(COIN_ID_2, PUID_1, CoinApi.BB3_TAG, now.minusDays(10), now.plusDays(3)),
                buildActiveAuthCoinRow(COIN_ID_3, PUID_1, CoinApi.USUAL_TAG, now.minusDays(10), now.plusDays(10))
        );

        List<StepOutputRow> outputRows = execute(action, step);
        Set<Long> ids = outputRows.stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(PUID_1), ids);

        Optional<Map<String, YTreeNode>> vars = outputRows.stream()
                .map(StepOutputRow::getData)
                .map(StepOutputRow.Data::getVars)
                .findFirst();

        Assertions.assertTrue(vars.isPresent());
        Set<Long> coinsIds = vars.get()
                .get("coins")
                .asList()
                .stream()
                .map(v -> v.asMap().get("id").longValue())
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(COIN_ID_1, COIN_ID_2), coinsIds);
    }

    /**
     * Если пользователям по их монетам уже были отпрвлены уведомления,
     * то при повторном запуске акции отправка уведомлений не происходит
     */
    @Test
    public void testCorrectPreparingCoinsDataWithNotifiedUsersCoins() throws Exception {
        ActionStep prepareLoyaltyCoinsDataStep = prepareLoyaltyCoinsData(PreparingCoinsDataType.COINS_ISSUE);
        ActionStep saveNotifiedUsersCoinsStep = saveNotifiedUsersCoinsStep();
        PlainAction action = prepareAction(prepareLoyaltyCoinsDataStep, saveNotifiedUsersCoinsStep);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.BB3_TAG, now, now.plusDays(10)),
                buildActiveAuthCoinRow(COIN_ID_2, PUID_2, CoinApi.BB3_TAG, now.minusDays(1), now.plusDays(10))
        );

        Set<Long> ids1 = actionTestHelper.execute(action, prepareLoyaltyCoinsDataStep, saveNotifiedUsersCoinsStep)
                .stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(PUID_1, PUID_2), ids1);

        Set<Long> ids2 = execute(action, prepareLoyaltyCoinsDataStep).stream()
                .map(StepOutputRow::getIdValue)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        Assertions.assertTrue(ids2.isEmpty());
    }

    /**
     * Если задан тип данных "DELIVERY_COINS_ISSUED_30_DAYS_AGO" в переменные включаются только
     * бонусы с тэгом "welcome_delivery_coin_2020", выданные от 30-35 дней назад.
     */
    @Test
    public void testPrepareDeliveryCoinsIssued30DaysAgo() throws Exception {
        ActionStep prepareLoyaltyCoinsDataStep = prepareLoyaltyCoinsData(DELIVERY_COINS_ISSUED_30_DAYS_AGO);
        PlainAction action = prepareAction(prepareLoyaltyCoinsDataStep);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_1, CoinApi.BB3_TAG, now.minusDays(30)), // Пройдет
                buildActiveAuthCoinRow(COIN_ID_2, PUID_2, CoinApi.BB3_TAG, now.minusDays(35)), // Пройдет
                buildActiveAuthCoinRow(COIN_ID_3, PUID_2, CoinApi.BB3_TAG, now.minusDays(15)), // Не пройдет. Слишком
                // новый
                buildActiveAuthCoinRow(COIN_ID_4, PUID_1, CoinApi.USUAL_TAG, now.minusDays(31)), // Не пройдет. Не
                // тот тэг
                buildActiveAuthCoinRow(COIN_ID_5, PUID_3, CoinApi.BB3_TAG, now.minusDays(50)) // Не пройдет. Слишком
                // старый
        );

        Set<Long> coinIds = runAndGetCoinIds(action, prepareLoyaltyCoinsDataStep);
        Assertions.assertEquals(Set.of(COIN_ID_1, COIN_ID_2), coinIds);
    }

    /**
     * Если задан тип данных "DELIVERY_COINS_ISSUED_60_DAYS_AGO" в переменные включаются только
     * бонусы с тэгом "welcome_delivery_coin_2020", выданные от 60-65 дней назад.
     */
    @Test
    public void testPrepareDeliveryCoinsIssued60DaysAgo() throws Exception {
        ActionStep prepareLoyaltyCoinsDataStep = prepareLoyaltyCoinsData(DELIVERY_COINS_ISSUED_60_DAYS_AGO);
        PlainAction action = prepareAction(prepareLoyaltyCoinsDataStep);

        ZonedDateTime now = ZonedDateTime.now();

        prepareCoins(
                buildActiveAuthCoinRow(COIN_ID_1, PUID_2, CoinApi.BB3_TAG, now.minusDays(50)), // Не пройдет. Слишком
                // новый
                buildActiveAuthCoinRow(COIN_ID_2, PUID_1, CoinApi.USUAL_TAG, now.minusDays(62)), // Не пройдет. Не
                // тот тэг
                buildActiveAuthCoinRow(COIN_ID_3, PUID_3, CoinApi.BB3_TAG, now.minusDays(70)), // Не пройдет. Слишком
                // старый
                buildActiveAuthCoinRow(COIN_ID_4, PUID_1, CoinApi.BB3_TAG, now.minusDays(60)), // Пройдет
                buildActiveAuthCoinRow(COIN_ID_5, PUID_2, CoinApi.BB3_TAG, now.minusDays(65)) // Пройдет
        );

        Set<Long> coinIds = runAndGetCoinIds(action, prepareLoyaltyCoinsDataStep);
        Assertions.assertEquals(Set.of(COIN_ID_4, COIN_ID_5), coinIds);
    }

    @NotNull
    private Set<Long> runAndGetCoinIds(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step).stream()
                .flatMap(row -> row.getData().getVars().get("coins").asList().stream())
                .map(YTreeNode::mapNode)
                .map(coin -> coin.getLong("id"))
                .collect(Collectors.toSet());
    }

    private PlainAction prepareAction(ActionStep... steps) {
        PlainAction action = actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
        actionTestHelper.prepareSegmentationResult(action.getId(), Collections.emptyList());
        return action;
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    private void prepareCoins(YTreeMapNode... row) {
        ytClient.write(crmYtTables.getLoyaltyActiveAuthCoinsTable(), List.of(row));
    }
}
