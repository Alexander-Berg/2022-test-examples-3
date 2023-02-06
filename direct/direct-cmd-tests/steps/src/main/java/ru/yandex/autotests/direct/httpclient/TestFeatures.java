package ru.yandex.autotests.direct.httpclient;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 19.09.14
 */
public class TestFeatures {

    public static final String AJAX_SAVE = "Ajax save";

    public static final String STEP_ZERO_PROCESS = "контроллер stepZeroProcess";

    public class StepZeroProcess {

        public static final String SUCCESSFUL_REDIRECT = "Успешный выбор пользователя, проверка редиректа";
        public static final String CLIENTS = "Проверка разных типов клиентов";
    }

    public static final String PHRASES = "Фразы";

    public class Phrases {
        public static final String AJAX_TEST_PHRASES = "ajaxTestPhrases";

        public static final String AJAX_UPDATE_PHRASES_AND_PRICES = "тесты контроллера ajaxUpdateShowConditions";

        public static final String AJAX_GET_TRANSITIONS_BY_PHRASES = "тесты контроллера ajaxGetTransitionsByPhrases";

        public static final String AJAX_APPLY_REJECT_CORRECTION = "тесты контроллера ajaxApplyRejectCorrection";

        public static final String AJAX_UPDATE_SHOW_CONDITIONS = "тесты контроллера ajaxUpdateShowConditions";

        public static final String AJAX_CHECK_CAMP_MINUS_WORDS = "тесты контроллера ajaxCheckCampMinusWords";

        public static final String AJAX_CHECK_BANNERS_MINUS_WORDS = "тесты контроллера ajaxCheckBannersMinusWords";
    }

    public class AjaxSave {

        public static final String AJAX_SAVE_AUTOBUDGET = "ajaxSaveAutobudget";

        public static final String AJAX_SAVE_DAY_BUDGET = "ajaxSaveDayBudget";

        public static final String AJAX_SAVE_STRATEGY = "ajaxSaveStrategy";

        public static final String AJAX_SAVE_STRATEGY_CHECK_ERRORS = "ajaxCheckStrategyErrors";
    }

    public static final String CONTACT_INFO = "Контактная информация";

    public class ContactInfo {
        public static final String CONTACT_INFO_VALIDATION = "Валидация контактной информации";
    }

    public static final String BANNERS = "Banners";

    public class Banners {
        public static final String IMAGE_BANNERS_PARAMETERS = "Параметры графических объявлений";

        public static final String CANVAS_BANNERS_PARAMETERS = "Параметры canvas объявлений";

        public static final String MOBILE_BANNERS_PARAMETERS = "Параметры мобильных объявлений";

        public static final String BANNERS_PARAMETERS = "Параметры объявлений";

        public static final String MANAGE_VCARDS = "Проверка страницы мастера визиток (manageVCards)";

        public static final String SAVE_TEXT_ADGROUPS_FIELDS = "Проверка полей в контроллере saveTextAdGroups";

        public static final String BANNERS_LIGHT_PARAMETERS = "Проверка параметров объявлений в контроллерах легкого интерфейса";

        public static final String BANNERS_LIGHT_VALIDATION = "Проверка валидации объявлений в контроллерах легкого интерфейса";

        public static final String CHECK_BANNERS_MINUS_WORDS = "тесты контроллера ajaxCheckBannersMinusWords";

        public static final String AJAX_GET_BANNERS_COUNT = "Контроллер ajaxGetBannersCount";

        public static final String DELETE_BANNER = "Удаление баннера контроллером delBanner";

        public static final String SEARCH_BANNERS = "Контроллер searchBanners";

        public static final String BANNER_FLAGS = "Установка и удаление флагов модерации";

	    public static final String BANNER_TURBO_LANDINGS = "Установка и удаление турболендингов";

        public static final String GET_AD_GROUP = "тесты контроллера getAdGroup";

        public static final String SWITCH_OFF_VCARD = "Удаление визитки из баннера";

        public static final String COPY_GROUP = "Копирование группы";

