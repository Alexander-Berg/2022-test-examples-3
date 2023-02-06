package ru.yandex.market.logistics.tarifficator.base;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.model.entity.PriceList;

import static org.assertj.core.api.Assertions.assertThat;

public class CopyPriceListTest {

    @Test
    @DisplayName("Проверка правильности копирования файла прайс-листов")
    void checkCopyCorrectness() {
        EnhancedRandom random = new EnhancedRandomBuilder().build();
        PriceList priceListToCopy = random.nextObject(PriceList.class);
        PriceList copiedPriceList = new PriceList(priceListToCopy);
        assertThat(copiedPriceList).usingRecursiveComparison().isEqualTo(copiedPriceList);
    }
}
