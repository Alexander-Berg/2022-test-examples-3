package ru.yandex.market.wh_network_monitor.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.wh_network_monitor.controller.model.CheckMessageDTO;

import java.time.Instant;


@RunWith(SpringJUnit4ClassRunner.class)
public class WhNetMonControllerTest {

    private WhNetMonController whNetMonController;

    @Before
    public void setUp() {
        whNetMonController = new WhNetMonController();
    }

    @Test
    public void createEmptyMessage() throws Exception {
        var empty = new CheckMessageDTO();
        var resp = whNetMonController.createMessage(empty);
        Assert.assertTrue(resp.getStatusCode().is4xxClientError());
    }

    @Test
    public void createGoodMessage() throws Exception {
        var empty = new CheckMessageDTO("dev0", "sofino", Instant.now().toEpochMilli(), "ping", "ok");
        var resp = whNetMonController.createMessage(empty);
        Assert.assertTrue(resp.getStatusCode().is2xxSuccessful());
    }
}