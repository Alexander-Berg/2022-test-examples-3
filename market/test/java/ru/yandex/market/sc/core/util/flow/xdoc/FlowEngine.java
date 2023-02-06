package ru.yandex.market.sc.core.util.flow.xdoc;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.inbound.model.PutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.InvalidTestParameters;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;

/**
 * Класс для совершения различных действий для XDoc потока
 * по-умолчанию вызывает TestFactory методы
 * <p>
 * в sc-int и sc-api представленны расширения данного класса, которые переопределяют вызовы TestFactory на вызовы
 * своих контроллеров
 * {@link ru.yandex.market.sc.api.util.flow.xdoc.ApiFlowEngine}
 * {@link ru.yandex.market.sc.internal.util.flow.xdoc.IntFlowEngine}
 * <p>
 * методы с перфиксом call доступны только в соответствующем модуле sc-api или sc-int
 * и позволяют выполнить проверки ответа контроллера
 */
@Component
@RequiredArgsConstructor
public class FlowEngine {

    protected final TestFactory testFactory;
    protected final SortableTestFactory sortableTestFactory;

    public void callAcceptInbound(String inboundExternalId, Consumer<SneakyResultActions> resultActions) {
        throw new InvalidTestParameters("api вызов доступен только в sc-api");
    }

    public void callLinkToInbound(String inboundExternalId, String barcode, SortableType type,
                                  Consumer<SneakyResultActions> resultActions) {
        throw new InvalidTestParameters("api вызов доступен только в sc-api");
    }

    public void callReadyToReceive(String externalIdOrInfoListCode, Consumer<SneakyResultActions> resultActions) {
        throw new InvalidTestParameters("int вызов доступен только в sc-int");
    }

    public void callFixInbound(String inboundExternalId, Consumer<SneakyResultActions> resultActions) {
        throw new InvalidTestParameters("int вызов доступен только в sc-int");
    }

    public void callShipOutbound(String outboundExternalId, Consumer<SneakyResultActions> resultActions) {
        throw new InvalidTestParameters("int вызов доступен только в sc-int");
    }

    public void linkToInbound(String inboundExternalId, String barcode, SortableType type) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        testFactory.linkSortableToInbound(inboundExternalId, barcode, type, user);
    }

    public void inboundCarArrived(Inbound inbound, PutCarInfoRequest request) {
        testFactory.inboundCarArrived(inbound, request);
    }

    public void readyToReceive(Inbound inbound) {
        testFactory.readyToReceiveInbound(inbound);
    }

    public void fixInbound(Inbound inbound) {
        testFactory.finishInbound(inbound);
    }

    public void shipOutbound(String outboundExternalId) {
        testFactory.shipOutbound(outboundExternalId);
    }

    public void sort(String sortableBarcode, long cellId) {
        sortableTestFactory.sortByBarcode(sortableBarcode, cellId);
    }

    public void lotSort(long sortableId, String lotBarcode) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        sortableTestFactory.lotSort(sortableId, lotBarcode, user);
    }

    public void preship(long sortableId, SortableType type, SortableAPIAction action) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        sortableTestFactory.preship(sortableId, type, action, user);
    }

    public void createBasket(PartnerLotRequestDto partnerLotRequestDto) {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        sortableTestFactory.createEmptyLot(sc, partnerLotRequestDto);
    }

    public void saveVgh(SortingCenter sortingCenter, SaveVGHRequestDto request, String barcode) {
        testFactory.inboundSaveVgh(sortingCenter, request, barcode);
    }

    public void finishAcceptance(Inbound inbound) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        testFactory.finishAcceptance(user, inbound.getExternalId());
    }

}
