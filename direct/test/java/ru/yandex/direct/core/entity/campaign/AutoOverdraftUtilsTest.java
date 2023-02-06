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
                        // Простой случай -- размер порога просто используется целиком
                        BigDecimal.valueOf(17000.57),
                        "Сумма автоовердрафта добавляется к wallet.sum"
                },
                {
                        CurrencyCode.BYN, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // Простой случай -- размер порога просто используется целиком
                        BigDecimal.valueOf(17000.57),
                        "Автоовердрафт для BYN"
                },
                {
                        CurrencyCode.KZT, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // Простой случай -- размер порога просто используется целиком
                        BigDecimal.valueOf(17000.57),
                        "Автоовердрафт для KZT"
                },
                {
                        CurrencyCode.UAH, BigDecimal.valueOf(345000), ZERO,
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(100000), BigDecimal.valueOf(17000.57)),
                        // функционал открыт только для России, Белоруссии и Казахстана
                        BigDecimal.valueOf(0),
                        "Автоовердрафт неприменим для UAH"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1200),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // Клиент открутился уже на 1200, но успел внести только 1000
                        // Баланс его "забанил", а значит, AutoOverdraftLim далее ему недоступен,
                        // но он уже открутился на 1200, поэтому в качестве SUMCur мы должны
                        // отправить 1200 (если отправим меньше, то потраченные "кредитные" деньги
                        // будут откачены как технические перекруты). Соответственно, добавка будет = 200.
                        BigDecimal.valueOf(200),
                        "Автоовердрафт фиксируется на использованном (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1700),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // По какой-то причине клиент открутился на 1700, что даже больше чем Sum+AutoOverdraftLim
                        // (например, из-за перекрутов в БК). Баланс его "забанил", значит,
                        // AutoOverdraftLim далее ему недоступен. Из-за ограничения сверху в Sum+AutoOverdraftLim
                        // (чтобы не уйти в таких случаях в бесконечность) отправим 1000+500,
                        // лишние 200 будут списаны как технические перекруты (что фактически и произошло)
                        BigDecimal.valueOf(500),
                        "Не отправляем в БК значения, превышающие Sum+AutoOverdraftLim (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-800),
                        autoOverdraftInfo(ZERO, true, BigDecimal.valueOf(50), BigDecimal.valueOf(50)),
                        // Клиент открутился на 1000, а заплатил уже 800. Баланс его "забанил", а значит,
                        // AutoOverdraftLim далее ему недоступен. Но он ещё и не докрутил свои деньги,
                        // а значит, мы должны отправить 1000. Добавка равна нулю.
                        BigDecimal.valueOf(0),
                        "Деньги ещё есть, не ограничиваем клиента (statusBalanceBanned=true)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1200),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(0), BigDecimal.valueOf(500)),
                        // Случай 2, но в этом месте мы узнаём о том, что клиент "забанен" в Балансе
                        // косвенно -- из-за того, что overdraftLim = 0, при том что autoOverdraftLim > 0
                        BigDecimal.valueOf(200),
                        "Автоовердрафт фиксируется на использованном (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-1700),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(500), BigDecimal.valueOf(500)),
                        // Случай 3, но в этом месте мы узнаём о том, что клиент "забанен" в Балансе
                        // косвенно -- из-за того, что overdraftLim = 0, при том что autoOverdraftLim > 0
                        BigDecimal.valueOf(500),
                        "Не отправляем в БК значения, превышающие Sum+AutoOverdraftLim (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), BigDecimal.valueOf(-800),
                        autoOverdraftInfo(ZERO, false, BigDecimal.valueOf(0), BigDecimal.valueOf(500)),
                        // Случай 4, но в этом месте мы узнаём о том, что клиент "забанен" в Балансе
                        // косвенно -- из-за того, что overdraftLim = 0, при том что autoOverdraftLim > 0
                        BigDecimal.valueOf(0),
                        "Деньги ещё есть, не ограничиваем клиента (overdraftLim=0)"
                },
                {
                        CurrencyCode.RUB, BigDecimal.valueOf(1000), ZERO,
                        autoOverdraftInfo(BigDecimal.valueOf(150), false, BigDecimal.valueOf(2000),
                                BigDecimal.valueOf(1500)),
                        // Если у клиента есть неоплаченный счёт, который он выставил себе в рамках
                        // обычного овердрафта (не АВТОовердрафта), то лимит автоовердрафта уменьшается
                        // на сумму этого счёта (пока клиент не оплатит того, что уже задолжал).
                        BigDecimal.valueOf(1350),
                        "Учитываем неоплаченный счёт (clients_options.debt > 0)"
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
