package ru.yandex.direct.core.entity.campaign;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.client.model.ClientAutoOverdraftInfo;
import ru.yandex.direct.currency.CurrencyCode;

import static java.math.BigDecimal.ZERO;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.AutoOverdraftUtils.calculateAutoOverdraftAddition;

@RunWith(Parameterized.class)
public class AutoOverdraftUtilsTest {

    private static final long TEST_CLIENT_ID = 100L;

    @Parameterized.Parameter(0)
    public CurrencyCode walletCurrency;

    @Parameterized.Parameter(1)
    public BigDecimal walletSum;

    @Parameterized.Parameter(2)
    public BigDecimal walletSumSpent;

    @Parameterized.Parameter(3)
    public ClientAutoOverdraftInfo clientAutoOverdraftInfo;

    @Parameterized.Parameter(4)
    public BigDecimal expectedAddition;

    @Parameterized.Parameter(5)
    public String description;

    private static ClientAutoOverdraftInfo autoOverdraftInfo(BigDecimal debt, boolean statusBalanceBanned,
                                                             BigDecimal overdraftLimit, BigDecimal autoOverdraftLimit) {
        return new ClientAutoOverdraftInfo()
                .withClientId(TEST_CLIENT_ID)
                .withDebt(debt)
                .withStatusBalanceBanned(statusBalanceBanned)
                .withOverdraftLimit(overdraftLimit)
                .withAutoOverdraftLimit(autoOverdraftLimit);
    }

    @Parameterized.Parameters(name = "walletCurrency={0}, walletSum={1}, walletSumSpent={2},"
            + " clientAutoOverdraftInfo={3}, name={5}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // ?????????????? ???????????? -- ???????????? ???????????? ???????????? ???????????????????????? ??????????????
                        BigDecimal.valueOf(17000.57),
                        "?????????? ???????????????????????????? ?????????????????????? ?? wallet.sum"
                },
                {
                        CurrencyCode.BYN, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // ?????????????? ???????????? -- ???????????? ???????????? ???????????? ???????????????????????? ??????????????
                        BigDecimal.valueOf(17000.57),
                        "?????????????????????????? ?????? BYN"
                },
                {
                        CurrencyCode.KZT, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // ?????????????? ???????????? -- ???????????? ???????????? ???????????? ???????????????????????? ??????????????
                        BigDecimal.valueOf(17000.57),
                        "?????????????????????????? ?????? KZT"
                },
                {
                        CurrencyCode.UAH, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // ???????????????????? ???????????? ???????????? ?????? ????????????, ???????????????????? ?? ????????????????????
                        BigDecimal.valueOf(0),
                        "?????????????????????????? ???????????????????? ?????? UAH"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1200),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // ???????????? ???????????????????? ?????? ???? 1200, ???? ?????????? ???????????? ???????????? 1000
                        // ???????????? ?????? "??????????????", ?? ????????????, AutoOverdraftLim ?????????? ?????? ????????????????????,
                        // ???? ???? ?????? ???????????????????? ???? 1200, ?????????????? ?? ???????????????? SUMCur ???? ????????????
                        // ?????????????????? 1200 (???????? ???????????????? ????????????, ???? ?????????????????????? "??????????????????" ????????????
                        // ?????????? ???????????????? ?????? ?????????????????????? ??????????????????). ????????????????????????????, ?????????????? ?????????? = 200.
                        BigDecimal.valueOf(200),
                        "?????????????????????????? ?????????????????????? ???? ???????????????????????????? (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1700),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // ???? ??????????-???? ?????????????? ???????????? ???????????????????? ???? 1700, ?????? ???????? ???????????? ?????? Sum+AutoOverdraftLim
                        // (????????????????, ????-???? ???????????????????? ?? ????). ???????????? ?????? "??????????????", ????????????,
                        // AutoOverdraftLim ?????????? ?????? ????????????????????. ????-???? ?????????????????????? ???????????? ?? Sum+AutoOverdraftLim
                        // (?????????? ???? ???????? ?? ?????????? ?????????????? ?? ??????????????????????????) ???????????????? 1000+500,
                        // ???????????? 200 ?????????? ?????????????? ?????? ?????????????????????? ?????????????????? (?????? ???????????????????? ?? ??????????????????)
                        BigDecimal.valueOf(500),
                        "???? ???????????????????? ?? ???? ????????????????, ?????????????????????? Sum+AutoOverdraftLim (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-800),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(50), BigDecimal.valueOf(50)),
                        // ???????????? ???????????????????? ???? 1000, ?? ???????????????? ?????? 800. ???????????? ?????? "??????????????", ?? ????????????,
                        // AutoOverdraftLim ?????????? ?????? ????????????????????. ???? ???? ?????? ?? ???? ???????????????? ???????? ????????????,
                        // ?? ????????????, ???? ???????????? ?????????????????? 1000. ?????????????? ?????????? ????????.
                        BigDecimal.valueOf(0),
                        "???????????? ?????? ????????, ???? ???????????????????????? ?????????????? (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1200),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(0), BigDecimal.valueOf(500)),
                        // ???????????? 2, ???? ?? ???????? ?????????? ???? ???????????? ?? ??????, ?????? ???????????? "??????????????" ?? ??????????????
                        // ???????????????? -- ????-???? ????????, ?????? overdraftLim = 0, ?????? ?????? ?????? autoOverdraftLim > 0
                        BigDecimal.valueOf(200),
                        "?????????????????????????? ?????????????????????? ???? ???????????????????????????? (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1700),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // ???????????? 3, ???? ?? ???????? ?????????? ???? ???????????? ?? ??????, ?????? ???????????? "??????????????" ?? ??????????????
                        // ???????????????? -- ????-???? ????????, ?????? overdraftLim = 0, ?????? ?????? ?????? autoOverdraftLim > 0
                        BigDecimal.valueOf(500),
                        "???? ???????????????????? ?? ???? ????????????????, ?????????????????????? Sum+AutoOverdraftLim (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-800),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(0), BigDecimal.valueOf(500)),
                        // ???????????? 4, ???? ?? ???????? ?????????? ???? ???????????? ?? ??????, ?????? ???????????? "??????????????" ?? ??????????????
                        // ???????????????? -- ????-???? ????????, ?????? overdraftLim = 0, ?????? ?????? ?????? autoOverdraftLim > 0
                        BigDecimal.valueOf(0),
                        "???????????? ?????? ????????, ???? ???????????????????????? ?????????????? (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), ZERO,
                        autoOverdraftInfo(BigDecimal.valueOf(150), false, BigDecimal.valueOf(2000),
                                BigDecimal.valueOf(1500)),
                        // ???????? ?? ?????????????? ???????? ???????????????????????? ????????, ?????????????? ???? ???????????????? ???????? ?? ????????????
                        // ???????????????? ???????????????????? (???? ????????????????????????????), ???? ?????????? ???????????????????????????? ??????????????????????
                        // ???? ?????????? ?????????? ?????????? (???????? ???????????? ???? ?????????????? ????????, ?????? ?????? ????????????????).
                        BigDecimal.valueOf(1350),
                        "?????????????????? ???????????????????????? ???????? (clients_options.debt > 0)"
                }
        };
        return Arrays.asList(data);
    }

    @Test
    public void testAutoOverdraftAddition() {
        assertThat(
                calculateAutoOverdraftAddition(walletCurrency, walletSum, walletSumSpent, clientAutoOverdraftInfo),
                equalTo(expectedAddition));
    }
}
