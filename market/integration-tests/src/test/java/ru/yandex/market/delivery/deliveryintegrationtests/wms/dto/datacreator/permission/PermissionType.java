package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.permission;

/**
 * Данный enum содержит в себе значения из:
 * ru.yandex.market.wms.common.model.enums.TaskType
 * ru.yandex.market.wms.common.model.enums.LaborActivityType
 * Значение для OLD_CONSOLIDATION взял напрямую из бд
 *
 * в запросе на создание разрешений нужно использовать именно значение через getValue
 */
public enum PermissionType {
    ANOMALY_CONSOLIDATION("ACNS"),
    ANOMALY_PLACEMENT("APLCMNT"),
    ASST_CYCLECOUNT("ASSTCC"),
    ASST_MOVE ("ASSTMV"),
    ASST_OPTIMIZEMOVE("ASSTOM"),
    ASST_PHYSICAL_TEAMA("ASSTPIA"),
    ASST_PHYSICAL_TEAMB("ASSTPIB"),
    ASST_PICK("ASSTPK"),
    ASST_PUTAWAY("ASSTPA"),
    ASST_QC("ASSTQC"),
    CHERRYPICKREPLENISHMENT("CR"),
    CYCLECOUNT("CC"),
    LOADING("LD"),
    MOVE("MV"),
    OLD_CONSOLIDATION("CD"),
    OPTIMIZEMOVE("OM"),
    PACKING("PKG"),
    PHYSICAL_TEAMA("PIA"),
    PHYSICAL_TEAMB("PIB"),
    PICK("PK"),
    PUTAWAY("PA"),
    QC("QC"),
    RECEIPT("RC"),
    RELOCATION("APLCMNT"),
    REPLENISHMENT("RP"),
    REPLENISHMENT_MOVE("REP_MOVE"),
    REPLENISHMENT_ORDER("RO"),
    REPLENISHMENT_PICK("REP_PICK"),
    SORTATION("SRT"),
    STOCKERPUTAWAY("SP"),
    TRANSPORT_ORDER("TRNSPRT"),
    WORKCENTERMOVE("WC"),
    WORKORDER("WO"),
    UNKNOWN("UNKNOWN"),
    ;

    private final String value;

    PermissionType(String value) { this.value = value; }

    public String getValue() { return value; }
}
