package ru.yandex.market.tpl.integration.tests.service;

import lombok.Data;

@Data
public class LmsSortingCenterPropertyDetailDto {
    String title = "Параметры свойства";

    Long id;
    Long scPropertyId;
    String key;
    String keyComment;
    String sortingCenterId;
    String value;

}
