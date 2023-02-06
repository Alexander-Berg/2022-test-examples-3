package ru.yandex.market.admin.service.remote;

import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.magic.defender.DefenderDataStore;
import ru.yandex.market.admin.service.AdminCampaignService;
import ru.yandex.market.core.billing.BillingService;
import ru.yandex.market.core.billing.model.OperationType;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.security.SecManager;

import static org.junit.Assert.fail;

/**
 * @author zoom
 */
public class RemoteCampaignUIServiceTest {

    private static final ThrowsException THROWS_EXCEPTION = new ThrowsException(new UnsupportedOperationException());
    private static final CampaignInfo CAMPAIGN_INFO_301 = new CampaignInfo(301, 100001, 103, 1);
    private static final CampaignInfo CAMPAIGN_INFO_302 = new CampaignInfo(302, 100002, 103, 1);
    private static final CampaignInfo CAMPAIGN_INFO_303 = new CampaignInfo(303, 100003, 103, 1);
    private static final CampaignInfo CAMPAIGN_INFO_304_NEW = new CampaignInfo(304, 100001, 0, 1);
    private static final int USER_ID = 1000;
    private RemoteCampaignUIService service;
    private CampaignService campaignService;
    private PartnerService partnerService;
    private BillingService billingService;
    private Transaction transaction;
    private DefenderDataStore defenderDataStore;
    private SecManager secManager;
    private DatasourceService datasourceService;
    private AdminCampaignService adminCampaignService;


