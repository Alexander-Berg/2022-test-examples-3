package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PickingAssignmentControllerTestListSortLOTOrderASC extends PickingAssignmentControllerTestList {

    String uriTemplate() {
        return endPoint() + "?sort=LOT&order=ASC";
    }

    String uriTemplateL() {
        return endPoint() + "?sort=LOT&order=ASC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?sort=LOT&order=ASC&limit={limit}&cursor={cursor}";
    }

    @Test
    void limitNone() throws Exception {
        expectMaxFor(uri(uriTemplate()));
    }

    @Test
    void limit8() throws Exception {
        expectMaxFor(limit(8));
    }

    @Test
    void limit100() throws Exception {
        expectMaxFor(limit(100));
    }


    void expectMaxFor(URI uri) throws Exception {
        expectFor(uri, json(i(json4(), json5(), json6(), json1(), json2(), json3(), json7(), json8())));
    }
}
