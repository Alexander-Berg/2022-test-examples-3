package ru.yandex.autotests.innerpochta.cal.util;

import com.google.common.collect.Sets;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * @author cosmopanda
 */
public class CalConsts {

    public static final String CORP_BASE_URL = "https://calendar.yandex-team.ru";
    public static final String CAL_BASE_URL = "https://calendar.yandex.ru";

    // Offset in current day column

    public static final int OUTSIDE_COLUMN = 700;
    public static final int COLUMN_CENTER = 100;
    public static final int TIME_11AM = 300;

    // Layer color, type
    public static final String YELLOW_COLOR = "#FFAB00";
    public static final String PURPLE_COLOR = "#6453BC";
    public static final String RED_COLOR = "#FF0000";
    public static final String USER_TYPE = "user";

    public static final Set<Coords> IGNORED = Sets.newHashSet(
        new Coords(0, 60, 240, 230), // скачет мини-календарь немного
        new Coords(240, 60, 58, 900) // полоса времени, текущее время постоянно меняется
    );

    //Repetitions consts
    public static final String REPEAT_EVERY_WEEK = "weekly";
    public static final String REPEAT_EVERY_DAY = "mon,tue,wed,thu,fri,sat,sun";

    //Grid links
    public static final String DAY_GRID = "/day";
    public static final String WEEK_GRID = "/week";
    public static final String MONTH_GRID = "/month";
    public static final String EVENT_GRID = "/event";

    //Event warnings
    public static final String ALL_EVENT_WARNING = "Вы редактируете серию событий.";
    public static final String ONE_EVENT_WARNING = "Вы редактируете одно событие в серии.";
    public static final String EXCEPTION_WARNING = "Вы редактируете исключение из серии.";
    public static final String EDIT_ALL_LINK = "Редактировать всю серию";
    public static final String EDIT_ONE_LINK = "Перейти к одному событию";

    public static final String LOCATION = "набережная реки Фонтанки, Санкт-Петербург";
    public static final int RANDOM_X_COORD = 600;
    public static final int RANDOM_Y_COORD = 500;
    public static String[] PARTICIPANTS = new String[]{
        "yandex-team-mailt-190@yandex.ru",
        "yandex-team-mailt-191@yandex.ru",
        "yandex-team-mailt-192@yandex.ru",
        "yandex-team-mailt-193@yandex.ru",
        "yandex-team-mailt-194@yandex.ru",
        "yandex-team-mailt-195@yandex.ru",
        "yandex-team-mailt-196@yandex.ru",
        "yandex-team-mailt-198@yandex.ru",
        "yandex-team-mailt-199@yandex.ru",
        "yandex-team-mailt-200@yandex.ru",
    };

    //To Do
    public static final String TODO_MENU_TITLE = "Список дел";
    public static final String DISABLED_TAB_SELECTOR = "qa-TabsItem_disabled";
    public static final String ENABLE_TAB_SELECTOR = "qa-TabsItem_active";
    public static final DateTimeFormatter TODO_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TODO_DATE_EN_FORMAT = DateTimeFormatter.ofPattern("MM.dd.yyyy");
    public static final DateTimeFormatter TODO_DATE_API_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TODO_THIS_YEAR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM");

    //Availability
    public static final String AVAILABILITY_BUSY = "Занят";
    public static final String AVAILABILITY_MAYBE_BUSY = "Возможно, занят";
    public static final String AVAILABILITY_UNBUSY = "Свободен";
    public static final String AVAILABILITY_UNBUSY_API = "available";

    //Invite status buttons names
    public static final String INVITE_BUTTON_YES = "Пойду";
    public static final String INVITE_BUTTON_MAYBE = "Возможно";
    public static final String INVITE_BUTTON_NO = "Не пойду";

    //Invite status notifications
    public static final String INVITE_NOTIFICATION_ACCEPTED = "Приглашение принято.";
    public static final String INVITE_NOTIFICATION_REJECTED = "Приглашение отклонено.";

    //Visibility
    public static final String VIEW_ALL = "Видят все";
    public static final String VIEW_MEMBERS = "Только участники";

    //View
    public static final String SCHEDULE = "/schedule";
    public static final String SCHEDULE_BUTTON = "Расписание";
    public static final String PLANNING_BUTTON = "Планирование";
    public static final String SCHEDULE_VIEW = "SCHEDULE";
}