    @Before
    public void setUp() throws Exception {
        service = new RemoteCampaignUIService() {
            @Override
            protected long getUid() {
                return USER_ID;
            }
        };
        campaignService = Mockito.mock(CampaignService.class, THROWS_EXCEPTION);
        service.setCampaignService(campaignService);
        partnerService = Mockito.mock(PartnerService.class);
        billingService = Mockito.mock(BillingService.class, THROWS_EXCEPTION);
        Mockito.doAnswer(inv -> 12345L)
                .when(billingService)
                .makeCorrection(
                        Mockito.anyInt(),
                        Mockito.anyLong(),
                        Mockito.anyLong(),
                        Mockito.any(Date.class),
                        Mockito.anyBoolean(),
                        Mockito.anyString()
                );
        service.setBillingService(billingService);
        transaction = Mockito.mock(Transaction.class);
        service.setTransactionTemplate(new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                transaction.begin();
                try {
                    return action.doInTransaction(new SimpleTransactionStatus());
                } catch (Throwable ex) {
                    transaction.rollback();
                    throw ex;
                } finally {
                    transaction.commit();
                }
            }
        });
        defenderDataStore = Mockito.mock(DefenderDataStore.class);
        service.setDefenderInfoStore(defenderDataStore);
        secManager = Mockito.mock(SecManager.class);
        Mockito.doReturn(true).when(secManager).canDo(Mockito.anyString(), Mockito.any());
        datasourceService = Mockito.mock(DatasourceService.class);

        adminCampaignService = new AdminCampaignService(campaignService,
                Mockito.mock(ParamService.class), secManager, datasourceService,
                Mockito.mock(PartnerPlacementProgramService.class), partnerService);

        Mockito.doReturn(CAMPAIGN_INFO_301)
                .when(campaignService)
                .getCampaign(Mockito.eq(CAMPAIGN_INFO_301.getId()));
        Mockito.doReturn(2136)
                .when(campaignService)
                .getProductId(Mockito.anyLong());
        Mockito.doReturn(CAMPAIGN_INFO_301)
                .when(campaignService)
                .getCampaignByDatasource(Mockito.eq(CAMPAIGN_INFO_301.getDatasourceId()));
        Mockito.doReturn(CAMPAIGN_INFO_302)
                .when(campaignService)
                .getCampaignByDatasource(Mockito.eq(CAMPAIGN_INFO_302.getDatasourceId()));
        Mockito.doReturn(CAMPAIGN_INFO_303)
                .when(campaignService)
                .getCampaignByDatasource(Mockito.eq(CAMPAIGN_INFO_303.getDatasourceId()));
        Mockito.doReturn(CAMPAIGN_INFO_304_NEW).when(campaignService).
                createCampaign(Mockito.any(),
                        Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt());
        Mockito.doNothing().when(campaignService).syncCampaignWithBalance(Mockito.any(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyInt());
        Mockito.doReturn(Optional.of(PartnerId.supplierId(CAMPAIGN_INFO_301.getDatasourceId())))
                .when(partnerService).getPartner(Mockito.eq(CAMPAIGN_INFO_301.getDatasourceId()));
    }

    @Test
    public void shouldCorrectlyMakeOneLineCorrectionWhenShopIdDefined() {
        service.makeCorrection(CAMPAIGN_INFO_301.getDatasourceId() + ",01.02.2017,222222", "comment", true);
        Mockito.verify(billingService)
                .makeCorrection(
                        OperationType.REFUND.getId(),
                        CAMPAIGN_INFO_301.getId(),
                        222222,
                        java.sql.Date.valueOf(LocalDate.of(2017, Month.FEBRUARY, 1)),
                        false,
                        USER_ID + ": comment"
                );
        Mockito.verifyNoMoreInteractions(billingService);
    }

    @Test
    public void createOrderTest() {
        adminCampaignService.createOrder(CAMPAIGN_INFO_301.getDatasourceId(), 1, 10, null, 10);
        Mockito.verify(campaignService).createCampaign(
                Mockito.eq(new CampaignInfo(CAMPAIGN_INFO_301.getDatasourceId(), 0,
                        CAMPAIGN_INFO_301.getTariffId())), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    public void shouldCorrectlyMakeOneLineCorrectionWhenCampaignIdDefined() {
        service.makeCorrection(CAMPAIGN_INFO_301.getId() + ",01.02.2017,222222", "comment", false);
        Mockito.verify(billingService)
                .makeCorrection(
                        OperationType.REFUND.getId(),
                        CAMPAIGN_INFO_301.getId(),
                        222222,
                        java.sql.Date.valueOf(LocalDate.of(2017, Month.FEBRUARY, 1)),
                        false,
                        USER_ID + ": comment"
                );
        Mockito.verifyNoMoreInteractions(billingService);
    }

    @Test
    public void shouldThrowDateFormatExceptionWhenIllegalDateValue() {
        try {
            service.makeCorrection(CAMPAIGN_INFO_301.getId() + ",01.02.17,222222", "comment", false);
            fail();
        } catch (RemoteCampaignUIService.DateFormatException ex) {
            verifyTransactionAbsent();
        }
    }

    @Test
    public void shouldThrowIdFormatExceptionWhenIllegalIdValue() {
        try {
            service.makeCorrection(CAMPAIGN_INFO_301.getId() + "q,01.02.2017,222222", "comment", false);
            fail();
        } catch (RemoteCampaignUIService.IdFormatException ex) {
            verifyTransactionAbsent();
        }
    }

    @Test
    public void shouldThrowIdFormatExceptionWhenIllegalCentsValue() {
        try {
            service.makeCorrection(CAMPAIGN_INFO_301.getId() + ",01.02.2017,222222q", "comment", false);
            fail();
        } catch (RemoteCampaignUIService.IdFormatException ex) {
            verifyTransactionAbsent();
        }
    }

    private void verifyTransactionAbsent() {
        Mockito.verifyNoMoreInteractions(transaction);
    }

    @Test
    public void shouldNotMakeAnyCorrectionWhenEmptyText() {
        service.makeCorrection("", "comment", false);
        verifyTransactionAbsent();
        Mockito.verifyNoMoreInteractions(transaction, billingService);
    }

    @Test
    public void shouldSortCorrectionByCampaignId() {
        service.makeCorrection(
                CAMPAIGN_INFO_301.getDatasourceId() + ",01.02.2017,111111\n"
                        + CAMPAIGN_INFO_303.getDatasourceId() + ",03.04.2016,333333\n"
                        + CAMPAIGN_INFO_302.getDatasourceId() + ",02.04.2016,222222\n",
                "comment",
                true
        );
        InOrder inOrder = Mockito.inOrder(billingService);
        inOrder.verify(billingService)
                .makeCorrection(
                        OperationType.REFUND.getId(),
                        CAMPAIGN_INFO_301.getId(),
                        111111,
                        java.sql.Date.valueOf(LocalDate.of(2017, Month.FEBRUARY, 1)),
                        false,
                        USER_ID + ": comment"
                );
        inOrder.verify(billingService)
                .makeCorrection(
                        OperationType.REFUND.getId(),
                        CAMPAIGN_INFO_302.getId(),
                        222222,
                        java.sql.Date.valueOf(LocalDate.of(2016, Month.APRIL, 2)),
                        false,
                        USER_ID + ": comment"
                );
        inOrder.verify(billingService)
                .makeCorrection(
                        OperationType.REFUND.getId(),
                        CAMPAIGN_INFO_303.getId(),
                        333333,
                        java.sql.Date.valueOf(LocalDate.of(2016, Month.APRIL, 3)),
                        false,
                        USER_ID + ": comment"
                );
        Mockito.verifyNoMoreInteractions(billingService);
    }


    interface Transaction {
        void begin();

        void commit();

        void rollback();
    }

}
