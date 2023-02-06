package ru.yandex.market.mbo.db.modelstorage;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;

import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 01.02.2018
 */
public class ParameterValueMatchers {

    private ParameterValueMatchers() {
    }

    public static Matcher<ParameterValue> paramOption(String xslName, long optionId) {
        return new CustomTypeSafeMatcher<ParameterValue>("should have '" + xslName + "' with option #" + optionId) {
            @Override
            protected boolean matchesSafely(ParameterValue item) {
                return xslName.equals(item.getXslName()) && item.getOptionId() == optionId;
            }
        };
    }

    public static Matcher<ParameterValue> paramOption(String xslName, String ruValue) {
        return new CustomTypeSafeMatcher<ParameterValue>("should have '" + xslName + "'" +
            " with option '" + ruValue + "'") {
            @Override
            protected boolean matchesSafely(ParameterValue item) {
                return xslName.equals(item.getXslName()) &&
                    item.getStringValue().size() == 1 &&
                    ruValue.equals(item.getStringValue().get(0).getWord());
            }
        };
    }
}