        public static final String ADD_FIRST_GROUP_WITH_TWO_DIFF_BANNERS = "Создание первой группы в новой кампании, " +
                "где будет 1 десктопный и 1 мобильный баннер";

        public static final String ADD_GROUP_WITH_TWO_DIFF_BANNERS_TO_OLD_CAM = "Создание группы с 2 мя баннерами " +
                "разного типа в компании с уже существующими группами";

        public static final String AUTO_CORRECTION = "Автоисправление баннера перед модерацией";

        public static final String BANNERS_CALLOUTS = "Текстовые дополнения баннеров";

        public static final String BANNERS_DISCLAIMER = "Дисклеймер баннеров";

        public static final String DISPLAY_HREF = "Отображаемая ссылка";

        public static final String SITELINKS = "Быстрые ссылки";

        public static final String VCARD = "Визитка";

        public static final String STATUS_BS_SYNCED = "Статус синхронизации с БК (statusBsSynced)";

        public static final String MEDIA_RESOURCES = "Аудио/видео";

        public static final String VIDEO_ADDITION  = "Видеодополнение";
    }

    public static final String CAMPAIGNS = "Кампании";


    public class Campaigns {

        public static final String SHOW_CAMP = "Параметры кампании (showCamp)";

        public static final String AJAX_STOP_RESUME_CAMP = "контроллер ajaxStopResumeCamp";

        public static final String AJAX_SAVE_CAMP_DESCRIPTION = "контроллер ajaxSaveCampDescription";

        public static final String ADJUSTMENT_MOBILE = "Корректировка ставок для мобильных приложений";

        public static final String ADJUSTMENT_DEMOGRAPHY = "Корректировка ставок по полу и возрасту";

        public static final String ADJUSTMENT_RETARGERING = "Корректировка ставок для посетивших сайт";

        public static final String ADJUSTMENT_PERFORMANCE_TGO = "Корректировка ставок для Смарт-ТГО";

        public static final String ADJUSTMENT_AB_SEGMENT = "Корректировка ставок для аб-сегментов";

        public static final String SHOW_ADJUSTMENTS = "Возможные корректировки ставок на баннере";

        public static final String EDIT_CAMP = "Страница параметров кампании (контроллер editCamp)";

        public static final String SAVE_NEW_CAMP = "Контроллер сохранения параметров новой кампании (saveNewCamp)";

        public static final String SAVE_CAMP = "Контроллер сохранения параметров кампании (saveCamp)";

        public static final String DISABLED_SSP = "Запрещенные SSP-платформы";

        public static final String SAVE_CAMP_EASY = "Контроллер сохранения параметров кампании в легком интерфейсе " +
                "(saveCampEasy)";

        public static final String COPY_CAMP = "Копирование кампаний (copyCamp)";

        public static final String CAMP_UNARC = "Разархивирование кампаний (CampUnarc)";

        public static final String SHOW_CAMPS = "Страница кампаний (showCamps)";

        public static final String DEL_CAMP = "Удаление кампаний (delCamp)";

        public static final String SET_AUTO_PRICE_AJAX = "изменение цен кампании (setAutoPriceAjax)";

        public static final String SHOW_CONTACT_INFO = "Попап визитки (showContactInfo)";

        public static final String SHOW_CAMP_STAT = "showCampStat";

        public static final String CREATE_AB_TEST = "ajaxCreateExperiment";
    }

    public static final String RETARGETING = "Условия ретаргетинга";

    public class Retargeting {

        public static final String AJAX_SAVE_RETARGETING_CONDITIONS = "тесты контроллера ajaxSaveRetargetingCond";

        public static final String AJAX_REPLACE_GOALS_IN_RETARGETINGS = "тесты контроллера ajaxReplaceGoalsInRetargetings";

        public static final String AJAX_DELETE_RETARGETING_CONDITIONS = "тесты контроллера ajaxDeleteRetargetingCond";

        public static final String AJAX_GET_GOALS_FOR_RETARGETING = "тесты контроллера ajaxGetGoalsForRetargeting";

        public static final String AJAX_GET_RET_COND_WITH_GOALS = "тесты контроллера ajaxGetRetCondWithGoals";

