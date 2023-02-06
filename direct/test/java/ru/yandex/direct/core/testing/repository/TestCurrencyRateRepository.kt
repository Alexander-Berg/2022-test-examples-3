package ru.yandex.direct.core.testing.repository

import org.springframework.stereotype.Repository
import ru.yandex.direct.core.entity.currency.model.CurrencyRate
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository
import ru.yandex.direct.dbschema.ppcdict.tables.CurrencyRates.CURRENCY_RATES
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.jooqmapperhelper.InsertHelper

@Repository
class TestCurrencyRateRepository(
    private val dslContextProvider: DslContextProvider,
) {
    private val currencyRateMapper = CurrencyRateRepository.createCurrencyRateMapper()

    fun addCurrencyRate(rate: CurrencyRate) {
        InsertHelper(dslContextProvider.ppcdict(), CURRENCY_RATES)
            .add(currencyRateMapper, rate)
            .onDuplicateKeyUpdate()
            .set(CURRENCY_RATES.RATE, rate.rate)
            .execute()
    }
}
