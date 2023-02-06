package ru.yandex.autotests.directintapi.tests;

/**
 * Created by pashkus on 26.04.16.
 */
public class IntapiConstants {

    public static final String DIRECT_DOMAIN_FORMAT = "direct.yandex.%s";
    public static final String BAYAN_DOMAIN_FORMAT = "ba.yandex.%s";

    public static final String NOTIFY_ORDER_ERROR_1 = "invalid TotalConsumeQty from balance: undef, must be >= 0";
    public static final String NOTIFY_ORDER_ERROR_2 = "invalid TotalConsumeQty from balance: None, must be >= 0";

    //Заголовки писем от ручки NotifyOrder2
    public static final String NOTIFY_ORDER_MONEY_IN_SUBJECT_TEXT = "Счет кампании N%d пополнен";
    public static final String NOTIFY_ORDER_MONEY_IN_FOR_MCB_SUBJECT_TEXT =
            "Яндекс.Баян/Медийная кампания N%d/ На Ваш счет поступила оплата";
    public static final String NOTIFY_ORDER_MONEY_IN_FOR_WALLET_SUBJECT_TEXT =
            "Счет аккаунта %s  пополнен";
    public static final String NOTIFY_ORDER_CAMP_FINISHED_SUBJECT_TEXT = "Яндекс.Директ/Кампания N%d закончилась %s";
    public static final String NOTIFY_ORDER_MONEY_OUT_BLOCKING_SUBJECT_TEXT =
            "Яндекс.Директ/Произведен возврат средств с кампании N%d";
    public static final String NOTIFY_ORDER_MONEY_IN_SMS_TEXT = "Директ: зачисление %s на заказ  N%d. Чтобы " +
            "реклама не остановилась неожиданно, подключите автоплатёж https://ya.cc/direct/autopay";
    public static final String NOTIFY_ORDER_CAMP_FINISHED_SMS_TEXT = "Директ: кампания N%d закончилась %s.";

    //Активное объявление из прода
    public static final Long ACTIVE_BANNER_ID = 158948962L;
    public static final String ACTIVE_PHRASE_TEXT = "сглаз компьютера";
    public static final String ACTIVE_TEXT = "Снимем сглаз с Вашего ПК. Методики древних шаманов.";

    /**
     * Продакшен значение PriorityID для групп, успешно синхронизированных с БК;
     * see also: https://st.yandex-team.ru/TESTIRT-10115#1471960301000
     */
    public static final Long PRIORITY_ID_FOR_SYNCED_GROUPS = 1L;
}
