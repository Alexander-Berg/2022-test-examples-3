package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PickingAssignmentControllerTestListSortORDERKEYOrderDESC extends PickingAssignmentControllerTestList {

    String uriTemplate0() {
        return endPoint() + "?x=y&sort=ORDER_KEY&order=DESC";
    }

    String uriTemplateL() {
        return endPoint() + "?sort=ORDER_KEY&order=DESC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?sort=ORDER_KEY&order=DESC&limit={limit}&cursor={cursor}";
    }

    @Test
    void limitNone() throws Exception {
        expectMaxFor(uri(uriTemplate0()));
    }


    @Test
    void limitNoneOrderKeyB000001002() throws Exception {
        expectFor(
                uri(uriTemplate0() + "&orderKey={orderKey}", "B000001002"),
                json(i(json2(), json3()))
        );
    }


    @Test
    void limit1() throws Exception {
        String cursor1 = cursor("id", "TDK0004", "orderKey", "B000001005");
        String cursor2 = cursor("id", "TDK0005", "orderKey", "B000001005");

        expectFor(
                limit(1),
                json(cursor1, i(json4()))
        );
        expectFor(
                uri(uriTemplateLC(), 1, cursor1),
                json(cursor2, i(json5()))
        );
    }

    @Test
    void limit1OrderKeyB000001002() throws Exception {
        String cursor1 = cursor("id", "TDK0002", "orderKey", "B000001002");

        expectFor(
                uri(uriTemplateL() + "&orderKey={orderKey}", 1, "B000001002"),
                json(cursor1, i(json2()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 1, cursor1, "B000001002"),
                json(i(json3()))
        );
    }


    @Test
    void limit2OrderKeyB000001005() throws Exception {
        String cursor1 = cursor("id", "TDK0005", "orderKey", "B000001005");
        String cursor2 = cursor("id", "TDK0007", "orderKey", "B000001005");

        expectFor(
                uri(uriTemplateL() + "&orderKey={orderKey}", 2, "B000001005"),
                json(cursor1, i(json4(), json5()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 2, cursor1, "B000001005"),
                json(cursor2, i(json6(), json7()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 2, cursor2, "B000001005"),
                json(i(json8()))
        );
    }


    @Test
    void limit4() throws Exception {
        String cursor1 = cursor("id", "TDK0007", "orderKey", "B000001005");
        expectFor(
                limit(4),
                json(cursor1, i(json4(), json5(), json6(), json7()))
        );
        expectFor(
                uri(uriTemplateLC(), 4, cursor1),
                json(i(json8(), json2(), json3(), json1()))
        );
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
        expectFor(uri, json(i(json4(), json5(), json6(), json7(), json8(), json2(), json3(), json1())));
    }
}
