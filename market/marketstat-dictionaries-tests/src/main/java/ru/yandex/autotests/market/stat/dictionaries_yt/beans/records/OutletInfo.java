package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by kateleb on 08.06.17.
 */
@Data
@DictTable(name = "outlet_info")
public class OutletInfo implements DictionaryRecord {
    @DictionaryIdField
    private Long id;
    private Long datasourceId;
    private String type;
    private String name;
    private String emails;
    private String note;
    private String url;
    private String addressCountry;
    private String addressCity;
    private String addressStreet;
    private String addressNumber;
    private String addressBuilding;
    private String addressOffice;
    private String gpsCoord;
    @RequiredField
    private Long hidden;
    private Long status;
    private String addressEstate;
    private String addressBlock;
    private Long regionId;
    private String updateTime;
    private Boolean isMain;
    private Long addressKm;
    private String checkTime;
    private String shopOutletId;
    private Boolean isBookNow;
    private Long deliveryServiceId;
    private String deliveryServiceOutletId;
    private Long source;
    private Boolean isInlet;
    private Boolean deleted;
    private String deliveryServiceOutletCode;
    private String addressAdd;
    private String pointType;
    private Boolean isSupplierWarehouse;
    private String postCode;
    private Boolean forReturn;
    private String contactName;
    private String addressFlat;
    private String longitude;
    private String latitude;
}
