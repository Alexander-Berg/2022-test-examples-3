package ru.yandex.autotests.innerpochta.util;

/**
 * @author crafty
 */

public class ScriptConst {
    /*Отключаем самолётик в хоструте*/
    public static final String HIDE_PLANE_SCRIPT =
        "const hidePlane = () => {\n" +
            "    const plane = document.querySelector('.FlyingLetter-Container');\n" +
            "    if (!plane) {\n" +
            "        setTimeout(hidePlane, 100);\n" +
            "        return;\n" +
            "    }\n" +
            "    plane.style.display = 'none';\n" +
            "};\n" +
            "hidePlane();";

    /**
     * Отключаем смену людей в хоструте
     */
    public static final String DISABLE_PERSON_CHANGE_SCRIPT =
        "(temp = document.querySelector('.with-him')) && (temp.classList.remove('with-him'), temp.classList.add" +
            "('with-her'))";

    /**
     * Отключаем таймер, который переводит на DONE через 15 секунд
     */
    public static final String FREEZE_DONE_SCRIPT = "for (var i = 0; i < 10000000; i++) window.clearTimeout(i)";

    /**
     * Отключаем браузерный алерт композа, который спрашивает, точно ли мы хотим покинуть страницу
     */
    public static final String DISABLE_COMPOSE_SCRIPT = "$(window).off('beforeunload')";

    /**
     * Скроллим левую колонку
     */
    public static final String SCRIPT_FOR_SCROLLDOWN_LEFT_COLUMN = "$('.js-scroller-left').scrollTop(1000)";

    /**
     * Скроллим всю страницу вниз
     */
    public static final String SCROLL_PAGE_SCRIPT = "window.scrollBy(0,250)";

    public static final String SCROLL_PAGE_SCRIPT_MEDIUM = "window.scrollBy(0,400)";

    public static final String SCROLL_PAGE_SCRIPT_HIGH = "window.scrollBy(0,800)";

    public static final String SCROLL_PAGE_UP_SCRIPT_LIGHT = "window.scrollBy(0,-100)";

    /* Ускоряет jquery анимацию в 1000 раз */
    public static final String SCRIPT_SPEEDUP_JQUERY_ANIMATION =
        "setSpeed = function(){" +
            "if(typeof jQuery == 'function' && !jQuery.oldSpeed){" +
            "    jQuery.oldSpeed = jQuery.speed;" +
            "    jQuery.speed = function() {" +
            "        var opt = jQuery.oldSpeed.apply(this, arguments);" +
            "        opt.duration = Math.floor(opt.duration / 100);" +
            "        return(opt);}}};" +
            "setTimeout(setSpeed, 3000);";

    /* Полностью отключает transition- и animation- анимации в календаре */
    public static final String SCRIPT_DISABLE_ANIMATION_CAL =
        "if(!styleEl){" +
            "console.log('add new styleEl');" +
            "var styleEl = document.createElement('style');" +
            "styleEl.textContent = '*{animation-duration: 10ms !important; animation-delay: 0s !important;" +
            "transition-delay: 0s !important; transition-duration: 10ms !important}';" +
            "document.head.appendChild(styleEl);}";

    /* Полностью отключает transition- и animation- анимации в Лизе и Таче */
    public static final String SCRIPT_DISABLE_ANIMATION_LIZA_AND_TOUCH =
        "if(!styleEl){" +
            "console.log('add new styleEl');" +
            "var styleEl = document.createElement('style');" +
            "styleEl.textContent = '*{animation-duration: 10ms !important; animation-delay: 0s !important;" +
            "transition-delay: 0s !important; transition-duration: 0ms !important}';" +
            "document.head.appendChild(styleEl);}";

    /* Делает каретку прозначной чтобы скринка её не замечала */
    public static final String SCRIPT_REMOVE_CARET = "document.body.style.caretColor = 'transparent'";
    /**
     * Замораживаем страницу с задержкой в 3 секунды
     */
    public static final String FREEZE_PAGE_WITH_WAIT_SCRIPT = "setSpeed = function(){" +
        "for (var i = 0; i < 100000000; i++) window.clearTimeout(i);};" +
        "setTimeout(setSpeed, 3000);";

    /* Скроллим недельную сетку календаря к 10 утра */
    public static final String SCRIPT_SCROLL_CAL_TO_10AM =
        "if(document.querySelector('.qa-WeekGridColumn') != null) { " +
            "document.querySelector('div[class^=WeekGrid__times]').scrollTop = 560 }";

    public static final String DISABLE_ALERT_SCRIPT = "window.onbeforeunload = function() {};";

    public static final String CHECK_DOWNLOAD_STATE_SCRIPT = "return document.querySelector('downloads-manager').shadowRoot.getElementById('downloadsList').items[0].state;";

    public static final String GET_DOWNLOADED_FILEPATH_SCRIPT = "return document.querySelector('downloads-manager').shadowRoot.getElementById('downloadsList').items[0].filePath;";

