package ru.yandex.market.sc.internal.controller.tm;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.outbound.model.partner.AxaptaOutboundDocs;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundDocsRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundDocsStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author ogonek
 * <p>
 * Тесты на {@link DocumentsController}
 */
@ScIntControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class DocumentsControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    XDocFlow flow;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    ScIntControllerCaller caller;

    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    RegistrySortableRepository registrySortableRepository;
    @Autowired
    OutboundDocsRepository docsRepository;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
    }

    @Test
    @SneakyThrows
    void docsFlow() {
        var warehouse = testFactory.storedWarehouse("warehouse");
        var outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        caller.prepareToShipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());
        var registry = StreamEx.of(registryRepository.findAllByOutboundId(outbound.getId()))
                .filterBy(Registry::getType, RegistryType.PREPARED)
                .findAny().get();
        var docs = new AxaptaOutboundDocs(
                AxaptaOutboundDocs.AxaptaOutboundDocsStatus.SUCCESS,
                registry.getId(),
                List.of("mds_docs/1", "mds_docs/2")
        );
        caller.saveOutboundDocs(warehouse.getYandexId(), outbound.getExternalId(), docs);
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk());

        var actualDocs = docsRepository.findByOutbound(outbound);
        Assertions.assertEquals(OutboundDocsStatus.READY, actualDocs.get().getStatus());
        Assertions.assertEquals(docs.getDocs(), actualDocs.get().getDocuments());
    }

    @Test
    @SneakyThrows
    void expiredDocsFlow() {
        var warehouse = testFactory.storedWarehouse("warehouse");
        var outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        var outbound2 = flow.createInbound("in2")
                .linkPallets("XDOC-3")
                .fixInbound()
                .createOutbound("out2")
                .externalId("reg2")
                .buildRegistry("XDOC-3")
                .and()
                .getOutbound("out2");
        caller.prepareToShipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());

        var wrongRegistry = StreamEx.of(registryRepository.findAllByOutboundId(outbound2.getId()))
                .findAny().get();
        var docs = new AxaptaOutboundDocs(
                AxaptaOutboundDocs.AxaptaOutboundDocsStatus.SUCCESS,
                wrongRegistry.getId(),
                List.of("mds_docs/1", "mds_docs/2")
        );
        caller.saveOutboundDocs(warehouse.getYandexId(), outbound.getExternalId(), docs);
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk());

        var actualDocs = docsRepository.findByOutbound(outbound);
        Assertions.assertEquals(OutboundDocsStatus.REQUESTED, actualDocs.get().getStatus());
        Assertions.assertNull(actualDocs.get().getDocuments());
    }

    @Test
    @SneakyThrows
    void inconsistentDocsFlow() {
        var warehouse = testFactory.storedWarehouse("warehouse");
        var outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        caller.prepareToShipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());
        var registry = StreamEx.of(registryRepository.findAllByOutboundId(outbound.getId()))
                .filterBy(Registry::getType, RegistryType.PREPARED)
                .findAny().get();
        var docs = new AxaptaOutboundDocs(
                AxaptaOutboundDocs.AxaptaOutboundDocsStatus.SUCCESS,
                registry.getId(),
                List.of("mds_docs/1", "mds_docs/2")
        );
        caller.saveOutboundDocs(warehouse.getYandexId(), outbound.getExternalId(), docs);

        registrySortableRepository.save(new RegistrySortable(registry, "XDOC_NEW", RegistryUnitType.PALLET));

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk());

        var actualDocs = docsRepository.findByOutbound(outbound);
        Assertions.assertEquals(OutboundDocsStatus.INCONSISTENT_STATE, actualDocs.get().getStatus());
        Assertions.assertEquals(docs.getDocs(), actualDocs.get().getDocuments());
    }

    @Test
    @DisplayName("Страница отгрузок после запроса документов")
    void getOutboundsAfterDocRequest() {
        Outbound outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null);
        caller.prepareToShipOutbound("out1");
        caller.shipOutbound("out1");
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null);
        assertThat(docsRepository.findByOutbound(outbound).orElseThrow().getStatus())
                .isNotEqualTo(OutboundDocsStatus.INCONSISTENT_STATE);
    }
}
