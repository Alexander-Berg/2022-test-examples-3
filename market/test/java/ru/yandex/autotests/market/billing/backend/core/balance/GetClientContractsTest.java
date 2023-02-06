package ru.yandex.autotests.market.billing.backend.core.balance;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.balance.lib.environment.Environment;
import ru.yandex.autotests.balance.lib.scripts.DateUtil;
import ru.yandex.autotests.balance.lib.xmlrpc.Balance;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * User: jkt
 * Date: 28.10.13
 * Time: 11:22
 */
public class GetClientContractsTest {

    private static  String BALANCE_XMLRPC_URL = Environment.GREED_TS1F.getXmlRpcUrl();

    private static final Logger LOG = Logger.getLogger(GetClientContractsTest.class);

    @Ignore
    @Test
    public void test_getClientContracts() throws Exception {

        int clientID = 1207631;
        int personID = 0;

        //комиссионный
        //        int clientID = 5297439;
        //        int personID = 3938449;

        //прямой агентский
        //        int clientID = 5297460;
        //        int personID = 3938465;

        //Преобразуем дату к нужному формату
        String contractStateDateString = DateTime.now().toString("YYYY-MM-dd");

        Balance b = new Balance(BALANCE_XMLRPC_URL);
        //Object answer = b.GetClientContracts(clientID, personID, contractStateDateString);
    }
}
