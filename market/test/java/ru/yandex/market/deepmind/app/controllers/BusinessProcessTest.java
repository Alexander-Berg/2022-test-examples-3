package ru.yandex.market.deepmind.app.controllers;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.deepmind.app.web.aprove.BusinessProcess;

public class BusinessProcessTest {

    @Test
    public void testEachBusinessProcessHasHumanName() {
        for (BusinessProcess process : BusinessProcess.values()) {
            Assertions.assertThat(process.getHumanName()).isNotEmpty();
        }
    }
}
