package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PickingAssignmentControllerTestListSortUSERKEYOrderASC extends PickingAssignmentControllerTestList {

    String uriTemplate0() {
        return endPoint() + "?x=y&sort=USER_KEY&order=ASC";
    }

    String uriTemplateL() {
        return endPoint() + "?sort=USER_KEY&order=ASC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?sort=USER_KEY&order=ASC&limit={limit}&cursor={cursor}";
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
        expectFor(uri, json(i(json6(), json7(), json8(), json1(), json2(), json3(), json4(), json5())));
    }
}
