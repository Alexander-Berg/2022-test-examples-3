package ru.yandex.autotests.innerpochta.touch.data;

/**
 * @author oleshko
 */
public class PromoConstants {
    //Настройка при которой покажется велкам скрин
    public static final String SHOW_WELCOME = "%7B%22subs_closed%22%3A10%2C%22subs_first-show%22%3A1647609402378%2C" +
        "%22subs_last-show%22%3A1647609403171%2C%22subs_opened%22%3A0%2C%22subs_show-count-all%22%3A10%7D";
    //Настройка при которой покажется смарт банер
    public static final String SHOW_SMART = "%7B%22ws_show-count-all%22%3A22%2C%22ws_show-count%22" +
        "%3A22%2C%22ws_close-count%22%3A22%2C%22ws_stage%22%3A%22wait%22%2C%22ws_first-show%22%3A1496078912522%2C" +
        "%22ws_last-show%22%3A" + String.valueOf(System.currentTimeMillis() / 1000 - 172800) +
        "000%2C%22subs_show-count-all%22%3A10%2C%22subs_opened%22%3A0%2C" +
        "%22subs_closed%22%3A0%2C%22subs_first-show%22%3A1621520241977%2C%22subs_last-show%22%3A1616235692000%7D";
    //ВС псследний раз показывался <180 дней назад - велкам скрин не покажется
    public static final String NOT_SHOW_WELCOME_BEFORE_14_DAYS = "%7B%22subs_closed%22%3A10%2C%22subs_first-show%22%3A" +
        "1647609402378%2C%22subs_last-show%22%3A1647609403171%2C%22subs_opened%22%3A0%2C%22subs_show-count-all%22%3A" +
        "10%2C%22ws_show-count-all%22%3A22%2C%22ws_show-count%22%3A22%2C%22ws_close-count%22%3A22%2C%22ws_stage%22%3A" +
        "%22show%22%2C%22ws_first-show%22%3A1496078912522%2C%22ws_last-show%22%3A" +
        String.valueOf(System.currentTimeMillis() / 1000 - 1209400) + "000%7D";
    //ВС псследний раз показывался >180 дней назад - велкам скрин  покажется
    public static final String SHOW_WELCOME_AFTER_14_DAYS = "%7B%22subs_closed%22%3A10%2C%22subs_first-show%22%3A" +
        "1647609402378%2C%22subs_last-show%22%3A1647609403171%2C%22subs_opened%22%3A0%2C%22subs_show-count-all%22%3A" +
        "10%2C%22ws_last-show%22%3A"  +  String.valueOf(System.currentTimeMillis() / 1000 - 1209600) +
        "000%2C%22ws_show-count-all%22%3A1%7D";
    //Последний раз смарт банер показался менее 3 недель назад - смарт банер не покажется
    public static final String NOT_SHOW_SMART_DURING_3_WEEKS = "%7B%22ws_show-count-all%22%3A22%2C%22ws_show-count%22" +
        "%3A22%2C%22ws_close-count%22%3A22%2C%22ws_stage%22%3A%22wait%22%2C%22ws_first-show%22%3A1496078912522%2C" +
        "%22ws_last-show%22%3A" + String.valueOf(System.currentTimeMillis() / 1000 - 172800) +
        "000%2С%22ws_stage%22%3A%22wait%22%2C%22sb_show-count" +
        "%22%3A22%2C%22sb_close-count%22%3A22%2C%22sb_first-show%22%3A1492520707124%2C%22sb_last-show%22%3A" +
        String.valueOf(System.currentTimeMillis() / 1000 - 1810000) + "000%2C%22sb_show-in-a-row-count%22%3A0%7D";
    //Последний раз смарт банер показался более 3 недель назад - смарт банер покажется
    public static final String SHOW_SMART_AFTER_3_WEEKS = "%7B%22ws_show-count-all%22%3A22%2C%22ws_show-count%22" +
        "%3A22%2C%22ws_close-count%22%3A22%2C%22ws_stage%22%3A%22wait%22%2C%22ws_first-show%22%3A1496078912522%2C" +
        "%22ws_last-show%22%3A" + String.valueOf(System.currentTimeMillis() / 1000 - 172800) +
        "000%2C%22subs_show-count-all%22%3A10%2C%22subs_opened%22%3A0%2C%22subs_closed%22%3A0%2C%22subs_last-show" +
        "%22%3A1534138741000%2C%22sb_show-count%22%3A22%2C%22sb_close-count%22%3A22%2C%22sb_first-show" +
        "%22%3A1492520707124%2C%22sb_last-show%22%3A" + String.valueOf(System.currentTimeMillis() / 1000 - 1815000) +
        "000%2C%22sb_show-in-a-row-count%22%3A22%7D";

