package ru.yandex.autotests.direct.httpclient.data;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.05.15
 */
public class Logins {

    public static final String MANAGER = "at-direct-mngr-full";
    public static final String TRANSFER_MANAGER = "at-direct-transfer-mngr";
    public static final String SUPER = "at-direct-super";
    public static final String AGENCY = "at-direct-ag-full";
    public static final String ADGROUPS_AGENCY = "adgroups-agency";
    public static final String PLACER = "at-direct-api-placer";
    public static final String MEDIAPLANER = "at-direct-media-new";
    public static final String SUPER_READER = "at-direct-web-superreader";
    public static final String SUPPORT = "at-direct-support";
    public static final String AGENCY_CLIENT = "at-direct-b-ag-c3";
    public static final String CLIENT_WITH_ACCOUNT = "at-direct-backend-c8";
    public static final String AGENCY_MAIN_REP = "rep-adgroups-agency";
    public static final String AGENCY_LIMITED_REP = "repcl-adgroups-agency";
    // Клиент для проверки оплаты кампаний. У этого клиента отключен общий счет, и стоит флаг, который запрещает
    // автоматически включать ОС при создании первой кампании.
    // Важно случайно не включить этому клиенту ОС - отключить его будет уже нельзя.
    public static final String CLIENT_WITHOUT_WALLET = "at-direct-back-nowallet7";
}
