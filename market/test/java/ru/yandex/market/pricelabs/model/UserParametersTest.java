package ru.yandex.market.pricelabs.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserParametersTest {

    @Test
    void parseJson() {
        var example = UserParameters.fromJsonString(Utils.readResource("pricelabs/models/user_settings.json"));

        var expect = new UserParameters();
        var stats = new UserParameters.StatOrders();
        stats.setYm_source_type("A");
        stats.setYm_source_value("B");
        stats.setYm_offer_type("C");
        stats.setGa_source_type("D");
        stats.setGa_source_value("E");
        stats.setGa_offer_type("F");
        expect.setStat_orders(stats);

        assertEquals(expect, example);
    }
}