    //Не покажется промо уведомлений
    public static final String NOT_SHOW_PUSH_PROMO = "{\"success\":false,\"twoweeks\":true,\"threedays\":false,\"timestamp\":1591270730734%7D";

    //Покажется промо уведомлений
    public static final String SHOW_PUSH_PROMO = "%7B%22success%22%3Afalse%2C%22twoweeks%22%3Afalse%2C%22threedays%22%3Atrue%2C%22timestamp%22%3A0%7D";

    //Покажется промо рассылок, количество показов 9
    public static final String SHOW_SUBS_PROMO = "%7B%22subs_show-count-all%22%3A9%2C%22subs_opened%22%3A0%2C" +
        "%22subs_closed%22%3A0%2C%22subs_last-show%22%3A1534138741000%7D";

    //Не покажется промо рассылок, количество показов 10
    public static final String NOW_SHOW_SUBS_PROMO_MORE_10_TIMES = "%7B%22subs_show-count-all%22%3A10%2C" +
        "%22subs_opened%22%3A0%2C%22subs_closed%22%3A0%2C%22subs_last-show%22%3A1534138741000%7D";

    //Не покажется промо рассылок, количество показов 1, 1 закрытие промки
    public static final String NOT_SHOW_SUBS_PROMO_AFTER_CLOSE = "%7B%22subs_show-count-all%22%3A1%2C"
        + "%22subs_opened%22%3A0%2C%22subs_closed%22%3A1%2C%22subs_last-show%22%3A1534138741000%7D";

    //Не покажется промо рассылок, количество показов 1, 1 переход по промке
    public static final String NOT_SHOW_SUBS_PROMO_AFTER_OPEN = "%7B%22subs_show-count-all%22%3A1%2C" +
        "%22subs_opened%22%3A1%2C%22subs_closed%22%3A0%2C%22subs_last-show%22%3A1534138741000%7D";

    //Не покажется промо рассылок, количество показов 1, последний показ < месяца назад
    public static final String NOT_SHOW_SUBS_PROMO_DURING_MONTH = "%7B%22subs_show-count-all%22%3A1%2C" +
        "%22subs_opened%22%3A0%2C%22subs_closed%22%3A0%2C%22subs_last-show%22%3A" +
        String.valueOf(System.currentTimeMillis() / 1000 - 2505600) + "000%7D";

    //Покажется промо рассылок, количество показов 1, последний показ > месяца назад
    public static final String SHOW_SUBS_PROMO_AFTER_MONTH = "%7B%22subs_show-count-all%22%3A1%2C" +
        "%22subs_opened%22%3A0%2C%22subs_closed%22%3A0%2C%22subs_last-show%22%3A" +
        String.valueOf(System.currentTimeMillis() / 1000 - 2592000) + "000%7D";

    //Покажется промо рассылок, количество показов 1, последний показ > месяца назад
    public static final String NOT_SHOW_ANY_PROMO = "%7B%22ws_show-count-all%22%3A22%2C%22ws_show-count%22" +
        "%3A22%2C%22ws_close-count%22%3A22%2C%22ws_stage%22%3A%22show%22%2C%22ws_first-show%22%3A1496078912522%2C" +
        "%22ws_last-show%22%3A" + String.valueOf(System.currentTimeMillis() / 1000 - 172800) +
        "000%2C%22ws_stage%22%3A%22wait%22%2C%22sb_show-count"+
        "%22%3A22%2C%22sb_close-count%22%3A22%2C%22sb_first-show%22%3A1492520707124%2C%22sb_last-show%22%3A" +
        String.valueOf(System.currentTimeMillis() / 1000 - 172800) + "000%2C%22sb_show-in-a-row-count%22%3A0%7D";
}
