package ru.yandex.direct.model.generator.uf;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.iterate;

@ParametersAreNonnullByDefault
class UnionFindTestData {

    private static final int SIZE = 10;

    static final List<Integer> ITEMS = iterate(0, a -> a + 1).limit(SIZE).collect(toList());

    static EntryStream<Integer, Integer> connections() {
        return StreamEx.of(
                Pair.of(4, 3),  // 0 1 2 3-4 5 6 7 8 9
                Pair.of(3, 8),  // 0 1 2 3-4-8 5 6 7 9
                Pair.of(6, 5),  // 0 1 2 3-4-8 5-6 7 9
                Pair.of(9, 4),  // 0 1 2 3-4-8-9 5-6 7
                Pair.of(2, 1),  // 0 1-2 3-4-8-9 5-6 7
                Pair.of(5, 0)   // 0-5-6|1-2|3-4-8-9|7
        ).mapToEntry(Pair::getLeft, Pair::getRight);
    }

    static Iterable<Object[]> connected() {
        return asList(
                new ConnectedTestCase().left(8).right(9).expect(TRUE),
                new ConnectedTestCase().left(0).right(6).expect(TRUE),
                new ConnectedTestCase().left(2).right(1).expect(TRUE),
                new ConnectedTestCase().left(8).right(4).expect(TRUE),
                new ConnectedTestCase().left(5).right(0).expect(TRUE),

                new ConnectedTestCase().left(0).right(1).expect(FALSE),
                new ConnectedTestCase().left(7).right(8).expect(FALSE),
                new ConnectedTestCase().left(7).right(2).expect(FALSE),
                new ConnectedTestCase().left(6).right(1).expect(FALSE),
                new ConnectedTestCase().left(5).right(4).expect(FALSE)
        );
    }

    private static class ConnectedTestCase {
        int left = -1, right = -1;

        ConnectedTestCase left(int left) {
            this.left = left;
            return this;
        }

        ConnectedTestCase right(int right) {
            this.right = right;
            return this;
        }

        Object[] expect(boolean expected) {
            checkState(left != -1 && right != -1, "not initialized");
            return new Object[]{left, right, expected};
        }

    }

}
