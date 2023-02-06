package ru.yandex.direct.metrika.client.asynchttp;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.metrika.client.MetrikaConfiguration;
import ru.yandex.direct.tvm.TvmIntegration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_AVAILABLE_SOURCES_REQUEST;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_AVAILABLE_SOURCES_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_WITHOUT_REVENUE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_WITH_ANY_GOAL_CONVERSION_RATE_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CHIEF_LOGIN_REQUEST;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CONVERSION_RATE_REQUEST;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_E2E_ANALYTICS_WITH_GOAL_REQUEST;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_DETAILED_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_REQUEST;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_RESPONSE;
import static ru.yandex.direct.metrika.client.asynchttp.MockedDataKt.GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_WITH_ROW_IDS_REQUEST;

public class MockedMetrika extends ExternalResource {
    private static final Logger logger =
            LoggerFactory.getLogger(ru.yandex.direct.metrika.client.asynchttp.MockedMetrika.class);

    private static final String DIRECT_GET_GOALS_REQUEST =
            "POST:/retargeting_conditions?uid=%5B123%2C234%2C345%5D";
    private static final String DIRECT_GET_GOALS_RESPONSE =
            "{\"123\":[{\"id\":1233456,\"name\":\"visit@1\",\"owner\":23456789,\"type\":\"goal\"," +
                    "\"counter_id\":11111111,\"counter_domain\":\"foobar.com\",\"counter_name\":\"foobar.com-1\"}," +
                    "{\"id\":1233457,\"name\":\"visit@2\",\"owner\":23456789,\"type\":\"goal\"," +
                    "\"counter_id\":11111111,\"counter_domain\":\"foobar.com\",\"counter_name\":\"foobar.com-1\"}]}";

    private static final String DIRECT_ESTIMATE_REQUEST =
            "GET:/get_users_sketched?condition=id%3D%3D4010108741%20interval%2030%20days";
    private static final String DIRECT_ESTIMATE_RESPONSE = "{\"response\":42}";

    private static final String DIRECT_ESTIMATE_COMPLEX_REQUEST =
            "GET:/get_users_sketched?condition=%28id%3D%3D4010108741%20interval%2030%20days%20and%20id%3D" +
                    "%3D4010108742%20interval%2014%20days%29%20and%20%28id%3D%3D4010108743%20interval%2030%20days" +
                    "%20or%20id%3D%3D4010108744%20interval%2014%20days%29%20and%20%28not%20id%3D%3D4010108745" +
                    "%20interval%2030%20days%20and%20not%20id%3D%3D4010108746%20interval%2014%20days%29";
    private static final String DIRECT_ESTIMATE_COMPLEX_RESPONSE = "{\"response\":51119}";

    private static final String DIRECT_ESTIMATE_ERROR_REQUEST =
            "GET:/get_users_sketched?condition=id%3D%3D401010874100%20interval%2030%20days";
    private static final String DIRECT_ESTIMATE_ERROR_RESPONSE =
            "{\"errors\":[{\"error_type\":\"backend_error\",\"message\":\"Сервис временно недоступен\"}]," +
                    "\"code\":503,\"message\":\"Сервис временно недоступен\"}";

    private static final String INTERNAL_GRANT_REQUEST =
            "POST:/internal/grant_requests:{\"requests\":[{\"object_type\":\"counter\",\"object_id\":\"counter-1\"," +
                    "\"service_name\":\"direct\"},{\"object_type\":\"site\",\"object_id\":\"site-2\"," +
                    "\"service_name\":\"direct\"}]}";
    private static final String INTERNAL_GRANT_RESPONSE =
            "{\"response\":[{\"result\":\"ok\",\"object_id\":\"counter-1\",\"grants_affected\":1}," +
                    "{\"result\":\"error\",\"object_id\":\"site-2\",\"error_text\":\"No counters with this site found" +
                    " for login\",\"error_code\":\"ERR_COUNTER_NOT_FOUND\"}]}";

    private static final String IMPRESSIONS_REQUEST =
            "GET:/stat/v1/data?metrics=ym:s:productImpressions&dimensions=ym:s:counterID&date1=2016-06-13&date2=2016" +
                    "-06-27&ids=32233222";
    private static final String IMPRESSIONS_RESPONSE =
            "{\"random\":\"stuff\",\"data\":[{\"random\":\"stuff\",\"dimensions\":[{\"name\":\"3+hits\"," +
                    "\"id\":\"32233222\"}], \"metrics\":[42]}]}";

