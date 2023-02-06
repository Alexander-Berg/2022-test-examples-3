package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInternalStatusDto;
import ru.yandex.market.sc.core.domain.outbound.model.api.ApiInboundTaskDto;
import ru.yandex.market.sc.core.domain.outbound.model.api.ApiInboundTaskStatus;
import ru.yandex.market.sc.core.domain.outbound.model.api.ApiOutboundDto;
import ru.yandex.market.sc.core.domain.outbound.model.api.ApiOutboundTaskDto;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
class OutboundControllerTest {

    private final XDocFlow flow;
    private final ScApiControllerCaller caller;
    private final TestFactory testFactory;
    private final SortableQueryService sortableQueryService;
    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
    }

    @Test
    void getOutbounds() {
        var bufferCell = flow.createBufferCellAndGet("buf-cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var shipCell = flow.createShipCellAndGet("cell-1");
        var shipCell2 = flow.createShipCellAndGet("cell-2");
        var shipCell3 = flow.createShipCellAndGet("cell-3");
        var lot = flow.createBasket(bufferCell);

        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createInbound("in-3")
                .linkBoxes("XDOC-3")
                .fixInbound()
                .createInbound("in-4")
                .linkPallets("XDOC-4", "XDOC-5")
                .fixInbound()
                .createOutbound("out-1")
                .externalId("reg-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .createOutbound("out-2")
                .externalId("reg-2")
                .buildRegistry("XDOC-2")
                .sortToAvailableCell("XDOC-2");

        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();
        flow.sortBoxToLot(box, lot);
        Sortable basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();
        flow.packLot(basket.getRequiredBarcodeOrThrow());

        flow.createOutbound("out-3")
                .externalId("reg-3")
                .addRegistryBoxes("XDOC-3")
                .buildRegistry("XDOC-4", basket.getRequiredBarcodeOrThrow(), "XDOC-5")
                .sortToAvailableCell("XDOC-4", "XDOC-5")
                .prepareToShip("XDOC-4");

        ApiOutboundDto dto1 = ApiOutboundDto.builder()
                .status(OutboundInternalStatusDto.READY_TO_SHIP)
                .shipCell(shipCell.getScNumber())
                .preparedPalletCount(1)
                .plannedPalletCount(1)
                .inboundCount(1)
                .destination(TestFactory.warehouse().getIncorporation())
                .externalId("out-1")
                .shipCellActivated(true)
                .build();

        ApiOutboundDto dto2 = ApiOutboundDto.builder()
                .status(OutboundInternalStatusDto.NOT_STARTED)
                .shipCell(shipCell2.getScNumber())
                .plannedPalletCount(1)
                .preparedPalletCount(1)
                .inboundCount(1)
                .destination(TestFactory.warehouse().getIncorporation())
                .externalId("out-2")
                .shipCellActivated(true)
                .build();

        ApiOutboundDto dto3 = ApiOutboundDto.builder()
                .status(OutboundInternalStatusDto.NOT_FULLY_PREPARED)
                .shipCell(shipCell3.getScNumber())
                .plannedPalletCount(3)
                .preparedPalletCount(2)
                .inboundCount(2)
                .destination(TestFactory.warehouse().getIncorporation())
                .externalId("out-3")
                .shipCellActivated(true)
                .build();

        caller.getXDocOutbounds(null)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponse(dto3, dto2, dto1), true)); //Result should be sorted
    }

    @Test
    void getOutboundsWithFilter() {
        var bufferCell = flow.createBufferCellAndGet("buf-cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var shipCell = flow.createShipCellAndGet("cell-1");
        var shipCell2 = flow.createShipCellAndGet("cell-2");
        var shipCell3 = flow.createShipCellAndGet("cell-3");
        var lot = flow.createBasket(bufferCell);

        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createInbound("in-3")
                .linkBoxes("XDOC-3")
                .fixInbound()
                .createInbound("in-4")
                .linkPallets("XDOC-4", "XDOC-5")
                .fixInbound()
                .createOutbound("out-1")
                .externalId("reg-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .createOutbound("out-2")
                .externalId("reg-2")
                .buildRegistry("XDOC-2")
                .sortToAvailableCell("XDOC-2");

        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();
        flow.sortBoxToLot(box, lot);
        Sortable basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();
        flow.packLot(basket.getRequiredBarcodeOrThrow());

        flow.createOutbound("out-3")
                .externalId("reg-3")
                .addRegistryBoxes("XDOC-3")
                .buildRegistry("XDOC-4", basket.getRequiredBarcodeOrThrow(), "XDOC-5")
                .sortToAvailableCell("XDOC-4", "XDOC-5")
                .prepareToShip("XDOC-4");

        ApiOutboundDto dto3 = ApiOutboundDto.builder()
                .status(OutboundInternalStatusDto.NOT_FULLY_PREPARED)
                .shipCell(shipCell3.getScNumber())
                .plannedPalletCount(3)
                .preparedPalletCount(2)
                .inboundCount(2)
                .destination(TestFactory.warehouse().getIncorporation())
                .externalId("out-3")
                .shipCellActivated(true)
                .build();

        caller.getXDocOutbounds(OutboundInternalStatusDto.NOT_FULLY_PREPARED)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponse(dto3), true));
    }

    @Test
    void getOutboundInfo() {
        var bufferCell = flow.createBufferCellAndGet("buf-cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var bufferCell2 = flow.createBufferCellAndGet("buf-cell-2", TestFactory.WAREHOUSE_YANDEX_ID);
        var shipCell = flow.createShipCellAndGet("cell-1");
        var lot = flow.createBasket(bufferCell2);

        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-2")
                .build()
                .linkPallets("XDOC-2", "XDOC-3")
                .fixInbound()
                .inboundBuilder("in-3")
                .informationListBarcode("Зп-3")
                .build()
                .linkPallets("XDOC-4", "XDOC-5", "XDOC-6")
                .fixInbound()
                .inboundBuilder("in-4")
                .informationListBarcode("Зп-4")
                .build()
                .linkBoxes("XDOC-7")
                .fixInbound()
                .inboundBuilder("in-5")
                .informationListBarcode("Зп-5")
                .build()
                .linkBoxes("XDOC-8")
                .fixInbound();

        Sortable box1 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-7").orElseThrow();
        Sortable box2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-8").orElseThrow();
        Sortable basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();

        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        flow
                .sortToAvailableCell("XDOC-1", "XDOC-2", "XDOC-3", "XDOC-4", "XDOC-5", "XDOC-6")
                .createOutbound("out-1")
                .externalId("reg-1")
                .addRegistryBoxes("XDOC-7", "XDOC-8")
                .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3", "XDOC-4", "XDOC-5", "XDOC-6", basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell("XDOC-2", "XDOC-3", "XDOC-4", "XDOC-5")
                .prepareToShip("XDOC-3", "XDOC-4");

        ApiInboundTaskDto inboundTaskDto1 = ApiInboundTaskDto.builder()
                .preparedPallets(0)
                .sortedPallets(0)
                .keepedPallets(1)
                .informationListCode("Зп-1")
                .cells(Set.of(bufferCell.getScNumber()))
                .status(ApiInboundTaskStatus.NOT_STARTED)
                .build();

        ApiInboundTaskDto inboundTaskDto2 = ApiInboundTaskDto.builder()
                .preparedPallets(1)
                .sortedPallets(1)
                .keepedPallets(0)
                .informationListCode("Зп-2")
                .cells(Set.of())
                .status(ApiInboundTaskStatus.PREPARING)
                .build();

        ApiInboundTaskDto inboundTaskDto3 = ApiInboundTaskDto.builder()
                .preparedPallets(1)
                .sortedPallets(1)
                .keepedPallets(1)
                .informationListCode("Зп-3")
                .cells(Set.of(bufferCell.getScNumber()))
                .status(ApiInboundTaskStatus.SORTING)
                .build();

        ApiInboundTaskDto inboundTaskDto4 = ApiInboundTaskDto.builder()
                .preparedPallets(0)
                .sortedPallets(0)
                .keepedPallets(1)
                .informationListCode("Зп-4")
                .cells(Set.of(bufferCell2.getScNumber()))
                .status(ApiInboundTaskStatus.NOT_STARTED)
                .build();

        ApiInboundTaskDto inboundTaskDto5 = ApiInboundTaskDto.builder()
                .preparedPallets(0)
                .sortedPallets(0)
                .keepedPallets(1)
                .informationListCode("Зп-5")
                .cells(Set.of(bufferCell2.getScNumber()))
                .status(ApiInboundTaskStatus.NOT_STARTED)
                .build();

        ApiOutboundDto apiOutboundDto = ApiOutboundDto.builder()
                .status(OutboundInternalStatusDto.NOT_FULLY_PREPARED)
                .shipCell(shipCell.getScNumber())
                .plannedPalletCount(7)
                .preparedPalletCount(4)
                .inboundCount(5)
                .destination(TestFactory.warehouse().getIncorporation())
                .externalId("out-1")
                .shipCellActivated(true)
                .build();


        ApiOutboundTaskDto dto = ApiOutboundTaskDto.builder()
                .inbounds(List.of(inboundTaskDto2, inboundTaskDto3, inboundTaskDto4, inboundTaskDto5, inboundTaskDto1))
                .outbound(apiOutboundDto)
                .build();

        caller.getXDocOutbound("out-1")
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponse(dto)));
    }

    private String getExpectedResponse(ApiOutboundDto... dtos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"outbounds\":[");
        for (ApiOutboundDto dto : dtos) {
            sb.append(getApiOutboundDto(dto)).append(",");
        }
        return sb.substring(0, sb.length() - 1) + "]}";
    }

    private String getApiOutboundDto(ApiOutboundDto dto) {
        return "{\"externalId\":\"" +
                dto.getExternalId() +
                "\",\"destination\":\"" +
                dto.getDestination() +
                "\",\"status\":\"" +
                dto.getStatus() +
                "\",\"shipCell\":\"" +
                dto.getShipCell() +
                "\",\"inboundCount\":" +
                dto.getInboundCount() +
                ",\"plannedPalletCount\":" +
                dto.getPlannedPalletCount() +
                ",\"preparedPalletCount\":" +
                dto.getPreparedPalletCount() +
                ",\"shipCellActivated\":" +
                dto.isShipCellActivated() +
                "}";
    }

    private String getResponseForApiInbound(ApiInboundTaskDto dto) {
        String cells = "[" + String.join(",", dto.getCells()) + "]";
        return "{\"informationListCode\":\"" +
                dto.getInformationListCode() +
                "\",\"status\":\"" +
                dto.getStatus().name() +
                "\",\"sortedPallets\":" +
                dto.getSortedPallets() +
                ",\"keepedPallets\":" +
                dto.getKeepedPallets() +
                ",\"preparedPallets\":" +
                dto.getPreparedPallets() +
                ",\"cells\":" +
                cells +
                "}";
    }

    private String getExpectedResponse(ApiOutboundTaskDto dto) {
        String inbounds =
                "[" + dto.getInbounds().stream().map(this::getResponseForApiInbound).collect(Collectors.joining(
                        ",")) + "]";
        return """
                {
                    "outbound": %s,
                    "inbounds": %s
                }
                """.formatted(getApiOutboundDto(dto.getOutbound()), inbounds);
    }

}
