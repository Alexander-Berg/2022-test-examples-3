package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PickingAssignmentControllerTestListSrtIDOrderDESC extends PickingAssignmentControllerTestList {

    String uriTemplate() {
        return endPoint() + "?sort=ID&order=DESC";
    }

    String uriTemplateL() {
        return endPoint() + "?sort=ID&order=DESC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?sort=ID&order=DESC&limit={limit}&cursor={cursor}";
    }


    @Test
    void limitNone() throws Exception {
        expectMaxFor(uri(uriTemplate()));
    }


    @Test
    void limit1() throws Exception {
        expectFor(
                limit(1),
                json(cursor("id", "TDK0008"), i(json8()))
        );
        expectFor(
                uri(uriTemplateLC(), 1, cursor("id", "TDK0008")),
                json(cursor("id", "TDK0007"), i(json7()))
        );
    }


    @Test
    void limit1OrderKeyB000001002() throws Exception {
        expectFor(
                uri(uriTemplateL() + "&orderKey={orderKey}", 1, "B000001002"),
                json(cursor("id", "TDK0003"), i(json3()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 1, cursor("id", "TDK0003"), "B000001002"),
                json(i(json2()))
        );
    }


    @Test
    void limit2OrderKeyB000001005() throws Exception {
        expectFor(
                uri(uriTemplateL() + "&orderKey={orderKey}", 2, "B000001005"),
                json(cursor("id", "TDK0007"), i(json8(), json7()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 2, cursor("id", "TDK0007"), "B000001005"),
                json(cursor("id", "TDK0005"), i(json6(), json5()))
        );
        expectFor(
                uri(uriTemplateLC() + "&orderKey={orderKey}", 2, cursor("id", "TDK0005"), "B000001005"),
                json(i(json4()))
        );
    }


    @Test
    void limit4() throws Exception {
        expectFor(
                limit(4),
                json(cursor("id", "TDK0005"), i(json8(), json7(), json6(), json5()))
        );
        expectFor(
                uri(uriTemplateLC(), 4, cursor("id", "TDK0005")),
                json(i(json4(), json3(), json2(), json1()))
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
        expectFor(uri, json(i(json8(), json7(), json6(), json5(), json4(), json3(), json2(), json1())));
    }
}