    public static final String SCRIPT_SCROLL_3PANE_MESSAGELIST_DOWN =
        "var msgList = document.querySelector('.js-messages-scroll-area');" +
            "msgList.scrollBy(0, msgList.scrollHeight);";

    public static final String SCRIPT_RESIZE_COMPOSE = "$('.cke_contents').css('height', '250')";

    public static final String SCRIPT_FOR_SCROLLTOP = "$('.composeReact__scrollable-content')[0].scrollTop = 0;";

    /**
     * Определяем плотность пикселей девайса (координаты элементов всегда берутся из рассчета 1:1, но на мобильных
     * она обычно 2:1 или 3:1 из-за этого все координаты отдаваемые за пределы WebDriver надо умножать на полученный
     * pixelRatio
     */
    public static final String GET_DEVICE_PIXEL_RATIO =
        "var ratio = 1;\n" +
            "// To account for zoom, change to use deviceXDPI instead of systemXDPI\n" +
            "if (window.screen.systemXDPI !== undefined && window.screen.logicalXDPI!== undefined && window" +
            ".screen.systemXDPI > window.screen.logicalXDPI) {\n" +
            "    // Only allow for values > 1\n" +
            "    ratio = window.screen.systemXDPI / window.screen.logicalXDPI;\n" +
            "}\n" +
            "else if (window.devicePixelRatio !== undefined) {\n" +
            "    ratio = window.devicePixelRatio;\n" +
            "}\n" +
            "return ratio;";

    /**
     * Ставим фиксированный паддинг в шапке потому что он иногда съезжает на 2 пикселя только в автотестах
     */
    public static final String SET_HEADER_CONTACTS_PADDING =
        "if (document.getElementsByClassName('yandex-header__nav').length > 0) {\n" +
            "    document.getElementsByClassName('yandex-header__nav')[0].style.paddingLeft = '40px';\n" +
            "}";

    /**
     * Убираем залипающий QR в просмотре письма
     */
    public static final String REMOVE_FIXED_QR = "$('.mail-QuickReply').hide()";

    public static final String GET_CLIPBOARD_FROM_BROWSER = "async function getCBContents() { " +
        "try { " +
        "window.cb = await navigator.clipboard" +
        ".readText" +
        "(); console.log(\"Pasted content: \", window.cb); " +
        "} catch (err) { " +
        "console.error(\"Failed to read clipboard contents: \", err); window.cb = \"Error : \" + err; } " +
        "} " +
        "getCBContents();";

    public static final String SCRIPT_TO_COLLAPSE_WIDGETS = "var opened = false;\n";
    public static final String SCRIPT_TO_EXPAND_WIDGETS = "var opened = true;\n";
    public static final String SCRIPT_TO_UPDATE_WIDGETS =
        "fetch(\"https://cloud-api.yandex.ru/web_mail/v1/data/app/databases/.ext.ps@common/snapshot?collection_id=settings\", {\n" +
            "  \"referrer\": \"https://mail.yandex.ru/\",\n" +
            "  \"referrerPolicy\": \"origin\",\n" +
            "  \"body\": null,\n" +
            "  \"method\": \"GET\",\n" +
            "  \"mode\": \"cors\",\n" +
            "  \"credentials\": \"include\"\n" +
            "}).then(data => data.json()).then((data) => {\n" +
            "    const fieldRevision = data.records.items.find(item => item['record_id'] === 'widgetsExpanded');\n" +
            "    const revision = data.revision;\n" +
            "\n" +
            "    return fetch(\"https://cloud-api.yandex.ru/web_mail/v1/data/app/databases/.ext.ps@common/deltas\", {\n" +
            "      \"referrer\": \"https://mail.yandex.ru/\",\n" +
            "      \"referrerPolicy\": \"origin\",\n" +
            "      \"headers\": {\n" +
            "          \"If-Match\": revision,\n" +
            "          \"Content-Type\": \"application/json\"\n" +
            "      },\n" +
            "      \"body\": JSON.stringify({\n" +
            "        \"base_revision\": revision,\n" +
            "        \"delta_id\": `ya_cloud_data_js_api_1_0${Date.now()}`,\n" +
            "        \"changes\": [\n" +
            "            {\n" +
            "                \"record_id\": \"widgetsExpanded\",\n" +
            "                \"collection_id\": \"settings\",\n" +
            "                \"change_type\": \"update\",\n" +
            "                \"changes\": [\n" +
            "                    {\n" +
            "                        \"field_id\": \"value\",\n" +
            "                        \"change_type\": \"set\",\n" +
            "                        \"value\": {\n" +
            "                            \"type\": \"string\",\n" +
            "                            \"string\": `${opened},${Date.now()}`\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }),\n" +
            "      \"method\": \"POST\",\n" +
            "      \"mode\": \"cors\",\n" +
            "      \"credentials\": \"include\"\n" +
            "    });\n" +
            "}).then(() => console.log('Done. isOpened=' + opened));";
}
