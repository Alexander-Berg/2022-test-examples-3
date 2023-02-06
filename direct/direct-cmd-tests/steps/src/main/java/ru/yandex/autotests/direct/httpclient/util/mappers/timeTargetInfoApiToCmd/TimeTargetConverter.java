package ru.yandex.autotests.direct.httpclient.util.mappers.timeTargetInfoApiToCmd;

import org.dozer.DozerConverter;
import ru.yandex.autotests.directapi.common.api45.TimeTargetItem;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.04.15
 */
public class TimeTargetConverter extends DozerConverter<String, TimeTargetItem[]> {

    public TimeTargetConverter() {
        super(String.class, TimeTargetItem[].class);
    }

    @Override
    public TimeTargetItem[] convertTo(String source, TimeTargetItem[] destination) {
        return null;

    }

    //convert [0,1,2,3,4] -> ABCDE
    private String hoursFromApi(int[] hours) {
        String result = "";
        for (int hour : hours) {
            result += (char) (hour + 65);
        }
        return result;
    }

    @Override
    public String convertFrom(TimeTargetItem[] source, String destination) {
        String result = "";
        for (TimeTargetItem timeTargetItem : source) {
            String hours = hoursFromApi(timeTargetItem.getHours());
            String currentDayHours = "";
            for (int day : timeTargetItem.getDays()) {
                currentDayHours += String.valueOf(day) + hours;
            }
            if (result.length() == 0 || result.charAt(0) > timeTargetItem.getDays()[0]) {
                result = currentDayHours + result;
            } else {
                result = result + currentDayHours;
            }
        }
        return result;
    }
}
