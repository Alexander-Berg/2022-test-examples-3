package ru.yandex.market.billing.agency_reward.program.purchase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
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
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.program.ProgramRewardsService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtil.PING_ANCESTOR_TRANSACTION;
import static ru.yandex.market.mbi.yt.matchers.JsonNodeMatchers.hasIntValue;
import static ru.yandex.market.mbi.yt.matchers.JsonNodeMatchers.hasStrValue;

/**
 * Тесты для {@link ProgramRewardsService}.
 */
@ExtendWith(MockitoExtension.class)
public class PurchaseProgramRewardsServiceTest extends FunctionalTest {
    private static final LocalDate LD_2019_01_10 = LocalDate.of(2019, 1, 10);
    private static final GUID TR_GUID = new GUID(33, 55);

    @Autowired
    private ProgramRewardsService purchaseRewardsService;

    @Autowired
    private ProgramRewardsService purchaseRewardsPreliminaryService;

    @Autowired
    private Yt yt;

    @Autowired
    private Cypress cypress;

    @Value("${mbi.billing.agency-reward.purchase-program.daily-draft.yt.path}")
    private String ytPathRewardsDailyDraft;

    @Value("${mbi.billing.agency-reward.purchase-program.monthly.yt.path}")
    private String ytPathRewardsMonthly;

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Captor
    private ArgumentCaptor<IteratorF> nodesCaptor;

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

    @DisplayName("Ежедневный draft расчет и публикация")
    @Test
    @DbUnitDataSet(before = "PurchaseProgramRewardsServiceTest.before.csv")
    void test_calcDailyDraftRewardAndPublish() {
        testCalc(
                ytPathRewardsDailyDraft + "/%s/2019-01-10",
                () -> purchaseRewardsService.calcDailyDraftRewardAndPublish(LD_2019_01_10)
        );
    }

    @DisplayName("Ежемесячный расчет и публикация")
    @Test
    @DbUnitDataSet(
            before = "PurchaseProgramRewardsServiceTest.before.csv",
            after = "PurchaseProgramRewardsServiceTest.after.csv")
    void test_calcMonthlyRewardAndPublish() {
        testCalc(
                ytPathRewardsMonthly + "/%s/201901",
                () -> purchaseRewardsService.calcMonthlyRewardAndPublish(LD_2019_01_10)
        );
    }

    @DisplayName("Предварительный Ежемесячный расчет и публикация (без проставления признака онбординг премии)")
    @Test
    @DbUnitDataSet(
            before = "PurchaseProgramRewardsServiceTest.before.csv",
            after = "PurchaseProgramRewardsServiceTest.onBoardingFalse.after.csv")
    void test_calcPreliminaryMonthlyRewardAndPublish() {
        testCalc(
                ytPathRewardsMonthly + "/%s/201901",
                () -> purchaseRewardsPreliminaryService.calcMonthlyRewardAndPublish(LD_2019_01_10)
        );
    }

