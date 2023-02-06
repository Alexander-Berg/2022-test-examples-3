package ru.yandex.market.pharmatestshop.domain.stock;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@JsonDeserialize
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {

    long warehouseId;
    String partnerWarehouseId;
    List<String> skus;
}
