package ui_tests.src.test.java.tools;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Date {
    /**
     * Получить интервал 2 дат для groovy скрипта (за последние countOfMonths месяцев)
     *
     * @param countOfMonths
     * @return
     */
    public String getDateRangeForSeveralMonths(int countOfMonths) {
       return getDateRangeForSeveralMonths(countOfMonths,9);
    }

    /**
     * Получить интервал 2 дат для groovy скрипта (за последние countOfMonths месяцев)
     *
     * @param countOfMonths
     * @return
     */
    public String getDateRangeForSeveralMonths(int countOfMonths,int minusNDays) {
        String firstDate;
        String secondDate;
        ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(ZoneOffset.ofHours(3));
        firstDate = now.minusMonths(countOfMonths).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        secondDate = now.minusDays(minusNDays).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("'%s','%s'",
                firstDate, secondDate);
    }

    /**
     * Получить текущую дату
     * @return
     */
    public String getDateNow(){
        return OffsetDateTime.now().atZoneSameInstant(ZoneOffset.ofHours(3)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Получить интервал 2 дат для groovy скрипта (прошлый месяц 1 число - текущий месяц 28 число)
     */
    public String getDatesInterval() {
        return getDateRangeForSeveralMonths(12);
    }

    /**
     * Получить широкий интервал 2 дат для groovy скрипта (последние полгода)
     */
    public String getWideDatesInterval() {

        return getDateRangeForSeveralMonths(6);
    }

    /**
     * Сгенерировать строку с датой по шаблону
     */
    public String generateCurrentDateAndTimeStringOfFormat(String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.ofHours(3));
        return dtf.format(dateTime);
    }

    /**
     * Сгенерировать строку с заданной датой по шаблону
     */
    public String generateCurrentDateAndTimeStringOfFormat(String date, String fromFormat, String toFormat) {
        DateTimeFormatter todtf = DateTimeFormatter.ofPattern(toFormat, Locale.US);
        DateTimeFormatter fromdtf = DateTimeFormatter.ofPattern(fromFormat, Locale.US);
        LocalDateTime dateTime = LocalDateTime.parse(date,fromdtf);
        return todtf.format(dateTime);
    }
}
