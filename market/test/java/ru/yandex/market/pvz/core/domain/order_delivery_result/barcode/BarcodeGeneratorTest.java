package ru.yandex.market.pvz.core.domain.order_delivery_result.barcode;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order_delivery_result.barcode.BarcodeType.FBY;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BarcodeGeneratorTest {

    @InjectMocks
    private final BarcodeGenerator barcodeGenerator;

    @ParameterizedTest
    @EnumSource(value = BarcodeType.class, names = "FBS_OLD", mode = EnumSource.Mode.EXCLUDE)
    void testGetNext(BarcodeType barcodeType) {
        String barcode = barcodeGenerator.getNext(barcodeType);
        assertThat(barcode).startsWith(barcodeType.getPrefix());
        assertThat(BarcodeGenerator.isPrintable(barcode)).isTrue();
    }

    @Test
    void testGetNextDoesNotRepeat() {
        int barcodesNumber = 1000;
        long uniqueCount = Stream.generate(() -> barcodeGenerator.getNext(FBY))
                .parallel()
                .limit(barcodesNumber)
                .distinct()
                .count();
        assertThat(uniqueCount).isEqualTo(barcodesNumber);
    }
}
