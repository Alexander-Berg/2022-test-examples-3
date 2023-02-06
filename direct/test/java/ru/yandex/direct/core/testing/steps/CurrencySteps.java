package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCurrencyConvertMoneyCorrespondenceRepository;
import ru.yandex.direct.core.testing.repository.TestCurrencyConvertQueueRepository;
import ru.yandex.direct.core.testing.repository.TestCurrencyRateRepository;
import ru.yandex.direct.currency.CurrencyCode;

public class CurrencySteps {
    @Autowired
    private TestCurrencyConvertQueueRepository currencyConvertQueueRepository;

    @Autowired
    private TestCurrencyConvertMoneyCorrespondenceRepository currencyConvertMoneyCorrespondenceRepository;

    @Autowired
    private TestCurrencyRateRepository currencyRateRepository;

    public void createCurrencyRate(CurrencyCode currencyCode, LocalDate date, BigDecimal rate) {
        currencyRateRepository.addCurrencyRate(new CurrencyRate()
                .withCurrencyCode(currencyCode)
                .withDate(date)
                .withRate(rate));
    }

    public void createCurrencyConversionQueueEntry(ClientInfo clientInfo, long regionId) {
        currencyConvertQueueRepository.createCurrencyConversionQueueEntry(clientInfo, regionId);
    }

    public void createCurrencyConvertMoneyCorrespondenceEntry(ClientInfo clientInfo, long oldCid, Long newCid) {
        currencyConvertMoneyCorrespondenceRepository.createCurrencyConvertMoneyCorrespondenceEntry(clientInfo, oldCid,
                newCid);
    }

}