    private void testCalc(String resultsPath, Runnable runnable) {
        //готовим данные
        YPath gmvDetailedPath = YPath.simple(String.format(resultsPath, "gmv_detailed"));
        YPath onBoardDetailedPath = YPath.simple(String.format(resultsPath, "on_boarding_detailed"));
        YPath draftAgencyPath = YPath.simple(String.format(resultsPath, "by_agency"));

        when(cypress.exists(draftAgencyPath))
                .thenReturn(true);
        when(cypress.exists(gmvDetailedPath))
                .thenReturn(true);
        when(cypress.exists(onBoardDetailedPath))
                .thenReturn(true);

        // вызов
        runnable.run();

        // проверяем
        Mockito.verify(cypress)
                .exists(eq(gmvDetailedPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(PING_ANCESTOR_TRANSACTION), eq(gmvDetailedPath));

        Mockito.verify(cypress)
                .exists(eq(onBoardDetailedPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(PING_ANCESTOR_TRANSACTION), eq(onBoardDetailedPath));

        Mockito.verify(cypress)
                .exists(eq(draftAgencyPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(PING_ANCESTOR_TRANSACTION), eq(draftAgencyPath));


        // проверяем опубликованные записи в разрезе по агентствам
        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(draftAgencyPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> agencyRecords = (List<JsonNode>) nodesCaptor.getValue().collect(Collectors.toList());

        assertThat(agencyRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("amount_kop", 208),
                                hasStrValue("program_type", "gmv")
                        ),
                        allOf(
                                /*
                                    Партнер 12348 был откреплен от агентства 5 и прикреплен к агентству 2,
                                    технически агентсво 5, а не 2, осуществило онбординг, но у нас награда указывается
                                    менеджером, поэтому если она указана - мы ее учитываем.
                                 */
                                hasIntValue("agency_id", 2),
                                hasIntValue("amount_kop", 3000000),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("amount_kop", 8),
                                hasStrValue("program_type", "gmv")
                        ),
                        allOf(
                                /*
                                 Агентство 3 произвело онбординг поставщика и он успел получить 10 заказов,
                                 пока работал с этим агентством, поэтому награда выплачивается,
                                 несмотря на то, что поставщик уже откреплен от агентства
                                 */
                                hasIntValue("agency_id", 3),
                                hasIntValue("amount_kop", 5000000),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                /*
                                 Агентство 4 не получает вознаграждения, т.к. поставщик не получил
                                 достаточное количество заказов при работе с ним
                                 */
                                hasIntValue("agency_id", 4),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                /*
                                Агентство 5 получает вознаграждение за онбординг,
                                несмотря на то, что поставщик уже откреплен от агентства,
                                при этом оно не мешает агентству 2 получить вознаграждение
                                за онбординг того же самого поставщика, если таково решение менеджера
                                 */
                                hasIntValue("agency_id", 5),
                                hasIntValue("amount_kop", 5000000),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                /*
                                Агентство 6 получает вознаграждение GMV, т.к.
                                были заказы до даты открепления магазина до агентства
                                 */
                                hasIntValue("agency_id", 6),
                                hasIntValue("amount_kop", 2),
                                hasStrValue("program_type", "gmv")
                        )
                )
        );

        // проверяем опубликованные записи в разрезе по сабклиентам
        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(onBoardDetailedPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> subclientsRecords = (List<JsonNode>) nodesCaptor.getValue().collect(Collectors.toList());

        assertThat(subclientsRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12344),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12345),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding"),
                                hasStrValue("partner_contract", "451545/18")
                        ),
                        //дубль, т.к. партнер был отсоединен и снова присоединен к тому же агентству
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12345),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding"),
                                hasStrValue("partner_contract", "451545/18")
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12346),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding"),
                                hasStrValue("partner_contract", "451546/19")
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12347),
                                hasIntValue("amount_kop", 3000000),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        //Дубль, т.к. партнер был отсоединен и снова присоединен к тому же агентству, выставлена
                        // нулевая выплата
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12347),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                /*
                                    Т.к. 12347 и 12348 совпадают по ОГРН, награду должен получить
                                    первый из них, т.к. он был подключет к агентству 2 раньше.
                                    Агентство 5 не должно влиять на эту расстановку.
                                 */
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12348),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 3),
                                hasIntValue("subclient_id", 12349),
                                hasIntValue("amount_kop", 5000000),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 4),
                                hasIntValue("subclient_id", 12350),
                                hasIntValue("amount_kop", 0),
                                hasStrValue("program_type", "on_boarding")
                        ),
                        allOf(
                                hasIntValue("agency_id", 5),
                                hasIntValue("subclient_id", 12348),
                                hasIntValue("amount_kop", 5000000),
                                hasStrValue("program_type", "on_boarding")
                        )
                )
        );

        // проверяем опубликованные записи в разрезе по клиентам с дополнительной детализацией по категориям
        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(gmvDetailedPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> categoryRecords = (List<JsonNode>) nodesCaptor.getValue().collect(Collectors.toList());

        assertThat(categoryRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12345),
                                hasIntValue("amount_kop", 80),
                                hasStrValue("category_id", "90403"),
                                hasStrValue("gmv", "2006"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv"),
                                hasStrValue("partner_contract", "451545/18")
                        ),
                        allOf(
                                hasIntValue("agency_id", 1),
                                hasIntValue("subclient_id", 12346),
                                hasIntValue("amount_kop", 128),
                                hasStrValue("category_id", "90403"),
                                hasStrValue("gmv", "3212"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv"),
                                hasStrValue("partner_contract", "451546/19")

                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12347),
                                hasIntValue("amount_kop", 4),
                                hasStrValue("category_id", "90403"),
                                hasStrValue("gmv", "100"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv")
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12348),
                                hasIntValue("amount_kop", 3),
                                hasStrValue("category_id", "90403"),
                                hasStrValue("gmv", "80"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv")
                        ),
                        allOf(
                                hasIntValue("agency_id", 2),
                                hasIntValue("subclient_id", 12348),
                                hasIntValue("amount_kop", 1),
                                hasStrValue("category_id", "90402"),
                                hasStrValue("gmv", "20"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv")
                        ),
                        allOf(
                                hasIntValue("agency_id", 6),
                                hasIntValue("subclient_id", 12351),
                                hasIntValue("amount_kop", 2),
                                hasStrValue("category_id", "90403"),
                                hasStrValue("gmv", "50"),
                                hasStrValue("tariff", "400"),
                                hasStrValue("program_type", "gmv")
                        )
                )
        );

    }
}
