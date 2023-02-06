package ru.yandex.market.wms.autostart.pickingorders.controller;

interface PickingAssignmentControllerTestData {

    default String json1() {
        //language=JSON5
        return "{"
                + "id:'TDK0001',"
                + "orderKey:'B000001001',"
                + "waveKey:'W5',"
                + "pickDetailKey:'PDK5',"
                + "lot:'L5',"
                + "sku:'ROV5',"
                + "userKey:'U5'"
                + "}";
    }

    default String json2() {
        //language=JSON5
        return "{"
                + "id:'TDK0002',"
                + "orderKey:'B000001002',"
                + "waveKey:'W1',"
                + "pickDetailKey:'PDK5',"
                + "lot:'L5',"
                + "sku:'ROV5',"
                + "userKey:'U5'"
                + "}";
    }

    default String json3() {
        //language=JSON5
        return "{"
                + "id:'TDK0003',"
                + "orderKey:'B000001002',"
                + "waveKey:'W2',"
                + "pickDetailKey:'PDK1',"
                + "lot:'L5',"
                + "sku:'ROV5',"
                + "userKey:'U5'"
                + "}";
    }

    default String json4() {
        //language=JSON5
        return "{"
                + "id:'TDK0004',"
                + "orderKey:'B000001005',"
                + "waveKey:'W2',"
                + "pickDetailKey:'PDK2',"
                + "lot:'L1',"
                + "sku:'ROV5',"
                + "userKey:'U5'"
                + "}";
    }

    default String json5() {
        //language=JSON5
        return "{"
                + "id:'TDK0005',"
                + "orderKey:'B000001005',"
                + "waveKey:'W5',"
                + "pickDetailKey:'PDK2',"
                + "lot:'L2',"
                + "sku:'ROV1',"
                + "userKey:'U5'"
                + "}";
    }

    default String json6() {
        //language=JSON5
        return "{"
                + "id:'TDK0006',"
                + "orderKey:'B000001005',"
                + "waveKey:'W5',"
                + "pickDetailKey:'PDK5',"
                + "lot:'L2',"
                + "sku:'ROV2',"
                + "userKey:'U1'"
                + "}";
    }

    default String json7() {
        //language=JSON5
        return "{"
                + "id:'TDK0007',"
                + "orderKey:'B000001005',"
                + "waveKey:'W5',"
                + "pickDetailKey:'PDK5',"
                + "lot:'L5',"
                + "sku:'ROV2',"
                + "userKey:'U2'"
                + "}";
    }

    default String json8() {
        //language=JSON5
        return "{"
                + "id:'TDK0008',"
                + "orderKey:'B000001005',"
                + "waveKey:'W5',"
                + "pickDetailKey:'PDK5',"
                + "lot:'L5',"
                + "sku:'ROV5',"
                + "userKey:'U2'"
                + "}";
    }
}
