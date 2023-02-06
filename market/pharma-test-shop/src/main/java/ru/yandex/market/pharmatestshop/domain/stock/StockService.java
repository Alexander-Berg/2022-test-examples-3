package ru.yandex.market.pharmatestshop.domain.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

   StockMapper stockMapper;

    @Autowired
    public StockService(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    public Stock getStock(StockDto stockDto) {
        return stockMapper.map(stockDto);
    }

}
