package ru.yandex.market.wms.autostart.pickingorders.controller;

class PickingAssignmentControllerTestListSortNONEOrderASC extends PickingAssignmentControllerTestListSortIDOrderASC {

    String uriTemplate() {
        return endPoint() + "?order=ASC";
    }

    String uriTemplateL() {
        return endPoint() + "?order=ASC&limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?order=ASC&limit={limit}&cursor={cursor}";
    }
}
