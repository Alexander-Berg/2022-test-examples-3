package ru.yandex.market.pharmatestshop.domain.stock.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuItem {
    String type;
    int count;// Всегда 10
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss.SSSX") //DateTimeFormatter
    // .ISO_OFFSET_DATE_TIME
    String updatedAt;
}
