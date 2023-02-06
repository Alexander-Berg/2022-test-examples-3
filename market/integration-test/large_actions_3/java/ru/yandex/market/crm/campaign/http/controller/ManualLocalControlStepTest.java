package ru.yandex.market.crm.campaign.http.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.Streams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.ManualLocalControlStepStatus;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.issueCoins;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.manualLocalControlStep;
import static ru.yandex.market.crm.campaign.test.utils.CoinStepTestUtils.outputRowWithCoin;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

public class ManualLocalControlStepTest extends AbstractControllerLargeTest {

    @Inject
    private ActionTestHelper actionTestHelper;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private StepsStatusDAO stepsStatusDAO;
    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.PUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.UUID, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.YUID, UserTestHelper.CRYPTA_ID);
    }

    @AfterEach
    public void tearDown() {
        LocalControlSaltModifier.setClock(Clock.system(MOSCOW_ZONE));
    }

    /**
     * Шаг ручного локального контроля корректно выделяет целевую группу из полученных на предыдущем шаге идентификаторов,
     * причем как для идентификаторов с cryptaId, так и без
     */
    @Test
    public void testCorrectnessTargetGroupSizeForIdsWithCryptaIdAndWithout() throws Exception {
        var controlPercent = 25;
        double calculationError = 5;
        var idsCount = 10000;
        var yuidsWithCryptaId = IntStream.range(0, idsCount / 2)
                .mapToObj(i -> "yuid" + i)
                .collect(Collectors.toList());
        var yuidsWithoutCryptaId = IntStream.range(idsCount / 2, idsCount)
                .mapToObj(i -> "yuid" + i)
                .collect(Collectors.toList());

        var cryptaEntries = yuidsWithCryptaId.stream()
                .map(yuid -> cryptaMatchingEntry(yuid, UserTestHelper.YUID, "cryptaId-" + yuid))
                .toArray(YTreeMapNode[]::new);

        globalSplitsTestHelper.prepareCryptaMatchingEntries(UserTestHelper.YUID, cryptaEntries);

        var issueCoins = issueCoins(123L);
        var manualLocalControlStep = manualLocalControlStep(controlPercent);
        var action = actionTestHelper.prepareAction(
                "segment_id", LinkingMode.NONE, issueCoins, manualLocalControlStep
        );

        actionTestHelper.finishSegmentation(action.getId());
        actionTestHelper.finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        var preparedRows = Streams.concat(yuidsWithCryptaId.stream(), yuidsWithoutCryptaId.stream())
                .map(yuid -> outputRowWithCoin(UidType.YUID, yuid))
                .toArray(StepOutputRow[]::new);

        actionTestHelper.prepareStepOutput(action, issueCoins, preparedRows);

        var result = actionTestHelper.execute(action, manualLocalControlStep);
        assertThat((double) result.size() / idsCount * 100)
                .isBetween(100 - controlPercent - calculationError, 100 - controlPercent + calculationError);

        var status = (ManualLocalControlStepStatus) stepsStatusDAO
                .get(action.getId(), manualLocalControlStep.getId());
        var counts = status.getCounts();
        assertEquals(counts.size(), 1);

        var yuidsCount = counts.get(UidType.YUID);
        assertNotNull(yuidsCount);
        assertEquals(result.size(), yuidsCount);
    }

    /**
     * Шаг ручного локального контроля при выделении целевой группы схлапывает идентификаторы с одним cryptaId
     */
    @Test
    public void testIdsWithSameCryptaIdPresentAsOne() throws Exception {
        var controlPercent = 25;
        double calculationError = 5;
        var usersCount = 5000;
        var users = IntStream.range(0, usersCount)
                .mapToObj(String::valueOf)
                .flatMap(i -> Stream.of(
                        Pair.of("yuid_1_for_" + i, "crypta_id_" + i),
                        Pair.of("yuid_2_for_" + i, "crypta_id_" + i)
                ))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        var cryptaEntries = users.entrySet().stream()
                .map(e -> cryptaMatchingEntry(e.getKey(), UserTestHelper.YUID, e.getValue()))
                .toArray(YTreeMapNode[]::new);

        globalSplitsTestHelper.prepareCryptaMatchingEntries(UserTestHelper.YUID, cryptaEntries);

        var issueCoins = issueCoins(123L);
        var manualLocalControlStep = manualLocalControlStep(controlPercent);
        var action = actionTestHelper.prepareAction(
                "segment_id", LinkingMode.NONE, issueCoins, manualLocalControlStep
        );

        actionTestHelper.finishSegmentation(action.getId());
        actionTestHelper.finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        var preparedRows = Arrays.stream(cryptaEntries)
                .map(e -> outputRowWithCoin(UidType.YUID, e.getString("id")))
                .toArray(StepOutputRow[]::new);

        actionTestHelper.prepareStepOutput(action, issueCoins, preparedRows);

        var result = actionTestHelper.execute(action, manualLocalControlStep);
        assertThat((double) result.size() / users.size() * 100)
                .isBetween(100 - controlPercent - calculationError, 100 - controlPercent + calculationError);

        var status = (ManualLocalControlStepStatus) stepsStatusDAO
                .get(action.getId(), manualLocalControlStep.getId());
        var counts = status.getCounts();
        assertEquals(counts.size(), 1);

        var yuidsCount = counts.get(UidType.YUID);
        assertNotNull(yuidsCount);
        assertEquals(result.size(), yuidsCount);

        result.stream()
                .collect(Collectors.groupingBy(x -> users.get(x.getIdValue()), Collectors.counting()))
                .values()
                .forEach(count -> assertEquals(2, count));
    }

    /**
     * Во время выполнения шага "Ручной локальный контроль" группа пользователей,
     * попадающих в ЛК не должна меняться в рамках одного месяца
     */
    @Test
    public void testGenerateSimilarLocalControlInOneMonth() throws Exception {
        var controlPercent = 25;
        double calculationError = 5;
        var idsCount = 5000;
        var yuids = IntStream.range(0, idsCount)
                .mapToObj(i -> "yuid" + i)
                .collect(Collectors.toList());

        var issueCoins = issueCoins(123L);
        var manualLocalControlStep = manualLocalControlStep(controlPercent);
        var action = actionTestHelper.prepareAction(
                "segment_id", LinkingMode.NONE, issueCoins, manualLocalControlStep
        );

        actionTestHelper.finishSegmentation(action.getId());
        actionTestHelper.finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);
        var preparedRows = yuids.stream()
                .map(yuid -> outputRowWithCoin(UidType.YUID, yuid))
                .toArray(StepOutputRow[]::new);

        actionTestHelper.prepareStepOutput(action, issueCoins, preparedRows);

        var clock = Clock.fixed(LocalDateTime.now().withDayOfMonth(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        var result1 = actionTestHelper.execute(action, manualLocalControlStep).stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toSet());

        assertThat((double) result1.size() / idsCount * 100)
                .isBetween(100 - controlPercent - calculationError, 100 - controlPercent + calculationError);

        var result2 = actionTestHelper.execute(action, manualLocalControlStep).stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toSet());

        assertEquals(result1, result2);
    }

    /**
     * Во время выполнения шага "Ручной локальный контроль" группа пользователей,
     * попадающих в ЛК должна меняться каждый месяц
     */
    @Test
    public void testGenerateDifferentLocalControlInDifferentMonths() throws Exception {
        var controlPercent = 25;
        double calculationError = 5;
        var idsCount = 5000;
        var yuids = IntStream.range(0, idsCount)
                .mapToObj(i -> "yuid" + i)
                .collect(Collectors.toList());

        var issueCoins = issueCoins(123L);
        var manualLocalControlStep = manualLocalControlStep(controlPercent);
        var action = actionTestHelper.prepareAction(
                "segment_id", LinkingMode.NONE, issueCoins, manualLocalControlStep
        );

        actionTestHelper.finishSegmentation(action.getId());
        actionTestHelper.finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);
        var preparedRows = yuids.stream()
                .map(yuid -> outputRowWithCoin(UidType.YUID, yuid))
                .toArray(StepOutputRow[]::new);

        actionTestHelper.prepareStepOutput(action, issueCoins, preparedRows);

        var result1 = actionTestHelper.execute(action, manualLocalControlStep).stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toSet());

        assertThat((double) result1.size() / idsCount * 100)
                .isBetween(100 - controlPercent - calculationError, 100 - controlPercent + calculationError);

        var clock = Clock.fixed(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        var result2 = actionTestHelper.execute(action, manualLocalControlStep).stream()
                .map(StepOutputRow::getIdValue)
                .collect(Collectors.toSet());

        assertThat((double) result2.size() / idsCount * 100)
                .isBetween(100 - controlPercent - calculationError, 100 - controlPercent + calculationError);

        assertNotEquals(result1, result2);
    }
}
