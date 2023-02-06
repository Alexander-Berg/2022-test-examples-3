package ru.yandex.mail.diffusion;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.diffusion.IncrementalObject.NonIncremental;
import ru.yandex.mail.diffusion.patch.FieldChange;
import ru.yandex.mail.diffusion.patch.PatchProvider;
import ru.yandex.mail.diffusion.patch.exception.UnexpectedFieldException;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatchApplierTest extends BaseTest {
    private static final IncrementalObject OBJECT = new IncrementalObject(
        true,
        "str",
        42,
        100500L,
        Set.of("4", "8", "15", "16", "23", "42"),
        Set.of((byte) 4, (byte) 8, (byte) 15, (byte) 16, (byte) 23, (byte) 42),
        new NonIncremental("trs", 24),
        OptionalLong.of(0),
        Optional.of((short) 14),
        IncrementalObject.Enumeration.ONE
    );

    private static FieldChange mockChange(String name) {
        val change = mock(FieldChange.class);
        when(change.name()).thenReturn(name);
        return change;
    }

    private static void verifyChange(FieldChange change, Consumer<FieldChange> methodCall) {
        verify(change, atLeastOnce()).name();
        methodCall.accept(verify(change, atLeastOnce()));
        verifyNoMoreInteractions(change);
    }

    private static void verifySetChange(FieldChange change) {
        verify(change, atLeastOnce()).name();
        verify(change, atLeastOnce()).getAdded(any());
        verify(change, atLeastOnce()).getRemoved(any());
        verifyNoMoreInteractions(change);
    }

    @Test
    @DisplayName("Verify empty patch applies correctly")
    void testApplyEmptyPatch(@Mock PatchProvider provider) {
        when(provider.changes()).thenReturn(emptyList());

        val result = APPLIER.apply(OBJECT, provider);
        assertThat(result).isEqualTo(OBJECT);
    }

    @Test
    @DisplayName("Verify partial patch applies correctly")
    void testApplyPartialPatch(@Mock PatchProvider provider) {
        val bool = mockChange("bool");
        when(bool.getBool()).thenReturn(false);

        val string = mockChange("string");
        when(string.getString()).thenReturn("change");

        when(provider.changes()).thenReturn(List.of(bool, string));

        val result = APPLIER.apply(OBJECT, provider);

        val expectedObject = OBJECT.withBool(false).withString("change");
        assertThat(result).isEqualTo(expectedObject);

        verifyChange(bool, FieldChange::getBool);
        verifyChange(string, FieldChange::getString);
    }

    @Test
    @DisplayName("Verify full patch applies correctly")
    void testApplyFullPatch(@Mock PatchProvider provider) {
        val addedSet = Set.of("0", "100");
        val removedSet = Set.of("15", "42");
        val expectedSet = new HashSet<>(OBJECT.getSet());
        expectedSet.addAll(addedSet);
        expectedSet.removeAll(removedSet);

        val addedByteSet = Set.of((byte) 0, (byte) 100);
        val removedByteSet = Set.of((byte) 15, (byte) 42);
        val expectedByteSet = new HashSet<>(OBJECT.getByteSet());
        expectedByteSet.addAll(addedByteSet);
        expectedByteSet.removeAll(removedByteSet);

        val expectedObject = OBJECT
            .withBool(false)
            .withString("change")
            .withInteger(100)
            .withBoxedLong(0L)
            .withSet(expectedSet)
            .withByteSet(expectedByteSet)
            .withPojo(new NonIncremental("trs", 0))
            .withOptionalLong(OptionalLong.empty())
            .withOptionalShort(Optional.empty())
            .withEnumeration(IncrementalObject.Enumeration.TWO);

        val bool = mockChange("bool");
        when(bool.getBool()).thenReturn(expectedObject.isBool());

        val string = mockChange("string");
        when(string.getString()).thenReturn(expectedObject.getString());

        val integer = mockChange("integer");
        when(integer.getLong()).thenReturn((long) expectedObject.getInteger());

        val boxedLong = mockChange("boxedLong");
        when(boxedLong.getLong()).thenReturn(expectedObject.getBoxedLong());

        val set = mockChange("set");
        when(set.getAdded(String.class)).thenReturn(addedSet);
        when(set.getRemoved(String.class)).thenReturn(removedSet);

        val byteSet = mockChange("byteSet");
        when(byteSet.getAdded(Byte.class)).thenReturn(addedByteSet);
        when(byteSet.getRemoved(Byte.class)).thenReturn(removedByteSet);

        val pojo = mockChange("pojo");
        when(pojo.get(NonIncremental.class)).thenReturn(expectedObject.getPojo());

        val optionalLong = mockChange("optionalLong");
        when(optionalLong.getOptionalLong()).thenReturn(expectedObject.getOptionalLong());

        val optionalShort = mockChange("optionalShort");
        when(optionalShort.getOptional(Short.class)).thenReturn(expectedObject.getOptionalShort());

        val enumeration = mockChange("enumeration");
        when(enumeration.get(IncrementalObject.Enumeration.class)).thenReturn(expectedObject.getEnumeration());

        val changeMocks = List.of(bool, string, integer, boxedLong, set, byteSet, pojo, optionalLong, optionalShort,
            enumeration);
        when(provider.changes()).thenReturn(changeMocks);

        val result = APPLIER.apply(OBJECT, provider);
        assertThat(result).isEqualTo(expectedObject);

        verifyChange(bool, FieldChange::getBool);
        verifyChange(string, FieldChange::getString);
        verifyChange(integer, FieldChange::getLong);
        verifyChange(boxedLong, FieldChange::getLong);
        verifySetChange(set);
        verifySetChange(byteSet);
        verifyChange(pojo, mock -> mock.get(NonIncremental.class));
        verifyChange(optionalLong, FieldChange::getOptionalLong);
        verifyChange(optionalShort, mock -> mock.getOptional(Short.class));
        verifyChange(enumeration, mock -> mock.get(IncrementalObject.Enumeration.class));
    }

    @Test
    @DisplayName("Verify exception is thrown if patch contains unexpected field")
    void testUnexpectedField(@Mock PatchProvider provider, @Mock FieldChange invalidChange) {
        when(invalidChange.name()).thenReturn("invalidField");
        when(provider.changes()).thenReturn(singletonList(invalidChange));

        assertThrows(UnexpectedFieldException.class,
            () -> APPLIER.apply(OBJECT, provider));
    }
}
