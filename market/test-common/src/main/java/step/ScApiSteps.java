package step;

import client.ScApiClient;
import dto.requests.scapi.ApiSortableSortRequest;
import dto.responses.inbounds.ScInboundsAcceptResponse;
import dto.responses.scapi.orders.ApiOrderDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

@Slf4j
public class ScApiSteps {

    private static final ScApiClient SCAPI = new ScApiClient();

    /**
     * Принимаем поставку в СЦ
     **/
    @Step("Принимаем поставку в СЦ")
    public void inboundsAccept(String inboundYandexId) {
        log.debug("Accepting inbound in sorting center");
        Retrier.retry(() -> {
            ScInboundsAcceptResponse request = SCAPI.inboundsAccept(inboundYandexId);
            Assertions.assertTrue(request.getStatus().equals("CREATED") ||
                request.getStatus().equals("CONFIRMED") ||
                request.getStatus().equals("READY_TO_RECEIVE"),
                "Статус поставки не CREATED, CONFIRMED, READY_TO_RECEIVE");
        });
    }

    /**
     * Привязываем поставку к sortableId -- внутреннему уникальному id СЦ, начинающемуся с XDOC-
     **/
    @Step("Привязываем поставку в СЦ к sortableId")
    public String inboundsLink(String inboundId) {
        log.debug("Linking inbound with pallet in sorting center");
        return Retrier.retry(() -> SCAPI.xDocInboundsLink(inboundId));
    }

    /**
     * Принимаем заказ в СЦ
     **/
    @Step("Принимаем поставку в СЦ")
    public ApiOrderDto acceptOrdersAndGetZoneForCells(String sortableId) {
        log.debug("Accepting order in sorting center");
        return Retrier.retry(() -> SCAPI.ordersAccept(sortableId));
    }

    /**
     * Кладем заказ в СЦ в ячейку, deprecated
     **/
    @Step("Размещаем заказ в ячейке в СЦ")
    public void sortOrder(Long orderId, Long cellId) {
        log.debug("Sorting order in sorting center");
        Retrier.retry(() -> SCAPI.sortOrder(orderId, cellId));
    }

    /**
     * Завершаем поставку в СЦ
     **/
    @Step("Завершаем поставку в СЦ")
    public void fixInbound(String inboundId) {
        log.debug("Closing inbound in sorting center");
        Retrier.retry(() -> SCAPI.fixInbound(inboundId));
    }

    @Step("Кладём заказ в ячейку методом /api/sortable/beta/sort")
    public void sortableSort(String sortableExternalId, String cellId) {
        log.debug("Sorting sortable into the cell in sorting center");
        ApiSortableSortRequest request = new ApiSortableSortRequest();
        request.setSortableExternalId(sortableExternalId);
        request.setDestinationExternalId(cellId);
        Retrier.retry(() -> SCAPI.sortableSort(request));
    }

}
