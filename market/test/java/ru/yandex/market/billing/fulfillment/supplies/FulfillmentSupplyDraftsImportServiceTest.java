package ru.yandex.market.billing.fulfillment.supplies;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.fulfillment.supplies.dao.FulfillmentSupplyDraftTrantimesService;
import ru.yandex.market.billing.fulfillment.supplies.dao.FulfillmentSupplyDraftsYtDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.fulfillment.supplies.dao.FulfillmentSupplyDraftsDao;
import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupplyDraft;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentSupplyDraftsImportServiceTest extends FunctionalTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 2, 9);

    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @Autowired
    private FulfillmentSupplyDraftTrantimesService fulfillmentSupplyDraftTrantimesService;

    @Autowired
    private FulfillmentSupplyDraftsDao fulfillmentSupplyDraftsDao;

    @Autowired
    private FulfillmentSupplyDraftsImportService fulfillmentSupplyDraftsImportService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void initYt() {
        when(hahnYt.cypress()).thenReturn(cypress);
    }


    @Test
    @DbUnitDataSet(
            before = "FulfillmentSupplyDraftsImportServiceTest.importSupplyDrafts.before.csv",
            after = "FulfillmentSupplyDraftsImportServiceTest.importSupplyDrafts.after.csv"
    )
    void importSupplyDrafts() {
        FulfillmentSupplyDraftsYtDao ytDaoMock = mock(FulfillmentSupplyDraftsYtDao.class);

        doAnswer(invocation -> {
            Consumer<FulfillmentSupplyDraft> consumer = invocation.getArgument(1);

            getFulfillmentSupplyDrafts().forEach(consumer);

            return null;
        }).when(ytDaoMock).importSupplyDrafts(any(LocalDate.class), any());

        doAnswer(invocation -> true).when(ytDaoMock).verifyYtTableExist(any(LocalDate.class));

        FulfillmentSupplyDraftsImportService dailyImportSupplyService = new FulfillmentSupplyDraftsImportService(
                ytDaoMock,
                fulfillmentSupplyDraftsDao,
                fulfillmentSupplyDraftTrantimesService,
                supplierService,
                transactionTemplate,
                environmentService
        );


        dailyImportSupplyService.process(IMPORT_DATE);


        verify(ytDaoMock, Mockito.times(1)).verifyYtTableExist(eq(IMPORT_DATE));
        verify(ytDaoMock, Mockito.times(1)).importSupplyDrafts(eq(IMPORT_DATE), any());
        verifyNoMoreInteractions(ytDaoMock);
    }

    @Test
    void testFailYtTableDoesntExist() {
        initCypressAnswer(List.of("table1"));

        Exception e = Assertions.assertThrows(
                IllegalStateException.class,
                () -> fulfillmentSupplyDraftsImportService.process(IMPORT_DATE)
        );
        assertEquals(
                e.getMessage(),
                "YT tables //home/market/production/mstat/dictionaries/fulfillment/supply_order_item/2021-02-10" +
                        " and //home/market/production/mstat/dictionaries/fulfillment/crossdock_inbound_draft/1d/" +
                        "2021-02-09 for day 2021-02-09 not found"
        );
    }

    private static List<FulfillmentSupplyDraft> getFulfillmentSupplyDrafts() {
        return List.of(
                FulfillmentSupplyDraft.builder()
                        .setDraftId(101L)
                        .setShopSku("shopSku1")
                        .setFinalizationDate(IMPORT_DATE)
                        .setPartnerId(11L)
                        .setMarketSku("marketSku1")
                        .setMarketName("marketName1")
                        .setCount(1001)
                        .build(),

                FulfillmentSupplyDraft.builder()
                        .setDraftId(102L)
                        .setShopSku("shopSku2")
                        .setFinalizationDate(IMPORT_DATE)
                        .setPartnerId(12L)
                        .setMarketSku("marketSku2")
                        .setMarketName("marketName2")
                        .setCount(1002)
                        .build(),

                FulfillmentSupplyDraft.builder()
                        .setDraftId(109L)
                        .setShopSku("shopSku9")
                        .setFinalizationDate(IMPORT_DATE)
                        .setPartnerId(19L)
                        .setMarketSku("marketSku9")
                        .setMarketName("marketName9")
                        .setCount(1009)
                        .build()
        );
    }

    private void initCypressAnswer(List<String> existingTables) {
        doAnswer(answer -> {
            final YPath tablePath = answer.getArgument(0);

            return existingTables.contains(tablePath.toString());
        }).when(cypress).exists(any(YPath.class));
    }
}
