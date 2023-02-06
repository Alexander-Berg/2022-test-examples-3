package ru.yandex.mail.diffusion;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.diffusion.patch.DiffUtils.Diff;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.diffusion.patch.DiffUtils.findDiff;

class DiffUtilsTest {
    @Test
    @DisplayName("Verify findDiff for empty sets results an empty diff")
    void testFindDiffOnEmptySets() {
        assertThat(findDiff(emptySet(), emptySet()))
            .isEqualTo(Diff.empty());
    }

    @Test
    @DisplayName("Verify findDiff returns correct diff")
    void testFindDiff() {
        val from = Set.of(1, 2, 3);
        val to = Set.of(2, 3, 4);

        val expectedDiff = new Diff<>(Set.of(4), Set.of(1));

        assertThat(findDiff(from, to))
            .isEqualTo(expectedDiff);
    }
}
