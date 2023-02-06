package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.CalendaringStatus;
import ru.yandex.market.delivery.transport_manager.domain.dto.IncludedSupplyDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.Tag;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationLegalInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.tag.TagReceiver;

class XDocIncludedTransportationsInfoBuilderTest extends AbstractContextualTest {
    public static final CalendaringStatus INCLUDED_SUPPLY_STATUS = CalendaringStatus.ACCEPTED_BY_XDOC_SERVICE;
    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private TransportationUnitMapper transportationUnitMapper;
    @Autowired
    private RegisterMapper registerMapper;
    @Autowired
    private RegisterUnitMapper registerUnitMapper;
    @Autowired
    private TransportationLegalInfoMapper legalInfoMapper;
    @Autowired
    private TagReceiver tagReceiver;

    @DisplayName("Получить информацию о вложенных поставках перемещения РЦ-ФФ")
    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void getIncludedSuppliesInfo() {
        doTest(false);
    }

    @DisplayName("Получить информацию о вложенных поставках перемещения РЦ-ФФ для Break Bulk XDock")
    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml",
    })
    @Test
    void getIncludedSuppliesInfoBreakBulkXdock() {
        doTest(true);
    }

    private void doTest(boolean breakBulkXdock) {
        List<IncludedSupplyDto> supplyDtos = new XDocIncludedTransportationsInfoBuilder(
            transportationMapper,
            transportationUnitMapper,
            registerMapper,
            registerUnitMapper,
            tagReceiver,
            List.of(
                new Tag(1L, TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN, "1"),
                new Tag(1L, TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN, "3"),
                new Tag(1L, TagCode.FFWF_INCLUDED_REQUEST_ID_FACT, "5")
            ),
            TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN,
            Map.of(
                // Это коробка 1-й поставки, которая стоит на мультипаллете вместе с коробкой abc3 второй поставки
                "abc0", List.of("multiplallet2"),
                // Коробки второй поставки
                "abc1", List.of("multiplallet1"),
                "abc2", List.of("multiplallet1"),
                "abc3", List.of("multiplallet2")
            ),
            INCLUDED_SUPPLY_STATUS,
            breakBulkXdock
        )
            .build();

        softly.assertThat(supplyDtos).containsExactlyInAnyOrder(
            new IncludedSupplyDto(
                1,
                "wms-0001",
                "Зп-0001",
                2,
                1,
                3,
                new BigDecimal("0.000018"),
                "12345",
                IncludedSupplyDto.SupplierType.FIRST_PARTY,
                "ИП Петров",
                new BigDecimal("3.40"),
                true,
                false,
                INCLUDED_SUPPLY_STATUS
            ),
            new IncludedSupplyDto(
                3,
                "wms-0002",
                null,
                2,
                3,
                0,
                new BigDecimal("0.000000"),
                "2",
                IncludedSupplyDto.SupplierType.THIRD_PARTY,
                "Сторонний поставщик",
                new BigDecimal("2.50"),
                false,
                true,
                INCLUDED_SUPPLY_STATUS
            )
        );
    }
}
