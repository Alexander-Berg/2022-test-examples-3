package ru.yandex.market.pharmatestshop.domain.stock;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class Stock {
    List<Sku> skus;
}
