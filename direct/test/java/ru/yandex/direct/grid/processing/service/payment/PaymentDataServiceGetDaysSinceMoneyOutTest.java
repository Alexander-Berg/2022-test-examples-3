package ru.yandex.direct.grid.processing.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletRestMoney;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientAutoOverdraftInfo;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.repository.ClientSpentRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.eventlog.model.EventCampaignAndTimeData;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.payment.repository.AutopaySettingsRepository;
import ru.yandex.direct.core.entity.payment.service.AutopayService;
import ru.yandex.direct.core.entity.promocodes.service.PromocodeHelper;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;
import ru.yandex.direct.intapi.client.IntApiClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class PaymentDataServiceGetDaysSinceMoneyOutTest {
    private static final int SHARD = 1;
    private static final CurrencyCode CURRENCY_CODE = CurrencyRub.getInstance().getCode();
    private static final long WALLET_ID = 1;
    private static final ClientId CLIENT_ID = mock(ClientId.class);

    private PaymentDataService service;
    private ShardHelper shardHelper;
    private CampaignService campaignService;
    private CampaignRepository campaignRepository;
    private ClientService clientService;
    private ClientRepository clientRepository;
    private EventLogRepository eventLogRepository;

    private WalletCampaign walletCampaign;

    @Before
    public void setUp() {
        shardHelper = mock(ShardHelper.class);
        campaignService = mock(CampaignService.class);
        campaignRepository = mock(CampaignRepository.class);
        clientService = mock(ClientService.class);
        clientRepository = mock(ClientRepository.class);
        eventLogRepository = mock(EventLogRepository.class);

        when(CLIENT_ID.asLong()).thenReturn(1L);
        when(shardHelper.getShardByClientIdStrictly(CLIENT_ID)).thenReturn(SHARD);
        when(clientService.getClient(CLIENT_ID)).thenReturn(
                new Client().withClientId(1L).withIsBrand(false).withWorkCurrency(CURRENCY_CODE));

        service = new PaymentDataService(shardHelper, mock(BalanceService.class), mock(IntApiClient.class),
                mock(AutopayService.class), campaignService, clientService, mock(WalletRepository.class),
                mock(AutopaySettingsRepository.class), mock(GridValidationResultConversionService.class),
                mock(PromocodeHelper.class), campaignRepository, clientRepository, eventLogRepository,
                mock(OrderStatService.class), mock(UserRepository.class), mock(FeatureService.class),
                mock(AutobudgetHourlyAlertRepository.class), mock(MailNotificationEventService.class),
                mock(ClientSpentRepository.class));
    }

    @Test
    public void testClientHasMoney() {
        walletCampaign = mock(WalletCampaign.class);
        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(1000));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //1000 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(1000), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));
        //no debt
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection())).thenReturn(Collections.emptyMap());

        ClientAutoOverdraftInfo clientAutoOverdraftInfo = new ClientAutoOverdraftInfo()
                .withClientId(1L)
                .withAutoOverdraftLimit(BigDecimal.ZERO);
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(List.of(clientAutoOverdraftInfo));

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, nullValue());

    }

    @Test
    public void testMoneyOutNoAutoOverdraft() {
        walletCampaign = mock(WalletCampaign.class);

        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(0));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //0 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(0), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));

        //debt 10 rub
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, BigDecimal.TEN));

        //no overdraft
        ClientAutoOverdraftInfo clientAutoOverdraftInfo = new ClientAutoOverdraftInfo()
                .withClientId(1L)
                .withAutoOverdraftLimit(BigDecimal.ZERO);
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(List.of(clientAutoOverdraftInfo));

        LocalDateTime eventLocalTime = LocalDateTime.now().minusDays(2);
        when(eventLogRepository.getLastEventByCliendIdAndCampaignId(
                eq(SHARD), eq(CLIENT_ID), eq(WALLET_ID), eq(EventLogType.MONEY_OUT_WALLET)))
                .thenReturn(new EventCampaignAndTimeData().withCampaignId(WALLET_ID).withEventTime(eventLocalTime));

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, equalTo(BigDecimal.valueOf(2)));
    }

    @Test
    public void testClientHasMoneyWithOverdraft() {
        walletCampaign = mock(WalletCampaign.class);
        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(0));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //0 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(0), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));

        //debt 10 rub
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, BigDecimal.TEN));

        //overdraft 1000 rub
        ClientAutoOverdraftInfo clientAutoOverdraftInfo = new ClientAutoOverdraftInfo()
                .withClientId(1L)
                .withAutoOverdraftLimit(BigDecimal.valueOf(1000))
                .withOverdraftLimit(BigDecimal.valueOf(1000))
                .withDebt(BigDecimal.TEN)
                .withStatusBalanceBanned(false);
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(List.of(clientAutoOverdraftInfo));

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, nullValue());
    }

    @Test
    public void testMoneyOutWithOverdraft() {
        walletCampaign = mock(WalletCampaign.class);
        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(0));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //0 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(0), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));

        //debt 0 rub
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, BigDecimal.ZERO));

        //overdraft 2 rub
        ClientAutoOverdraftInfo clientAutoOverdraftInfo = new ClientAutoOverdraftInfo()
                .withClientId(1L)
                .withAutoOverdraftLimit(BigDecimal.valueOf(2))
                .withOverdraftLimit(BigDecimal.valueOf(2))
                .withDebt(BigDecimal.ZERO)
                .withStatusBalanceBanned(false);
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(List.of(clientAutoOverdraftInfo));

        LocalDateTime eventLocalTime = LocalDateTime.now().minusDays(2);
        when(eventLogRepository.getLastEventByCliendIdAndCampaignId(
                eq(SHARD), eq(CLIENT_ID), eq(WALLET_ID), eq(EventLogType.MONEY_OUT_WALLET_WITH_AO)))
                .thenReturn(new EventCampaignAndTimeData().withCampaignId(WALLET_ID).withEventTime(eventLocalTime));

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, equalTo(BigDecimal.valueOf(2)));
    }

    @Test
    public void testNewUser() {
        walletCampaign = mock(WalletCampaign.class);
        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(0));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //0 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(0), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));

        //debt 0 rub
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, BigDecimal.ZERO));

        //no overdraft
        ClientAutoOverdraftInfo clientAutoOverdraftInfo = new ClientAutoOverdraftInfo()
                .withClientId(1L)
                .withAutoOverdraftLimit(BigDecimal.ZERO);
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(List.of(clientAutoOverdraftInfo));
        //dont find a event
        when(eventLogRepository.getLastEventByCliendIdAndCampaignId(
                eq(SHARD), eq(CLIENT_ID), eq(WALLET_ID), eq(EventLogType.MONEY_OUT_WALLET)))
                .thenReturn(null);

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, nullValue());
    }

    @Test
    public void testMoneyOutWithoutClientOverdraftInfo() {
        walletCampaign = mock(WalletCampaign.class);

        when(walletCampaign.getCurrency()).thenReturn(CURRENCY_CODE);
        when(walletCampaign.getSum()).thenReturn(BigDecimal.valueOf(0));
        when(campaignRepository.getWalletsByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(List.of(walletCampaign));
        //0 rub on wallet
        Money restMoney = Money.valueOf(BigDecimal.valueOf(0), CURRENCY_CODE);
        WalletRestMoney walletRestMoney = mock(WalletRestMoney.class);
        when(walletRestMoney.getRest()).thenReturn(restMoney);
        when(campaignService.getWalletsRestMoneyByWalletCampaignIds(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, walletRestMoney));

        //debt 10 rub
        when(campaignRepository.getWalletsDebt(eq(SHARD), anyCollection()))
                .thenReturn(Map.of(WALLET_ID, BigDecimal.TEN));

        //no overdraft info
        when(clientRepository.getClientsAutoOverdraftInfo(eq(SHARD), anyCollection()))
                .thenReturn(Collections.emptyList());

        LocalDateTime eventLocalTime = LocalDateTime.now().minusDays(2);
        when(eventLogRepository.getLastEventByCliendIdAndCampaignId(
                eq(SHARD), eq(CLIENT_ID), eq(WALLET_ID), eq(EventLogType.MONEY_OUT_WALLET)))
                .thenReturn(new EventCampaignAndTimeData().withCampaignId(WALLET_ID).withEventTime(eventLocalTime));

        BigDecimal result = service.getDaysSinceMoneyOut(SHARD, WALLET_ID, CLIENT_ID);
        assertThat(result, equalTo(BigDecimal.valueOf(2)));
    }
}
