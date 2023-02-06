package ru.yandex.market.checkout.mstat;

import java.sql.Types;
import java.util.Set;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.mstat.MStatContractValidationResult;
import ru.yandex.market.checkout.checkouter.mstat.validators.MStatContractValidator;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMStatContractTest<T extends MStatContractValidator> extends AbstractPaymentTestBase {

    @Autowired
    protected OrderServiceHelper orderServiceHelper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    protected void createOrder() {
        order.set(yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build());
    }

    protected void validateTableStructure() {
        MStatContractValidationResult result = getValidator().validate();
        assertTrue(result.getMissingColumnsErrors().isEmpty(),
                String.format(
                        "Не прошла проверка состава атрибутов таблицы: из таблицы удалены атрибуты:\n$s",
                        result.getMissingColumnsErrors()
                ));
    }

    protected void validateTableAttributesType() {
        MStatContractValidationResult result = getValidator().validate();
        assertTrue(result.getColumnTypesErrors().isEmpty(),
                String.format(
                        "Не прошла проверка типа атрибутов таблицы: изменены типы атрибутов:\n%s",
                        result.getColumnTypesErrors()
                ));
    }

    protected void validatePossibleEnumValues() {
        MStatContractValidationResult result = getValidator().validate();
        assertTrue(result.getEnumValuesErrors().isEmpty(),
                String.format(
                        "Не прошла проверка типа атрибутов таблицы: изменены значения перечислений:\n%s",
                        result.getEnumValuesErrors()
                ));
    }

    protected void validateSavedEntity(Object entityId) {
        validateSavedEntity("id", entityId);
    }

    protected void validateSavedEntity(String conditionColumnName,
                                       Object entityId) {
        SqlRowSet rs = jdbcTemplate.queryForRowSet(
                "SELECT * FROM " + getValidator().getTableName() + " WHERE " + conditionColumnName + " = ?",
                entityId
        );

        assertTrue(rs.next());
        getTableNecessaryColumns().forEach(
                (column) -> assertNotNull(
                        getColumnValue(column, getValidator().getTableColumns().get(column), rs),
                        column
                ));
    }

    protected abstract T getValidator();

    protected abstract Set<String> getTableNecessaryColumns();

    private Object getColumnValue(String columnName, int columnType, SqlRowSet rs) {
        switch (columnType) {
            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(columnName);
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return rs.getInt(columnName);
            case Types.BIGINT:
                return rs.getLong(columnName);
            case Types.NUMERIC:
            case Types.DECIMAL:
                return rs.getBigDecimal(columnName);
            case Types.REAL:
                return rs.getFloat(columnName);
            case Types.FLOAT:
            case Types.DOUBLE:
                return rs.getDouble(columnName);
            case Types.DATE:
                return rs.getDate(columnName);
            case Types.TIME:
                return rs.getTime(columnName);
            case Types.TIMESTAMP:
                return rs.getTimestamp(columnName);
            default:
                return rs.getString(columnName);
        }
    }
}
