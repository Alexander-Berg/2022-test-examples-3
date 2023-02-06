package ru.yandex.market.sc.api.util.flow.xdoc;

import java.util.function.Consumer;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;
import ru.yandex.market.sc.core.util.flow.xdoc.FlowEngine;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Переопределяет вызов методов TestFactory на вызовы контроллеров
 */
@Primary
@Component
public class ApiFlowEngine extends FlowEngine {

    private final ScApiControllerCaller caller;

    private final CellRepository cellRepository;

    public ApiFlowEngine(
            TestFactory testFactory,
            SortableTestFactory sortableTestFactory,
            CellRepository cellRepository,
            ScApiControllerCaller caller
    ) {
        super(testFactory, sortableTestFactory);
        this.cellRepository = cellRepository;
        this.caller = caller;
    }

    @Override
    public void callAcceptInbound(String inboundExternalId, Consumer<SneakyResultActions> resultActions) {
        var actions = caller.acceptInbound(inboundExternalId, null);
        resultActions.accept(actions);
    }

    @Override
    public void callLinkToInbound(String inboundExternalId, String barcode, SortableType type,
                                  Consumer<SneakyResultActions> resultActions) {
        var actions = caller.linkToInbound(inboundExternalId, new LinkToInboundRequestDto(barcode, type));
        resultActions.accept(actions);
    }

    @Override
    public void linkToInbound(String inboundExternalId, String barcode, SortableType type) {
        callLinkToInbound(inboundExternalId, barcode, type, res -> res.andExpect(status().isOk()));
    }

    @Override
    public void sort(String sortableBarcode, long cellId) {
        Cell cell = cellRepository.findByIdOrThrow(cellId);
        caller.sort(sortableBarcode, cell)
                .andExpect(status().isOk());
    }

    @Override
    public void preship(long sortableId, SortableType type, SortableAPIAction action) {
        caller.preship(sortableId, type, action)
                .andExpect(status().isOk());
    }

    @Override
    public void saveVgh(SortingCenter sortingCenter, SaveVGHRequestDto request, String barcode) {
        caller.saveVgh(barcode, request)
                .andExpect(status().isOk());
    }

    @Override
    public void finishAcceptance(Inbound inbound) {
        caller.finishAcceptance(inbound.getExternalId())
                .andExpect(status().isOk());
    }

}
