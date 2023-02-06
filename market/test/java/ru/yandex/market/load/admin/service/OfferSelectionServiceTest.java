package ru.yandex.market.load.admin.service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.OfferSelectResponse;
import ru.yandex.mj.generated.client.report.model.ReportSearch;
import ru.yandex.mj.generated.client.report.model.Search;
import ru.yandex.mj.generated.client.ss.model.GetStockAmountResponse;
import ru.yandex.mj.generated.client.ss.model.SSItemAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 4/5/22
 */
public class OfferSelectionServiceTest extends AbstractFunctionalTest {

    @MockBean
    protected ReportService reportService;

    @MockBean
    protected StockStorageService ssService;

    @MockBean
    protected YqlService yqlService;

    @Autowired
    protected OfferSelectionService osService;

    @Test
    public void testSelectOffers_Ok() throws ExecutionException, InterruptedException, JsonProcessingException,
            SQLException, ClassNotFoundException {
        OfferSelectResponse osrMock1 = OfferSelectResponse.builder()
                .offerId("1")
                .warehouseId(1)
                .availableAmount(1)
                .wareMd5("ware")
                .shopSku("1")
                .supplierId(1)
                .build();
        OfferSelectResponse osrMock2 = OfferSelectResponse.builder()
                .offerId("2")
                .warehouseId(2)
                .availableAmount(1)
                .wareMd5("ware2")
                .shopSku("2")
                .supplierId(2)
                .build();
        when(yqlService.executeYQL(any(), any())).thenReturn(List.of(osrMock1, osrMock2));
        ReportSearch reportMockAnswer = new ReportSearch();
        reportMockAnswer.setSearch((new Search()).total(1));
        when(reportService.getOfferFromOfferInfo(any(), any())).thenReturn(reportMockAnswer);
        when(ssService.getGetStockAmountResponse(any())).thenReturn((new GetStockAmountResponse()).addItemsItem((new SSItemAmount()).amount(1)));

        List<OfferSelectResponse> selectedOffers = osService.selectOffers(2, 1, 2, List.of(1, 2), List.of(1, 2));

        assertEquals("1", selectedOffers.get(0).getOfferId());
        assertEquals("2", selectedOffers.get(1).getOfferId());
    }

    @Test
    public void testSelectOffers_NotEnough() throws ExecutionException, InterruptedException, JsonProcessingException
            , SQLException, ClassNotFoundException {
        OfferSelectResponse osrMock1 = OfferSelectResponse.builder()
                .offerId("1")
                .warehouseId(1)
                .availableAmount(1)
                .wareMd5("ware")
                .shopSku("1")
                .supplierId(1)
                .build();
        when(yqlService.executeYQL(any(), any())).thenReturn(List.of(osrMock1));
        ReportSearch reportMockAnswer = new ReportSearch();
        reportMockAnswer.setSearch((new Search()).total(1));
        when(reportService.getOfferFromOfferInfo(any(), any())).thenReturn(reportMockAnswer);
        when(ssService.getGetStockAmountResponse(any())).thenReturn((new GetStockAmountResponse()).addItemsItem((new SSItemAmount()).amount(1)));

        assertThrows(IllegalStateException.class, () -> osService.selectOffers(2, 1, 2, List.of(1, 2), List.of(1, 2)));
    }
}
