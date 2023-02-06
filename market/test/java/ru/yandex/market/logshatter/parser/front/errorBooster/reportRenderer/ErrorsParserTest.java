package ru.yandex.market.logshatter.parser.front.errorBooster.reportRenderer;


import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.logshatter.sanitizer.FakeSanitizer;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Environment;
import ru.yandex.market.logshatter.parser.front.errorBooster.LogLevel;
import ru.yandex.market.logshatter.parser.front.errorBooster.Parser;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;
import ru.yandex.market.logshatter.parser.front.errorBooster.Runtime;

public class ErrorsParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new ErrorsParser(new FakeSanitizer()));
        checker.setLogBrokerTopic("report-renderrer-topic");
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWithoutTemplatePath() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
            "\"timestamp\":1551088049049,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"web\"," +
            "\"prj\":\"web-main-low\",\"ctype\":\"prestable\",\"geo\":\"vla\",\"provider\":\"RENDERER\"," +
            "\"workerId\":2,\"requestId\":\"1551088048670983-1137770479696364483293305-vla1-1192\"," +
            "\"level\":\"ERROR\",\"message\":\"TypeError: blocks.reviews-embed is not a function\\n    at Object" +
            ".blocks.entity-search__reviews " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:58129:44)\\n    at Object.wrappedFunc [as entity-search__reviews] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\\n    at " +
            "/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:57089:52\\n    at Array.map (<anonymous>)\\n    at Object.blocks.entity-search__card " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:57088:10)\\n    at Object.wrappedFunc [as entity-search__card] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\\n    at Object.blocks.entity-search " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:56129:42)\\n    at Object.bl " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:56246:27)\\n    at Object.wrappedFunc [as entity-search] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\\n    at Object.blocks.construct__block " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:46636:26)\",\"source\":\"TEMPLATE\"}\n";

        String originalStacktrace = "TypeError: blocks.reviews-embed is not a function\n" +
            "    at Object.blocks.entity-search__reviews " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:58129:44)\n" +
            "    at Object.wrappedFunc [as entity-search__reviews] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\n" +
            "    at /place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates" +
            "-bstr/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
            ".wrapped-server-templates.js:57089:52\n" +
            "    at Array.map (<anonymous>)\n" +
            "    at Object.blocks.entity-search__card " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:57088:10)\n" +
            "    at Object.wrappedFunc [as entity-search__card] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\n" +
            "    at Object.blocks.entity-search " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:56129:42)\n" +
            "    at Object.bl (/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC" +
            "/templates-bstr/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
            ".wrapped-server-templates.js:56246:27)\n" +
            "    at Object.wrappedFunc [as entity-search] " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\n" +
            "    at Object.blocks.construct__block " +
            "(/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
            "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:46636:26)";

        String stacktrace = "TypeError: blocks.reviews-embed is not a function\n" +
            "    at Object.blocks.entity-search__reviews " +
            "(/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates.js:58129:44)" +
            "\n" +
            "    at Object.wrappedFunc [as entity-search__reviews] " +
            "(/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates.js:32208:26)" +
            "\n" +
            "    at /templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:57089:52\n" +
            "    at Array.map (<anonymous>)\n" +
            "    at Object.blocks.entity-search__card " +
            "(/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:57088:10)\n" +
            "    at Object.wrappedFunc [as entity-search__card] " +
            "(/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\n" +
            "    at Object.blocks.entity-search (/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
            ".wrapped-server-templates.js:56129:42)\n" +
            "    at Object.bl (/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
            ".wrapped-server-templates.js:56246:27)\n" +
            "    at Object.wrappedFunc [as entity-search] " +
            "(/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
            ".js:32208:26)\n" +
            "    at Object.blocks.construct__block (/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
            ".wrapped-server-templates.js:46636:26)";


        String message = "TypeError: blocks.reviews-embed is not a function";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1551088049049L),
            "report-renderer-empty-template", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1551088048670983-1137770479696364483293305-vla1-1192", // REQUEST_ID
            UnsignedLong.valueOf("476275119303624107"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("geo", "metaprj", "prj"), // KV_KEYS
            Arrays.asList("vla", "web", "web-main-low"), // KV_VALUES]
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("13232338411311814403"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("727992040849483547"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Object.blocks.entity-search__reviews", "Object.wrappedFunc [as entity-search__reviews]",
            "(anonymous)", "Array.map", "Object.blocks.entity-search__card", "Object.wrappedFunc [as " +
             "entity-search__card]", "Object.blocks.entity-search", "Object.bl", "Object.wrappedFunc [as " +
              "entity-search]", "Object.blocks.construct__block"), // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC" +
             "/templates-bstr/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
              ".wrapped-server-templates.js", "/place/db/iss3/instances" +
                    "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr/1550869562/" +
                    "templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates.js",
                "/place/db" +
                    "/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
                    "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
                    ".wrapped-server-templates.js", "<anonymous>", "/place/db/iss3/instances" +
                    "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr/1550869562" +
                    "/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates.js",
                "/place/db/iss3/instances/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC" +
                    "/templates-bstr/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
                          ".wrapped-server-templates.js", "/place/db/iss3/instances" +
                    "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr/1550869562" +
                    "/templates-web4-lowload/pages-touch-phone/entity-view/entity-view.wrapped-server-templates" +
                    ".js", "/place/db/iss3/instances" +
                    "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr/1550869562" +
                    "/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
                    ".wrapped-server-templates.js", "/place/db/iss3/instances" +
                    "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
                    "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
                                 ".wrapped-server-templates.js", "/place/db/iss3/instances" +
                                  "/13240_production_report_renderer_vla_web_lowload_GjkxfVEZjqC/templates-bstr" +
                    "/1550869562/templates-web4-lowload/pages-touch-phone/entity-view/entity-view" +
                    ".wrapped-server-templates.js"), // STACK_TRACE_URLS
            Arrays.asList(58129, 32208, 57089, 0, 57088, 32208, 56129, 56246, 32208, 46636), // STACK_TRACE_LINES
            Arrays.asList(44, 26, 52, 0, 10, 26, 42, 27, 26, 26), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWithUnknownProject() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
            "\"timestamp\":1551088044492,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"video\"," +
            "\"prj\":\"video-ultra\",\"ctype\":\"prod\",\"geo\":\"vla\",\"provider\":\"RENDERER\",\"workerId\":3," +
            "\"requestId\":\"1551088044153587-265164120889213433081317-vla1-1575-V-TCH\"," +
            "\"templatePath\":\"video3_granny:phone\",\"level\":\"ERROR\",\"message\":\"JS Exception: Wrong field " +
            "type, napi_string expected, napi_undefined received. Field path: argument 0, params.url in " +
            "blocks['serp-list_type_search__items']()\\n\\nblocks['serp-list_type_search__items'] = function (data," +
            " items, current) {\\n    var isSearchPage = data.page === 'search';\\n    var isFavoritesPage = data" +
            ".page === 'favorites';\\n    var selectedId = current.selected || current.first || (items[0] && " +
            "items[0].videoid) || '';\\n\\n    return items.map(function(item, pos) {\\n        var itemBlock;" +
            "\\n\\n        // Судебная заглушка показывается только на 1-3 позиции на выдаче и Мои Видео\\n    " +
                   "    var needLegalPlug = item.BanDescription &&\\n            ((_.inRange(pos, 3) && isSearchPage)" +
                    " || isFavoritesPage);\\n\\n        // Тестовые судебные заглушки показываются только на 2ой " +
                     "позиции под тестовым флагом\\n        // на выдаче и Мои Видео\\n        var " +
                      "needTestingLegalPlug = data.expFlags.video_legal_plugs_test && pos === 1 &&\\n            " +
            "(isSearchPage || isFavoritesPage);\\n\\n        if (needLegalPlug || needTestingLegalPlug) " +
                        "{\\n            itemBlock = BEMPRIV.json({ block: 'prevention', mods: { type: 'judical' } }," +
                         " data, {\\n                instance: 'item',\\n                item: item\\n            });" +
            "\\n        } else {\\n            itemBlock = BEMPRIV.json({ block: 'serp-item', mods: { " +
                           "type: 'search' } }, { globalData: data, item, current });\\n        }\\n\\n        if " +
                            "(selectedId && selectedId === item.videoid && data.expFlags.video_prerender_pane) {\\n  " +
                             "          itemBlock.mods.selected = 'yes';\\n        }\\n\\n        return itemBlock;" +
            "\\n    });\\n}\\n\\nError: Wrong field type, napi_string expected, napi_undefined " +
                               "received. Field path: argument 0, params.url\\n    at Util.signUrl " +
            "(/place/db/iss3/instances" +
                                 "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/report-renderer/lib" +
                                  "/view/util.js:231:24)\\n    at Object.data.redirCounterUrl.data.redirCounterUrl " +
            "(/place/db/iss3/instances" +
                                    "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                                     "/video_touch_phone_granny/pages-touch/common/common.renderer.js:17078:30)\\n   " +
            " at Object.getDefaultParams " +
                                       "(/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                                         "/video_touch_phone_granny/pages-touch/common/common.renderer.js:26808:32)" +
            "\\n    at Object.result " +
                                           "(/place/db/iss3/instances" +
                                            "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates" +
                                             "/YxWeb/video_touch_phone_granny/pages-touch/common/common.renderer" +
            ".js:181:36)\\n    at Object.<anonymous> " +
                                               "(/place/db/iss3/instances" +
                                                "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD" +
            "/templates/YxWeb/video_touch_phone_granny/pages-touch/common/common" +
                                                  ".renderer.js:694:40)\\n    at Object.result [as getDefaultParams] " +
            "(/place/db/iss3/instances" +
                                                    "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD" +
            "/templates/YxWeb/video_touch_phone_granny/pages-touch/common" +
                                                      "/common.renderer.js:181:36)\\n    at Object.__constructor " +
            "(/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD" +
                                                         "/templates/YxWeb/video_touch_phone_granny/pages-touch" +
            "/common/common.renderer.js:378:35)\\n    at new " +
            "<anonymous> (/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb/video_touch_phone_gra" +
            "nny/pages-touch/common/common.renderer.js:227:43)\\n    at Function.create [as __base] (/place/d" +
            "b/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb/vide" +
            "o_touch_phone_granny/pages-touch/common/common.renderer.js:749:16)\\n    at Function.create (/pla" +
            "ce/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
            "/video_touch_phone_granny/pages-touch/common/common.renderer.js:860:25)\",\"source\":\"TEMPLATE\"}\n";

        String stacktrace = "JS Exception: Wrong field type, napi_string expected, napi_undefined received. Field " +
         "path: argument 0, params.url in blocks['serp-list_type_search__items']()\n" +
            "\n" +
            "blocks['serp-list_type_search__items'] = function (data, items, current) {\n" +
            "    var isSearchPage = data.page === 'search';\n" +
            "    var isFavoritesPage = data.page === 'favorites';\n" +
            "    var selectedId = current.selected || current.first || (items[0] && items[0].videoid) || '';\n" +
            "\n" +
            "    return items.map(function(item, pos) {\n" +
            "        var itemBlock;\n" +
            "\n" +
            "        // Судебная заглушка показывается только на 1-3 позиции на выдаче и Мои Видео\n" +
            "        var needLegalPlug = item.BanDescription &&\n" +
            "            ((_.inRange(pos, 3) && isSearchPage) || isFavoritesPage);\n" +
            "\n" +
            "        // Тестовые судебные заглушки показываются только на 2ой позиции под тестовым флагом\n" +
            "        // на выдаче и Мои Видео\n" +
            "        var needTestingLegalPlug = data.expFlags.video_legal_plugs_test && pos === 1 &&\n" +
            "            (isSearchPage || isFavoritesPage);\n" +
            "\n" +
            "        if (needLegalPlug || needTestingLegalPlug) {\n" +
            "            itemBlock = BEMPRIV.json({ block: 'prevention', mods: { type: 'judical' } }, data, {\n" +
            "                instance: 'item',\n" +
            "                item: item\n" +
            "            });\n" +
            "        } else {\n" +
            "            itemBlock = BEMPRIV.json({ block: 'serp-item', mods: { type: 'search' } }, { globalData: " +
 "data, item, current });\n" +
            "        }\n" +
            "\n" +
            "        if (selectedId && selectedId === item.videoid && data.expFlags.video_prerender_pane) {\n" +
            "            itemBlock.mods.selected = 'yes';\n" +
            "        }\n" +
            "\n" +
            "        return itemBlock;\n" +
            "    });\n" +
            "}\n" +
            "\n" +
            "Error: Wrong field type, napi_string expected, napi_undefined received. Field path: argument 0, params" +
             ".url\n" +
            "    at Util.signUrl (/report-renderer/lib/view/util.js:231:24)\n" +
            "    at Object.data.redirCounterUrl.data.redirCounterUrl " +
             "(/video_touch_phone_granny/pages-touch/common/common.renderer.js:17078:30)\n" +
            "    at Object.getDefaultParams (/video_touch_phone_granny/pages-touch/common/common.renderer" +
             ".js:26808:32)\n" +
            "    at Object.result (/video_touch_phone_granny/pages-touch/common/common.renderer.js:181:36)\n" +
            "    at Object.<anonymous> (/video_touch_phone_granny/pages-touch/common/common.renderer.js:694:40)\n" +
            "    at Object.result [as getDefaultParams] (/video_touch_phone_granny/pages-touch/common/common.renderer" +
             ".js:181:36)\n" +
            "    at Object.__constructor (/video_touch_phone_granny/pages-touch/common/common.renderer.js:378:35)\n" +
            "    at new <anonymous> (/video_touch_phone_granny/pages-touch/common/common.renderer.js:227:43)\n" +
            "    at Function.create [as __base] (/video_touch_phone_granny/pages-touch/common/common.renderer" +
             ".js:749:16)\n" +
            "    at Function.create (/video_touch_phone_granny/pages-touch/common/common.renderer.js:860:25)";
        String message = "JS Exception: Wrong field type, napi_string expected, napi_undefined received. Field path: " +
         "argument 0, params.url in blocks['serp-list_type_search__items']()";

        String originalStacktrace = "JS Exception: Wrong field type, napi_string expected, napi_undefined received. " +
            "Field path: argument 0, params.url in blocks['serp-list_type_search__items']()\n" +
            "\n" +
            "blocks['serp-list_type_search__items'] = function (data, items, current) {\n" +
            "    var isSearchPage = data.page === 'search';\n" +
            "    var isFavoritesPage = data.page === 'favorites';\n" +
            "    var selectedId = current.selected || current.first || (items[0] && items[0].videoid) || '';\n" +
            "\n" +
            "    return items.map(function(item, pos) {\n" +
            "        var itemBlock;\n" +
            "\n" +
            "        // Судебная заглушка показывается только на 1-3 позиции на выдаче и Мои Видео\n" +
            "        var needLegalPlug = item.BanDescription &&\n" +
            "            ((_.inRange(pos, 3) && isSearchPage) || isFavoritesPage);\n" +
            "\n" +
            "        // Тестовые судебные заглушки показываются только на 2ой позиции под тестовым флагом\n" +
            "        // на выдаче и Мои Видео\n" +
            "        var needTestingLegalPlug = data.expFlags.video_legal_plugs_test && pos === 1 &&\n" +
            "            (isSearchPage || isFavoritesPage);\n" +
            "\n" +
            "        if (needLegalPlug || needTestingLegalPlug) {\n" +
            "            itemBlock = BEMPRIV.json({ block: 'prevention', mods: { type: 'judical' } }, data, {\n" +
            "                instance: 'item',\n" +
            "                item: item\n" +
            "            });\n" +
            "        } else {\n" +
            "            itemBlock = BEMPRIV.json({ block: 'serp-item', mods: { type: 'search' } }, { globalData: " +
             "data, item, current });\n" +
            "        }\n" +
            "\n" +
            "        if (selectedId && selectedId === item.videoid && data.expFlags.video_prerender_pane) {\n" +
            "            itemBlock.mods.selected = 'yes';\n" +
            "        }\n" +
            "\n" +
            "        return itemBlock;\n" +
            "    });\n" +
            "}\n" +
            "\n" +
            "Error: Wrong field type, napi_string expected, napi_undefined received. Field path: argument 0, params" +
             ".url\n" +
            "    at Util.signUrl " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/report-renderer" +
              "/lib/view/util.js:231:24)\n" +
            "    at Object.data.redirCounterUrl.data.redirCounterUrl " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:17078:30)\n" +
            "    at Object.getDefaultParams " +
            "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:26808:32)\n" +
            "    at Object.result " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:181:36)\n" +
            "    at Object.<anonymous> " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:694:40)\n" +
            "    at Object.result [as getDefaultParams] " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:181:36)\n" +
            "    at Object.__constructor " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:378:35)\n" +
            "    at new <anonymous> " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
              "/video_touch_phone_granny/pages-touch/common/common.renderer.js:227:43)\n" +
            "    at Function.create [as __base] " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
        "/video_touch_phone_granny/pages-touch/common/common.renderer.js:749:16)\n" +
            "    at Function.create " +
        "(/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
