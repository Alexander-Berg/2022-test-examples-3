package ru.yandex.market.sc.internal.domain.report;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.common.util.db.PgSequenceIdGenerator;
import ru.yandex.market.sc.core.domain.cell.CellQueryService;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.InboundQueryService;
import ru.yandex.market.sc.core.domain.inbound.model.GroupPutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfoRepository;
import ru.yandex.market.sc.core.domain.outbound.OutboundQueryService;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.route.model.TransferActDto;
import ru.yandex.market.sc.core.domain.route_so.RouteSoQueryService;
import ru.yandex.market.sc.core.domain.sortable.SortableCommandService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.internal.util.ScIntControllerCaller.PUT_CAR_INFO_REQUEST;

/**
 * @author ogonek
 */
@EmbeddedDbIntTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class XdocPartnerReportServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    XDocFlow flow;
    @Autowired
    SortableCommandService sortableCommandService;
    @Autowired
    SortableQueryService sortableQueryService;
    @SpyBean
    XdocPartnerReportService xdocPartnerReportService;
    @SpyBean
    OutboundQueryService outboundQueryService;
    @SpyBean
    InboundQueryService inboundQueryService;
    @Autowired
    RouteSoQueryService routeSoQueryService;
    @Autowired
    CellQueryService cellQueryService;
    @Autowired
    Clock clock;
    @MockBean
    TplClient tplClient;
    @Autowired
    InboundInfoRepository inboundInfoRepository;
    @Autowired
    @Qualifier("transfer_material_values_for_storage_act_seq")
    PgSequenceIdGenerator transferMaterialValuesForStorageActSeq;
    @Autowired
    ScIntControllerCaller caller;

    @Test
    void canNotGetOutboundTransferActPdfForNotShippedOutbound() {
        var sortingCenter = testFactory.storedSortingCenter();
        Outbound outbound = testFactory.createOutbound(sortingCenter);
        assertThrows(
                TplIllegalArgumentException.class,
                () -> xdocPartnerReportService.getOutboundTransferActPdf(outbound.getExternalId())
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("?????????????????? ?????? ????????????????. ???????????????? outboundTransferAct.pdf")
    void getOutboundTransferActPdf() {
        Mockito.doAnswer(invocation -> TransferActDto.builder()
                        .number("??-111")
                        .date(LocalDate.of(2021, 4, 22))
                        .sender("?????? ????????????.????????????")
                        .executor("legalName")
                        .recipient("?????? ????????????.????????????")
                        .senderScName("???? ?????? ????????????.????????????")
                        .recipientScName("???? ????????????????????")
                        .orders(
                                List.of(
                                        TransferActDto.Order.builder()
                                                .externalId("single-place-order")
                                                .placeMainPartnerCode("single-place-order")
                                                .totalSum(new BigDecimal("336456.00"))
                                                .lotName("SC_LOT_pallet-1")
                                                .build()
                                )
                        )
                        .courier("name")
                        .totalPlaces(1)
                        .build())
                .when(outboundQueryService).getTransferAct(anyString());
        var actual = xdocPartnerReportService.getOutboundTransferActPdf("111");

        assertThat(actual).isNotEmpty();

        Files.write(Paths.get("out.pdf"), actual);

        assertThat(actual).isNotEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("?????????????????? QR-???????? ????????????????. ???????????????? outboundQrPdf.pdf")
    void getOutboundQrPdfTest() {
        Outbound outbound = flow.createOutbound("OUT-QR").buildRegistryAndGetOutbound();

        byte[] actual = xdocPartnerReportService.getOutboundQrPdf(outbound.getExternalId());

        Files.write(Paths.get("out.pdf"), actual);

        assertThat(actual).isNotEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("?????????????????? ???????? ???? ????????????????. ???????????????? partnerOutboundInfoPdf.pdf")
    void getPartnerOutboundInfoPdfTest() {
        SortingCenter sortingCenter = flow.getSortingCenter();

        Inbound inbound = flow.createInboundAndGet("IN-123", "????-3700994010", "324234234-2");
        Sortable pallet = flow.createSortableAndGet("XDOC-pallet-1", SortableType.XDOC_PALLET, inbound);
        Sortable box = flow.createSortableAndGet("XDOC-box-1", SortableType.XDOC_BOX, inbound);
        Sortable box2 = flow.createSortableAndGet("XDOC-box-2", SortableType.XDOC_BOX, inbound);
        Sortable box3 = flow.createSortableAndGet("XDOC-box-3", SortableType.XDOC_BOX, inbound);

        Inbound inbound2 = flow.createInboundAndGet("IN-501", "1??-222", "324234234-2");
        Sortable pallet2 = flow.createSortableAndGet("XDOC-pallet-2", SortableType.XDOC_PALLET, inbound2);

        Warehouse warehouse = testFactory.storedWarehouse("324234234-2");

        Cell bufferCell = flow.createBufferCellAndGet("BUFFER_SOF", warehouse.getYandexId());
        var lot = flow.createBasket(bufferCell);
        var lot2 = flow.createBasket(bufferCell);
        Sortable sortableBasket = sortableTestFactory.getLotAsSortable(lot);

        flow.sortBoxToLot(box, lot);
        flow.sortBoxToLot(box2, lot2);

        Outbound outbound = flow.createOutbound("OUT-123")
                .addRegistryPallets(sortableBasket.getRequiredBarcodeOrThrow(), "XDOC-pallet-1", "XDOC-pallet-2")
                .addRegistryBoxes(box.getRequiredBarcodeOrThrow(), box2.getRequiredBarcodeOrThrow(), box3.getRequiredBarcodeOrThrow())
                .buildRegistryAndGetOutbound();

        Long shipCellId = sortableTestFactory.getAnyShipCellId(outbound);
        if (shipCellId == null) {
            throw new IllegalStateException("Outbound has no ship cell");
        }

        sortableTestFactory.sort(pallet.getId(), shipCellId, flow.getUser());

        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        byte[] actual = xdocPartnerReportService.getPartnerOutboundInfoPdf(
                List.of("OUT-123")
        );

        Files.write(Paths.get("out.pdf"), actual);

        assertThat(actual).isNotEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("?????????????????? ???????? ???? ????????????????. ???????????????? partnerOutboundInfoPdf.pdf")
    void getPartnerOutboundInfoPdfWithNullCourierTest() {
        SortingCenter sortingCenter = flow.getSortingCenter();

        Inbound inbound = flow.createInboundAndGet("IN-123", "????-3700994010", "324234234-2");
        Sortable pallet = flow.createSortableAndGet("XDOC-pallet-1", SortableType.XDOC_PALLET, inbound);
        Sortable box = flow.createSortableAndGet("XDOC-box-1", SortableType.XDOC_BOX, inbound);
        Sortable box2 = flow.createSortableAndGet("XDOC-box-2", SortableType.XDOC_BOX, inbound);
        Sortable box3 = flow.createSortableAndGet("XDOC-box-3", SortableType.XDOC_BOX, inbound);

        Inbound inbound2 = flow.createInboundAndGet("IN-501", "1??-222", "324234234-2");
        Sortable pallet2 = flow.createSortableAndGet("XDOC-pallet-2", SortableType.XDOC_PALLET, inbound2);

        Warehouse warehouse = testFactory.storedWarehouse("324234234-2");

        Cell bufferCell = flow.createBufferCellAndGet("BUFFER_SOF", warehouse.getYandexId());
        var lot = flow.createBasket(bufferCell);
        var lot2 = flow.createBasket(bufferCell);
        Sortable sortableBasket = sortableTestFactory.getLotAsSortable(lot);

        flow.sortBoxToLot(box, lot);
        flow.sortBoxToLot(box2, lot2);

        Outbound outbound = flow.outboundBuilder("OUT-123")
                .courierExternalId(null)
                .toRegistryBuilder()
                .addRegistryPallets(sortableBasket.getRequiredBarcodeOrThrow(), "XDOC-pallet-1", "XDOC-pallet-2")
                .addRegistryBoxes(box.getRequiredBarcodeOrThrow(), box2.getRequiredBarcodeOrThrow(), box3.getRequiredBarcodeOrThrow())
                .buildRegistryAndGetOutbound();

        Long shipCellId = sortableTestFactory.getAnyShipCellId(outbound);
        if (shipCellId == null) {
            throw new IllegalStateException("Outbound has no ship cell");
        }

        sortableTestFactory.sort(pallet.getId(), shipCellId, flow.getUser());

        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        byte[] actual = xdocPartnerReportService.getPartnerOutboundInfoPdf(
                List.of("OUT-123")
        );

        Files.write(Paths.get("out.pdf"), actual);

        assertThat(actual).isNotEmpty();
    }

    @DisplayName("?????? ?? ????????????-???????????????? ?????????????? ???????????????????????? ?????????????????? ???? ???????????????? ????-1")
    @Test
    @SneakyThrows
    void transferMaterialValuesForStorageAct() {
        var sc = testFactory.storedSortingCenter();

        List<Inbound> inbounds = new ArrayList<>();
        IntStream.rangeClosed(1, 10)
                .forEach(i ->
                        inbounds.add(flow.inboundBuilder("in-" + i)
                                .informationListBarcode("????-37000" + i)
                                .build()
                                .linkPallets("XDOC-1" + i)
                                .getInbound())
                );

        var inboundInfos = inboundInfoRepository.findAll();
        assertThat(inboundInfos)
                .hasSize(10)
                .allMatch(info -> info.getDocNumberMH() == null,
                        "???? ???????????????? ?????????????????? ?? ???????????? ?????? ?????????? ?????????????????? ??????????????????????");

        long expectedDocNumber = transferMaterialValuesForStorageActSeq.getId() + 1;

        // ?????????????????? ?????????????????? ?? ???????????? ??????
        byte[] actual = xdocPartnerReportService.getTransferMaterialValuesForStorageAct(sc, inbounds);

        Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();

        inboundInfos = inboundInfoRepository.findAll();
        assertThat(inboundInfos)
                .hasSize(10)
                .allMatch(info -> expectedDocNumber == info.getDocNumberMH(),
                        "?????????? ???????????????? ?????????????????? ?? ???????????? ??????, ???????? ?? ?????? ???? ?????????? ?????????????????? ???????????????? ?? ???????? " +
                                "????????????????");

        // ?????????????????? ???????????? ???? ?????????????????? ??????????????????
        xdocPartnerReportService.getTransferMaterialValuesForStorageAct(sc, inbounds);

        inboundInfos = inboundInfoRepository.findAll();
        assertThat(inboundInfos)
                .hasSize(10)
                .allMatch(info -> expectedDocNumber == info.getDocNumberMH(),
                        "?????????? ?????????????????? ???????????????? ???????????????????? ?????????? ?????????????????? ?????????????? ?????????????????? ??????????????????");
    }


    @DisplayName("?????? ?? ????????????-???????????????? ?????????????? ???????????????????????? ?????????????????? ???? ???????????????? ????-1 ???????????? ?????????? (???? ????????????????????????" +
            " ???? ???????? ??????????????/????????????)")
    @Test
    @SneakyThrows
    void transferMaterialValuesForStorageActEmpty() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("????-370001")
                .build();

        // ?????????????????? ?????????????????? ?? ???????????? ??????
        byte[] actual = xdocPartnerReportService
                .getTransferMaterialValuesForStorageAct(testFactory.storedSortingCenter(), "????-370001");

       Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();
    }

    @DisplayName("???????? ???????????????? ???????????????????? ??????????????, ???? ?????? ?????????????? ?????????????????????? ????-1 ?????? ?????????? ????????????????, ??????????????????????????" +
            " ?????? ???????? ????????????")
    @Test
    @SneakyThrows
    void transferMaterialValuesForStorageActGeneratedForAllInboundsInGroupWhenRequestedForOneInbound() {
        var inbound1 = flow.inboundBuilder("in-1")
                .informationListBarcode("????-370001")
                .realSupplierName("?????? ?????? ???? ??????")
                .build()
                .getInbound();
        var inbound2 = flow.inboundBuilder("in-2")
                .informationListBarcode("????-370002")
                .realSupplierName("?????? ?????? ???? ??????")
                .build()
                .getInbound();

        // ???????????????????? ?? ????????????
        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of(inbound1.getExternalId(), inbound2.getExternalId()));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);
        caller.inboundsCarArrived(request).andExpect(status().isOk());

        // ?????????????? ???????????? ?? ??????????????????
        flow.toArrival("in-1").linkPallets("XDOC-1");
        flow.toArrival("in-2").linkPallets("XDOC-2");


        var sc = testFactory.storedSortingCenter();
        byte[] actual = xdocPartnerReportService.getTransferMaterialValuesForStorageAct(sc, inbound1.getExternalId());

        Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();

        Mockito.verify(xdocPartnerReportService, Mockito.atLeastOnce())
                .getTransferMaterialValuesForStorageAct(sc, List.of(inbound1, inbound2));
    }


    @SneakyThrows
    @Test
    void getConsignmentNote() {
        flow.inboundBuilder("in-1").informationListBarcode("????-1").build();
        byte[] actual = xdocPartnerReportService.getConsignmentNote("in-1", flow.getSortingCenter());
        Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();
    }

    @SneakyThrows
    @Test
    void getConsignmentNoteInfoListCode() {
        flow.inboundBuilder("in-1").informationListBarcode("????-1").build().linkPallets("XDOC-1").fixInbound()
                .inboundBuilder("in-2").informationListBarcode("????-2").build().linkPallets("XDOC-2").fixInbound()
                .inboundBuilder("in-3").informationListBarcode("????-3").build().linkPallets("XDOC-3").fixInbound()
                .inboundBuilder("in-4").informationListBarcode("????-4").build().linkPallets("XDOC-4").fixInbound()
                .inboundBuilder("in-5").informationListBarcode("????-5").build().linkPallets("XDOC-5").fixInbound()
                .inboundBuilder("in-6").informationListBarcode("????-6").build().linkPallets("XDOC-6").fixInbound()
                .inboundBuilder("in-7").informationListBarcode("????-7").build().linkPallets("XDOC-7").fixInbound()
                .inboundBuilder("in-8").informationListBarcode("????-8").build().linkPallets("XDOC-8").fixInbound()
                .inboundBuilder("in-9").informationListBarcode("????-9").build().linkPallets("XDOC-9").fixInbound()
                .inboundBuilder("in-10").informationListBarcode("????-10").build().linkPallets("XDOC-31").fixInbound();

        byte[] actual = xdocPartnerReportService.getConsignmentNote("????-2", flow.getSortingCenter());
        Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();
    }

    @SneakyThrows
    @Test
    void getConsignmentNoteForInboundGroup() {
        var inbound1 = flow.inboundBuilder("in-1").informationListBarcode("????-1").build().getInbound();
        var inbound2 = flow.inboundBuilder("in-2").informationListBarcode("????-2").build().getInbound();
        var inbound3 = flow.inboundBuilder("in-3").informationListBarcode("????-3").build().getInbound();


        // ?????????????????????? ????????????????
        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of("in-1", "in-2", "in-3"));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);
        caller.inboundsCarArrived(request).andExpect(status().isOk());


        // ???????????? ???????????????? ?????????????????? ?????? ????????????
        byte[] actual = xdocPartnerReportService.getConsignmentNote("????-1", flow.getSortingCenter());
        Files.write(Paths.get("out.pdf"), actual);
        assertThat(actual).isNotEmpty();


        // ?????????????????? ?????????? ???????????????? ???????????????? ?????????????????? ?? 3 ????????????????????
        Mockito.verify(xdocPartnerReportService, Mockito.atLeastOnce())
                .getConsignmentNote(List.of(inbound1, inbound2, inbound3));
    }

}