        public static final String SHOW_RETARGETING_CONDITIONS = "тесты контроллера showRetargetingCond";

        public static final String AJAX_UPDATE_SHOW_CONDITIONS = "тесты контроллера ajaxUpdateShowConditions";

    }

    public static final String SEVERAL_CONTROLLER_PARAMETERS = "Параметры нескольких контроллеров";

    public class SeveralControllerParameters {

        public static final String STATUS_MOVE_AND_NEW_LOGIN_PARAMETERS_ABSENCE =
                "Отсутсвие параметров statusMove и newLogin";

        public static final String FIO_PARAMETER_CHANGE =
                "Изменение параметра FIO на fio";
    }

    public static final String REDIRECTS = "Редиректы";

    public class Redirects {

        public static final String ADVERTIZE_REDIRECTS =
                "Редиректы контроллера advertize";
    }

    public static final String NEW_CLIENT = "Новый клиент";

    public class NewClient {

        public static final String AJAX_VALIDATE_LOGIN =
                "Контроллер AjaxValidateLogin";

        public static final String AJAX_SUGGEST_LOGIN =
                "Контроллер AjaxSuggestLogin";

        public static final String AJAX_VALIDATE_PASSWORD =
                "Контроллер AjaxValidatePassword";

        public static final String SHOW_REGISTER_LOGIN_PAGE =
                "Контроллер showRegisterLoginPage";

        public static final String AJAX_REGISTER_LOGIN =
                "Контроллер ajaxRegisterLogin";
    }

    public static final String CLIENT = "Клиент";

    public class Client {

        public static final String MARKET_RATE =
                "Рейтинг магазина маркета";

        public static final String SEARCH_CLIENT_ID = "Контроллер поиска и создания клиента в Директе";

        public static final String CHOOSE_INTERFACE_TYPE =
                "Контроллер страницы выбора интерфейса (chooseInterfaceType)";

        public static final String MODIFY_USER = "Настройки пользователя (modifyUser)";

        public static final String USER_SETTINGS = "Настройки пользователя (saveSettings)";

        public static final String SWITCH_EASINESS = "Переключенние интерфейса пользователя (switchEasiness)";
    }

    public static final String FAILED_BECAUSE_OF_BUG = "Тесты, падающие из-за багов директа";

    public static final String STEP_ZERO = "Нулевой шаг";

    public class StepZero {
        public static final String STEP_ZERO_ROLES = "Тесты контроллера stepZero под разными ролями";

        public static final String STEP_ZERO_FOR_AGENCY = "Тесты вызова контроллера stepZero под разными ролями для агенства";

        public static final String STEP_ZERO_CLIENTS = "Проверки списка пользователей для контроллера stepZero";
    }

    public static final String FIRST_AID = "Первая помощь";

    public class FirstAid {

        public static final String SEND_OPTIMIZE = "Оптимизация групп баннеров (sendOptimize)";

        public static final String SEND_CAMPAIGN_OPTIMIZING_ORDER = "Отправка заявки на первую помощь " +
                "(sendCampaignOptimizingOrder)";

        public static final String ACCEPT_OPTIMIZE = "Первый шаг принятия рекомендаций первой помощи (acceptOptimize)";

        public static final String COMPLETE_OPTIMIZING = "Завершение оптимизации кампании (completeOptimizing)";

        public static final String ACCEPT_OPTIMIZE_STEP2 = "Второй шаг принятия рекомендаций первой помощи " +
                "(acceptOptimize_step2)";

    }


    public static final String BAYAN = "Баян";

    public class Bayan {

        public static final String SHOW_MEDIA_CAMP = "Страница медийной кампании (showMediaCamp)";

        public static final String SAVE_MEDIA_BANNER = "Сохранение медиа баннера (saveMediaBanner)";
    }

    public static final String PAYMENT = "Оплата";

    public class Payment {

        public static final String PAY = "Страница оплаты кампании (pay)";

        public static final String PAY_FOR_ALL = "Выставление счета (payForAll)";
    }

