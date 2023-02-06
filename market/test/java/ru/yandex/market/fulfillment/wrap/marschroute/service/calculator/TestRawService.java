package ru.yandex.market.fulfillment.wrap.marschroute.service.calculator;

import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceKey;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.RawService;

import java.math.BigDecimal;

public class TestRawService implements RawService {
    private final BigDecimal sum;
    private final MarschrouteServiceKey serviceKey;

    public TestRawService(BigDecimal sum, MarschrouteServiceKey serviceKey) {
        this.sum = sum;
        this.serviceKey = serviceKey;
    }

    @Override
    public BigDecimal getSum() {
        return sum;
    }

    @Override
    public MarschrouteServiceKey getMarschrouteServiceKey() {
        return serviceKey;
    }
}
