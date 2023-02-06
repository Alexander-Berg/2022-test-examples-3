package ru.yandex.market.tpl.carrier.core;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;

@CoreTestV2
public abstract class BaseCoreTest {
    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }
}
