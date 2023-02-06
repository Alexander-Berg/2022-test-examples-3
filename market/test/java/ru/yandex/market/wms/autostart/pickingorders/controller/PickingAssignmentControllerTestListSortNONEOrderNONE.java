package ru.yandex.market.wms.autostart.pickingorders.controller;

class PickingAssignmentControllerTestListSortNONEOrderNONE extends PickingAssignmentControllerTestListSortIDOrderASC {

    String uriTemplate() {
        return endPoint() + "?x=y";
    }

    String uriTemplateL() {
        return endPoint() + "?limit={limit}";
    }

    String uriTemplateLC() {
        return endPoint() + "?limit={limit}&cursor={cursor}";
    }
}
