package ru.yandex.market.wh_network_monitor.controller.model;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class CheckMessageDTOTest {

    @Test
    public void getTskv() {
        var regex = "tskv\tunixtime=.*\tdevId=dev1\twhName=sofino\tcheckTime=0\tcheckName=ping\tcheckResult=ok";
        var message = new CheckMessageDTO("dev1", "sofino", Instant.EPOCH.toEpochMilli(), "ping", "ok");
        var resTskv = message.getTskv();
        assertTrue(resTskv.matches(regex));
    }
}