package ru.yandex.market.global.partner.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

public class VisualCategoryUtil {

    private VisualCategoryUtil() {
    }

    public static List<String> getValidCategories(int count) {
        List<String> resultList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            resultList.add(RandomStringUtils.random(10, true, true).toLowerCase());
        }
        return resultList;
    }
}
