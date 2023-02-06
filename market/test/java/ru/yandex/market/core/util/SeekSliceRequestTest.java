package ru.yandex.market.core.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.mbi.web.paging.SeekableSlice;

@ParametersAreNonnullByDefault
class SeekSliceRequestTest {
    private static final List<Integer> TESTED_SORTED_LIST =
            Collections.unmodifiableList(Arrays.asList(1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37));

    private static final Map<String, SeekableSlice<Integer, String>> TEST_SLICE_LIST = createTestSliceList();

    private static List<Integer> accessSortedList(SeekSliceRequest<Integer> request) {
        if (request.reverseOrder()) {
            throw new UnsupportedOperationException("Reverse order is not supported");
        }

        return request.seekKey()
                .map(key -> TESTED_SORTED_LIST.stream().filter(i -> i > key))
                .orElse(TESTED_SORTED_LIST.stream())
                .limit(request.limit())
                .collect(Collectors.toList());
    }

    private static SeekableSlice<Integer, String> accessSlices(SeekSliceRequest<String> request) {
        if (request.reverseOrder()) {
            throw new UnsupportedOperationException("Reverse order is not supported");
        }

        return TEST_SLICE_LIST.get(request.seekKey().orElse("A"));
    }

    private static Map<String, SeekableSlice<Integer, String>> createTestSliceList() {
        Map<String, SeekableSlice<Integer, String>> result = new HashMap<>();
        List<Integer> aList = Collections.unmodifiableList(Arrays.asList(1, 2, 3));
        List<Integer> bList = Collections.unmodifiableList(Arrays.asList(4, 5));
        List<Integer> cList = Collections.singletonList(6);
        List<Integer> dList = Collections.emptyList();
        List<Integer> eList = Collections.singletonList(7);
        List<Integer> fList = Collections.unmodifiableList(Arrays.asList(8, 9));
        List<Integer> gList = Collections.unmodifiableList(Arrays.asList(10, 11, 12));
        result.put("A", SeekableSlice.<Integer, String>of(aList).withNextSliceKey("B"));
        result.put("B", SeekableSlice.<Integer, String>of(bList).withNextSliceKey("C"));
        result.put("C", SeekableSlice.<Integer, String>of(cList).withNextSliceKey("D"));
        result.put("D", SeekableSlice.<Integer, String>of(dList).withNextSliceKey("E"));
        result.put("E", SeekableSlice.<Integer, String>of(eList).withNextSliceKey("F"));
        result.put("F", SeekableSlice.<Integer, String>of(fList).withNextSliceKey("G"));
        result.put("G", SeekableSlice.of(gList));
        return Collections.unmodifiableMap(result);
    }

    @Test
    void testGenerateStreamFromSublists() {
        int[] expected = TESTED_SORTED_LIST.stream().mapToInt(Number::intValue).toArray();
        int[] generated =
                SeekSliceRequest.generateStream(3, SeekSliceRequestTest::accessSortedList, Function.identity())
                        .mapToInt(Number::intValue)
                        .toArray();
        Assertions.assertArrayEquals(expected, generated);
    }

    @Test
    void testGenerateStreamFromSlices() {
        int[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        int[] generated = SeekSliceRequest.generateStream(3, SeekSliceRequestTest::accessSlices)
                .mapToInt(Number::intValue)
                .toArray();
        Assertions.assertArrayEquals(expected, generated);
    }
}
