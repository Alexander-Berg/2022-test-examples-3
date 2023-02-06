package ru.yandex.market.deliverycalculator.indexer.controller;

import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.task.UpdateShopTariffsCacheTask;
import ru.yandex.market.deliverycalculator.test.TestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class PingControllerFunctionalTest extends FunctionalTest {

    @Test
    void testPing() {
        runTasks(UpdateShopTariffsCacheTask.class);

        // PingController работает в синхронном режиме
        String actual = REST_TEMPLATE.getForEntity(baseUrl + "/ping", String.class).getBody();

        assertThat(actual, equalTo("0;Ok"));
    }

    @Test
    void testPageMatch() {
        String actual = REST_TEMPLATE.getForEntity(baseUrl + "/pagematch", String.class).getBody();
        String expected = TestUtils.extractFileContent(getClass(), "pagematch.txt");

        assertThat(actual, equalTo(expected));
    }

}
