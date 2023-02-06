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
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.common.balance.BalanceConstants;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link CreateSupplierCampaignCommand}.
 */
@DbUnitDataSet(before = "CreateSupplierCampaignCommandTest/createSupplierCampaignTest.before.csv")
public class CreateSupplierCampaignCommandTest extends FunctionalTest {

    @Autowired
    private CreateSupplierCampaignCommand command;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;

    @BeforeEach
    private void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
        when(balanceService.getClient(anyLong()))
                .then(i -> new ClientInfo(i.getArgument(0), ClientType.PHYSICAL));
    }

    @Test
    @DbUnitDataSet(after = "CreateSupplierCampaignCommandTest/createSupplierCampaignTest.after.csv")
    void testAllArg() {
        check(new String[]{"all"});
    }

    @Test
    @DbUnitDataSet(after = "CreateSupplierCampaignCommandTest/createSupplierCampaignTest.after.csv")
    void testWithoutCampaignArg() {
        check(new String[]{"without_campaign"});
    }

    @Test
    @DbUnitDataSet(after = "CreateSupplierCampaignCommandTest/createSupplierCampaignTest.after.csv")
    void testCommand() {
        check(new String[]{"1", "2", "3", "-999"});
        Mockito.verify(printWriter).println("Supplier -999 does not exist");
    }

    void check(String[] args) {
        CommandInvocation commandInvocation = new CommandInvocation("create-supplier-campaign",
                args,
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
        Mockito.verify(printWriter).println("Creating campaign for 1 with id 11");
        Mockito.verify(printWriter).println("Creating campaign for 1 with id 11 OK!");
        Mockito.verify(printWriter).println("Supplier 2 already has campaign 12");
        Mockito.verify(printWriter).println("Creating campaign for 3 with id 13");
        Mockito.verify(printWriter).println("Creating campaign for 3 with id 13 OK!");


        final ArgumentCaptor<OrderInfo> orderInfoArgumentCaptor = ArgumentCaptor.forClass(OrderInfo.class);

        Mockito.verify(balanceService, times(2)).createOrUpdateOrderByCampaign(orderInfoArgumentCaptor.capture(),
                anyLong());
        final List<OrderInfo> actual = orderInfoArgumentCaptor.getAllValues();
        final CampaignInfo campaignInfo1 = new CampaignInfo(11, 1, 991, 1015, CampaignType.SUPPLIER);
        final OrderInfo expected1 = new OrderInfo(campaignInfo1, BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "s1");
        final CampaignInfo campaignInfo2 = new CampaignInfo(13, 3, 993, 1015, CampaignType.SUPPLIER);
        final OrderInfo expected2 = new OrderInfo(campaignInfo2, BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "s3");
        Assertions.assertEquals(List.of(expected1, expected2), actual);
    }


}
