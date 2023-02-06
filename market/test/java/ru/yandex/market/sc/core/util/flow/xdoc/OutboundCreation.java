package ru.yandex.market.sc.core.util.flow.xdoc;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class OutboundCreation {

    private final ApplicationContext appContext;
    private final TestFactory testFactory;
    private final Clock clock;
    private final OutboundRepository outboundRepository;

    private String externalId;
    private OutboundType type = OutboundType.XDOC;
    private OutboundStatus status;
    private Instant fromTime;
    private Instant toTime;
    private String logisticPointToExternalId;
    private String courierExternalId = "courier-101";

    public OutboundCreation externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public OutboundCreation type(OutboundType type) {
        this.type = type;
        return this;
    }

    public OutboundCreation status(OutboundStatus status) {
        this.status = status;
        return this;
    }

    public OutboundCreation fromTime(Instant fromTime) {
        this.fromTime = fromTime;
        return this;
    }

    public OutboundCreation toTime(Instant toTime) {
        this.toTime = toTime;
        return this;
    }

    public OutboundCreation logisticPointToExternalId(String externalId) {
        this.logisticPointToExternalId = externalId;
        return this;
    }

    public OutboundCreation courierExternalId(String externalId) {
        this.courierExternalId = externalId;
        return this;
    }

    public RegistryCreation toRegistryBuilder() {
        return toRegistryBuilder(
                Optional.ofNullable(toTime).orElse(this.clock.instant()),
                Optional.ofNullable(fromTime).orElse(this.clock.instant())
        );
    }

    public RegistryCreation toRegistryBuilder(Instant from, Instant to) {
        var outbound = build(from, to);
        var regCreation = this.appContext.getBean(RegistryCreation.class);
        regCreation.outbound(outbound);
        return regCreation;
    }

    public Outbound build() {
        return build(this.clock.instant(), this.clock.instant());
    }

    public Outbound build(Instant from, Instant to) {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        var warehouse = testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID);
        var outbound = testFactory.createOutbound(
                TestFactory.CreateOutboundParams.builder()
                        .externalId(this.externalId)
                        .sortingCenter(sc)
                        .partnerToExternalId(warehouse.getYandexId())
                        .logisticPointToExternalId(warehouse.getYandexId())
                        .toTime(to)
                        .fromTime(from)
                        .logisticPointToExternalId(
                                Optional.ofNullable(logisticPointToExternalId).orElse(warehouse.getYandexId())
                        )
                        .courierExternalId(courierExternalId)
                        .carNumber("A777MP77")
                        .type(this.type)
                        .build()
        );
        updateStatus();
        return outbound;
    }

    private void updateStatus() {
        if (this.status != null) {
            var outbound = outboundRepository.findByExternalId(externalId).orElseThrow();
            outbound.setStatus(this.status);
            outboundRepository.save(outbound);
        }
    }

    public Outbound get() {
        return this.outboundRepository.findByExternalId(this.externalId).orElseThrow();
    }
}