    public static final String TRANSFER = "Перенос средств";

    public class Transfer {

        public static final String TRANSFER = "Страница переноса средств (transfer)";

        public static final String TRANSFER_DONE = "Перенос средств (transfer_done)";

    }

    public static final String SHOW_STAT_RESPONSE_CHECKER = "[для пака статистики] Статистика. Проверка кодов ответа.";

    public static final String CREATIVES = "Креативы";

    public class Creatives {

        public static final String SEARCH_CREATIVES =
                "Контроллер searchCreatives (поиск креативов)";

        public static final String TEMPLATE_NAME =
                "Контроллер searchCreatives (поиск креативов) - вывод названия шаблона";

        public static final String FILTERS =
                "Контроллер searchCreatives (поиск креативов) - фильтрация";

        public static final String CAMPAIGNS_LIST =
                "Контроллер searchCreatives (поиск креативов) - вывод списка привязанных кампаний";
    }

    public static final String FEEDS = "Фиды";

    public class Feeds {
        public static final String AJAX_GET_FEEDS = "Контроллер ajaxGetFeeds";
        public static final String SAVE_FEED =
                "Контроллер SaveFeed";
        public static final String AJAX_DELETE_FEEDS =
                "Контроллер ajaxDeleteFeeds";
        public static final String AJAX_GET_FEED_HISTORY =
                "Контроллер ajaxGetFeedHistory";
    }

    public static final String PERFORMANCE = "ДМО кампании";

    public class Performance {
        public static final String AJAX_EDIT_PERFORMANCE_FILTERS =
                "Контроллер ajaxEditPerformanceFilters";
    }


    public static final String COMMON = "Общяя логика";

    public class Common {

        public static final String CSRF =
                "Проверка работы csrf-токена";
        public static final String RETPATH =
                "Параметр retpath";
    }

    public static final String GROUPS = "Группы";

    public class Groups {
        public static final String SAVE_DYNAMIC_AD_GROUPS = "Сохранение динамической группы";
        public static final String BANNER_MULTI_SAVE = "Сохранение обычной группы";
        public static final String SAVE_PERFORMANCE_AD_GROUPS = "Сохранение перфоманс-группы";

        public static final String ADJUSTMENT_MOBILE = "Корректировка ставок для мобильных приложений";
        public static final String ADJUSTMENT_DEMOGRAPHY = "Корректировка ставок по полу и возрасту";
        public static final String ADJUSTMENT_RETARGERING = "Корректировка ставок для посетивших сайт";
        public static final String ADJUSTMENT_PERFORMANCE_TGO = "Корректировка ставок для Смарт-ТГО";

        public static final String FEEDS = "Фиды";

        public static final String RARELY_LOADED_FLAG = "Статус мало показов";

        public static final String MINUS_GEO = "Минус гео";

        public static final String STATUS_BS_SYNCED = "Статус синхронизации с БК (statusBsSynced)";
    }

    public static final String BANNER_IMAGES = "Картинки для баннеров";

    public class BannerImages {
        public static final String UPLOAD_BANNER_IMAGE = "Загрузка картинок для баннеров";
        public static final String RESIZE_BANNER_IMAGE = "Ресайз картинок для баннеров";
        public static final String SAVE_BANNER_WITH_IMAGE = "Сохранение баннера с картинкой";
        public static final String CREATE_BANNER_IMAGE_VIA_EXCEL = "Загрузка баннеров с картинками через excel";

        public static final String UPLOAD_IMAGE = "Загрузка картинок для ГО";
    }

    public static final String EXCEL = "Excel";

    public class Excel {
        public static final String EXCEL_UPLOAD = "Загрузка из экселя";
    }

    public static final String REPRESENTATIVE = "Представитель";
    public class Representative {
        public static final String DELETE_CL_REP = "удаление представителя";
    }

    public static final String STAT = "Stat";

    public class Stat {
        public static final String REPORT_OPEN = "Открытие отчета";
        public static final String REPORT_SAVE = "Сохранение отчета";
        public static final String REPORT_DELETE = "Удаление отчета";

