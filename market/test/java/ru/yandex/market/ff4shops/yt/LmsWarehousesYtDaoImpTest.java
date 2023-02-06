package ru.yandex.market.ff4shops.yt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.stocks.model.Warehouse;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

/**
 * Тест для отладки/проверки кода при взаимдействии с yt
 */
@Disabled
class LmsWarehousesYtDaoImpTest extends FunctionalTest {

    private static final String LMS_PARTNER_TABLE = "//home/market/testing/combinator/outlets/partner";
    private static final String LMS_LOGISTIC_POINT_TABLE = "//home/market/testing/combinator/outlets/logistics_point";
    private static final String LMS_ADDRESS_TABLE = "//home/market/testing/combinator/outlets/address";
    private static final String LMS_PARTNER_REL_TABLE = "//home/market/testing/combinator/outlets/partner_relation";
    private static final String LMS_PARAMS_VALUES_TABLE =
            "//home/market/testing/combinator/outlets/partner_external_param_value";
    private static final String LMS_IS_EXPRESS_PARAM_VALUE_ID = "216";

    private LmsWarehousesYtDaoImp lmsWarehousesYtDaoImp;


    @BeforeEach
    void setUp() {
        var template = new NamedParameterJdbcTemplate(yqlDataSource());
        lmsWarehousesYtDaoImp = new LmsWarehousesYtDaoImp(template,
                "hahn",
                LMS_PARTNER_TABLE,
                LMS_LOGISTIC_POINT_TABLE,
                LMS_ADDRESS_TABLE,
                LMS_PARTNER_REL_TABLE,
                LMS_PARAMS_VALUES_TABLE,
                LMS_IS_EXPRESS_PARAM_VALUE_ID);
    }

    @Test
    public void selectAll() throws SQLException {
        var warehouses = new ArrayList<Warehouse>();
        lmsWarehousesYtDaoImp.processWarehouses(warehouses::addAll);
        var count = warehouses.size();
    }

    /**
     * Для запуска теста надо положить свой yql логин
     */
    private String getUser() {
        return "user";
    }

    /**
     * Для запуска теста надо положить свой yql токен
     */
    private String getToken() {
        return "token";
    }

    private String getHost() {
        return "jdbc:yql://yql.yandex.net:443";
    }

    public DataSource yqlDataSource() {
        final YqlProperties properties = new YqlProperties();

        properties.setUser(getUser());
        properties.setPassword(getToken());
        properties.setSyntaxVersion(1);
        properties.setConnectionTimeout((int) TimeUnit.MINUTES.toMillis(20));

        return new YqlDataSource(getHost(), properties);
    }
}
