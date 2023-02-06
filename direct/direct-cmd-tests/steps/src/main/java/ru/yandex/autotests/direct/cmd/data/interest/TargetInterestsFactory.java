package ru.yandex.autotests.direct.cmd.data.interest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TargetInterestsFactory {
    private TargetInterestsFactory(){}

    public static TargetInterests defaultTargetInterest(Long categoryId) {
        return new TargetInterests()
                .withTargetCategoryId(categoryId)
                .withRetId(0)
                .withPriceContext(0.78d)
                .withAutobudgetPriority(3);
    }

    public static List<TargetInterests> defaultInterests(Long... categoriIds) {
        return Arrays.stream(categoriIds)
                .map(TargetInterestsFactory::defaultTargetInterest)
                .collect(Collectors.toList());
    }
}
