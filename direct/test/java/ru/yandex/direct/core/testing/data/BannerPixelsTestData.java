package ru.yandex.direct.core.testing.data;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.direct.core.entity.retargeting.model.Goal;

/**
 * Данные для тестов, проверяющих валидацию пикселей
 */
public class BannerPixelsTestData {

    // Большой айдишник Placement, необходимый, чтобы создать тестовую кампанию-сделку
    // В тестах используются BIG_PLACEMENT_PAGE_ID и BIG_PLACEMENT_PAGE_ID+1
    public static final Long BIG_PLACEMENT_PAGE_ID = 63545363L;

    //Идентификатор публичной цели из таблицы crypta_goals
    public static final Long PUBLIC_GOAL_ID = 2499000002L;
    //Идентификатор приватной цели из таблицы crypta_goals
    public static final Long PRIVATE_GOAL_ID = 2499000010L;

    //Идентификатор цели из таблицы crypta_goals, соответствующий "мужщина"
    private static final Long MALE_CRYPTA_GOAL_ID = 2499000001L;
    //Идентификатор цели из таблицы crypta_goals, соответствующий "женщина"
    private static final Long FEMALE_CRYPTA_GOAL_ID = 2499000002L;
    //Идентификатор цели из таблицы crypta_goals, соответствующий "низкий доход"
    private static final Long LOW_INCOME_GOAL_ID = 2499000009L;
    //Идентификатор цели из таблицы crypta_goals, соответствующий "средний доход"
    private static final Long MID_INCOME_GOAL_ID = 2499000010L;
    //Идентификатор родительской цели из таблицы crypta_goals, соответствующий целям вида "пол"
    private static final Long SEX_PARENT_GOAL_ID = 2499000021L;
    //Идентификатор родительской цели из таблицы crypta_goals, соответствующий целям вида "доход"
    private static final Long INCOME_PARENT_GOAL_ID = 2499000023L;

    /**
     * Фейковая таблица crypta_goals для тестов
     * Используется, чтобы создать mock CryptaSegmentRepository и возвращать getFakeCryptaGoalsForTest
     * в случае вызова getAll()
     *
     * @return мапа идентификатора цели в цель
     * @see ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository
     */
    public static Map<Long, Goal> getFakeCryptaGoalsForTest() {
        Map<Long, Goal> result = new HashMap<>();
        Goal midIncomeGoal = new Goal();
        midIncomeGoal.withId(MID_INCOME_GOAL_ID).withParentId(INCOME_PARENT_GOAL_ID);
        result.put(MID_INCOME_GOAL_ID, midIncomeGoal);
        Goal femaleCryptaGoal = new Goal();
        femaleCryptaGoal.withId(FEMALE_CRYPTA_GOAL_ID).withParentId(SEX_PARENT_GOAL_ID);
        result.put(FEMALE_CRYPTA_GOAL_ID, femaleCryptaGoal);
        Goal maleCryptaGoal = new Goal();
        maleCryptaGoal.withId(MALE_CRYPTA_GOAL_ID).withParentId(SEX_PARENT_GOAL_ID);
        result.put(MALE_CRYPTA_GOAL_ID, maleCryptaGoal);
        Goal lowIncomeGoal = new Goal();
        lowIncomeGoal.withId(LOW_INCOME_GOAL_ID).withParentId(INCOME_PARENT_GOAL_ID);
        result.put(LOW_INCOME_GOAL_ID, lowIncomeGoal);
        return result;
    }

    /**
     * Тестовый пиксель аудита dcm
     */
    public static String dcmPixelUrl() {
        return "https://ad.doubleclick.net/pixel/529875658445014901?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита adfox
     */
    public static String adfoxPixelUrl() {
        return "https://ads.adfox.ru/pixel/529875658445014901?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита adfox
     */
    public static String adfoxPixelUrl2() {
        return "https://ads.adfox.ru/pixel/5298756584450149018192734691283467?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита Я.Аудиторий
     */
    public static String yaAudiencePixelUrl() {
        return "https://mc.yandex.ru/pixel/529875658445014901?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита Я.Аудиторий
     */
    public static String yaAudiencePixelUrl2() {
        return "https://mc.yandex.ru/pixel/5298756584450149018192734691283467?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита TNS(MediaScope)
     * Не стоит использовать в тестах кроме отдельных тестов на tns, см DIRECT-88264
     */
    public static String tnsPixelUrl() {
        return "https://www.tns-counter.ru/V13a****weborama_ad/ru/UTF-8/tmsec=wadwatch3_217461-1996-1/%25aw_RANDOM%25";
    }

    /**
     * Тестовый пиксель аудита adriver
     */
    public static String adriverPixelUrl() {
        return "https://ad.adriver.ru/pixel/529875658445014901?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита adriver
     */
    public static String adriverPixelUrl2() {
        return "https://ad.adriver.ru/pixel/5298756584450149018192734691283467?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита adjust
     */
    public static String adjustPixelUrl() {
        return "https://view.adjust.com/pixel/5298756584450149018192734691283467?rnd=%aw_random%";
    }

    /**
     * Тестовый пиксель аудита mail_ru_top_100
     */
    public static String mailRuTop100PixelUrl() {
        return "https://top-fwz1.mail.ru/tracker?id=3129261;e=RG%3A/trg-pixel-5323887-1604422581605;_=%random%";
    }

    public static String adlooxPixelUrl() {
        return "https://pixel.adlooxtracking.ru/ads/ic.php?_=%aw_random%&type=pixel&plat=30&tag_id=238&client=weborama&id1=1089&id2=5&id3=1&id4=1x1&id5=1&id6=0&id7=8939&id11=&id12=russia&id14=$ADLOOX_WEBSITE";
    }

    public static String sizmecPixelUrl() {
        return "https://bs.serving-sys.ru/Serving/adServer.bs?cn=display&c=19&pli=1000029314&adid=1000029315&rnd=%aw_random%";
    }

    /**
     * Тестовый невалидный пиксель аудита
     */
    public static String invalidUrl() {
        return "https://ad.adada.ru/pixel/7216398§253?rnd=%aw_random%";
    }
}
