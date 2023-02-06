package ru.yandex.market.sc.internal.util.flow.xdoc;

import java.util.function.Consumer;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.inbound.model.InboundAvailableAction;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;
import ru.yandex.market.sc.core.util.flow.xdoc.FlowEngine;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Переопределяет вызовы TestFactory на вызовы контроллеров
 */
@Primary
@Component
public class IntFlowEngine extends FlowEngine {

    private final ScIntControllerCaller caller;

    public IntFlowEngine(TestFactory testFactory, SortableTestFactory sortableTestFactory, ScIntControllerCaller caller) {
        super(testFactory, sortableTestFactory);
        this.caller = caller;
    }

    @Override
    public void callReadyToReceive(String externalIdOrInfoListCode, Consumer<SneakyResultActions> resultActions) {
        resultActions.accept(
                caller.performAction(externalIdOrInfoListCode, InboundAvailableAction.READY_TO_RECEIVE)
        );
    }

    @Override
    public void callFixInbound(String externalIdOrInfoListCode, Consumer<SneakyResultActions> resultActions) {
        resultActions.accept(
                caller.performAction(externalIdOrInfoListCode, InboundAvailableAction.FIX_INBOUND)
        );
    }

    @Override
    public void callShipOutbound(String outboundExternalId, Consumer<SneakyResultActions> resultActions) {
        resultActions.accept(
                caller.shipOutbound(outboundExternalId)
        );
    }

    @Override
    public void shipOutbound(String outboundExternalId) {
        callShipOutbound(outboundExternalId, res -> res.andExpect(status().isOk()));
    }

    @Override
    public void createBasket(PartnerLotRequestDto partnerLotRequestDto) {
        caller.createLot(partnerLotRequestDto).andExpect(status().isOk());
    }
}
