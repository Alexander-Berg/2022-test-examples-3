package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
    private String login;
    private String pass;
}
