package ru.yandex.market.crm.core.domain.segment.export;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class IdTypeTest {

    @Test
    public void testFromSourceType() {
        for (UidType uidType : UidType.values()) {
            try {
                assertNotNull(IdType.fromSourceType(uidType));
            } catch (IllegalArgumentException e) {
                fail("Нужно добавить значение enum-а IdType: " + uidType);
            }
        }
    }
}
