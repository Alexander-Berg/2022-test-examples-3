package ru.yandex.market.delivery.deliveryintegrationtests.yard.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class YardClientEventDto {

    private String clientId;
    private String eventType;
    private LocalDateTime eventDate;
    private Long yardClientId;

}
