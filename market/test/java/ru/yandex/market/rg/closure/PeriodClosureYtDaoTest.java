package ru.yandex.market.rg.closure;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.rg.closure.model.PeriodClosureIncomeOebsRow;
import ru.yandex.market.rg.closure.model.PeriodClosureOutcomeOebsRow;

public class PeriodClosureYtDaoTest {

    private final PeriodClosureYtDao ytDao = new PeriodClosureYtDao( null, null, null, null, null);

    @Test
    void mapOutcomeOebsRow() {
        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        jsonNode.put("ID", "17082519");
        jsonNode.put("CONTRACT_NAME", "ОФ-2351577");
        jsonNode.put("CONTRACT_ID", "4967068");
        jsonNode.put("DISTRIBUTION_SET_ID", "15129");
        jsonNode.put("DISTRIBUTION_SET_NAME", "AR Синий Маркет (субсидии) YMAR, руб");
        jsonNode.put("REWARD_AMOUNT_100", 2055.17);
        jsonNode.put("PERIOD_START_DATE", "2022-01-01");
        jsonNode.put("PERIOD_END_DATE", "2022-01-31");
        jsonNode.put("ATTRIBUTE1", "MARKETPLACEBLUE");
        jsonNode.put("DESCRIPTION", "AR Синий Маркет (субсидии) YMAR, рубли");

        PeriodClosureOutcomeOebsRow row = ytDao.mapOebsRow(jsonNode, PeriodClosureOutcomeOebsRow.class);
        Assertions.assertEquals(row.getId(), 17082519L);
        Assertions.assertEquals(row.getContractName(), "ОФ-2351577");
        Assertions.assertEquals(row.getRewardAmount(), new BigDecimal("2055.17"));
    }

    @Test
    void mapIncomeOebsRow() {
        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);

        jsonNode.put("K_NUMBER__CONTRACT_ID", 5526894L);
        jsonNode.put("K_ALIAS__OFFER_NUMBER", "ОФ-2351577");
        jsonNode.put("LINE00__OP_BALANCE", 515.95);
        jsonNode.put("LINE01__PAYMENT_AMOUNT_SUM", 185.05);
        jsonNode.put("LINE02__REFUND_AMOUNT_SUM", 217);
        jsonNode.put("LINE08__NETTING_PAID", 896.12);
        jsonNode.put("LINE09__PAYMENT_AMOUNT", 8596.2);
        jsonNode.put("LINE10__PAID_AMOUNT", 581);
        jsonNode.put("LINE12__COMPENSATIONS", 1052.36);
        jsonNode.put("LINE99__END_BALANCE", 2022.5);

        PeriodClosureIncomeOebsRow row = ytDao.mapOebsRow(jsonNode, PeriodClosureIncomeOebsRow.class);
        Assertions.assertEquals(row.getContractId(), 5526894L);
        Assertions.assertEquals(row.getContractName(), "ОФ-2351577");
        Assertions.assertEquals(row.getPayments(), new BigDecimal("185.05"));
        Assertions.assertEquals(row.getRefunds(), new BigDecimal("217"));
        Assertions.assertEquals(row.getFees(), new BigDecimal("896.12"));
        Assertions.assertEquals(row.getWillBePaid(), new BigDecimal("8596.2"));
        Assertions.assertEquals(row.getPaid(), new BigDecimal("581"));
        Assertions.assertEquals(row.getCompensationForLost(), new BigDecimal("1052.36"));
    }
}
