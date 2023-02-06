package ru.yandex.market.global.index.domain.plus;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseFunctionalTest;
import ru.yandex.market.global.index.domain.offer.OfferIndexSupplier;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PlusCalculatorTest extends BaseFunctionalTest {

    private final OfferIndexSupplier offerIndexSupplier;

    @Test
    public void calculatePlus() {
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(0L)).isEqualTo(0L);
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(1L)).isEqualTo(10L);
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(20_00L)).isEqualTo(2_00L);
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(1234_56L)).isEqualTo(123_50L);
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(1231_23L)).isEqualTo(123_20L);
        Assertions.assertThat(offerIndexSupplier.createMaxAmtPlus(1000_000_000_000_00L)).isEqualTo(400_00L);
    }

}
