package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PickingAssignmentControllerTestListSortWAVEKEYOrderASC extends PickingAssignmentControllerTestList {

    String uriTemplate0() {
        return endPoint() + "?x=y&sort=WAVE_KEY&order=ASC";
    }

    String uriTemplateL() {
        return endPoint() + "?sort=WAVE_KEY&order=ASC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?sort=WAVE_KEY&order=ASC&limit={limit}&cursor={cursor}";
    }

    @Test
    void limitNone() throws Exception {
        expectMaxFor(uri(uriTemplate0()));
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
        expectFor(uri, json(i(json2(), json3(), json4(), json1(), json5(), json6(), json7(), json8())));
    }
}
