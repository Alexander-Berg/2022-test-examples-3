package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.marketmanager.MarketManagerService.MARKET_SUPPORT_MANAGER_UID;
import static ru.yandex.market.common.balance.BalanceConstants.SHOP_BALANCE_ORDER_PRODUCT_ID;
import static ru.yandex.market.common.balance.BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID;

public class RecreateCampaignCommandTest extends FunctionalTest {

    @Autowired
    private RecreateCampaignCommand recreateCampaignCommand;

    @Autowired
    private Terminal terminal;

    @Autowired
    private PrintWriter printWriter;

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceContactService balanceContactService;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
        when(balanceService.getClient(anyLong()))
                .then(i -> new ClientInfo(i.getArgument(0), ClientType.PHYSICAL));
        when(balanceContactService.getUidsByClient(6)).thenReturn(List.of(887308676L, 887308677L));
        when(balanceContactService.getUidsByClient(5)).thenReturn(List.of(887308675L));
    }

    @Test
    @DbUnitDataSet(before = "RecreateCampaignCommandTest.before.csv",
            after = "RecreateCampaignCommandTest.after.csv")
    void test() {
        CommandInvocation commandInvocation = new CommandInvocation(recreateCampaignCommand.getNames()[0],
                new String[]{"10001", "10002", "10003", "10004"},
                Collections.emptyMap());
        recreateCampaignCommand.executeCommand(commandInvocation, terminal);

        final ArgumentCaptor<OrderInfo> orderInfoArgumentCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        Mockito.verify(balanceService, times(3))
                .createOrUpdateOrderByCampaign(orderInfoArgumentCaptor.capture(), anyLong());
        final List<OrderInfo> actual = orderInfoArgumentCaptor.getAllValues();
        Assertions.assertEquals(List.of(
                new OrderInfo(
                        new CampaignInfo(1, 1, 5, 1015, CampaignType.SUPPLIER),
                        SUPPLIER_BALANCE_ORDER_PRODUCT_ID, "s1"),
                new OrderInfo(
                        new CampaignInfo(2, 2, 6, 1015, CampaignType.SHOP),
                        SHOP_BALANCE_ORDER_PRODUCT_ID, "s2").setManagerUid(MARKET_SUPPORT_MANAGER_UID),
                new OrderInfo(
                        new CampaignInfo(3, 3, 5, 1015, CampaignType.SHOP),
                        SUPPLIER_BALANCE_ORDER_PRODUCT_ID, "s3").setManagerUid(MARKET_SUPPORT_MANAGER_UID)
                ),
                actual);
    }

}
