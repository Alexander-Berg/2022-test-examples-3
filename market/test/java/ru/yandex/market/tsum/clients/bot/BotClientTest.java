package ru.yandex.market.tsum.clients.bot;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 22.09.17
 */
public class BotClientTest {
    @Test
    public void parseOnlyServersNameResult() throws Exception {
        String example1 = "220850\tkiwi1520.search.yandex.net\tOPERATION\tRU\tSAS\tSASTA  SAS-1.1.1  " +
            "32\t5-\tC860004291A4\t" +
            "C86000429302\tInfrastructure Products > Системы хранения и обработки данных > KiKiMR\t" +
            "AQ/ASUS/RS500AE6PS4/KGPED16/4T3.5/1U/1P\tOPTERON6274\tSERVERS\tSRV\tKGPE-D16\t10BF484DCBBB\t692\n" +
            "330850\ttest1.search.yandex.net\t\t\t";

        HashSet<String> bareServers = BotClient.parseOnlyServersNameResult(example1);
        HashSet<String> expectedSet = new HashSet<>();
        expectedSet.add("kiwi1520.search.yandex.net");
        expectedSet.add("test1.search.yandex.net");

        Assert.assertEquals(expectedSet, bareServers);
    }

    @Test
    public void parseServersNameAndDc() throws Exception {
        String example1 = "102526520\tmarket-reserved-20484-03-vla-05-5b14-102526520v.yandex.ru\tOPERATION\tRU\t" +
            "VLADIMIR\tALPHA\tVLA-05\t5B14\t1\t-\t\t\t\t\tE0D55ECCF3A0\tMarket Services > - > -\t" +
            "GB/T174-N40-Y3N/4U2.5+8T3.5/1OU/N\tXEONE5-2660V4\tNODES\tSRV\tMY70-EX0-Y3N\tE0D55ECCCBC5\t905";

        String example2 = example1 + "\n" + "102348688\tmarket-reserved-20483-03-sas-09-09-09-17-102348688h.yandex" +
            ".ru\t" +
            "OPERATION\tRU\tSAS\tSASTA\tSAS-09\t09.09.17\t1\t-\t\t\t\t\tE0D55E97DC2C\t" +
            "Market Services > Эксплуатация Маркета > -\tGB/T174N40/MY70EX0/4U2.5+8T3.5/1OU/N\tXEONE5-2660V4\tNODES\t" +
            "SRV\tGA-MY70-EX0\tE0D55E99A5CA\t969";

        HashMap<String, String> expectedMap = new HashMap<>();
        expectedMap.put("market-reserved-20484-03-vla-05-5b14-102526520v.yandex.ru", "VLADIMIR");
        Assert.assertEquals(expectedMap, BotClient.parseServersNameAndDc(example1));

        expectedMap.put("market-reserved-20483-03-sas-09-09-09-17-102348688h.yandex.ru", "SAS");
        Assert.assertEquals(expectedMap, BotClient.parseServersNameAndDc(example2));

    }

}
