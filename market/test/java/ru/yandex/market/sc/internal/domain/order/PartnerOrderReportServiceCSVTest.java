package ru.yandex.market.sc.internal.domain.order;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.partner.order.PartnerOrderParamsDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbIntTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class PartnerOrderReportServiceCSVTest {

    @Autowired
    PartnerOrderReportService partnerOrderReportService;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;
    @Autowired
    XDocFlow flow;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        testFactory.setupMockClock(clock);
    }

    @DisplayName("Печать csv для orders | столбец со значением > 100 символов печатается корректно")
    @Test
    void ordersCSV() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-129292929|222222222222222222|222222222|22222222222222222|2222222222222222222|sdfsd")
                .build()
                .linkPallets("XDOC-1", "XDOC-2", "XDOC-3", "XDOC-4");

        mockDateTime("XDOC-1", Instant.now(clock));
        mockDateTime("XDOC-2", Instant.now(clock).plusSeconds(1));
        mockDateTime("XDOC-3", Instant.now(clock).plusSeconds(2));
        mockDateTime("XDOC-4", Instant.now(clock).plusSeconds(3));

        byte[] reportData = partnerOrderReportService.getOrdersAsXLSX(
                sortingCenter,
                PartnerOrderParamsDto.builder().build(),
                Pageable.unpaged()
        );

        assertThat(reportData).isNotEmpty();
    }

    private void mockDateTime(String barcode, Instant instant) {
        jdbcTemplate.update("UPDATE sortable " +
                "SET created_at = '" + instant + "' WHERE barcode = '" + barcode + "'");
    }
}
