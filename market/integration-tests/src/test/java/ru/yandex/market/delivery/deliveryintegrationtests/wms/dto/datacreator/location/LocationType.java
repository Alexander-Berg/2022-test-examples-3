package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

public enum LocationType {
    DOOR("DOOR"),
    DOOR_OUT("DOOR_OUT"),
    DROP("DROP"),
    IDZ("IDZ"),
    IN_TRANSIT("INTRANSIT"),
    OTHER("OTHER"),
    PACK("PACK"),
    PCC("PCC"),
    PICK("PICK"),
    PICK_TO("PICKTO"),
    SORT("SORT"),
    CONSOLIDATION("CONS"),
    RECEIPT_TABLE("RCP_TABLE"),
    VGH("VGH"),
    STAGED("STAGED"),
    ANO_CONSOLIDATION("ANO_CONS"),
    REJECT_STORE("REJECT_STORE"),
    REJECT_BUF("REJECT_BUF"),
    RP_BUFFER("RO_BUF"),
    ST_IN_BUF("ST_IN_BUF"),
    ST_OUT_BUF("ST_OUT_BUF"),
    T_IN_BUF("T_IN_BUF"),
    T_OUT_BUF("T_OUT_BUF"),
    REPLENISHMENT_BUF("REP_BUF"),
    REPLENISHMENT_BUF_ORDER("REP_BUF_O"),
    REPLENISHMENT_VIRTUAL_BUFFER("REP_BUF_VR"),
    REPLENISHMENT_BUF_WITHDRAWAL("REP_BUF_WD"),
    SHIPSORTEXIT("SHIPSORTEX"),
    SHIPSORT("SHIPSORT"),
    SHIP_STANDARD("SHIP_STD"),
    SHIP_STD_BUF("SHIP_BUF"),
    SHIP_WITHDRAWAL("SHIP_WITHD"),
    SHIP_WITHD_BUF("SHIP_WTBUF"),
    PLACEMENT_BUF("PLCMNT_BUF"),
    BBXD_SORT_BUF("BBXD_SORT"),
    SHIP_BBXD("SHIP_BBXD"),
    VGH_BUFFER("VGH_BUF");

    private final String code;

    LocationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
