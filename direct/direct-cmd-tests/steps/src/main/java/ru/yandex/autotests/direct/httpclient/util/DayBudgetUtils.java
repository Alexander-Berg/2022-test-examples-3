package ru.yandex.autotests.direct.httpclient.util;

import org.apache.commons.httpclient.HttpException;
import ru.yandex.autotests.direct.httpclient.data.strategy.DayBudget;
import ru.yandex.autotests.directapi.common.api45.DayBudgetInfo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 19.09.14
 */
public class DayBudgetUtils {

    public static DayBudgetInfo toDayBudgetInfo(DayBudget dayBudget) {
        DayBudgetInfo dayBudgetInfo = new DayBudgetInfo();
        try {
            if (dayBudget != null) {
                if (dayBudget.getSum() != null) {
                    dayBudgetInfo.setAmount(Float.valueOf(dayBudget.getSum()));
                }
                if (dayBudget.getShowMode() != null) {
                    dayBudgetInfo.setSpendMode(firstUpperCase(dayBudget.getShowMode()));
                }
            }
        } catch (Exception e) {
            throw new Error("Не удалось преобразовать параметры дневного бюджета к DayBudgetInfo" + dayBudget.toJson());
        }
        return dayBudgetInfo;
    }

    private static String firstUpperCase(String word){
        if(word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }
}