"/video_touch_phone_granny/pages-touch/common/common.renderer.js:860:25)";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1551088044492L),
            "report-renderer-unknown-project", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1551088044153587-265164120889213433081317-vla1-1575-V-TCH", // REQUEST_ID
            UnsignedLong.valueOf("1136018510550647650"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("templatePath", "geo", "metaprj", "prj"), // KV_KEYS
            Arrays.asList("video3_granny:phone", "vla", "video", "video-ultra"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("9104744224041527620"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("10254288461221127781"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Util.signUrl", "Object.data.redirCounterUrl.data.redirCounterUrl", "Object" +
             ".getDefaultParams", "Object.result", "Object.<anonymous>", "Object.result [as getDefaultParams]",
             "Object.__constructor", "new <anonymous>", "Function.create [as __base]", "Function.create"), //
             // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD" +
             "/report-renderer/lib/view/util.js", "/place/db/iss3/instances" +
              "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb/video_touch_phone_granny" +
               "/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                 "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                    "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                     "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                      "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                       "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                        "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                         "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3/instances" +
                          "/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                           "/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3" +
 "/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
"/video_touch_phone_granny/pages-touch/common/common.renderer.js", "/place/db/iss3" +
                              "/instances/10240_production_video_report_renderer_vla_rkub_Cb51t4qAvkD/templates/YxWeb" +
                               "/video_touch_phone_granny/pages-touch/common/common.renderer.js"), // STACK_TRACE_URLS
            Arrays.asList(231, 17078, 26808, 181, 694, 181, 378, 227, 749, 860), // STACK_TRACE_LINES
            Arrays.asList(24, 30, 32, 36, 40, 36, 35, 43, 16, 25), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "vla", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseMultipleFields() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
         "\"timestamp\":1551087999636,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"imgs\"," +
            "\"prj\":\"imgs-quick\",\"ctype\":\"priemka\",\"geo\":\"man\",\"provider\":\"RENDERER\",\"workerId\":4," +
           "\"requestId\":\"1551087999567585-700389078176957520016857-man1-6359-IMG\"," +
            "\"templatePath\":\"images3_granny:desktop\",\"level\":\"ERROR\",\"message\":\"Нет перевода ключа " +
             "\\\"февраль\\\" кейсета months, не забудь синкнуть переводы перед вливанием!\"," +
              "\"source\":\"TEMPLATE\"}\n{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
               "\"timestamp\":1551087999636,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"imgs\"," +
                "\"prj\":\"imgs-quick\",\"ctype\":\"prod\",\"geo\":\"man\",\"provider\":\"RENDERER\",\"workerId\":4," +
                 "\"requestId\":\"1551087999567585-700389078176957520016857-man1-6359-IMG\"," +
                  "\"templatePath\":\"images3_granny:desktop\",\"level\":\"ERROR\",\"message\":\"Нет перевода ключа " +
                   "\\\"март\\\" кейсета months, не забудь синкнуть переводы перед вливанием!\"," +
                    "\"source\":\"TEMPLATE\"}\n";

        String message = "Нет перевода ключа \"февраль\" кейсета months, не забудь синкнуть переводы перед вливанием!";
        String message2 = "Нет перевода ключа \"март\" кейсета months, не забудь синкнуть переводы перед вливанием!";

        checker.setParam("projects", "{\"images3_granny:desktop\":\"images-granny\"}");
        checker.setParam("platforms", "{\"images3_granny:desktop\":\"desktop\"}");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        List<Date> expectedDateList = Arrays.asList(
            new Date(1551087999636L),
            new Date(1551087999636L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                "images-granny", // PROJECT
                "", // SERVICE
                "", // PAGE
                Platform.DESKTOP, // PLATFORM
                "", // URL
                hashOfEmptyString, // URL_ID
                "", // VHOST
                Environment.TESTING, // ENVIRONMENT
                Arrays.asList(), // TEST_IDS
                Arrays.asList(), // EXP_FLAGS
                "", // BROWSER_ENGINE
                "", // BROWSER_ENGINE_VERSION
                "", // BROWSER_NAME
                "", // BROWSER_VERSION
                "", // BROWSER_VERSION_MAJOR
                "", // BROWSER_BASE
                "", // OS_FAMILY
                "", // OS_NAME
                "", // OS_VERSION
                "", // OS_VERSION_MAJOR
                "", // DEVICE_NAME
                "", // DEVICE_VENDOR
                false, // IN_APP_BROWSER
                false, // IS_ROBOT
                false, // IS_TV
                false, // IS_TABLET
                false, // IS_TOUCH
                false, // IS_MOBILE
                false, // ADBLOCK
                "", // VERSION
                0, // REGION
                "1551087999567585-700389078176957520016857-man1-6359-IMG", // REQUEST_ID
                UnsignedLong.valueOf("5399468702051832446"), // REQUEST_ID_HASH
                UnsignedLong.valueOf(0), // YANDEXUID
                Arrays.asList("geo", "metaprj", "prj"), // KV_KEYS
                Arrays.asList("man", "imgs", "imgs-quick"), // KV_VALUES
                false, // IS_INTERNAL
                "", // CDN
                "", // IPv6
                false, // LOGGED_IN
                message, // MESSAGE
                UnsignedLong.valueOf("13677651482189520175"), // MESSAGE_ID
                message, // ORIGINAL_MESSAGE
                Arrays.asList(), // ERROR_URLS_KEYS
                Arrays.asList(), // ERROR_URLS_VALUES
                Runtime.NODEJS, // RUNTIME
                LogLevel.ERROR, // LEVEL
                "", // FILE
                hashOfEmptyString, // FILE_ID
                "", // BLOCK
                "", // METHOD
                0, // LINE
                0, // COL
                "", // STACK_TRACE
                hashOfEmptyString, // STACK_TRACE_ID
                "", // ORIGINAL_STACK_TRACE
                Arrays.asList(), // STACK_TRACE_NAMES
                Arrays.asList(), // STACK_TRACE_URLS
                Arrays.asList(), // STACK_TRACE_LINES
                Arrays.asList(), // STACK_TRACE_COLS
                "", // USER_AGENT
                hashOfEmptyString, // USER_AGENT_ID
                "", // SOURCE
                "", // SOURCE_METHOD
                "", // SOURCE_TYPE
                UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
                UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
                Arrays.asList(), // REPLACED_URLS_KEYS
                Arrays.asList(), // REPLACED_URLS_VALUES
                Parser.REPORT_RENDERER, // PARSER
                UnsignedLong.valueOf("0"), // ICOOKIE
                false, // SILENT
                "man", // DC
                0d, // COORDINATES_LATITUDE
                0d, // COORDINATES_LONGITUDE
                0, // COORDINATES_PRECISION
                0L, // COORDINATES_TIMESTAMP
                checker.getHost(), // HOST
                checker.getLogBrokerTopic() // TOPIC
            },
            new Object[]{
                "images-granny", // PROJECT
                "", // SERVICE
                "", // PAGE
                Platform.DESKTOP, // PLATFORM
                "", // URL
                hashOfEmptyString, // URL_ID
                "", // VHOST
                Environment.PRODUCTION, // ENVIRONMENT
                Arrays.asList(), // TEST_IDS
                Arrays.asList(), // EXP_FLAGS
                "", // BROWSER_ENGINE
                "", // BROWSER_ENGINE_VERSION
                "", // BROWSER_NAME
                "", // BROWSER_VERSION
                "", // BROWSER_VERSION_MAJOR
                "", // BROWSER_BASE
                "", // OS_FAMILY
                "", // OS_NAME
                "", // OS_VERSION
                "", // OS_VERSION_MAJOR
                "", // DEVICE_NAME
                "", // DEVICE_VENDOR
                false, // IN_APP_BROWSER
                false, // IS_ROBOT
                false, // IS_TV
                false, // IS_TABLET
                false, // IS_TOUCH
                false, // IS_MOBILE
                false, // ADBLOCK
                "", // VERSION
                0, // REGION
                "1551087999567585-700389078176957520016857-man1-6359-IMG", // REQUEST_ID
                UnsignedLong.valueOf("5399468702051832446"), // REQUEST_ID_HASH
                UnsignedLong.valueOf(0), // YANDEXUID
                Arrays.asList("geo", "metaprj", "prj"), // KV_KEYS
                Arrays.asList("man", "imgs", "imgs-quick"), // KV_VALUES
                false, // IS_INTERNAL
                "", // CDN
                "", // IPv6
                false, // LOGGED_IN
                message2, // MESSAGE
                UnsignedLong.valueOf("8652936610555652192"), // MESSAGE_ID
                message2, // ORIGINAL_MESSAGE
                Arrays.asList(), // ERROR_URLS_KEYS
                Arrays.asList(), // ERROR_URLS_VALUES
                Runtime.NODEJS, // RUNTIME
                LogLevel.ERROR, // LEVEL
                "", // FILE
                hashOfEmptyString, // FILE_ID
                "", // BLOCK
                "", // METHOD
                0, // LINE
                0, // COL
                "", // STACK_TRACE
                hashOfEmptyString, // STACK_TRACE_ID
                "", // ORIGINAL_STACK_TRACE
                Arrays.asList(), // STACK_TRACE_NAMES
                Arrays.asList(), // STACK_TRACE_URLS
                Arrays.asList(), // STACK_TRACE_LINES
                Arrays.asList(), // STACK_TRACE_COLS
                "", // USER_AGENT
                hashOfEmptyString, // USER_AGENT_ID
                "", // SOURCE
                "", // SOURCE_METHOD
                "", // SOURCE_TYPE
                UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
                UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
                Arrays.asList(), // REPLACED_URLS_KEYS
                Arrays.asList(), // REPLACED_URLS_VALUES
                Parser.REPORT_RENDERER, // PARSER
                UnsignedLong.valueOf("0"), // ICOOKIE
                false, // SILENT
                "man", // DC
                0d, // COORDINATES_LATITUDE
                0d, // COORDINATES_LONGITUDE
                0, // COORDINATES_PRECISION
                0L, // COORDINATES_TIMESTAMP
                checker.getHost(), // HOST
                checker.getLogBrokerTopic() // TOPIC
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWeb4Fields() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
         "\"timestamp\":1551088174867,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"web\"," +
          "\"prj\":\"web-main-rkub\",\"ctype\":\"hamster\",\"geo\":\"man\",\"provider\":\"RENDERER\",\"workerId\":6," +
            "\"requestId\":\"1551088174620799-377500364064086083253503-man1-3697\",\"templatePath\":\"web4:desktop\"," +
            "\"level\":\"ERROR\",\"message\":\"Error: not enough data for counter - require path\\n    at blocks" +
            ".counter (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
            "/1550868591/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js:154313:15)\\n    " +
               "at wrappedFunc (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
                "/templates-bstr/1550868591/templates-web4/pages-desktop/search/all/all.wrapped-server-templates" +
                 ".js:138645:26)\\n    at Object.attrs " +
                  "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
                   "/1550868591/templates-web4/.build/src/lib/counter/counter.js:27:34)\\n    at " +
                    "/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
                     "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop" +
            ".server.js:150:64\\n    at Array.map (<anonymous>)\\n    at AdapterImagesViewer.AdapterImages" +
                       ".getPath (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
                        "/templates-bstr/1550868591/templates-web4/" +
            ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:149:52)\\n   " +
                          " at AdapterImagesViewer.AdapterImages.prepareDefaultState " +
                           "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates" +
                            "-bstr/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images" +
                             "/Images@desktop.server.js:84:28)\\n    at AdapterImagesViewer.AdapterImages" +
                              ".prepareGalleryState " +
                               "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
                                "/templates-bstr/1550868591/templates-web4/" +
                                 ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
                                  ".js:67:34)\\n    at AdapterImagesViewer.AdapterImages.prepareIntentState " +
            "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
                                    "/templates-bstr/1550868591/templates-web4/" +
                                     ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
                                      ".js:71:25)\\n    at AdapterImagesViewer.AdapterImages.prepareInitialState " +
                                       "(/place/db/iss3/instances" +
        "/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
        "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features" +
            "/Images/Images@desktop.server.js:64:21)\",\"source\":\"TEMPLATE\"}\n";

        String stacktrace = "Error: not enough data for counter - require path\n" +
            "    at blocks.counter (/templates-web4/pages-desktop/search/all/all.wrapped-server-templates" +
            ".js:154313:15)\n" +
            "    at wrappedFunc (/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js:138645:26)" +
            "\n" +
            "    at Object.attrs (/templates-web4/.build/src/lib/counter/counter.js:27:34)\n" +
            "    at /templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
             ".js:150:64\n" +
            "    at Array.map (<anonymous>)\n" +
            "    at AdapterImagesViewer.AdapterImages.getPath (/templates-web4/" +
             ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:149:52)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareDefaultState (/templates-web4/" +
             ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:84:28)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareGalleryState (/templates-web4/" +
             ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:67:34)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareIntentState (/templates-web4/" +
        ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:71:25)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareInitialState (/templates-web4/" +
             ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js:64:21)";

        String originalStacktrace = "Error: not enough data for counter - require path\n" +
            "    at blocks.counter (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
             "/templates-bstr/1550868591/templates-web4/pages-desktop/search/all/all.wrapped-server-templates" +
              ".js:154313:15)\n" +
            "    at wrappedFunc (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
             "/templates-bstr/1550868591/templates-web4/pages-desktop/search/all/all.wrapped-server-templates" +
              ".js:138645:26)\n" +
            "    at Object.attrs (/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
             "/templates-bstr/1550868591/templates-web4/.build/src/lib/counter/counter.js:27:34)\n" +
            "    at /place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
             "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
              ".js:150:64\n" +
            "    at Array.map (<anonymous>)\n" +
            "    at AdapterImagesViewer.AdapterImages.getPath " +
             "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
              "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
               ".js:149:52)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareDefaultState " +
             "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
              "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
               ".js:84:28)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareGalleryState " +
            "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
              "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
               ".js:67:34)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareIntentState " +
        "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
              "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
               ".js:71:25)\n" +
            "    at AdapterImagesViewer.AdapterImages.prepareInitialState " +
             "(/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
        "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server" +
               ".js:64:21)";

        String message = "Error: not enough data for counter - require path";

        checker.setParam("projects", "{\"web4:desktop\":\"web4\"}");
        checker.setParam("platforms", "{\"web4:desktop\":\"desktop\"}");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1551088174867L),
            "web4", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.DESKTOP, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.DEVELOPMENT, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1551088174620799-377500364064086083253503-man1-3697", // REQUEST_ID
            UnsignedLong.valueOf("16128639132989791362"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("geo", "metaprj", "prj"), // KV_KEYS
            Arrays.asList("man", "web", "web-main-rkub"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("14135056187446622572"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("10491793801450200531"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("blocks.counter", "wrappedFunc", "Object.attrs", "(anonymous)", "Array.map",
            "AdapterImagesViewer.AdapterImages.getPath", "AdapterImagesViewer.AdapterImages.prepareDefaultState",
            "AdapterImagesViewer.AdapterImages.prepareGalleryState", "AdapterImagesViewer.AdapterImages" +
             ".prepareIntentState", "AdapterImagesViewer.AdapterImages.prepareInitialState"), // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates" +
             "-bstr/1550868591/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js", "/place/db" +
 "/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr/1550868591" +
               "/templates-web4/pages-desktop/search/all/all.wrapped-server-templates.js", "/place/db/iss3/instances" +
                    "/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr/1550868591/templates-web4/" +
                 ".build/src/lib/counter/counter.js", "/place/db/iss3/instances" +
                  "/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr/1550868591/templates-web4/" +
                   ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js", "<anonymous>",
                    "/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
                    "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop" +
                     ".server.js", "/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR" +
                      "/templates-bstr/1550868591/templates-web4/" +
                       ".build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js", "/place/db" +
                        "/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr" +
                         "/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images/Images" +
                          "@desktop.server.js", "/place/db/iss3/instances" +
                           "/10240_production_report_renderer_man_web_SMPgvuFihaR/templates-bstr/1550868591/templates" +
                            "-web4/.build/src/experiments/arch_images_r/features/Images/Images@desktop.server.js",
                            "/place/db/iss3/instances/10240_production_report_renderer_man_web_SMPgvuFihaR/templates" +
                             "-bstr/1550868591/templates-web4/.build/src/experiments/arch_images_r/features/Images" +
                              "/Images@desktop.server.js"), // STACK_TRACE_URLS
            Arrays.asList(154313, 138645, 27, 150, 0, 149, 84, 67, 71, 64), // STACK_TRACE_LINES
            Arrays.asList(15, 26, 34, 64, 0, 52, 28, 34, 25, 21), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "man", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void parseWithUrls() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
         "\"timestamp\":1551087922547,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"web\"," +
            "\"prj\":\"web-main-low\",\"ctype\":\"prestable\",\"geo\":\"sas\",\"provider\":\"RENDERER\"," +
            "\"workerId\":4," +
            "\"level\":\"ERROR\",\"message\":\"[TURBO-templates] at https://yandex" +
            ".ru/turbo?text=https%3A%2F%2Fcomfort-wear.ru%2Fproduct%2Frattlin-bass-vibe-93-cb-01-madness&parent-reqid" +
             "=1551087127830032-1710339866850995292647365-man1-3509-TCH&trbsrc=wb&fallback=1&event-id=jsk5sc5hp1" +
            "\\nMessage: отсутствуют данные при рендеринга. isAjax=false\",\"source\":\"RENDERER\"}\n";

        String stacktrace = "[TURBO-templates] at {{REPLACED_STACKTRACE_URL_0}}\n" +
            "Message: отсутствуют данные при рендеринга. isAjax=false";

        String originalStacktrace = "[TURBO-templates] at https://yandex.ru/turbo?text=https://comfort-wear" +
            ".ru/product/rattlin-bass-vibe-93-cb-01-madness&parent-reqid=1551087127830032-1710339866850995292647365" +
            "-man1" +
          "-3509-TCH&trbsrc=wb&fallback=1&event-id=jsk5sc5hp1\n" +
            "Message: отсутствуют данные при рендеринга. isAjax=false";

        String message = "[TURBO-templates] at {{REPLACED_MESSAGE_URL_0}}";
        String originalMessage = "[TURBO-templates] at https://yandex.ru/turbo?text=https://comfort-wear" +
         ".ru/product/rattlin-bass-vibe-93-cb-01-madness&parent-reqid=1551087127830032-1710339866850995292647365-man1" +
            "-3509-TCH&trbsrc=wb&fallback=1&event-id=jsk5sc5hp1";

        String url = "https://yandex.ru/turbo?text=https://comfort-wear" +
         ".ru/product/rattlin-bass-vibe-93-cb-01-madness&parent-reqid=1551087127830032-1710339866850995292647365-man1" +
          "-3509-TCH&trbsrc=wb&fallback=1&event-id=jsk5sc5hp1";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");
        checker.setParam("defaultProjectPrefix", "hamster-");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1551087922547L),
            "hamster-report-renderer-empty-template", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "", // REQUEST_ID
            hashOfEmptyString, // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("geo", "metaprj", "prj"), // KV_KEYS
            Arrays.asList("sas", "web", "web-main-low"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("7527926498633642413"), // MESSAGE_ID
            originalMessage, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("2202906307356721367"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList(), // STACK_TRACE_NAMES
            Arrays.asList(), // STACK_TRACE_URLS
            Arrays.asList(), // STACK_TRACE_LINES
            Arrays.asList(), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList("MESSAGE_0", "STACKTRACE_0"), // REPLACED_URLS_KEYS
            Arrays.asList(url, url), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "sas", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWithNewLines() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
         "\"timestamp\":1551088218362,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"video\"," +
          "\"prj\":\"video-ultra\",\"ctype\":\"prod\",\"geo\":\"man\",\"provider\":\"RENDERER\",\"workerId\":3," +
           "\"requestId\":\"1551088218195810-1024692830748931012608763-man1-1455-V\"," +
            "\"templatePath\":\"video3_granny:desktop\",\"level\":\"ERROR\"," +
             "\"message\":\"\\n\\n==============\\nJavaScript error: in\\nblocks[\\\"content_type_index\\\"]\\nCannot" +
            " read property 'hasOwnProperty' of null\\nTypeError: Cannot read property 'hasOwnProperty' of null\\n " +
            "   at Function.prop " +
                "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates" +
            "/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:17:19940)\\n    at Object.getBlockData" +
                  " (/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates" +
                   "/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:20:27494)\\n    at Object.hasItems " +
                    "[as __base] (/place/db/iss3/instances" +
                     "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny" +
 "/pages-desktop/common/common.renderer.js:20:25829)\\n    at Object.<anonymous> " +
                       "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR" +
            "/templates/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\\n    at " +
                         "Object.n.(anonymous function) [as hasItems] " +
                          "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR" +
                           "/templates/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:13:25798)\\n    at" +
            " Object.init (/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
            "/video3_granny/pages-desktop/common/common.renderer.js:20:25286)\\n    at Object" +
            ".<anonymous> (/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
                                 "/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\\n    at Object.n." +
            "(anonymous function) [as init] " +
            "(/place/db/iss3/instances" +
            "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
            "/video3_granny/pages-desktop/common/common.renderer.js:13:25798)\\n    at " +
            "Object.__constructor " +
                                       "(/place/db/iss3/instances" +
                                        "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
                                         "/video3_granny/pages-desktop/common/common.renderer.js:13:27629)\\n    at " +
            "new <anonymous> " +
                                           "(/place/db/iss3/instances" +
                                            "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates" +
            "/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:13:26177)" +
            "\\n==============\\n\\n\",\"source\":\"TEMPLATE\"}";

        String stacktrace = "==============\n" +
            "JavaScript error: in\n" +
            "blocks[\"content_type_index\"]\n" +
            "Cannot read property 'hasOwnProperty' of null\n" +
            "TypeError: Cannot read property 'hasOwnProperty' of null\n" +
            "    at Function.prop (/video3_granny/pages-desktop/common/common.renderer.js:17:19940)\n" +
            "    at Object.getBlockData (/video3_granny/pages-desktop/common/common.renderer.js:20:27494)\n" +
            "    at Object.hasItems [as __base] (/video3_granny/pages-desktop/common/common.renderer.js:20:25829)\n" +
            "    at Object.<anonymous> (/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\n" +
            "    at Object.n.(anonymous function) [as hasItems] (/video3_granny/pages-desktop/common/common.renderer" +
             ".js:13:25798)\n" +
            "    at Object.init (/video3_granny/pages-desktop/common/common.renderer.js:20:25286)\n" +
            "    at Object.<anonymous> (/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\n" +
            "    at Object.n.(anonymous function) [as init] (/video3_granny/pages-desktop/common/common.renderer" +
             ".js:13:25798)\n" +
            "    at Object.__constructor (/video3_granny/pages-desktop/common/common.renderer.js:13:27629)\n" +
            "    at new <anonymous> (/video3_granny/pages-desktop/common/common.renderer.js:13:26177)\n" +
            "==============";

        String originalStacktrace = "==============\n" +
            "JavaScript error: in\n" +
            "blocks[\"content_type_index\"]\n" +
            "Cannot read property 'hasOwnProperty' of null\n" +
            "TypeError: Cannot read property 'hasOwnProperty' of null\n" +
            "    at Function.prop " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:17:19940)\n" +
            "    at Object.getBlockData " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:20:27494)\n" +
            "    at Object.hasItems [as __base] " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:20:25829)\n" +
            "    at Object.<anonymous> " +
            "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\n" +
            "    at Object.n.(anonymous function) [as hasItems] " +
            "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:13:25798)\n" +
            "    at Object.init (/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR" +
             "/templates/YxWeb/video3_granny/pages-desktop/common/common.renderer.js:20:25286)\n" +
            "    at Object.<anonymous> " +
            "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:13:29731)\n" +
            "    at Object.n.(anonymous function) [as init] " +
"(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:13:25798)\n" +
            "    at Object.__constructor " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
              "/video3_granny/pages-desktop/common/common.renderer.js:13:27629)\n" +
            "    at new <anonymous> " +
             "(/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
"/video3_granny/pages-desktop/common/common.renderer.js:13:26177)\n" +
            "==============";

        String message = "==============";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");
        checker.setParam("defaultProjectPrefix", "hamster-");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1551088218362L),
            "hamster-report-renderer-unknown-project", // PROJECT
            "", // SERVICE
            "", // PAGE
            Platform.UNKNOWN, // PLATFORM
            "", // URL
            hashOfEmptyString, // URL_ID
            "", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "", // BROWSER_ENGINE
            "", // BROWSER_ENGINE_VERSION
            "", // BROWSER_NAME
            "", // BROWSER_VERSION
            "", // BROWSER_VERSION_MAJOR
            "", // BROWSER_BASE
            "", // OS_FAMILY
            "", // OS_NAME
            "", // OS_VERSION
            "", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            false, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "", // VERSION
            0, // REGION
            "1551088218195810-1024692830748931012608763-man1-1455-V", // REQUEST_ID
            UnsignedLong.valueOf("15625141594096420243"), // REQUEST_ID_HASH
            UnsignedLong.valueOf(0), // YANDEXUID
            Arrays.asList("templatePath", "geo", "metaprj", "prj"), // KV_KEYS
            Arrays.asList("video3_granny:desktop", "man", "video", "video-ultra"), // KV_VALUES
            false, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("17562713940378794569"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "", // FILE
            hashOfEmptyString, // FILE_ID
            "", // BLOCK
            "", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("13705578281256903278"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Function.prop", "Object.getBlockData", "Object.hasItems [as __base]",
                "Object.<anonymous>", "Object.n.(anonymous function) [as hasItems]", "Object.init",
                "Object.<anonymous>", "Object.n." +
             "(anonymous function) [as init]", "Object.__constructor", "new <anonymous>"), // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/10240_production_video_report_renderer_man_rkub_n9D512dxMpR" +
             "/templates/YxWeb/video3_granny/pages-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
              "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny/pages" +
               "-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny/pages" +
 "-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                  "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny/pages" +
                   "-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                    "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny/pages" +
                     "-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                      "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny" +
                       "/pages-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                        "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny" +
                         "/pages-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                          "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb/video3_granny" +
                           "/pages-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
                            "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
                             "/video3_granny/pages-desktop/common/common.renderer.js", "/place/db/iss3/instances" +
 "/10240_production_video_report_renderer_man_rkub_n9D512dxMpR/templates/YxWeb" +
    "/video3_granny/pages-desktop/common/common.renderer.js"), // STACK_TRACE_URLS
            Arrays.asList(17, 20, 20, 13, 13, 20, 13, 13, 13, 13), // STACK_TRACE_LINES
            Arrays.asList(19940, 27494, 25829, 29731, 25798, 25286, 29731, 25798, 27629, 26177), // STACK_TRACE_COLS
            "", // USER_AGENT
            hashOfEmptyString, // USER_AGENT_ID
            "", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "man", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWithMeta() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":3," +
         "\"timestamp\":1562271170754,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"news\"," +
          "\"prj\":\"news-renderer\",\"ctype\":\"testing\",\"geo\":\"sas\",\"provider\":\"RENDERER\",\"workerId\":1," +
            "\"requestId\":\"1562271169677384-1759256397015165760300040-man1-7620-all-priemka-stable-news--cf1-9770" +
            "-NEWS_STORY\",\"templatePath\":\"story.desktop\",\"level\":\"ERROR\",\"message\":\"TypeError: Cannot " +
             "read property 'total' of undefined\\n    at Object.blocks.prepare_data_apphost-story-counts " +
              "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
               "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18659:43)\\n    at Object.blocks " +
                "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
                 "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at tools" +
                  ".get.forEach.story " +
                   "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
            "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35842:30)\\n    at Array" +
                     ".forEach (<anonymous>)\\n    at Object.bl " +
                      "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
                       "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\\n    at " +
            "Object.<anonymous> " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
                          "/templates/desktop/tools/experimentarium.js:40:33)\\n    at Object.blocks " +
                           "(/place/db/iss3/instances" +
                            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
            "/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at Object.blocks" +
                              ".prepare_data_apphost-story " +
                               "(/place/db/iss3/instances" +
                                "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                                 "/projects/story/.bundles/desktop/_desktop.renderer.js:18473:17)\\n    at Object" +
                                  ".blocks (/place/db/iss3/instances" +
            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
                                    "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at" +
            " newsd.forEach.item " +
                                      "(/place/db/iss3/instances" +
                                       "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
                                        "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18354:21)\"," +
            "\"source\":\"TEMPLATE\",\"meta\":[{\"name\":\"yandexuid\"," +
            "\"value\":\"9863946471548151861\"},{\"name\":\"isSuspectedRobot\"," +
                                           "\"value\":\"1\"},{\"name\":\"isInternalRequest\",\"value\":\"true\"}," +
                                            "{\"name\":\"page\",\"value\":\"story\"},{\"name\":\"page\"," +
                                             "\"value\":\"updatedValue\"},{\"name\":\"service\",\"value\":\"sport\"}," +
            "{\"name\":\"region\",\"value\":\"10716\"},{\"name\":\"loggedin\"," +
                                               "\"value\":\"true\"},{\"name\":\"env\",\"value\":\"production\"}," +
                                                "{\"name\":\"block\",\"value\":\"block_WITH_VERY_VERY_SECRET_\"}," +
                                                 "{\"name\":\"method\",\"value\":\"method\"},{\"name\":\"source\"," +
                                                  "\"value\":\"source\"},{\"name\":\"file\",\"value\":\"file\"}," +
            "{\"name\":\"source\",\"value\":\"source\"},{\"name\":\"source\"," +
            "\"value\":\"source\"},{\"name\":\"version\"," +
            "\"value\":\"0x857dfed86\"},{\"name\":\"platform\"," +
            "\"value\":\"desktop\"},{\"name\":\"ip\",\"value\":\"192.168.1" +
            ".1\"},{\"name\":\"project\",\"value\":\"news\"}," +
                                                        "{\"name\":\"url\",\"value\":\"https://news.stable.priemka" +
            ".yandex.ru/yandsearch?cl4url" +
                                                          "=3d576a43465c69f4acd5347a231afee7&title" +
                                                           "=Glava_Minzdrava_poprosila_francuzskij_Falcon_vmesto_SSJ" +
                                                            "-100&lr=213&lang=ru&stid=5gGz7CvLdjxmIjvDn1I0" +
            "&persistent_id=67709826&rubric=index&from=index&flags" +
            "=&flags=yxnews_nerpa_story__extended=1\"}," +
            "{\"name\":\"ua\",\"value\":\"Mozilla/5.0 (Macintosh; " +
                                                                "Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, " +
            "like Gecko) Chrome/73.0.3683.86 YaBrowser/19.4.0" +
                                                                  ".2400 Yowser/2.5 Safari/537.36\"}," +
                                                                   "{\"name\":\"slots\",\"value\":\"1111233,0,44;" +
            "144891,0,31\"},{\"name\":\"additional\"," +
            "\"value\":\"{\\n    \\\"block\\\": " +
                                                                      "\\\"autoload\\\",\\n    \\\"outside\\\": " +
 "\\\"true\\\",\\n    \\\"unwrap\\\": true,\\n " +
                                                                        "   \\\"urls\\\": [\\n        " +
                                                                         "\\\"/mirror?page=1\\\"\\n    ],\\n    " +
                                                                          "\\\"mix\\\": {\\n        \\\"block\\\": " +
                                                                           "\\\"related\\\"\\n    },\\n    " +
                                                                            "\\\"type\\\": \\\"related\\\",\\n    " +
                                                                             "\\\"urlReplacement\\\": false\\n}\"}]}";

        String stacktrace = "TypeError: Cannot read property 'total' of undefined\n" +
            "    at Object.blocks.prepare_data_apphost-story-counts (/templates/desktop/projects/story/" +
             ".bundles/desktop/_desktop.renderer.js:18659:43)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
             "\n" +
            "    at tools.get.forEach.story (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer" +
             ".js:35842:30)\n" +
            "    at Array.forEach (<anonymous>)\n" +
            "    at Object.bl (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\n" +
            "    at Object.<anonymous> (/templates/desktop/tools/experimentarium.js:40:33)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
 "\n" +
            "    at Object.blocks.prepare_data_apphost-story (/templates/desktop/projects/story/" +
             ".bundles/desktop/_desktop.renderer.js:18473:17)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
             "\n" +
            "    at newsd.forEach.item (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer" +
            ".js:18354:21)";
        String message = "TypeError: Cannot read property 'total' of undefined";

        String originalStacktrace = "TypeError: Cannot read property 'total' of undefined\n" +
            "    at Object.blocks.prepare_data_apphost-story-counts " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18659:43)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at tools.get.forEach.story " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35842:30)\n" +
            "    at Array.forEach (<anonymous>)\n" +
            "    at Object.bl " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\n" +
            "    at Object.<anonymous> " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/tools/experimentarium.js:40:33)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at Object.blocks.prepare_data_apphost-story " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18473:17)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at newsd.forEach.item " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18354:21)";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");
        checker.setParam("allowedProjects", "[\"station\"]");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1562271170492L),
            "report-renderer-disallowed-project", // PROJECT
            "sport", // SERVICE
            "updatedValue", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://news.stable.priemka.yandex.ru/yandsearch?cl4url=3d576a43465c69f4acd5347a231afee7&title" +
             "=Glava_Minzdrava_poprosila_francuzskij_Falcon_vmesto_SSJ-100&lr=213&lang=ru&stid=5gGz7CvLdjxmIjvDn1I0" +
              "&persistent_id=67709826&rubric=index&from=index&flags=&flags=yxnews_nerpa_story__extended=1", // URL
            UnsignedLong.valueOf("3299269991927339721"), // URL_ID
            "news.stable.priemka.yandex.ru", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(144891, 1111233), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "0x857dfed86", // VERSION
            10716, // REGION
            "1562271169677384-1759256397015165760300040-man1-7620-all-priemka-stable-news--cf1-9770-NEWS_STORY", //
            // REQUEST_ID
            UnsignedLong.valueOf("6319700499158728897"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("9863946471548151861"), // YANDEXUID
            Arrays.asList("project", "geo", "metaprj", "prj", "block", "outside", "unwrap", "urls", "mix", "type",
            "urlReplacement"), // KV_KEYS
            Arrays.asList("news", "sas", "news", "news-renderer", "autoload", "true", "true", "[\"/mirror?page=1\"]",
                "{\"block\":\"related\"}", "related", "false"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "::ffff:192.168.1.1", // IPv6
            true, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("7667834918382635183"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.ERROR, // LEVEL
            "file", // FILE
            hashOfEmptyString, // FILE_ID
            "block_WITH_VERY_VERY_SECRET_", // BLOCK
            "method", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("14320079667909212422"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Object.blocks.prepare_data_apphost-story-counts", "Object.blocks", "tools.get.forEach" +
             ".story", "Array.forEach", "Object.bl", "Object.<anonymous>", "Object.blocks", "Object.blocks" +
              ".prepare_data_apphost-story", "Object.blocks", "newsd.forEach.item"), // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
             "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects/story/" +
               ".bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects/story/" +
                 ".bundles/desktop/_desktop.renderer.js", "<anonymous>", "/place/db/iss3/instances" +
                  "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects" +
                   "/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                    "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/tools" +
    "/experimentarium.js", "/place/db/iss3/instances" +
        "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects" +
                       "/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                        "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
 "/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                           "/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                             "/projects/story/.bundles/desktop/_desktop.renderer.js"), // STACK_TRACE_URLS
            Arrays.asList(18659, 14292, 35842, 0, 35839, 40, 14292, 18473, 14292, 18354), // STACK_TRACE_LINES
            Arrays.asList(43, 29, 30, 0, 58, 33, 29, 17, 29, 21), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683" +
             ".86 YaBrowser/19.4.0.2400 Yowser/2.5 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("8983658141965199254"), // USER_AGENT_ID
            "source", // SOURCE
            "", // SOURCE_METHOD
            "", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "sas", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseWithMeta2() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":3," +
         "\"timestamp\":1562271170754,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"news\"," +
            "\"prj\":\"news-renderer\",\"ctype\":\"testing\",\"geo\":\"sas\",\"provider\":\"RENDERER\"," +
            "\"workerId\":1," +
           "\"requestId\":\"1562271169677384-1759256397015165760300040-man1-7620-all-priemka-stable-news--cf1-9770" +
            "-NEWS_STORY\",\"templatePath\":\"story.desktop\",\"level\":\"ERROR\",\"message\":\"TypeError: Cannot " +
             "read property 'total' of undefined\\n    at Object.blocks.prepare_data_apphost-story-counts " +
              "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18659:43)\\n    at Object.blocks " +
                "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
            "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at tools" +
                  ".get.forEach.story " +
                   "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
            "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35842:30)\\n    at Array" +
            ".forEach (<anonymous>)\\n    at Object.bl " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
                       "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\\n    at " +
                        "Object.<anonymous> " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
                          "/templates/desktop/tools/experimentarium.js:40:33)\\n    at Object.blocks " +
                           "(/place/db/iss3/instances" +
            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                             "/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at Object.blocks" +
                              ".prepare_data_apphost-story " +
                               "(/place/db/iss3/instances" +
                                "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
            "/projects/story/.bundles/desktop/_desktop.renderer.js:18473:17)\\n    at Object" +
                                  ".blocks (/place/db/iss3/instances" +
                                   "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
                                    "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\\n    at" +
                                     " newsd.forEach.item " +
                                      "(/place/db/iss3/instances" +
            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18354:21)\"," +
                                         "\"source\":\"TEMPLATE\",\"meta\":[{\"name\":\"yandexuid\"," +
                                          "\"value\":\"9863946471548151861\"},{\"name\":\"isSuspectedRobot\"," +
            "\"value\":\"1\"},{\"name\":\"isInternalRequest\",\"value\":\"true\"}," +
            "{\"name\":\"page\",\"value\":\"story\"},{\"name\":\"page\"," +
            "\"value\":\"updatedValue\"},{\"name\":\"service\",\"value\":\"sport\"}," +
            "{\"name\":\"region\",\"value\":\"10716\"},{\"name\":\"env\"," +
 "\"value\":\"production\"},{\"name\":\"block\"," +
            "\"value\":\"block_WITH_VERY_VERY_SECRET_\"},{\"name\":\"method\"," +
            "\"value\":\"method\"},{\"name\":\"source\",\"value\":\"source\"}," +
                                                  "{\"name\":\"file\",\"value\":\"file\"},{\"name\":\"source\"," +
 "\"value\":\"source\"},{\"name\":\"source\",\"value\":\"source\"}," +
                                                    "{\"name\":\"version\",\"value\":\"0x857dfed86\"}," +
            "{\"name\":\"level\",\"value\":\"fatal\"},{\"name\":\"type\"," +
                                                      "\"value\":\"type\"},{\"name\":\"platform\"," +
                                                       "\"value\":\"desktop\"},{\"name\":\"project\"," +
                                                        "\"value\":\"station\"},{\"name\":\"url\"," +
                                                         "\"value\":\"https://news.stable.priemka.yandex" +
            ".ru/yandsearch?cl4url=3d576a43465c69f4acd5347a231afee7" +
            "&title" +
                                                            "=Glava_Minzdrava_poprosila_francuzskij_Falcon_vmesto_SSJ" +
            "-100&lr=213&lang=ru&stid=5gGz7CvLdjxmIjvDn1I0" +
            "&persistent_id=67709826&rubric=index&from=index&flags" +
                                                               "=&flags=yxnews_nerpa_story__extended=1\"}," +
                                                                "{\"name\":\"ua\",\"value\":\"Mozilla/5.0 (Macintosh;" +
                                                                 " Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML," +
                                                                  " like Gecko) Chrome/73.0.3683.86 YaBrowser/19.4.0" +
                                                                   ".2400 Yowser/2.5 Safari/537.36\"}," +
                                                                    "{\"name\":\"slots\",\"value\":\"1111233,0,44;" +
                                                                     "144891,0,31\"},{\"name\":\"additional\"," +
                                                                      "\"value\":\"{\\n    \\\"block\\\": " +
                                                                       "\\\"autoload\\\",\\n    \\\"outside\\\": " +
                                                                        "\\\"true\\\",\\n    \\\"unwrap\\\": true,\\n" +
                                                                         "    \\\"urls\\\": [\\n        " +
                                                                          "\\\"/mirror?page=1\\\"\\n    ],\\n    " +
            "\\\"mix\\\": {\\n        \\\"block\\\": " +
                                                                            "\\\"related\\\"\\n    },\\n    " +
                                                                             "\\\"type\\\": \\\"related\\\",\\n    " +
                                                                              "\\\"urlReplacement\\\": false\\n}\"}]}";

        String stacktrace = "TypeError: Cannot read property 'total' of undefined\n" +
            "    at Object.blocks.prepare_data_apphost-story-counts (/templates/desktop/projects/story/" +
             ".bundles/desktop/_desktop.renderer.js:18659:43)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
             "\n" +
            "    at tools.get.forEach.story (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer" +
            ".js:35842:30)\n" +
            "    at Array.forEach (<anonymous>)\n" +
            "    at Object.bl (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\n" +
            "    at Object.<anonymous> (/templates/desktop/tools/experimentarium.js:40:33)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
             "\n" +
            "    at Object.blocks.prepare_data_apphost-story (/templates/desktop/projects/story/" +
             ".bundles/desktop/_desktop.renderer.js:18473:17)\n" +
            "    at Object.blocks (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)" +
             "\n" +
            "    at newsd.forEach.item (/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer" +
            ".js:18354:21)";
        String message = "TypeError: Cannot read property 'total' of undefined";

        String originalStacktrace = "TypeError: Cannot read property 'total' of undefined\n" +
            "    at Object.blocks.prepare_data_apphost-story-counts " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18659:43)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at tools.get.forEach.story " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
            "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35842:30)\n" +
            "    at Array.forEach (<anonymous>)\n" +
            "    at Object.bl " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:35839:58)\n" +
            "    at Object.<anonymous> " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/tools/experimentarium.js:40:33)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
