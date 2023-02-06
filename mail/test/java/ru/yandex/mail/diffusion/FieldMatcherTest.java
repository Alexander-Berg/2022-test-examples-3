package ru.yandex.mail.diffusion;

import lombok.Value;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.diffusion.IncrementalObject.Enumeration;
import ru.yandex.mail.diffusion.IncrementalObject.NonIncremental;
import ru.yandex.mail.diffusion.testing.Random;
import ru.yandex.mail.diffusion.testing.RandomParameterResolver;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(RandomParameterResolver.class)
class FieldMatcherTest extends BaseTest {
    @Test
    @DisplayName("Verify matcher doesn't find changes for the same object")
    void testSameObject(@Random IncrementalObject object, @Mock FieldMatchListener listener) {
        MATCHER.match(object, object, listener);
        verifyZeroInteractions(listener);
    }

    @Test
    @DisplayName("Verify matcher finds mismatch between two completely different objects")
    void testAbsoluteDifference(@Mock FieldMatchListener listener) {
        val oldObj = new IncrementalObject(
            true,
            "str",
            0,
            42L,
            Set.of("0", "1"),
            Set.of((byte) 0, (byte) 1),
            new NonIncremental("s", 1),
            OptionalLong.of(42L),
            Optional.of((short) 24),
            Enumeration.ONE
        );
        val newObj = new IncrementalObject(
            false,
            "rts",
            100,
            24L,
            Set.of("1", "4"),
            Set.of((byte) 1, (byte) 4),
            new NonIncremental("ss", 42),
            OptionalLong.of(24L),
            Optional.of((short) 42),
            Enumeration.TWO
        );

        MATCHER.match(oldObj, newObj, listener);

        verifyOnce(listener).onMismatch("bool", oldObj.isBool(), newObj.isBool());
        verifyOnce(listener).onMismatch("string", oldObj.getString(), newObj.getString());
        verifyOnce(listener).onMismatch("integer", oldObj.getInteger(), newObj.getInteger());
        verifyOnce(listener).onMismatch("boxedLong", (long) oldObj.getBoxedLong(), (long) newObj.getBoxedLong());
        verifyOnce(listener).onMismatch("set", oldObj.getSet(), newObj.getSet());
        verifyOnce(listener).onMismatch("byteSet", oldObj.getByteSet(), newObj.getByteSet());
        verifyOnce(listener).onMismatch("pojo", oldObj.getPojo(), newObj.getPojo());
        verifyOnce(listener).onMismatch("optionalLong", oldObj.getOptionalLong(), newObj.getOptionalLong());
        verifyOnce(listener).onMismatch("optionalShort", oldObj.getOptionalShort(), newObj.getOptionalShort());
        verifyOnce(listener).onMismatch("enumeration", oldObj.getEnumeration(), newObj.getEnumeration());

        verifyNoMoreInteractions(listener);
    }

    @Test
    @DisplayName("Verify matcher finds mismatch between two partially different objects")
    void testPartialDifference(@Random IncrementalObject oldObj) {
        val newObj = oldObj
            .withBool(!oldObj.isBool())
            .withString(oldObj.getString() + "suffix")
            .withPojo(oldObj.getPojo().withInteger(oldObj.getPojo().getInteger() + 1))
            .withOptionalShort(
                oldObj.getOptionalShort()
                    .map(value -> (short) (value + 1))
                    .or(() -> Optional.of((short) 17))
            );

        val listener = mock(FieldMatchListener.class);
        MATCHER.match(oldObj, newObj, listener);

        verifyOnce(listener).onMismatch("bool", oldObj.isBool(), newObj.isBool());
        verifyOnce(listener).onMismatch("string", oldObj.getString(), newObj.getString());
        verifyOnce(listener).onMismatch("pojo", oldObj.getPojo(), newObj.getPojo());
        verifyOnce(listener).onMismatch("optionalShort", oldObj.getOptionalShort(), newObj.getOptionalShort());

        verifyNoMoreInteractions(listener);
    }

    @Value
    public static class FieldNameHashCollision {
        String wws8vw;
        int wws8x9;
        double justField;
    }

    @Test
    @DisplayName("Verify that fields name hash collision does not break matching")
    void testFieldsNamesHashCodeCollision(@Mock FieldMatchListener listener) {
        val oldObj = new FieldNameHashCollision("str", 42, 0.0);
        val newObj = new FieldNameHashCollision("trs", 24, 17.4);

        val matcher = DIFFUSION.fieldMatcherFor(FieldNameHashCollision.class);
        matcher.match(oldObj, newObj, listener);

        verifyOnce(listener).onMismatch("wws8vw", oldObj.wws8vw, newObj.wws8vw);
        verifyOnce(listener).onMismatch("wws8x9", oldObj.wws8x9, newObj.wws8x9);
        verifyOnce(listener).onMismatch("justField", oldObj.justField, newObj.justField);
    }
}
