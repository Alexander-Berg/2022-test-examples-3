package ru.yandex.market.tsum.tms.tasks.aas;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.tsum.clients.yql.YqlApiClient;

import java.util.Set;
import java.util.concurrent.TimeoutException;

@Ignore
public class YtUsageCountingCronTaskIntegrationTest {

    @Test
    public void testGetAccounts() throws TimeoutException, InterruptedException {
        YqlApiClient yqlApiClient = new YqlApiClient("https://yql.yandex.net/api/v2", "", null);
        Set<String> accounts = YtUsageCountingCronTask.getAccounts(yqlApiClient, "arnold");
        System.out.println(accounts);
    }

}