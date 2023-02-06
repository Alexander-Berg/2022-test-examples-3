package ru.yandex.market.sc.core.util.flow.xdoc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class InboundCreation {

    private final ApplicationContext appContext;
    private final TestFactory testFactory;
    private final Clock clock;
    private final InboundRepository inboundRepository;

    private String externalId;
    private LocalDateTime dateTime;
    private String informationListBarcode;
    private String nextLogisticPoint;
    private InboundType type = InboundType.XDOC_TRANSIT;
    private boolean confirmed = true;
    private String realSupplierName = "ООО Сапплаер";
    private SortingCenter sc;

    public InboundCreation externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public InboundCreation date(LocalDate date) {
        this.dateTime = LocalDateTime.of(date, LocalTime.of(0, 0));
        return this;
    }

    public InboundCreation informationListBarcode(String informationListBarcode) {
        this.informationListBarcode = informationListBarcode;
        return this;
    }

    public InboundCreation nextLogisticPoint(String nextLogisticPoint) {
        this.nextLogisticPoint = nextLogisticPoint;
        return this;
    }

    public InboundCreation type(InboundType type) {
        this.type = type;
        return this;
    }

    public InboundCreation realSupplierName(String realSupplierName) {
        this.realSupplierName = realSupplierName;
        return this;
    }

    public InboundCreation sortingCenter(SortingCenter sc) {
        this.sc = sc;
        return this;
    }

    public InboundArrival build() {

        if (sc == null) {
             sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        }

        var warehouse = testFactory.findWarehouseBy(TestFactory.WAREHOUSE_YANDEX_ID);

        OffsetDateTime offsetDateTime = this.dateTime == null ? OffsetDateTime.now(this.clock) :
            OffsetDateTime.of(this.dateTime, this.clock.getZone().getRules().getOffset(this.dateTime));

        var params = TestFactory.CreateInboundParams.builder()
            .inboundExternalId(this.externalId)
            .inboundType(this.type)
            .fromDate(offsetDateTime)
            .warehouseFromExternalId(warehouse.getYandexId())
            .toDate(offsetDateTime)
            .sortingCenter(sc)
            .registryMap(Map.of())  // для xdoc реестр order_registry пустой
            .informationListBarcode(this.informationListBarcode)
            .nextLogisticPointId(Optional.ofNullable(this.nextLogisticPoint).orElse(TestFactory.WAREHOUSE_YANDEX_ID))
            .confirmed(confirmed)
            .realSupplierName(realSupplierName)
            .build();
        var inbound = this.testFactory.createInbound(params);
        var inboundArrival = appContext.getBean(InboundArrival.class);
        inboundArrival.setInbound(inbound);
        return inboundArrival;
    }

    public Inbound createAndGet() {
        build();
        return inboundRepository.findByExternalId(externalId).orElseThrow();
    }

    public InboundArrival toArrival() {
        var inbound = inboundRepository.findByExternalId(externalId).orElseThrow();
        var inboundArrival = appContext.getBean(InboundArrival.class);
        inboundArrival.setInbound(inbound);
        return inboundArrival;
    }

    public InboundCreation confirm(boolean isConfirmed) {
        this.confirmed = isConfirmed;
        return this;
    }
}
