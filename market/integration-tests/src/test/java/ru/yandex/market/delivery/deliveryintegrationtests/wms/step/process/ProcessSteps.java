package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.process;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui.UISteps;

public class ProcessSteps {
    private final Incoming incoming;
    private final Outgoing outgoing;

    public ProcessSteps(UISteps uiSteps) {
        this.incoming = new Incoming(uiSteps);
        this.outgoing = new Outgoing(uiSteps);
    }

    public Incoming Incoming() {
        return incoming;
    }

    public Outgoing Outgoing() {
        return outgoing;
    }
}
