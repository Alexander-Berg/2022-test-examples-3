package ru.yandex.market.mbi.api.listener;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.supplier.state.BalanceContractPartnerStateListener;
import ru.yandex.market.core.supplier.state.PartnerStateChangedEvent;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Проверяем заведение контрактов в балансе в слушателе на изменение партнера.
 */
class BalanceContractPartnerStateListenerTest extends FunctionalTest {

    @Autowired
    private BalanceContractPartnerStateListener balanceContractPartnerStateListener;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private CheckouterShopApi checkouterShopApi;
    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Test
    @DbUnitDataSet(before = "BalanceContractOnPartnerStateListenerTest.before.csv",
            after = "BalanceContractOnPartnerStateListenerTest.after.csv")
    void check() {
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(17L);
        when(balanceService.createOrUpdatePerson(any(), anyLong())).thenReturn(177L);

        //уже есть договор
        balanceContractPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(10L));
        verifyZeroInteractions(balanceService);

        //включенная DBS реплика с договором
        balanceContractPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(12L));
        verifyZeroInteractions(balanceService);

        //недовключенная DBS реплика без договора
        balanceContractPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(30L));
        verifyZeroInteractions(balanceService);

        //включенная DBS реплика без договора
        willReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .build()
        )).given(marketIdGrpcService).findByPartner(anyLong(), any(CampaignType.class));

        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        balanceContractPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(20L));
        verify(balanceService, times(2)).createOrUpdatePerson(any(), anyLong());
        verify(balanceService, times(2)).createOffer(any(), anyLong());

        verify(checkouterShopApi).updateShopData(eq(20L), any());

    }

}