    private static final String GOAL_COUNTS_REQUEST = "GET:/direct/get_goal_counts?startDate=2016-06-13&endDate=2016" +
            "-06-27&counterIds=32233222";
    private static final String GOAL_COUNTS_RESPONSE = "{" +
            "   \"response\": [" +
            "       {" +
            "           \"counter_id\": 32233222," +
            "           \"counts\": [" +
            "               {\"goal_id\": 30606879, \"count\": 420.0, \"has_price\": 1}," +
            "               {\"goal_id\": 41646742, \"count\": 155.0, \"has_price\": 1}," +
            "               {\"goal_id\": 30606889, \"count\": 101.0, \"has_price\": 0}," +
            "               {\"goal_id\": 30606884, \"count\": 1.0, \"has_price\": 0}" +
            "           ]" +
            "       }" +
            "   ]" +
            "}";
    private static final String GOALS_STATISTIC_REQUEST =
            "GET:/stat/v1/data?metrics=ym:s:visits&dimensions=ym:s:goal&limit=100000&date1=2016-06-13&date2=2016-06" +
                    "-27" +
                    "&ids=32233222";
    private static final String GOALS_STATISTIC_RESPONSE =
            "{\"data\":[{\"dimensions\":[{\"name\":\"3+hits\",\"id\":\"30606879\"}],\"metrics\":[420.0]}," +
                    "{\"dimensions\":[{\"name\":\"Промо:КнопкаTrylivedemoглавная\",\"id\":\"41646742\"}]," +
                    "\"metrics\":[155.0]},{\"dimensions\":[{\"name\":\"Viewfeatures\",\"id\":\"30606889\"}]," +
                    "\"metrics\":[101.0]},{\"dimensions\":[{\"name\":\"Toblog\",\"id\":\"30606884\"}],\"metrics\":[1" +
                    ".0]}]}";

    private static final String MASS_COUNTER_GOALS_REQUEST =
            "GET:/direct/counter_goals?counters=323322";

    private static final String MASS_COUNTER_GOALS_RESPONSE1 =
            "{\"goals\":[{\"counter_id\":323322,\"goal\":{\"id\":123,\"name\":\"foobar\",\"type\":\"number\"}}]}";

    private static final String MASS_COUNTER_GOALS_RESPONSE =
            "{\"goals\":[{\"counter_id\":323322,\"goal\":{\"id\":123,\"name\":\"url_goal_name\",\"type\":\"url\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":125,\"name\":\"number_goal_name\",\"type\":\"number\", " +
                    "\"goal_source\":\"user\", \"default_price\": 10.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":127,\"name\":\"step_goal_name\",\"type\":\"step\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":129,\"name\":\"action_goal_name\",\"type\":\"action\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":131,\"name\":\"offline_goal_name\",\"type\":\"offline\"," +
                    " \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":133,\"name\":\"call_goal_name\",\"type\":\"call\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":135,\"name\":\"phone_goal_name\",\"type\":\"phone\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":137,\"name\":\"email_goal_name\",\"type\":\"email\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":139,\"name\":\"form_goal_name\",\"type\":\"form\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":141,\"name\":\"cdp_order_in_progress_goal_name\"," +
                    "\"type\":\"cdp_order_in_progress\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":143,\"name\":\"cdp_order_paid_goal_name\"," +
                    "\"type\":\"cdp_order_paid\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":145,\"name\":\"messenger_goal_name\"," +
                    "\"type\":\"messenger\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":147,\"name\":\"file_goal_name\",\"type\":\"file\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":149,\"name\":\"search_goal_name\",\"type\":\"search\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":151,\"name\":\"button_goal_name\",\"type\":\"button\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":153,\"name\":\"e_cart_goal_name\",\"type\":\"e_cart\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":155,\"name\":\"e_purchase_goal_name\"," +
                    "\"type\":\"e_purchase\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":157,\"name\":\"a_cart_goal_name\",\"type\":\"a_cart\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":159,\"name\":\"a_purchase_goal_name\"," +
                    "\"type\":\"a_purchase\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":161,\"name\":\"conditional_call_goal_name\"," +
                    "\"type\":\"conditional_call\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":163,\"name\":\"social_goal_name\",\"type\":\"social\", " +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":165,\"name\":\"payment_system_goal_name\"," +
                    "\"type\":\"payment_system\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":166,\"name\":\"contact_data_goal_name\"," +
                    "\"type\":\"contact_data\", \"goal_source\":\"user\", \"default_price\": 0.0}}," +
                    "{\"counter_id\":323322,\"goal\":{\"id\":167,\"name\":\"url_goal_name\",\"type\":\"url\", " +
                    "\"goal_source\":\"auto\", \"default_price\": 0.0}}" +
                    "]}";

