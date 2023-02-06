package ru.yandex.ir.common.features.relevance.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TSignedPostingTest {

    @Test
    void pack() {
        TSignedPosting pos = new TSignedPosting(
                0b100000000010101,
                0b011001,
                0b11,
                0b0011
        );

        int packed = pos.Pack();
        Assertions.assertEquals(
                0b100000000010101_011001_11_0011,
                packed
        );
    }

    @Test
    void unpack() {
        int packed = 0b100000000010101_011001_11_0011;

        TSignedPosting pos = TSignedPosting.Unpack(packed);
        Assertions.assertEquals(0b100000000010101, pos.Break);
        Assertions.assertEquals(0b011001, pos.Word);
        Assertions.assertEquals(0b11, pos.Relev);
        Assertions.assertEquals(0b0011, pos.Nform);
    }
}
