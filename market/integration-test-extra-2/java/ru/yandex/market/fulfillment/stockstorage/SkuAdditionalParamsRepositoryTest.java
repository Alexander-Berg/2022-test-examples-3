package ru.yandex.market.fulfillment.stockstorage;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.SkuAdditionalParams;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuAdditionalParamsRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkuAdditionalParamsRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SkuAdditionalParamsRepository skuAdditionalParamsRepository;

    @Test
    public void createAndSearchSkuAdditionalParams() {
        skuAdditionalParamsRepository.saveAndFlush(new SkuAdditionalParams(1L, LocalDateTime.now()));
        Optional<SkuAdditionalParams> bySkuId = skuAdditionalParamsRepository.findBySkuId(1L);

        assertTrue(bySkuId.isPresent());
        assertEquals(1L, (long) bySkuId.get().getSkuId());
    }
}
