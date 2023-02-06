package ru.yandex.market.sc.core.util.flow.xdoc;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.inbound.model.PutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.util.SneakyResultActions;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@Setter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class InboundArrival {

    public static final PutCarInfoRequest INBOUND_PUT_REQUEST = PutCarInfoRequest.builder()
            .fullName("name")
            .phoneNumber("+79998882211")
            .carNumber("XXX")
            .carBrand("volvo")
            .trailerNumber("YYY")
            .comment("no comments")
            .build();

    private final FlowEngine flowEngine;
    private final XDocFlow flow;
    private Inbound inbound;

    public InboundArrival linkBoxes(String... barcodes) {
        linkPalletBoxes(Collections.emptySet(), Set.of(barcodes));
        return this;
    }

    public InboundArrival linkBoxes(int amount) {
        linkPalletBoxes(Collections.emptySet(), XDocFlow.generateBarcodes(amount, XDocFlow.BOX_PREFIX));
        return this;
    }

    public InboundArrival linkBoxes(int amount, String prefix) {
        linkPalletBoxes(Collections.emptySet(), XDocFlow.generateBarcodes(amount, prefix));
        return this;
    }

    public InboundArrival linkPallets(String... barcodes) {
        linkPalletBoxes(Set.of(barcodes), Collections.emptySet());
        return this;
    }

    public InboundArrival linkPallets(int amount) {
        linkPalletBoxes(XDocFlow.generateBarcodes(amount, XDocFlow.PALLET_PREFIX), Collections.emptySet());
        return this;
    }

    public InboundArrival linkPallets(int amount, String prefix) {
        linkPalletBoxes(XDocFlow.generateBarcodes(amount, prefix), Collections.emptySet());
        return this;
    }

    public InboundArrival carArrived() {
        return carArrived(INBOUND_PUT_REQUEST);
    }

    public InboundArrival carArrived(PutCarInfoRequest request) {
        flowEngine.inboundCarArrived(this.inbound, request);
        return this;
    }

    public InboundArrival readyToReceive() {
        flowEngine.readyToReceive(this.inbound);
        return this;
    }

    public InboundArrival transitionToReadyToReceive() {
        var currentStatus = inbound.getInboundStatus();
        switch (currentStatus) {
            case CONFIRMED:
                carArrived();
                readyToReceive();
                break;
            case CAR_ARRIVED:
                readyToReceive();
                break;
            case READY_TO_RECEIVE:
                break;
            default:
                throw new TplIllegalStateException("Нельзя провести поставку из статуса " + currentStatus +
                        " в статус " + InboundStatus.READY_TO_RECEIVE);
        }
        return this;
    }

    public InboundArrival carArrivedAndReadyToReceive() {
        return carArrived().readyToReceive();
    }

    public InboundArrival saveVgh(SaveVGHRequestDto requestDto, String barcode) {
        flowEngine.saveVgh(flow.getSortingCenter(), requestDto, barcode);
        return this;
    }

    public InboundArrival finishAcceptance() {
        flowEngine.finishAcceptance(this.inbound);
        return this;
    }

    public XDocFlow fixInbound() {
        flowEngine.fixInbound(this.inbound);
        return flow;
    }

    public XDocFlow and() {
        return flow;
    }

    public InboundArrival apiAcceptInbound(Consumer<SneakyResultActions> resultActions) {
        flowEngine.callAcceptInbound(this.inbound.getExternalId(), resultActions);
        return this;
    }

    public InboundArrival apiLinkBox(String barcode, Consumer<SneakyResultActions> resultActions) {
        flowEngine.callLinkToInbound(this.inbound.getExternalId(), barcode, SortableType.XDOC_BOX, resultActions);
        return this;
    }

    public InboundArrival apiLinkPallet(String barcode, Consumer<SneakyResultActions> resultActions) {
        flowEngine.callLinkToInbound(this.inbound.getExternalId(), barcode, SortableType.XDOC_PALLET, resultActions);
        return this;
    }

    public XDocFlow apiFixInbound(Consumer<SneakyResultActions> resultActions) {
        flowEngine.callFixInbound(inbound.getExternalId(), resultActions);
        return flow;
    }

    private void linkPalletBoxes(Set<String> pallets, Set<String> boxes) {
        pallets.forEach(barcode -> link(barcode, SortableType.XDOC_PALLET));
        boxes.forEach(barcode -> link(barcode, SortableType.XDOC_BOX));
    }

    private void link(String barcode, SortableType type) {
        flowEngine.linkToInbound(this.inbound.getExternalId(), barcode, type);
    }

    public Inbound getInbound() {
        return inbound;
    }
}
