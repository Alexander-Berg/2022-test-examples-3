package ru.yandex.mail.diffusion;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.diffusion.patch.FieldChange;
import ru.yandex.mail.diffusion.patch.exception.InconsistentPatchException;
import ru.yandex.mail.diffusion.patch.exception.InvalidValueException;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplierOpsTest {
    private static final Set<Integer> SET = Set.of(4, 8, 15, 16, 23, 42);
    private static final Class<Integer> TYPE = Integer.class;

    private static <T> FieldChange mockChange(Set<T> added, Set<T> removed, Class<T> elementType) {
        val change = mock(FieldChange.class);
        when(change.getAdded(elementType)).thenReturn(added);
        when(change.getRemoved(elementType)).thenReturn(removed);
        return change;
    }

    private static void verifyChange(FieldChange change, int callsCount) {
        verify(change, atLeast(callsCount)).getAdded(any());
        verify(change, atLeast(callsCount)).getRemoved(any());
        verifyNoMoreInteractions(change);
    }

    private static void verifyChange(FieldChange change) {
        verifyChange(change, 1);
    }

    @Test
    @DisplayName("Verify empty change applicable to non-empty set")
    void testApplyingEmptyChangeToNonEmptySet() {
        val change = mockChange(emptySet(), emptySet(), TYPE);

        val result = ApplierOps.applySetChange("", SET, change, TYPE);
        assertThat(result).isEqualTo(SET);

        verifyChange(change);
    }

    @Test
    @DisplayName("Verify empty change applicable to empty set")
    void testApplyingEmptyChangeToEmptySet() {
        val change = mockChange(emptySet(), emptySet(), TYPE);

        val result = ApplierOps.applySetChange("", emptySet(), change, TYPE);
        assertThat(result).isEmpty();

        verifyChange(change);
    }

    @Test
    @DisplayName("Verify non-empty change, not containing removed elements, applicable to an empty set")
    void testApplyingNonEmptyChangeToEmptySetWithoutRemove() {
        val added = Set.of(100500, 0);
        val change = mockChange(added, emptySet(), TYPE);

        val result = ApplierOps.applySetChange("", emptySet(), change, TYPE);
        assertThat(result).isEqualTo(added);

        verifyChange(change);
    }

    @Test
    @DisplayName("Verify non-empty change, containing removed elements, is not applicable to an empty set")
    void testApplyingNonEmptyChangeToEmptySetWithRemove() {
        val change = mockChange(emptySet(), Set.of(0, 100500), TYPE);

        assertThrows(InconsistentPatchException.class,
            () -> ApplierOps.applySetChange("", emptySet(), change, TYPE));

        verifyChange(change, 0);
    }

    @Test
    @DisplayName("Verify non-empty change, containing consistent removed elements, applicable to non-empty set")
    void testApplyingNonEmptyChangeToNonEmptySetWithConsistentRemove() {
        val change = mockChange(Set.of(0, 100500), Set.of(15, 42), TYPE);

        val result = ApplierOps.applySetChange("", SET, change, TYPE);
        assertThat(result).isEqualTo(Set.of(0, 100500, 4, 8, 16, 23));

        verifyChange(change);
    }

    @Test
    @DisplayName("Verify non-empty change, containing inconsistent removed elements, is not applicable to non-empty set")
    void testApplyingNonEmptyChangeToNonEmptySetWithInconsistentRemove() {
        val change = mockChange(emptySet(), Set.of(1), TYPE);

        assertThrows(InconsistentPatchException.class,
            () -> ApplierOps.applySetChange("", SET, change, TYPE));

        verifyChange(change, 0);
    }

    @Test
    @DisplayName("Verify boxedByteValue call for valid number")
    void testValidBoxedByteValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(50L);
        assertThat(ApplierOps.boxedByteValue("", change)).isEqualTo((byte) 50);
    }

    @Test
    @DisplayName("Verify boxedByteValue call for invalid number")
    void testInvalidBoxedByteValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(-500L);
        assertThrows(InvalidValueException.class, () -> ApplierOps.boxedByteValue("", change));
    }

    @Test
    @DisplayName("Verify boxedShortValue call for valid number")
    void testValidBoxedShortValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(5000L);
        assertThat(ApplierOps.boxedShortValue("", change)).isEqualTo((short) 5000);
    }

    @Test
    @DisplayName("Verify boxedShortValue call for invalid number")
    void testInvalidBoxedShortValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(-1_000_000L);
        assertThrows(InvalidValueException.class, () -> ApplierOps.boxedShortValue("", change));
    }

    @Test
    @DisplayName("Verify boxedIntegerValue call for valid number")
    void testValidBoxedIntegerValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(10L);
        assertThat(ApplierOps.boxedIntegerValue("", change)).isEqualTo(10);
    }

    @Test
    @DisplayName("Verify boxedIntegerValue call for invalid number")
    void testInvalidBoxedIntegerValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(-8_000_000_000L);
        assertThrows(InvalidValueException.class, () -> ApplierOps.boxedIntegerValue("", change));
    }

    @Test
    @DisplayName("Verify boxedLongValue call")
    void testBoxedLongValue(@Mock FieldChange change) {
        when(change.getLong()).thenReturn(-10_000_000_000L);
        assertThat(ApplierOps.boxedLongValue("", change)).isEqualTo(-10_000_000_000L);
    }
}
