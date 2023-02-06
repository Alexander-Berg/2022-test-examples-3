package ru.yandex.crypta.graph2.model.matching.component;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class ComponentCenterTest {

    @Test
    public void cryptaIdConvertableToUInt64() {

        // case when resulting number fits in signed int64 (long)
        String cryptaId = new ComponentCenter("xxx", EIdType.YANDEXUID).getCryptaId();
        assertEquals("4024228623459645301", cryptaId);
        assertEquals(4024228623459645301L, Long.parseLong(cryptaId));

        // case when resulting number overflows signed int64 but fits in unsigned int64
        String possibleLongOverflow = new ComponentCenter(
                "dfe0faae949b3204e0be6512d9da",
                EIdType.PHONE_MD5
        ).getCryptaId();

        assertEquals("15748610619566753340", possibleLongOverflow);
        Assert.assertThrows(() -> Long.parseLong(possibleLongOverflow), NumberFormatException.class);

        long numericCryptaId = Long.parseUnsignedLong(possibleLongOverflow);
        assertEquals("15748610619566753340", Long.toUnsignedString(numericCryptaId));

    }
}
