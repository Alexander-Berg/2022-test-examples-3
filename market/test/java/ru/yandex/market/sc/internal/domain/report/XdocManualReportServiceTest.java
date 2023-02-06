package ru.yandex.market.sc.internal.domain.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.inbound.model.TmInboundDto;
import ru.yandex.market.sc.core.domain.inbound.model.TmInboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.model.DistributionCenterStateDto;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@EmbeddedDbIntTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class XdocManualReportServiceTest {

    @Autowired
    XDocFlow flow;
    @Autowired
    XdocManualReportService xdocManualReportService;
    @MockBean
    TmClient tmClient;
    @Autowired
    ScIntControllerCaller caller;

    @Test
    void getDiffXlsx() throws IOException {
        TmInboundDto first = new TmInboundDto()
                .setBoxNumber(0)
                .setPalletNumber(1)
                .setStatus(TmInboundStatus.ARRIVED_ON_DC)
                .setDestinationName(TestFactory.warehouse().getIncorporation())
                .setDestinationId(TestFactory.warehouse().getYandexId())
                .setInformationListCode("Зп-111");

        TmInboundDto second = new TmInboundDto()
                .setBoxNumber(0)
                .setPalletNumber(0)
                .setStatus(TmInboundStatus.CONFIRMED)
                .setDestinationName(TestFactory.warehouse().getIncorporation())
                .setDestinationId(TestFactory.warehouse().getYandexId())
                .setInformationListCode("Зп-112");

        TmInboundDto third = new TmInboundDto()
                .setBoxNumber(0)
                .setPalletNumber(2)
                .setStatus(TmInboundStatus.SHIPPED_FROM_DC)
                .setDestinationName(TestFactory.warehouse().getIncorporation())
                .setDestinationId(TestFactory.warehouse().getYandexId())
                .setInformationListCode("Зп-114");

        when(tmClient.getDcState(flow.getSortingCenter().getId()))
                .thenReturn(new DistributionCenterStateDto(List.of(first, second, third)));

        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-113")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-112")
                .build()
                .and()
                .inboundBuilder("in-4")
                .informationListBarcode("Зп-114")
                .build()
                .linkPallets("XDOC-4", "XDOC-5")
                .fixInbound();
        byte[] actual = xdocManualReportService.getDiffBetweenTmAndSc(flow.getSortingCenter().getId());

        Files.write(Paths.get("out.xlsx"), actual);
        assertThat(actual).isNotEmpty();
    }

}