    private static final String MASS_COUNTER_GOALS_UNKNOWN_TYPE_REQUEST =
            "GET:/direct/counter_goals?counters=323323";
    private static final String MASS_COUNTER_GOALS_UNKNOWN_TYPE_RESPONSE =
            "{\"goals\":[{\"counter_id\":323323,\"goal\":{\"id\":123,\"name\":\"foobar\",\"type\":\"abrakadabra\"," +
                    "\"goal_source\":\"user\"}}," +
                    "{\"counter_id\":323323,\"goal\":{\"id\":1234,\"name\":\"url_goal_name\",\"type\":\"url\"," +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}]}";

    private static final String MASS_COUNTER_GOALS_UNKNOWN_SOURCE_REQUEST =
            "GET:/direct/counter_goals?counters=323324";
    private static final String MASS_COUNTER_GOALS_UNKNOWN_SOURCE_RESPONSE =
            "{\"goals\":[{\"counter_id\":323324,\"goal\":{\"id\":223,\"name\":\"foobar\",\"type\":\"url\"," +
                    "\"goal_source\":\"abrakadabra\"}}," +
                    "{\"counter_id\":323324,\"goal\":{\"id\":2234,\"name\":\"url_goal_name\",\"type\":\"url\"," +
                    "\"goal_source\":\"user\", \"default_price\": 0.0}}]}";

    private static final String GET_USERS_COUNTERS_NUM_REQUEST = "POST:/direct/user_counters_num?uids=123%2C234";
    private static final String GET_USERS_COUNTERS_NUM_RESPONSE =
            "[{\"owner\":123,\"counters_cnt\":42,\"counter_ids\":[111,222]},{\"owner\":234,\"counters_cnt\":51," +
                    "\"counter_ids\":[333,444,555]}]";

    private static final String GET_USERS_COUNTERS_NUM_EXTENDED_REQUEST = "POST:/direct/user_counters_num_extended" +
            "?uids=123%2C234";
    private static final String GET_USERS_COUNTERS_NUM_EXTENDED_RESPONSE =
            "[" +
                    "{\"owner\":123,\"counters_cnt\":42,\"counters\":[" +
                    "{" +
                    "\"id\": 111," +
                    "\"name\": \"name_111\"," +
                    "\"counter_source\": \"turbodirect\"," +
                    "\"counter_permission\": \"edit\"," +
                    "\"ecommerce\": true," +
                    "\"site_path\": \"site_path_111\"" +
                    "}," +
                    "{" +
                    "\"id\": 222," +
                    "\"name\": \"name_222\"," +
                    "\"counter_source\": \"sprav\"," +
                    "\"counter_permission\": \"own\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_222\"" +
                    "}" +
                    "]}," +
                    "{\"owner\":234,\"counters_cnt\":51,\"counters\":[" +
                    "{" +
                    "\"id\": 333," +
                    "\"name\": \"name_333\"," +
                    "\"site_path\": \"site_path_333\"," +
                    "\"ecommerce\": true," +
                    "\"counter_permission\": \"view\"" +
                    "}," +
                    "{" +
                    "\"id\": 444," +
                    "\"name\": \"name_444\"," +
                    "\"counter_source\": \"sprav\"," +
                    "\"site_path\": \"site_path_444\"," +
                    "\"ecommerce\": false," +
                    "\"counter_permission\": \"edit\"" +
                    "}," +
                    "{" +
                    "\"id\": 555," +
                    "\"name\": \"name_555\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_555\"" +
                    "}," +
                    "{" +
                    "\"id\": 666," +
                    "\"name\": \"name_666\"," +
                    "\"counter_source\": \"system\"," +
                    "\"counter_permission\": \"own\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_666\"" +
                    "}," +
                    "{" +
                    "\"id\": 777," +
                    "\"name\": \"name_777\"," +
                    "\"counter_source\": \"partner\"," +
                    "\"counter_permission\": \"view\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_777\"" +
                    "}," +
                    "{" +
                    "\"id\": 888," +
                    "\"name\": \"name_888\"," +
                    "\"counter_source\": \"market\"," +
                    "\"counter_permission\": \"own\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_888\"" +
                    "}," +
                    "{" +
                    "\"id\": 999," +
                    "\"name\": \"name_999\"," +
                    "\"counter_source\": \"eda\"," +
                    "\"counter_permission\": \"view\"," +
                    "\"ecommerce\": false," +
                    "\"site_path\": \"site_path_999\"" +
                    "}" +
                    "]}]";

    private static final String GET_UPDATED_USER_COUNTERS_NUM_REQUEST =
            "POST:/direct/updated_user_counters_num?last_time=2016-06-22%2013%3A14%3A15";
    private static final String GET_UPDATED_USER_COUNTERS_NUM_RESPONSE =
            "[{\"owner\":123,\"last_update_time\":\"2016-06-22 19:20:21\",\"counters_cnt\":42},{\"owner\":234," +
                    "\"last_update_time\":\"2016-06-22 21:22:23\",\"counters_cnt\":51}]";

    private static final String UPDATE_COUNTER_GRANTS_SUCCESS_REQUEST =
            "PUT:/yandexservices/edit_counter/1234567?quota_ignore=1:{\"counter\":{\"grants\":[{\"perm\":\"view\"," +
                    "\"user_login\":\"login1\"},{\"perm\":\"view\",\"user_login\":\"login2\"}]}}";
    private static final String UPDATE_COUNTER_GRANTS_SUCCESS_RESPONSE =
            "{\"counter\":{\"id\":1234567,\"status\":\"Active\"}}";

    private static final String UPDATE_COUNTER_GRANTS_FAILED_REQUEST =
            "PUT:/yandexservices/edit_counter/890?quota_ignore=1:{\"counter\":{\"grants\":[{\"perm\":\"view\"," +
                    "\"user_login\":\"login1\"},{\"perm\":\"view\",\"user_login\":\"login2\"}]}}";
    private static final String UPDATE_COUNTER_GRANTS_FAILED_RESPONSE =
            "{\"errors\":[{\"error_type\":\"not_found\",\"message\":\"Entity not found\"}],\"code\":404," +
                    "\"message\":\"Entity not found\"}";

    private static final String GET_SEGMENTS_REQUEST = "GET:/management/v1/counter/1234567/segments";
    private static final String GET_SEGMENT_RESPONSE = "{\"segments\":[{\"segment_id\":1,\"counter_id\":1234567," +
            "\"name\":\"Новые посетители\",\"expression\":\"ym:s:isNewUser=='Yes'\",\"segment_source\":\"api\"}," +
            "{\"segment_id\":2,\"counter_id\":1234567,\"name\":\"Отказы\",\"expression\":\"ym:s:bounce=='Yes'\"," +
            "\"segment_source\":\"api\"}]}";

    private static final String GET_EDITABLE_COUNTERS_REQUEST = "GET:/management/v1/counters?permission=own%2Cedit";
    private static final String GET_EDITABLE_COUNTERS_RESPONSE =
            "{\"counters\":[{\"id\":1,\"site2\":{\"site\":\"ya.ru\"}},{\"id\":2,\"site2\":{\"site\":\"yandex.ru\"}}," +
                    "{\"id\":3,\"site2\":{\"site\":\"yandex.net\"}}]}";

    private static final String GET_COUNTER_REQUEST = "GET:/management/v1/counter/20220126";
    private static final String GET_COUNTER_RESPONSE = "{\"counter\":{\"id\":20220126,\"site2\":{\"site\":\"ya.ru\"}," +
            "\"features\":[\"ecommerce\"]}}";

    private static final String CREATE_SEGMENT_REQUEST =
            "POST:/management/v1/counter/1234567/segments:{\"segment\":{\"name\":\"Неотказы\"," +
                    "\"expression\":\"ym:s:bounce=='No'\"}}";
    private static final String CREATE_SEGMENT_RESPONSE = "{\"segment\":{\"segment_id\":1,\"counter_id\":1234567," +
            "\"name\":\"Неотказы\",\"expression\":\"ym:s:bounce=='No'\"}}";

    private static final String TURN_ON_CALL_TRACKING_REQUEST = "POST:/direct/turn_on_call_tracking?counterId=1234567";
    private static final String TURN_ON_CALL_TRACKING_RESPONSE = "{\"goal\":{\"id\":1111,\"is_retargeting\":0," +
            "\"name\":\"Звонок\",\"type\":\"call\"}}";

    private MockWebServer server;

    @Override
    protected void before() throws Throwable {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String req = request.getMethod() + ":" + request.getPath();
                String body = request.getBody().readUtf8();
                if (body.length() > 0) {
                    req = req + ":" + body;
                }
                switch (req) {
                    case DIRECT_GET_GOALS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")    //!!!
                                .setBody(DIRECT_GET_GOALS_RESPONSE);
                    case DIRECT_ESTIMATE_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")    //!!!
                                .setBody(DIRECT_ESTIMATE_RESPONSE);
                    case DIRECT_ESTIMATE_COMPLEX_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(DIRECT_ESTIMATE_COMPLEX_RESPONSE);
                    case DIRECT_ESTIMATE_ERROR_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")    //!!!
                                .setResponseCode(503).setBody(DIRECT_ESTIMATE_ERROR_RESPONSE);
                    case INTERNAL_GRANT_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")  //!!!
                                .setBody(INTERNAL_GRANT_RESPONSE);
                    case IMPRESSIONS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(IMPRESSIONS_RESPONSE);
                    case GET_BYTIME_STAT_E2E_ANALYTICS_WITH_GOAL_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_E2E_ANALYTICS_RESPONSE);
                    case GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA_RESPONSE);
                    case GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CHIEF_LOGIN_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA_RESPONSE);
                    case GET_BYTIME_STAT_E2E_ANALYTICS_WITHOUT_REVENUE:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_E2E_ANALYTICS_SKIPPED_GOAL_DATA_RESPONSE);
                    case GET_BYTIME_STAT_E2E_ANALYTICS_WITH_CONVERSION_RATE_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_E2E_ANALYTICS_WITH_ANY_GOAL_CONVERSION_RATE_RESPONSE);
                    case GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_RESPONSE);
                    case GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_WITH_ROW_IDS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_BYTIME_STAT_TRAFFIC_SOURCE_ANALYTICS_DETAILED_RESPONSE);
                    case GET_AVAILABLE_SOURCES_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_AVAILABLE_SOURCES_RESPONSE);
                    case GOALS_STATISTIC_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(GOALS_STATISTIC_RESPONSE);
                    case GOAL_COUNTS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(GOAL_COUNTS_RESPONSE);
                    case MASS_COUNTER_GOALS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(MASS_COUNTER_GOALS_RESPONSE);
                    case MASS_COUNTER_GOALS_UNKNOWN_TYPE_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(MASS_COUNTER_GOALS_UNKNOWN_TYPE_RESPONSE);
                    case MASS_COUNTER_GOALS_UNKNOWN_SOURCE_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(MASS_COUNTER_GOALS_UNKNOWN_SOURCE_RESPONSE);
                    case GET_USERS_COUNTERS_NUM_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(GET_USERS_COUNTERS_NUM_RESPONSE);
                    case GET_USERS_COUNTERS_NUM_EXTENDED_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(GET_USERS_COUNTERS_NUM_EXTENDED_RESPONSE);
                    case GET_UPDATED_USER_COUNTERS_NUM_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(GET_UPDATED_USER_COUNTERS_NUM_RESPONSE);
                    case UPDATE_COUNTER_GRANTS_SUCCESS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setBody(UPDATE_COUNTER_GRANTS_SUCCESS_RESPONSE);
                    case UPDATE_COUNTER_GRANTS_FAILED_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")   //!!!
                                .setResponseCode(404)
                                .setBody(UPDATE_COUNTER_GRANTS_FAILED_RESPONSE);
                    case GET_EDITABLE_COUNTERS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_EDITABLE_COUNTERS_RESPONSE);
                    case GET_COUNTER_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_COUNTER_RESPONSE);
                    case GET_SEGMENTS_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(GET_SEGMENT_RESPONSE);
                    case CREATE_SEGMENT_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(CREATE_SEGMENT_RESPONSE);
                    case TURN_ON_CALL_TRACKING_REQUEST:
                        return new MockResponse().addHeader("Content-Type", "application/json")
                                .setBody(TURN_ON_CALL_TRACKING_RESPONSE);
                }
                logger.error("UNEXPECTED REQUEST: {}", req);
                return new MockResponse().setResponseCode(404).setBody("Request not supported");
            }
        });
        server.start();
    }

    @Override
    protected void after() {
        try {
            server.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MockWebServer getServer() {
        return server;
    }

    public String getBaseUrl() {
        return server.url("/").toString();
    }


    public MetrikaAsyncHttpClient createClient(TvmIntegration tvmIntegration) {
        MetrikaConfiguration configuration =
                new MetrikaConfiguration(getBaseUrl(), getBaseUrl(), getBaseUrl());
        var ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        when(ppcPropertiesSupport.get(any(PpcPropertyName.class), any(Duration.class))).thenCallRealMethod();
        return new MetrikaAsyncHttpClient(configuration, new DefaultAsyncHttpClient(), tvmIntegration,
                ppcPropertiesSupport, true) {
            @Override
            protected LocalDate getLocalDate() {
                return LocalDate.of(2016, 6, 27);
            }
        };
    }
}
