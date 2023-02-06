package ru.yandex.direct.jobs.moderation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.jobs.moderation.ModerationUtils.subtractByIdentity;

public class ModerationUtilsSubtractByIdentityTest {

    private static final Obj ABC_1 = new Obj("abc");
    private static final Obj ABC_2 = new Obj("abc");
    private static final Obj DEF = new Obj("def");

    @ParameterizedTest
    @MethodSource("args")
    public void subtractByIdentityTest(List<Obj> a, List<Obj> b, List<Obj> expected) {
        List<Obj> actual = subtractByIdentity(a, b);
        assertEquals(actual.size(), expected.size(), "expected and actual lists sizes are not equal");
        for (int i = 0; i < actual.size(); i++) {
            assertSame(actual.get(i), expected.get(i), "elems are not same at index " + i);
        }
    }

    static Stream<Arguments> args() {
        return Stream.of(
                arguments(
                        List.of(ABC_1),
                        List.of(ABC_1),
                        List.of()),
                arguments(
                        List.of(ABC_1, ABC_2),
                        List.of(ABC_1),
                        List.of(ABC_2)),
                arguments(
                        List.of(ABC_1, ABC_2, DEF),
                        List.of(ABC_1, DEF),
                        List.of(ABC_2)),
                arguments(
                        List.of(ABC_1, ABC_2),
                        List.of(ABC_2, ABC_1),
                        List.of()),

                arguments(
                        List.of(ABC_1),
                        List.of(ABC_2),
                        List.of(ABC_1)),
                arguments(
                        List.of(ABC_1, ABC_2),
                        List.of(DEF),
                        List.of(ABC_1, ABC_2)),
                arguments(
                        List.of(ABC_1, ABC_2),
                        List.of(ABC_2, DEF),
                        List.of(ABC_1)),

                arguments(
                        List.of(),
                        List.of(ABC_1),
                        List.of()),
                arguments(
                        List.of(ABC_1, DEF),
                        List.of(),
                        List.of(ABC_1, DEF))
        );
    }

    private static class Obj {
        private final String str;

        public Obj(String str) {
            this.str = str;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Obj obj = (Obj) o;
            return Objects.equals(str, obj.str);
        }

        @Override
        public int hashCode() {
            return Objects.hash(str);
        }
    }
}
