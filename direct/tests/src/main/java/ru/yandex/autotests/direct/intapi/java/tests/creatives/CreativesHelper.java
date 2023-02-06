package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import ru.yandex.autotests.direct.intapi.java.core.DirectJavaIntapiError;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.irt.testutils.RandomUtils;

/*
 * todo javadoc
 */
public class CreativesHelper {
    private DirectRule directRule;

    public CreativesHelper(DirectRule directRule) {
        this.directRule = directRule;
    }

    public Long getNotExistentCreativeId() {
        for (int i = 0; i < 10; i++) {
            Integer randomId = RandomUtils.getRandomInteger(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
            if (!directRule.dbSteps().shardingSteps().isCreativeExist(randomId.longValue())) {
                return randomId.longValue();
            }
        }
        throw new DirectJavaIntapiError("Cannot set not existent creative id");

    }
}