"/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at Object.blocks.prepare_data_apphost-story " +
            "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18473:17)\n" +
            "    at Object.blocks " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:14292:29)\n" +
            "    at newsd.forEach.item " +
             "(/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates" +
              "/desktop/projects/story/.bundles/desktop/_desktop.renderer.js:18354:21)";

        checker.setParam("projects", "{}");
        checker.setParam("platforms", "{}");
        checker.setParam("allowedProjects", "[\"station\"]");
        checker.setParam("sanitizer", "true");

        UnsignedLong hashOfEmptyString = UnsignedLong.valueOf("2202906307356721367");

        checker.check(
            line,
            new Date(1562271170492L),
            "station", // PROJECT
            "sport", // SERVICE
            "updatedValue", // PAGE
            Platform.DESKTOP, // PLATFORM
            "https://news.stable.priemka.yandex.ru/yandsearch?cl4url=3d576a43465c69f4acd5347a231afee7&title" +
             "=Glava_Minzdrava_poprosila_francuzskij_Falcon_vmesto_SSJ-100&lr=213&lang=ru&stid=5gGz7CvLdjxmIjvDn1I0" +
                "&persistent_id=67709826&rubric=index&from=index&flags=&flags=yxnews_nerpa_story__extended=1", // URL
            UnsignedLong.valueOf("3299269991927339721"), // URL_ID
            "news.stable.priemka.yandex.ru", // VHOST
            Environment.PRODUCTION, // ENVIRONMENT
            Arrays.asList(144891, 1111233), // TEST_IDS
            Arrays.asList(), // EXP_FLAGS
            "WebKit", // BROWSER_ENGINE
            "537.36", // BROWSER_ENGINE_VERSION
            "YandexBrowser", // BROWSER_NAME
            "18.9.0.3363", // BROWSER_VERSION
            "18.9", // BROWSER_VERSION_MAJOR
            "Chromium", // BROWSER_BASE
            "MacOS", // OS_FAMILY
            "Mac OS X Sierra", // OS_NAME
            "10.12.6", // OS_VERSION
            "10.12", // OS_VERSION_MAJOR
            "", // DEVICE_NAME
            "", // DEVICE_VENDOR
            false, // IN_APP_BROWSER
            true, // IS_ROBOT
            false, // IS_TV
            false, // IS_TABLET
            false, // IS_TOUCH
            false, // IS_MOBILE
            false, // ADBLOCK
            "0x857dfed86", // VERSION
            10716, // REGION
            "1562271169677384-1759256397015165760300040-man1-7620-all-priemka-stable-news--cf1-9770-NEWS_STORY", //
            // REQUEST_ID
            UnsignedLong.valueOf("6319700499158728897"), // REQUEST_ID_HASH
            UnsignedLong.valueOf("9863946471548151861"), // YANDEXUID
            Arrays.asList("geo", "metaprj", "prj", "block", "outside", "unwrap", "urls", "mix", "type",
            "urlReplacement"), // KV_KEYS
            Arrays.asList("sas", "news", "news-renderer", "autoload", "true", "true", "[\"/mirror?page=1\"]",
                "{\"block\":\"related\"}", "related", "false"), // KV_VALUES
            true, // IS_INTERNAL
            "", // CDN
            "", // IPv6
            false, // LOGGED_IN
            message, // MESSAGE
            UnsignedLong.valueOf("7667834918382635183"), // MESSAGE_ID
            message, // ORIGINAL_MESSAGE
            Arrays.asList(), // ERROR_URLS_KEYS
            Arrays.asList(), // ERROR_URLS_VALUES
            Runtime.NODEJS, // RUNTIME
            LogLevel.CRITICAL, // LEVEL
            "file", // FILE
            hashOfEmptyString, // FILE_ID
            "block_WITH_VERY_VERY_XXXXXX_", // BLOCK
            "method", // METHOD
            0, // LINE
            0, // COL
            stacktrace, // STACK_TRACE
            UnsignedLong.valueOf("14320079667909212422"), // STACK_TRACE_ID
            originalStacktrace, // ORIGINAL_STACK_TRACE
            Arrays.asList("Object.blocks.prepare_data_apphost-story-counts", "Object.blocks", "tools.get.forEach" +
             ".story", "Array.forEach", "Object.bl", "Object.<anonymous>", "Object.blocks", "Object.blocks" +
              ".prepare_data_apphost-story", "Object.blocks", "newsd.forEach.item"), // STACK_TRACE_NAMES
            Arrays.asList("/place/db/iss3/instances/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH" +
             "/templates/desktop/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
              "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects/story/" +
               ".bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
    "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects/story/" +
                 ".bundles/desktop/_desktop.renderer.js", "<anonymous>", "/place/db/iss3/instances" +
                  "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects" +
            "/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
 "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/tools" +
                     "/experimentarium.js", "/place/db/iss3/instances" +
            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop/projects" +
                       "/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                        "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                         "/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
        "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                           "/projects/story/.bundles/desktop/_desktop.renderer.js", "/place/db/iss3/instances" +
                            "/grllhpwvxiodpv7u_priemka__templates__news_renderer_yp_h0kGzA25BhH/templates/desktop" +
                             "/projects/story/.bundles/desktop/_desktop.renderer.js"), // STACK_TRACE_URLS
            Arrays.asList(18659, 14292, 35842, 0, 35839, 40, 14292, 18473, 14292, 18354), // STACK_TRACE_LINES
            Arrays.asList(43, 29, 30, 0, 58, 33, 29, 17, 29, 21), // STACK_TRACE_COLS
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683" +
             ".86 YaBrowser/19.4.0.2400 Yowser/2.5 Safari/537.36", // USER_AGENT
            UnsignedLong.valueOf("8983658141965199254"), // USER_AGENT_ID
            "source", // SOURCE
            "", // SOURCE_METHOD
            "type", // SOURCE_TYPE
            UnsignedLong.valueOf(0), // CLIENT_TIMESTAMP
            UnsignedLong.valueOf(0), // CLIENT_INIT_TIMESTAMP
            Arrays.asList(), // REPLACED_URLS_KEYS
            Arrays.asList(), // REPLACED_URLS_VALUES
            Parser.REPORT_RENDERER, // PARSER
            UnsignedLong.valueOf("0"), // ICOOKIE
            false, // SILENT
            "sas", // DC
            0d, // COORDINATES_LATITUDE
            0d, // COORDINATES_LONGITUDE
            0, // COORDINATES_PRECISION
            0L, // COORDINATES_TIMESTAMP
            checker.getHost(), // HOST
            checker.getLogBrokerTopic() // TOPIC
        );
    }

    @Test
    public void skipWithLevelDebug() throws Exception {
        String line = "{\"scarab:format\":{\"type\":\"json\",\"version\":2},\"scarab:version\":2," +
         "\"timestamp\":1551087922547,\"scarab:type\":\"RENDERER_DEBUG_EVENT\",\"metaprj\":\"web\"," +
          "\"prj\":\"web-main-low\",\"ctype\":\"prestable\",\"geo\":\"sas\",\"provider\":\"RENDERER\",\"workerId\":4," +
           "\"level\":\"DEBUG\",\"message\":\"test\",\"templatePath\":\"video3:desktop\",\"source\":\"RENDERER\"}\n";
        checker.setParam("projects", "{\"video3:desktop\":\"video\"}");
        checker.setParam("platforms", "{\"video3:desktop\":\"desktop\"}");

        checker.checkEmpty(line);
    }
}