        public static final String FILTER_OPEN = "Открытие фильтра";
        public static final String FILTER_SAVE = "Сохранение фильтра";
        public static final String FILTER_DELETE = "Удаление фильтра";
    }

    public static final String RIGHTS = "Проверка прав";

    public class Rights {
        public static final String SAVE_VCARD_RIGHTS = "Проверка прав редактирования визитки cmd=saveVCard";
    }

    public static final String SHOW_DIAG = "Причины отклонения";

    public class ShowDiag {
        public static final String GET_SHOW_DIAG = "Получение причин отклонения на картиночный баннер";
    }

    public static final String TAGS = "Тэги";

    public class Tags {
        public static final String SAVE_ADGROUP_TAGS = "Сохранение тэгов группы";
    }

    public static final String FORECAST = "Forecast";

    public class Forecast {
        public static final String NEW_BUDGET_FORECAST = "Новый прогнозатор для менеджеров";
        public static final String BUDGET_FORECAST = "Прогнозатор для менеджеров";
    }

    public static final String COUNTERS = "Counters";

    public class Counters {
        public static final String AJAX_CHECK_USER_COUNTERS = "проверка доступности счетчика";
        public static final String COUNTER_ECOMMERCE = "проверка ecommerce счетчика метрики";
    }

        public static final String WALLET = "Wallet";
    public class Wallet {
        public static final String AJAX_SAVE_AUTOPAY_SETTINGS = "сохранение информации об автоплатеже";
        public static final String AJAX_RESUME_AUTOPAY = "Возобновление автоплатежа";
        public static final String AJAX_GET_BINDING_FORM = "получение формы привязки карты";

        public static final String CLIENT_WALLET = "просмотр ОС";
        public static final String DISABLE_WALLET = "отключение ОС";
        public static final String AJAX_SAVE_WALLET_SETTINGS = "сохранение настроек ОС";
        public static final String DAY_BUDGET = "дневной бюджет";
        public static final String EDIT_CAMP = "ОС при редактировании кампании";

        public static final String AUTOPAY_SETTINGS = "получение информации об автоплатеже";
        public static final String SHOW_CAMP_EASY = "автоплатеж в легком интерфейсе";
        public static final String SHOW_CAMPS = "автоплатеж в проф интерфейсе";
    }

    public static final String AUTOBUDGET_ALERTS = "Уведомления по авто-стратегиям";

    public static final String CONDITIONS = "Условия нацеливания";

    public class Conditions {
        public static final String AJAX_EDIT_PERFORMANCE_FILTERS =
                "Контроллер ajaxEditPerformanceFilters";
        public static final String AJAX_EDIT_DYNAMIC_CONDITIONS = "тесты контроллера ajaxEditDynamicConditions";
        public static final String AJAX_UPDATE_SHOW_CONDITIONS = "тесты контроллера ajaxUpdateShowConditions";
        public static final String SHOW_CAMP_EASY = "автоплатеж в легком интерфейсе";
        public static final String SHOW_CAMPS = "автоплатеж в проф интерфейсе";
    }

    public static final String USER_OPTIONS = "Настройки пользователя";

    public class UserOptions {
        public static final String AJAX_USER_OPTIONS = "Контроллер ajaxUserOptions";
        public static final String AJAX_SET_RECOMEDATIONS_EMAIL = "Контроллер ajaxSetRecomendationsEmail";
    }

    public static final String PPC_CAMP_AUTO_PRICE = "офлайновый мастер цен";

    public class PpcCampAutoPrice {
        public static final String PPC_CAMP_AUTO_PRICE = "тесты скрипта ppcCampAutoPrice";
    }


    public static final String VCARDS= "Визитки";
    public class VCards {
        public static final String SAVE_VCARD = "Сохранение изменений визитки";
    }

    public static final String UPDATE_SHOW_CONDITIONS= "Обновление условий показа";
    public class UpdateShowConditions {
        public static final String AJAX_UPDATE_SHOW_CONDITIONS = "Изменение условий показа через ajaxUpdateShowConditions";
    }
}
