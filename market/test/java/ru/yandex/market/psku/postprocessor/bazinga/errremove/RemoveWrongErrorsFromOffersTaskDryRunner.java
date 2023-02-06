package ru.yandex.market.psku.postprocessor.bazinga.errremove;

import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.config.CommonDaoConfig;
import ru.yandex.market.psku.postprocessor.common.db.dao.WrongErrorRemovalDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.WrongErrorRemovalStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.WrongErrorRemovalType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.daos.WrongErrorRemovalStatsDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.WrongErrorRemoval;
import ru.yandex.market.psku.postprocessor.common.service.DataCampService;
import ru.yandex.market.psku.postprocessor.common.service.DataCampServiceImpl;
import ru.yandex.market.psku.postprocessor.config.ManualTestConfig;

/**
 * Для запуска необходимо заполнить YQL_USER, YQL_TOKEN и YQL_USER_TOKEN в ManualTestConfig.
 * Проверяет факт создания таблицы, обусловленный выполнением запроса, но для проверки результата необходимо
 * вручную проверить таблицу (с названием offers_with_bad_errors_*_*), создаваемую в папке пользователя в YT.
 * В случае, если недавно была выполнена очистка, пустая таблица с результатом валидна.
 **/
@ContextConfiguration(classes = {ManualTestConfig.class, CommonDaoConfig.class})
public class RemoveWrongErrorsFromOffersTaskDryRunner extends BaseDBTest {
    private final static String ENVIRONMENT = "testing";
    /* Для запросов в DataCamp необходим tvm ticket, пример команды для генерации:
    ya tool tvmknife get_service_ticket client_credentials -d 2002296 -s 2016225 -S secret
    В файле secret должен лежать соответсвующий секрет (ищется в секретнице по "tvm.")
     */
    private final static String TVM_TICKET = "3:serv:CNKvARDPsPuWBiIICIbtexDjh3s:ElhPdpnSDgDEk-JPkGFB4kileEsky9-gAISbA35MePtSsa1XEe1TELgpa46XQEiWR8pDO4NI8YKyKrptzwgQXnBCmGn_N53HmA0AcJ_BK6CHfEYcixhQNB6yp-gSVPPdeepnB3ItnrMQtoRrjjWk9LrgMJ8VDGtku-ycyys-cMpUtBSOhT8mOaNqbRsUJ6_DKm0UO-6MKCzeL3b7S-Fu-S503tT__qsVAf8F4HZdRDNO0QQmkbWyQemUiGbwPg2BXUWwUsVaiRTjsjA1JE_zYpKmqgWihpAx805ukuaMhtm2x6mhm1TW8y2Wmb5Q8D1_ydxmA52zhOJ4H6igWodd2g";
    // Флаг для пропуска поиска офферов, для работы необходимо указать имя существующей таблицы
    private final static boolean SKIP_FINDING_OFFERS = false;
    private final static String EXISING_TABLE_NAME = "";
    private final static WrongErrorRemovalType TYPE = WrongErrorRemovalType.JUDGE_FORBID;

    private RemoveWrongErrorsFromOffersTask removeWrongErrorsFromOffersTask;

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    @Qualifier("hahnYtApi")
    private Yt yt;

    @Autowired
    private WrongErrorRemovalDao wrongErrRemovalDao;

    @Autowired
    private WrongErrorRemovalStatsDao wrongErrorRemovalStatsDao;

    @Before
    public void setUp() {
        String tablePathTemplate = String.format("//home/market/users/%s/remove_wrong_errors/${table_name}", ManualTestConfig.YT_USER); // Имя таблицы будет в логе

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(10000)))
                .setSocketTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(10000)))
                .setConnectionRequestTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(1000)))
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent("defaultUserAgent")
                .setDefaultHeaders(List.of(new BasicHeader("X-Ya-Service-Ticket", TVM_TICKET)))
                .build();

        DataCampService dataCampService = new DataCampServiceImpl(httpClient, "http://datacamp.white.tst.vs.market" +
                ".yandex.net");

        switch (TYPE) {
            case JUDGE_FORBID:
                removeWrongErrorsFromOffersTask = new RemoveJudgeForbidErrorsFromOffersTask(yqlJdbcTemplate, yt, wrongErrRemovalDao, tablePathTemplate, ENVIRONMENT, dataCampService, wrongErrorRemovalStatsDao);
                break;
            case REMOVED_FROM_OPERATOR_CARD:
                removeWrongErrorsFromOffersTask = new RemoveRemovedFromOperatorCardErrorsFromOffersTask(yqlJdbcTemplate, yt, wrongErrRemovalDao, tablePathTemplate, ENVIRONMENT, dataCampService, wrongErrorRemovalStatsDao);
                break;
        }
    }

    @Ignore("Для ручного запуска")
    @Test
    public void testExecute() {
        if (SKIP_FINDING_OFFERS) {
            WrongErrorRemoval wrongErrorRemoval = new WrongErrorRemoval();
            wrongErrorRemoval.setTableName(EXISING_TABLE_NAME);
            wrongErrorRemoval.setStatus(WrongErrorRemovalStatus.OFFERS_LOADED);
            wrongErrorRemoval.setType(WrongErrorRemovalType.JUDGE_FORBID);
            wrongErrorRemoval.setCreateDate(Timestamp.from(Instant.now()));
            wrongErrorRemoval.setUpdateDate(Timestamp.from(Instant.now()));
            wrongErrRemovalDao.insert(wrongErrorRemoval);
        }

        removeWrongErrorsFromOffersTask.execute(null);

        Optional<String> insertTablePath = removeWrongErrorsFromOffersTask.getInsertTablePath();
        Assertions.assertThat(insertTablePath).isNotEmpty();

        // Если запрос не был выполнен, то таблица не создана и select упадет с ошибкой.
        try {
            yqlJdbcTemplate.execute(String.format("select * from `%s`", insertTablePath.get()));
        } catch (DataAccessException ex) {
            // Не все типы поддерживаются JDBC-драйвером YQL, поэтому приходится проверять, упали ли из-за того, что ResultSet не может преобразовать типы: https://yql.yandex-team.ru/docs/yt/interfaces/jdbc#osobennosti-realizacii-i-ogranicheniya
            if (!(ex.getCause().getCause() instanceof SQLFeatureNotSupportedException)) {
                throw ex;
            }
        }

    }
}
