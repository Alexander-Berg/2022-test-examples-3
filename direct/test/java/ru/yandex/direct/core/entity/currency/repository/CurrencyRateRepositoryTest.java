package ru.yandex.direct.core.entity.currency.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.DSLContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.currency.CurrencyCode.BYN;
import static ru.yandex.direct.currency.CurrencyCode.CHF;
import static ru.yandex.direct.currency.CurrencyCode.EUR;
import static ru.yandex.direct.currency.CurrencyCode.KZT;
import static ru.yandex.direct.currency.CurrencyCode.TRY;
import static ru.yandex.direct.currency.CurrencyCode.UAH;
import static ru.yandex.direct.currency.CurrencyCode.USD;
import static ru.yandex.direct.dbschema.ppc.Tables.CURRENCY_RATES;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CurrencyRateRepositoryTest {
    @Autowired
    private CurrencyRateRepository repo;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Test
    public void testGetLastCurrencyRates() {
        // для теста очень важно, чтобы в таблице ppcdict.currency_rates были только те строки, которые туда запишет
        // сам тест, поэтому в транзакции чистим таблицу, инициализируем как хотим, и потом откатываем изменения
        runWithEmptyCurrencyRatesTable(dslContext -> {
            initCurrencies(dslContext);
            LocalDate day22 = LocalDate.of(2020, 4, 22);
            LocalDate day20 = LocalDate.of(2020, 4, 20);
            List<CurrencyRate> crs = repo.getLastCurrencyRates(dslContext, day22);
            assertThat(crs).containsExactlyInAnyOrder(
                    cur(TRY, day22, 10.8409),
                    cur(BYN, day22, 30.5082),
                    cur(UAH, day22, 2.75755),
                    cur(KZT, day22, 0.174402),
                    cur(EUR, day22, 81.1019),
                    cur(USD, day20, 73.9441),
                    cur(CHF, day22, 77.134)
            );
        });
    }

    @QueryWithoutIndex("Удаление по всей таблице без ключей")
    private void runWithEmptyCurrencyRatesTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcdictTransaction(configuration -> {
                DSLContext dsl = configuration.dsl();
                dsl.deleteFrom(CURRENCY_RATES).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }

    private void initCurrencies(DSLContext ctx) {
        ctx.execute("INSERT INTO currency_rates(currency, `date`, rate) VALUES\n" +
                "('TRY', '2020-04-23', 11.8409),\n" +
                "('TRY', '2020-04-22', 10.8409),\n" +
                "('UAH', '2020-04-22', 2.75755),\n" +
                "('KZT', '2020-04-22', 0.174402),\n" +
                "('EUR', '2020-04-22', 81.1019),\n" +
                "('CHF', '2020-04-22', 77.134),\n" +
                "('BYN', '2020-04-22', 30.5082),\n" +
                "('EUR', '2020-04-21', 81.1019),\n" +
                "('CHF', '2020-04-21', 77.134),\n" +
                "('UAH', '2020-04-21', 2.75755),\n" +
                "('TRY', '2020-04-21', 10.8409),\n" +
                "('KZT', '2020-04-21', 0.174402),\n" +
                "('BYN', '2020-04-21', 30.5082),\n" +
                "('KZT', '2020-04-20', 0.173283),\n" +
                "('CHF', '2020-04-20', 76.1212),\n" +
                "('USD', '2020-04-20', 73.9441),\n" +
                "('EUR', '2020-04-20', 80.111),\n" +
                "('UAH', '2020-04-20', 2.7319),\n" +
                "('BYN', '2020-04-20', 30.2677);" +
                "");
    }

    private static CurrencyRate cur(CurrencyCode currencyCode, LocalDate localDate, double rate) {
        return new CurrencyRate().withCurrencyCode(currencyCode).withDate(localDate)
                .withRate(BigDecimal.valueOf(rate).setScale(16, RoundingMode.UNNECESSARY));
    }
}
