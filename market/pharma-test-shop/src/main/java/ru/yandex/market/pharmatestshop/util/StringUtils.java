package ru.yandex.market.pharmatestshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {
    public static boolean isNaturalNumberFormat(String numberListStr) {
        if (numberListStr.isBlank())
            return true;
        List<String> numberIds = Arrays.stream(numberListStr.split(",")).collect(Collectors.toList());
        for (String numberStr : numberIds) {
            int number;
            try {
                number = Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                return false;
            }

            if (number < 0)
                return false;
        }
        return !numberIds.isEmpty();
    }
}
