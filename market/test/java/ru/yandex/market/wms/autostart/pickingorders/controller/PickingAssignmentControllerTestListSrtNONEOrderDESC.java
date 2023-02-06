package ru.yandex.market.wms.autostart.pickingorders.controller;

class PickingAssignmentControllerTestListSrtNONEOrderDESC extends PickingAssignmentControllerTestListSrtIDOrderDESC {

    String uriTemplate() {
        return endPoint() + "?order=DESC";
    }

    String uriTemplateL() {
        return endPoint() + "?order=DESC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?order=DESC&limit={limit}&cursor={cursor}";
    }
}
