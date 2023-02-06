package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class SomeTest {

    @Test
    public void someTest() {
        int[] arr = {1, 3, 9, 10, 8, 9};
        System.out.println(pair(18, arr));
    }

    public Pair<Integer, Integer> pair(int n, int[] arr) {
        Arrays.sort(arr);
        int l = 0;
        int r = arr.length - 1;
        while (l < r) {
            if (arr[l] + arr[r] - n > 0) {
                r--;
            } else if (arr[l] + arr[r] - n < 0) {
                l++;
            } else {
                return Pair.of(arr[l], arr[r]);
            }
        }
        return null;
    }
}
