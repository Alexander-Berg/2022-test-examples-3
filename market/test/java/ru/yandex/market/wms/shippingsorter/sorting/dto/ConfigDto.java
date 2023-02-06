package ru.yandex.market.wms.shippingsorter.sorting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigDto {
    private String maxCube;
    private String maxWeight;
    private String maxWidth;
    private String maxLength;
    private String maxHeight;
    private String minWeight;
    private String upperBoxWeightLimitForRound;
    private String orderWarnThreshold;
    private String weightMinDeviation;
    private String weightMaxDeviation;
    private String asyncShippingSorterOn;
    private String isBrokenScales;
    private String multiZoneSearchEnabled;
}
