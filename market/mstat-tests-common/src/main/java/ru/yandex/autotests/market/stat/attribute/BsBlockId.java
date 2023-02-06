package ru.yandex.autotests.market.stat.attribute;

import org.apache.commons.lang3.RandomStringUtils;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.date.DatePatterns;

/**
 * Created by entarrion on 21.01.15.
 */
public class BsBlockId {
    public static String generate()
    {
        return generate(LocalDateTime.now());
    }

    public static String generate(LocalDateTime showTime)
    {
        return generate(DatePatterns.RAW_FILES_DATE.format(showTime));
    }

    public static String generate(String showTime)
    {
        return generate(RandomStringUtils.random(10, Constants.NUMBERS), showTime);
    }

    public static String generate(String eventId, LocalDateTime showTime) {
        return generate(eventId,DatePatterns.RAW_FILES_DATE.format(showTime));
    }

    /**
     * @param eventId - number(len = [1,10])
     * @param showTime - "yyyy-MM-dd HH:mm:ss"";
     */
    public static String generate(String eventId, String showTime)
    {
        String normalizeEventId = String.format("%10s", eventId).replaceAll(" ","0");
        String normalizeShowtime = showTime.replaceAll("\\D","");
        return "2" + normalizeEventId + normalizeShowtime;
    }

    public static String resetNewShowTime(String bsBlockId, String showTime)
    {
        String normalizeShowtime = showTime.replaceAll("\\D","");
        return bsBlockId.substring(0,11) + normalizeShowtime;
    }

}
