package ru.yandex.market.sc.api.controller.inbound;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.model.ApiInboundDto;
import ru.yandex.market.sc.core.domain.inbound.model.ApiInboundListDto;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistrySortableRequest;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScApiControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundControllerAnomalyTest {

    private static final String CREATED_EXTERNAL_ID = "accepted-external-id";

    private final ScApiControllerCaller caller;
    private final XDocFlow flow;
    private final InboundCommandService inboundCommandService;
    private final RegistryRepository registryRepository;
    private final RegistrySortableRepository registrySortableRepository;
    private final TestFactory testFactory;
    private final SortableQueryService sortableQueryService;
    private final SortableRepository sortableRepository;
    private final SortingCenterRepository sortingCenterRepository;

    @Test
    void getAnomalyInboundForFix() {
        var inbound = flow.createInboundAndGet(CREATED_EXTERNAL_ID);
        inboundCommandService.createInboundRegistry(
            Collections.emptyList(),
            List.of(new CreateInboundRegistrySortableRequest(
                inbound.getExternalId(),
                "AN-12345",
                RegistryUnitType.BOX
            )),
            inbound.getExternalId(),
            "TEST_REGESTRY",
            testFactory.getOrCreateAnyUser(inbound.getSortingCenter())
        );
        var result = caller.getInboundForFix("AN-12345")
            .andExpect(status().isOk())
            .getResponseAsClass(ApiInboundDto.class);
        Assertions.assertEquals(CREATED_EXTERNAL_ID, result.getExternalId());
    }

    @Test
    void getAnomalyInboundForAcceptV2HappyPath() {
        var inbound = flow.createInboundAndGet(CREATED_EXTERNAL_ID);
        inboundCommandService.createInboundRegistry(
                Collections.emptyList(),
                List.of(new CreateInboundRegistrySortableRequest(
                        inbound.getExternalId(),
                        "AN-12345",
                        RegistryUnitType.BOX
                )),
                inbound.getExternalId(),
                "TEST_REGESTRY",
                testFactory.getOrCreateAnyUser(inbound.getSortingCenter())
        );
        var result = caller.acceptInboundsV2("AN-12345", null)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiInboundListDto.class);
        Assertions.assertEquals(CREATED_EXTERNAL_ID, result.getInbounds().get(0).getExternalId());
    }

    @Test
    void linkAnomalyToInboundHappyPath() {
        var inbound = flow.createInboundAndGet(CREATED_EXTERNAL_ID);
        inboundCommandService.createInboundRegistry(
            Collections.emptyList(),
            List.of(new CreateInboundRegistrySortableRequest(
                inbound.getExternalId(),
                "AN-12345",
                RegistryUnitType.BOX
            )),
            inbound.getExternalId(),
            "TEST_REGESTRY",
            testFactory.getOrCreateAnyUser(inbound.getSortingCenter())
        );
        caller.linkToInbound(
                CREATED_EXTERNAL_ID,
                new LinkToInboundRequestDto("AN-12345", SortableType.XDOC_BOX)
            )
            .andExpect(status().isOk());

        Sortable sortable = sortableQueryService.find(testFactory.getSortingCenterById(TestFactory.SC_ID), "AN-12345")
            .orElseThrow();
        assertThat(sortable.getInbound().getExternalId()).isEqualTo(CREATED_EXTERNAL_ID);
    }

    @Test
    void fixAnomalyInbound() {
        var inbound = flow.createInboundAndGet("SECOND");
        inboundCommandService.createInboundRegistry(
            Collections.emptyList(),
            List.of(new CreateInboundRegistrySortableRequest(
                inbound.getExternalId(),
                "AN-54321",
                RegistryUnitType.BOX
            )),
            inbound.getExternalId(),
            "TEST_REGESTRY",
            testFactory.getOrCreateAnyUser(inbound.getSortingCenter())
        );
        caller.linkToInbound(
                "SECOND",
                new LinkToInboundRequestDto("AN-54321", SortableType.XDOC_BOX)
            )
            .andExpect(status().isOk());
        caller.fixInbound("AN-54321")
            .andExpect(status().isOk());

        var factRegistry = StreamEx.of(registryRepository.findAllByInboundId(inbound.getId()))
            .filterBy(Registry::getType, RegistryType.FACTUAL)
            .findAny().orElseThrow(IllegalStateException::new);
        var sortables = registrySortableRepository.findAllByRegistryIn(List.of(factRegistry));
        assertThat(sortables.size()).isEqualTo(1);
        assertThat(sortables.get(0).getSortableExternalId()).isEqualTo("AN-54321");

        var sortable = sortableQueryService.find(sortingCenterRepository.getById(12L), "AN-54321");
        assertThat(sortable.orElseThrow().getType()).isEqualTo(SortableType.XDOC_ANOMALY);
    }
}
