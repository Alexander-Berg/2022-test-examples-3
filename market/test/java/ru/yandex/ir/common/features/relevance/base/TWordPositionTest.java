package ru.yandex.ir.common.features.relevance.base;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TWordPositionTest {

    // ---------- original bit scheme ------
    // NFORM_LEVEL_Bits = 4;
    // RELEV_LEVEL_Bits = 2;
    // WORD_LEVEL_Bits = 6;
    // BREAK_LEVEL_Bits = 15;
    // DOC_LEVEL_Bits = 26;
    // ---------- 53 bits total ------------
    // doc                        # break           # word   # relev # nform
    // [1-67_108_863]             # [1-32_767]      # [1-63] # [0-3] # [0-15]
    // xxxxxxxxxxxxxxxxxxxxxxxxxx # xxxxxxxxxxxxxxx # xxxxxx # xx    # xxxx
    // 53 <-------------------------------------------------------------> 0

    @Test
    void INT_N_MAX() {
        long n = TWordPosition.INT_N_MAX(5);
        Assertions.assertEquals(0b11111L, n);
    }

    @Test
    void doc() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long doc = TWordPosition.Doc(pos);
        Assertions.assertEquals(0b00000000000000010011001100L, doc);
    }

    @Test
    void testBreak() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long breakVal = TWordPosition.Break(pos);
        Assertions.assertEquals(0b000000000000011L, breakVal);
    }

    @Test
    void breakAndWord() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long breakAndWord = TWordPosition.BreakAndWord(pos);
        Assertions.assertEquals(0b000000000000011_001000L, breakAndWord);
    }

    @Test
    void word() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long word = TWordPosition.Word(pos);
        Assertions.assertEquals(0b001000L, word);
    }

    @Test
    void relev() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long relev = TWordPosition.Relev(pos);
        Assertions.assertEquals(0b11L, relev);
    }

    @Test
    void getRelevLevel() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        RelevLevel relev = TWordPosition.GetRelevLevel(pos);
        Assertions.assertEquals(RelevLevel.BEST_RELEV, relev);
    }

    @Test
    void form() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long form = TWordPosition.Form(pos);
        Assertions.assertEquals(0b0001L, form);
    }

    @Test
    void positionOnly() {
        long pos = 0b00000000000000010011001100_000000000000011_001000_11_0001L;
        long positionOnly = TWordPosition.PositionOnly(pos);
        Assertions.assertEquals(
                0b00000000000000010011001100_000000000000011_001000_00_0000L,
                positionOnly
        );
    }

    @Test
    void set() {
        MutableLong pos = new MutableLong(0);

        TWordPosition.Set(pos, 0b11101L, 0b1001L, 0b11L);
        Assertions.assertEquals(
                0b00000000000000000000011101_000000000001001_000011_00_0000L,
                pos.getValue()
        );

        TWordPosition.Set(pos, 0b1L, 0b11L, 0b111L, 0b10);
        Assertions.assertEquals(
                0b00000000000000000000000001_000000000000011_000111_00_0010L,
                pos.getValue()
        );

        TWordPosition.Set(pos, 0b10000L, 0b1010L, 0b1L, RelevLevel.MID_RELEV);
        Assertions.assertEquals(
                0b00000000000000000000010000_000000000001010_000001_01_0000L,
                pos.getValue()
        );

        TWordPosition.Set(pos, 0b1101L, 0b101L, 0b1001L, RelevLevel.HIGH_RELEV, 1);
        Assertions.assertEquals(
                0b00000000000000000000001101_000000000000101_001001_10_0001L,
                pos.getValue()
        );

        TWordPosition.Set(pos, 0b1000001101L, 0b10101L, 0b11001L, 0b11, 0b11);
        Assertions.assertEquals(
                0b00000000000000001000001101_000000000010101_011001_11_0011L,
                pos.getValue()
        );
    }

    @Test
    void pos() {
        long pos = TWordPosition.Pos(0b1000001101L, 0b10101L, 0b11001L, 0b11, 0b11);
        Assertions.assertEquals(
                0b00000000000000001000001101_000000000010101_011001_11_0011L,
                pos
        );
    }

    @Test
    void posting() {
        int posting = TWordPosition.Posting(0b100000000010101, 0b11001, 0b11, 0b11);
        Assertions.assertEquals(
                0b100000000010101_011001_11_0011,
                posting
        );
    }

    @Test
    void setDoc() {
        MutableLong pos = new MutableLong(0);
        TWordPosition.SetDoc(pos, 0b00000000000000001000001101);
        Assertions.assertEquals(
                0b00000000000000001000001101_000000000000000_000000_00_0000L,
                pos.getValue()
        );
    }

    @Test
    void setBreak() {
        MutableLong pos = new MutableLong(0);
        TWordPosition.SetBreak(pos, 0b100000000010101);
        Assertions.assertEquals(
                0b00000000000000000000000000_100000000010101_000000_00_0000L,
                pos.getValue()
        );
    }

    @Test
    void setWord() {
        MutableLong pos = new MutableLong(0);
        TWordPosition.SetWord(pos, 0b011001);
        Assertions.assertEquals(
                0b00000000000000000000000000_000000000000000_011001_00_0000L,
                pos.getValue()
        );
    }

    @Test
    void setRelevLevel() {
        MutableLong pos = new MutableLong(0);
        TWordPosition.SetRelevLevel(pos, 0b01);
        Assertions.assertEquals(
                0b00000000000000000000000000_000000000000000_000000_01_0000L,
                pos.getValue()
        );
    }

    @Test
    void setWordForm() {
        MutableLong pos = new MutableLong(0);
        TWordPosition.SetWordForm(pos, 0b10);
        Assertions.assertEquals(
                0b00000000000000000000000000_000000000000000_000000_00_0010L,
                pos.getValue()
        );
    }

    @Test
    void pack() {
        TWordPosition pos = new TWordPosition(
                0b00000000000000001000001101L,
                0b000000000010101L,
                0b011001L,
                0b11L,
                0b0011L
        );

        long packed = pos.Pack();
        Assertions.assertEquals(
                0b00000000000000001000001101_000000000010101_011001_11_0011L,
                packed
        );
    }

    @Test
    void unpack() {
        long packed = 0b00000000000000001000001101_000000000010101_011001_11_0011L;

        TWordPosition pos = TWordPosition.Unpack(packed);
        Assertions.assertEquals(0b00000000000000001000001101L, pos.Doc);
        Assertions.assertEquals(0b000000000010101L, pos.Break);
        Assertions.assertEquals(0b011001L, pos.Word);
        Assertions.assertEquals(0b11L, pos.Relev);
        Assertions.assertEquals(0b0011L, pos.Nform);
    }
}
