package ru.yandex.market.tpl.internal.service.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.api.model.transport.TransportTypeParams;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.partner.PartnerTransportTypeDtoMapper;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.internal.service.report.transport.TransportTypeReportDto;
import ru.yandex.market.tpl.internal.service.report.transport.TransportTypeReportDtoMapper;
import ru.yandex.market.tpl.internal.service.report.transport.TransportTypeReportDtoService;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
@Disabled
public class TransportTypeExportTest {

    private final ReportService reportService;

    @MockBean
    private TransportTypeReportDtoService transportTypeReportDtoService;

    // try to mock repo link at firs time
    private final PartnerTransportTypeDtoMapper partnerTransportTypeDtoMapper;
    private final TransportTypeReportDtoMapper transportTypeReportDtoMapper;
    private TransportTypeReportService transportTypeReportService;

    @BeforeEach
    void init() {

        transportTypeReportService = new TransportTypeReportService(
                reportService,
                transportTypeReportDtoService
        );

        given(transportTypeReportDtoService.find(new TransportTypeParams())).willReturn(getTestDataTransportTypes());
    }

    private List<TransportTypeReportDto> getTestDataTransportTypes() {
        RoutingOrderTag pvzTag = makeRoutingOrderTag("pvz", "ПВЗ,  оооооооооочень, оооооооооочень, " +
                "оооооооооочень, оооооооооочень, оооооооооочень много типов заказов ", RoutingOrderTagType.ORDER_TYPE);

        TransportTypeReportDto fordTransit = transportTypeReportDtoMapper.toTransportTypeReportDto(new TransportType(
                "ford transit",
                BigDecimal.TEN,
                Set.of(pvzTag),
                1000,
                RoutingVehicleType.YANDEX_DRIVE,
                0,
                null
        ));
        TransportTypeReportDto matiz = transportTypeReportDtoMapper.toTransportTypeReportDto(new TransportType(
                "matiz оооооооооочень оооооооооочень оооооооооочень оооооооооочень оооооооооочень оооооооооочень " +
                        "длинное название",
                BigDecimal.TEN,
                Set.of(pvzTag),
                100,
                RoutingVehicleType.COMMON,
                0,
                null
        ));

        return List.of(fordTransit, matiz);
    }


    private RoutingOrderTag makeRoutingOrderTag(String name, String description, RoutingOrderTagType tag) {
        return new RoutingOrderTag(
                name,
                description,
                BigDecimal.ONE,
                BigDecimal.ONE,
                tag,
                Set.of()
        );
    }

    @Test
    @Disabled
    @DisplayName("Тестик для отладки верстки отчета")
    void getTransportTypesReport() throws IOException {
        String path = System.getProperty("user.home") +  "/Downloads/transport" + Instant.now() + ".xlsx";
        FileOutputStream fos = new FileOutputStream(path);

        transportTypeReportService.getTransportTypeReport(fos, new TransportTypeParams());

        fos.flush();
        fos.close();
    }
}
