package ru.yandex.market.fulfillment.stockstorage.freeze.outbounds;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.configuration.AsyncTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.domain.converter.SSEntitiesConverter;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockFreeze;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnfreezeJob;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.repository.StockFreezeRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.StockRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.UnfreezeJobRepository;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.UnfreezeJobExecutor;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;
import ru.yandex.market.fulfillment.stockstorage.util.filter.FfUpdatedFieldFilter;
import ru.yandex.market.fulfillment.stockstorage.util.filter.UpdatedFieldFilter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.OUTBOUND;
import static ru.yandex.market.fulfillment.stockstorage.util.ModelUtil.resourceId;

@Import(AsyncTestConfiguration.class)
@DatabaseSetup("classpath:database/states/system_property.xml")
public class OutboundUnfreezeAsyncTest extends AbstractContextualTest {

    @Autowired
    private UnfreezeJobExecutor unfreezeJobExecutor;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private UnfreezeJobRepository unfreezeJobRepository;
    @Autowired
    private StockFreezeRepository stockFreezeRepository;
    @Autowired
    private FulfillmentClient lgwClient;
    @Autowired
    private AsyncWaiterService asyncWaiterService;

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void skipUnfreezeIfUnfreezeAlreadyCaught() throws Exception {
        mockMvc.perform(delete(OUTBOUND + "/123456"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, never()).logUnfreezeScheduled(any(FreezeReason.class), anyList());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unfreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = mockMvc.perform(delete(OUTBOUND + "/123456789"))
                .andExpect(status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);

        verify(freezeEventAuditService, never()).logUnfreezeScheduled(any(FreezeReason.class), anyList());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbound_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/two_unfreeze_scheduled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unfreezeScheduled() throws Exception {
        mockMvc.perform(delete(OUTBOUND + "/12345"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(delete(OUTBOUND + "/123456"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(2))
                .logUnfreezeScheduled(anyList());
        Thread.sleep(100);
        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/stocks_outbounds_unfreeze_scheduled.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {UpdatedFieldFilter.class, FfUpdatedFieldFilter.class})
    public void unfreezeJobExecuted() {
        UnitId unitId0 = new UnitId("sku0", 12L, 1);
        UnitId unitId1 = new UnitId("sku1", 12L, 1);
        when(lgwClient.getStocks(
                Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId0)), new Partner(1L)))
                .thenReturn(getSkuStock(unitId0));
        when(lgwClient.getStocks(
                Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId1)), new Partner(1L)))
                .thenReturn(getSkuStock(unitId1));

        unfreezeJobExecutor.executeNextJob();
        unfreezeJobExecutor.executeNextJob();

        asyncWaiterService.awaitTasks();
        checkStock(unitId0);
        checkStock(unitId1);
        checkFreezesDeletedCorrectly();
        checkUnfreezeJobExecuted();
    }

    private void checkUnfreezeJobExecuted() {
        Iterable<UnfreezeJob> unfreezeJobs = unfreezeJobRepository.findAll();
        softly
                .assertThat(unfreezeJobs)
                .hasSize(2);
        softly
                .assertThat(Streams.stream(unfreezeJobs).allMatch(UnfreezeJob::isExecuted))
                .isTrue();
        softly
                .assertThat(Streams.stream(unfreezeJobs)
                        .map(UnfreezeJob::getStockFreeze)
                        .allMatch(StockFreeze::isDeleted))
                .isTrue();

        Streams.stream(unfreezeJobs)
                .forEach(job -> {
                    verify(freezeEventAuditService).logUnfreezeSuccessful(job);
                    verify(skuEventAuditService).logStockUnfreeze(job);
                });

    }

    private void checkFreezesDeletedCorrectly() {
        FreezeReason reason = FreezeReason.of("123456", FreezeReasonType.OUTBOUND);
        List<StockFreeze> outboundFreezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                reason);
        softly
                .assertThat(outboundFreezes)
                .hasSize(0);

        List<StockFreeze> outboundUnfreezeFreezes =
                unfreezeJobRepository.findByFreezeReason(reason)
                        .stream().map(UnfreezeJob::getStockFreeze).collect(Collectors.toList());

        List<StockFreeze> restFreezes = stockFreezeRepository.findAll().stream()
                .filter(freeze -> outboundUnfreezeFreezes.stream().noneMatch(
                        deletedFreeze -> deletedFreeze.getId().equals(freeze.getId())))
                .collect(Collectors.toList());
        softly
                .assertThat(restFreezes)
                .hasSize(5);
    }

    private void checkStock(UnitId unitId) {
        Stock stock1 = stockRepository.findByUnitIdAndType(unitId,
                ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.DEFECT);
        softly
                .assertThat(stock1.getAmount())
                .isEqualTo(50000);
        softly
                .assertThat(stock1.getFreezeAmount())
                .isEqualTo(50000);
    }

    private List<ItemStocks> getSkuStock(UnitId unitId) {
        DateTime updated = DateTime.fromOffsetDateTime(OffsetDateTime.now());
        return Collections.singletonList(
                new ItemStocks(
                        new ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId(
                                null,
                                unitId.getVendorId(),
                                unitId.getSku()
                        ),
                        resourceId(String.valueOf(
                                unitId.getWarehouseId()),
                                String.valueOf(unitId.getWarehouseId())
                        ),
                        ImmutableList.of(
                                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock(
                                        StockType.DEFECT, 50000, updated
                                )
                        )
                )
        );
    }
}
