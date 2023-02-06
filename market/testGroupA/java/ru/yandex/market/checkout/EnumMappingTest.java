package ru.yandex.market.checkout;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentHistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubstatus;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundSubstatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.DeliveryCompensationType;
import ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnSubreason;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorizationType;
import ru.yandex.market.checkout.common.db.HasIntId;
import ru.yandex.market.checkout.liquibase.config.DbMigrationConfig;
import ru.yandex.market.checkout.liquibase.config.TestDbConfig;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERIVCE_UNDELIVERED;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(name = "root", classes = {TestDbConfig.class, DbMigrationConfig.class})
public class EnumMappingTest {

    private static final String UNKNOWN = "UNKNOWN";
    protected final Map<String, Class<? extends Enum<? extends HasIntId>>> enums = new HashMap<>();

    @Autowired
    private DataSource datasourceCheckouter;

    @PostConstruct
    public void init() {
        enums.put("mapdeliverytype", DeliveryType.class);
        enums.put("maporderstatus", OrderStatus.class);
        enums.put("mapeventtype", HistoryEventType.class);
        enums.put("maphistoryeventtype", HistoryEventType.class);
        enums.put("mapordersubstatus", OrderSubstatus.class);
        enums.put("maprefundstatus", RefundStatus.class);
        enums.put("mapreturnreason", ReturnReasonType.class);
        enums.put("mapreturnstatus", ReturnStatus.class);
        enums.put("mapreturnsubreason", ReturnSubreason.class);
        enums.put("maprgb", Color.class);
        enums.put("mapInitiatorType", TvmAuthorizationType.class);
        enums.put("mapPaymentGoal", PaymentGoal.class);
        enums.put("mapdeliverycompensationtype", DeliveryCompensationType.class);
        enums.put("mapReturnDeliveryStatus", ReturnDeliveryStatus.class);
        enums.put("mapReturnDecision", ReturnDecisionType.class);
        enums.put("mapPaymentSubmethod", PaymentSubmethod.class);
        enums.put("mapPaymentEvent", PaymentHistoryEventType.class);
        enums.put("mapRefundEvent", PaymentHistoryEventType.class);
        enums.put("mapReceiptType", ReceiptType.class);
        enums.put("mapPaymentSubstatus", PaymentSubstatus.class);
        enums.put("mapRefundReason", RefundReason.class);
        enums.put("mapRefundSubstatus", RefundSubstatus.class);
        enums.put("mapOrderChangeRequestType", ChangeRequestType.class);
        enums.put("mapOrderChangeRequestStatus", ChangeRequestStatus.class);
        enums.put("mapQueuedCallType", CheckouterQCType.class);
        // https://st.yandex-team.ru/MARKETCHECKOUT-28064
        //enums.put("mapPaymentMethod", PaymentMethod.class);
    }

    /**
     * Цель теста - поддержание соответствия хранимых процедур БД (mapdeliverytype, maporderstatus и пр.) java-енумам.
     * При изменениях java-енумов необходимо менять соответствующую хранимую процедуру.
     * <p>
     * Change set создания новых процедур следует помечать флагом runOnChange:true (см. enum_mapping_functions.sql),
     * а саму процедуру определять как CREATE OR REPLACE FUNCTION mapMyEnum(ordinal int)...
     * Дальнейшие изменения процедуры можно будет производить в этом же change set, не создавая нового
     * <p>
     * Если процедура уже была создана без флага runOnChange:true, то необходимо перезаписать процедуру в новом
     * change set в файле enum_mapping_functions.sql
     *
     * @author skutyev
     */
    @Test
    public void testEnumsMatching() throws Exception {
        try (Connection connection = datasourceCheckouter.getConnection()) {
            Statement statement = connection.createStatement();

            List<String> selects = new LinkedList<>();
            List<String> expectedValues = new LinkedList<>();
            fillSelectsAndExpectedValues(selects, expectedValues);
            String query = "select " + StringUtils.join(selects, ", ");

            try (ResultSet resultSet = statement.executeQuery(query)) {
                resultSet.next();
                int column = 1;
                for (String expectedValue : expectedValues) {
                    String select = selects.get(column - 1);
                    String gotValue = resultSet.getString(column++);
                    if (!expectedValue.startsWith(UNKNOWN)) {
                        //DELIVERY_SERIVCE_UNDELIVERED сохранен для обратной совместимости, будет выпелен,
                        //и тогда-же нужно будет убрать этот if из теста.
                        if (!DELIVERY_SERIVCE_UNDELIVERED.name().equals(expectedValue)) {
                            Assertions.assertEquals(expectedValue, gotValue, select);
                        }
                    }
                }
            }
        }
    }

    private void fillSelectsAndExpectedValues(List<String> selects, List<String> expectedValues) {
        for (String func : enums.keySet()) {
            Class enumClass = enums.get(func);
            for (Object enumItem : enumClass.getEnumConstants()) {
                HasIntId enumItemWithId = (HasIntId) enumItem;
                int id = enumItemWithId.getId();
                selects.add(func + "(" + id + ")");
                expectedValues.add(enumItemWithId.toString());
            }
        }
    }
}
