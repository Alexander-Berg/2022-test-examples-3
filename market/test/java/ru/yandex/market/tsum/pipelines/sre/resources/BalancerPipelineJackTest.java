package ru.yandex.market.tsum.pipelines.sre.resources;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomilov Maksim <a href="mailto:le087@yandex-team.ru">le087</a>
 * @date 19.09.18
 */

public class BalancerPipelineJackTest {

    @Test
    public void checkTicketKeyTest() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "CSADMIN-111");
        Assert.assertEquals(pipelineJack.getStartrekTicketKey(), "CSADMIN-111");
    }

    @Test
    public void checkTicketKeyTestInTestQueue() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "TEST-111");
        Assert.assertEquals(pipelineJack.getStartrekTicketKey(), "TEST-111");
    }

    @Test
    public void checkTicketKeyWithSpaces() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "   CSADMIN-111");
        Assert.assertEquals(pipelineJack.getStartrekTicketKey(), "CSADMIN-111");
    }

    @Test
    public void checkTicketKeyAsUrl() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "https://st.yandex-team" +
            ".ru/CSADMIN-111");
        Assert.assertEquals(pipelineJack.getStartrekTicketKey(), "CSADMIN-111");
    }

    @Test
    public void checkTicketKeyNull() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", null);
        Assert.assertNull(pipelineJack.getStartrekTicketKey());
    }

    @Test
    public void checkTicketKeyEmpty() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "");
        Assert.assertNull(pipelineJack.getStartrekTicketKey());
    }

    @Test
    public void checkTicketKeyAsUrlInTestQueue() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "https://st.yandex-team.ru/TEST-111");
        Assert.assertEquals(pipelineJack.getStartrekTicketKey(), "TEST-111");
    }

    @Test(expected = Exception.class)
    public void checkTicketWrongKey() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "TEST-111-1");
        pipelineJack.getStartrekTicketKey();
    }

    @Test(expected = Exception.class)
    public void checkTicketWrongUrl() throws Exception {
        BalancerPipelineJack pipelineJack = new BalancerPipelineJack("TESTING", "st.yandex-team.ru/TEST-1111");
        pipelineJack.getStartrekTicketKey();
    }
}
