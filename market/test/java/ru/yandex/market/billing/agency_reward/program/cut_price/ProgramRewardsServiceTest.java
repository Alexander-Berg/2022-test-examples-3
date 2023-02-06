package ru.yandex.market.billing.agency_reward.program.cut_price;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.program.ProgramRewardsService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtil.PING_ANCESTOR_TRANSACTION;
import static ru.yandex.market.mbi.yt.YtUtilTest.intNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.treeMapNode;
import static ru.yandex.market.mbi.yt.matchers.JsonNodeMatchers.hasIntValue;
import static ru.yandex.market.mbi.yt.matchers.JsonNodeMatchers.hasStrValue;

/**
 * Суровые тесты для {@link ProgramRewardsService}.
 * Виду того что h2 базу не подложить с пользой, то куча моков и тд.
 * Задача - проверить целостно весь контур от фетча до сохранения в yt.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class ProgramRewardsServiceTest extends FunctionalTest {
    private static final LocalDate LD_2019_01_10 = LocalDate.of(2019, 1, 10);
    private static final GUID TR_GUID = new GUID(33, 55);

    @Autowired
    private ProgramRewardsService cutPriceRewardsService;

    @Autowired
    private Yt yt;

    @Autowired
    private Cypress cypress;

    @Value("${mbi.billing.agency-reward.program.daily-draft.yt.path}")
    private String ytPathRewardsDailyDraft;

    @Value("${mbi.billing.agency-reward.program.monthly.yt.path}")
    private String ytPathRewardsMonthly;

    @Value("${mbi.billing.agency-reward.agency-info.yt.path}")
    private String ytPathRewardsAgencyInfo;

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Captor
    private ArgumentCaptor<IteratorF> nodesCaptor;

    private static List<YTreeMapNode> prepareAgencyInfo() {
        return ImmutableList.of(
                treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                        .put("agency_id", intNode(1))
                        .put("contract_id", intNode(11))
                        .put("contract_eid", stringNode("11/11"))
                        .put("scale_code", intNode(12))
                        .put("contract_dt", stringNode("2019-01-01"))
                        .put("contract_end_dt", stringNode("2019-10-10"))
                        .put("program_sign_status", intNode(1))
                        .build()
                ),
                treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                        .put("agency_id", intNode(2))
                        .put("contract_id", intNode(22))
                        .put("contract_eid", stringNode("22/22"))
                        .put("scale_code", intNode(13))
                        .put("contract_dt", stringNode("2019-01-01"))
                        .put("contract_end_dt", stringNode("2019-10-10"))
                        .put("program_sign_status", intNode(1))
                        .build()
                ),
                treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                        .put("agency_id", intNode(3))
                        .put("contract_id", intNode(33))
                        .put("contract_eid", stringNode("33/33"))
                        .put("scale_code", intNode(0))
                        .put("contract_dt", stringNode("2019-01-01"))
                        .put("contract_end_dt", stringNode("2019-10-10"))
                        .put("program_sign_status", intNode(1))
                        .build()
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        when(yt.cypress())
                .thenReturn(cypress);

        when(yt.tables())
                .thenReturn(ytTables);

        when(yt.transactions())
                .thenReturn(ytTransactions);

        when(ytTransactions.startAndGet(any()))
                .thenReturn(transaction);

        when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);

        when(transaction.getId())
                .thenReturn(TR_GUID);
    }

    private void prepareTableRead(YPath yPath, List<YTreeMapNode> nodes) {
        doAnswer(invocation -> {
                    final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                    nodes.forEach(consumer);
                    return null;
                }
        ).when(ytTables)
                .read(eq(yPath),
                        any(YTableEntryType.class),
                        any(Consumer.class)
                );
    }

    @DisplayName("Ежедневный draft расчет и публикация")
    @Test
    @DbUnitDataSet(before = {"ProgramRewardsServiceTest.before.csv", "ProgramRewardsServiceTest.missing_info_allowed.before.csv"})
    void test_calcDailyDraftRewardAndPublish() {
        testCalc(
                ytPathRewardsDailyDraft + "/%s/2019-01-10",
                () -> cutPriceRewardsService.calcDailyDraftRewardAndPublish(LD_2019_01_10)
        );
    }

    @DisplayName("Ежемесячный расчет и публикация")
    @Test
    @DbUnitDataSet(before = {"ProgramRewardsServiceTest.before.csv", "ProgramRewardsServiceTest.missing_info_allowed.before.csv"})
    void test_calcMonthlyRewardAndPublish() {
        testCalc(
                ytPathRewardsMonthly + "/%s/201901",
                () -> cutPriceRewardsService.calcMonthlyRewardAndPublish(LD_2019_01_10)
        );
    }

    @DisplayName("Ошибка, если есть счетчики к начислению, но отсутствует информация по агентству из баланса")
    @Test
    @DbUnitDataSet(before = "ProgramRewardsServiceTest.before.csv")
    void test_calcMonthlyRewardAndPublish_error_when_missingAgencyInfo() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> testCalc(
                        ytPathRewardsMonthly,
                        () -> cutPriceRewardsService.calcMonthlyRewardAndPublish(LD_2019_01_10)
                )
        );

        assertThat(ex.getMessage(), is("Missing agency_info for agency_client_id=3"));
    }

    @DisplayName("Отключение ежемесячного расчета и публикации")
    @Test
    @DbUnitDataSet(before = "ProgramRewardsServiceTest.disabled_monthly.before.csv")
    void test_calcMonthlyRewardAndPublish_blocked() {
        cutPriceRewardsService.calcMonthlyRewardAndPublish(LD_2019_01_10);

        Mockito.verifyZeroInteractions(yt);
    }

    @DisplayName("Перегруженная дата для загрузки информации по агентставм из баланса")
    @Test
    @DbUnitDataSet(before = {"ProgramRewardsServiceTest.overloaded_loadAgencyInfoDate.before.csv", "ProgramRewardsServiceTest.missing_info_allowed.before.csv"})
    void test_calcMonthlyRewardAndPublish_overloadedLoadAgencyDate() {
        cutPriceRewardsService.calcMonthlyRewardAndPublish(LD_2019_01_10);

        YPath agencyInfoPath = YPath.simple(ytPathRewardsAgencyInfo + "/2019-01-01");
        Mockito.verify(ytTables).read(eq(agencyInfoPath), eq(YTableEntryTypes.YSON), any(Consumer.class));
    }

    private void testCalc(String resultsPath, Runnable runnable) {
        //готовим данные
        YPath draftSubclientPath = YPath.simple(String.format(resultsPath, "cut_price_detailed"));
        YPath draftAgencyPath = YPath.simple(String.format(resultsPath, "by_agency"));
        YPath agencyInfoPath = YPath.simple(ytPathRewardsAgencyInfo + "/2019-01-10");

        when(cypress.exists(draftAgencyPath))
                .thenReturn(true);
        when(cypress.exists(draftSubclientPath))
                .thenReturn(true);

        prepareTableRead(agencyInfoPath, prepareAgencyInfo());

        // вызов
        runnable.run();

        // проверяем
        Mockito.verify(cypress)
                .exists(eq(draftSubclientPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(PING_ANCESTOR_TRANSACTION), eq(draftSubclientPath));

        Mockito.verify(cypress)
                .exists(eq(draftAgencyPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(PING_ANCESTOR_TRANSACTION), eq(draftAgencyPath));


        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(draftAgencyPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        // проверяем опубликованные записи в разререс по агентствам
        List<JsonNode> agencyRecords = (List<JsonNode>) nodesCaptor.getValue().collect(Collectors.toList());
        assertThat(agencyRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("contract_id", 11),
                                hasStrValue("contract_eid", "11/11"),
                                hasIntValue("amount_kop", 23_000_00),
                                hasStrValue("program_type", "cut_price")
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("contract_id", 22),
                                hasStrValue("contract_eid", "22/22"),
                                hasIntValue("amount_kop", 10_000_00),
                                hasStrValue("program_type", "cut_price")
                        )
                )
        );

        // проверяем опубликованные записи в разререс по сабклиентам
        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(draftSubclientPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> subclientsRecords = (List<JsonNode>) nodesCaptor.getValue().collect(Collectors.toList());
        assertThat(subclientsRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 1011),
                                hasIntValue("contract_id", 11),
                                hasStrValue("contract_eid", "11/11"),
                                hasIntValue("amount_kop", 11_000_00)
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 1012),
                                hasIntValue("contract_id", 11),
                                hasStrValue("contract_eid", "11/11"),
                                hasIntValue("amount_kop", 12_000_00)
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 1013),
                                hasIntValue("contract_id", 11),
                                hasStrValue("contract_eid", "11/11"),
                                hasIntValue("amount_kop", 0) // малое количество дней
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 2021),
                                hasIntValue("contract_id", 22),
                                hasStrValue("contract_eid", "22/22"),
                                hasIntValue("amount_kop", 10_000_00) // упирается в кап
                        )
                )
        );
    }

}
