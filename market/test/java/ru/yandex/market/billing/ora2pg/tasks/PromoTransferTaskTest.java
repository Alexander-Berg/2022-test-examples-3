package ru.yandex.market.billing.ora2pg.tasks;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.ora2pg.TransferOraToPgCommand;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_INPUT;
import static ru.yandex.market.billing.ora2pg.TransferOraToPgCommand.TASK_NAME;

@ParametersAreNonnullByDefault
class PromoTransferTaskTest extends FunctionalTest {

    @Autowired
    PromoTransferTask promoTransferTask;

    @Autowired
    TransferOraToPgCommand transferOraToPgCommand;

    @Autowired
    JdbcTemplate pgJdbcTemplate;

    @Test
    void springInitialization() {
        assertThat(promoTransferTask.getName()).isEqualTo("promo");
        assertThatThrownBy(() -> transferOraToPgCommand.registerTask(promoTransferTask)).hasMessageContaining("promo");
    }

    @Test
    @DisplayName("Записи только в cpa_order_promo, для которых нет записей в cpa_order_item_promo")
    @DbUnitDataSet(
            before = "PromoTransferTaskTest.orderPromoOnly.before.csv",
            after = "PromoTransferTaskTest.orderPromoOnly.after.csv")
    public void orderPromoOnly() {
        promoTransferTask.execute(input(0, 100000000), null);
    }

    @Test
    @DisplayName("Запись в cpa_order_promo, для которой есть одна запись в cpa_order_item_promo")
    @DbUnitDataSet(
            before = "PromoTransferTaskTest.orderPromoWithOneItemPromo.before.csv",
            after = "PromoTransferTaskTest.orderPromoWithOneItemPromo.after.csv")
    public void orderPromoWithOneItemPromo() {
        promoTransferTask.execute(input(0, 100000000), null);
    }

    @Test
    @DisplayName("Запись в cpa_order_promo, для которой есть несколько записей в cpa_order_item_promo")
    @DbUnitDataSet(
            before = "PromoTransferTaskTest.orderPromoWithSeveralItemPromo.before.csv",
            after = "PromoTransferTaskTest.orderPromoWithSeveralItemPromo.after.csv")
    public void orderPromoWithSeveralItemPromo() {
        promoTransferTask.execute(input(0, 100000000), null);
    }

    @Test
    @DisplayName("Матчинг cpa_order_item_promo с cpa_order_promo в разных вариациях")
    @DbUnitDataSet(
            before = "PromoTransferTaskTest.matching.before.csv",
            after = "PromoTransferTaskTest.matching.after.csv")
    public void orderItemPromoWithOrderPromoMatch() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "promo",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(63997590L, 63997599L))
        );

        transferOraToPgCommand.execute(
                new CommandInvocation("", new String[0], options), null);

        String select = "" +
                "select shop_promo_id, partner_cashback_percent " +
                "from market_billing.cpa_order_item_promo as ip " +
                "join market_billing.cpa_order_promo as op on ip.promo_id = op.id " +
                "where ip.order_id in (63997594, 63997595)";
        List<Map<String, Object>> result = new NamedParameterJdbcTemplate(pgJdbcTemplate)
                .queryForList(select, new MapSqlParameterSource());

        Map<String, BigDecimal> shopPromoIdToPercent = StreamEx.of(result)
                .mapToEntry(
                        record -> (String) record.get("shop_promo_id"),
                        record -> (BigDecimal) record.get("partner_cashback_percent")
                )
                .toMap();
        assertThat(shopPromoIdToPercent).hasSize(3);
        assertThat(shopPromoIdToPercent.get("ttt0")).isEqualTo(BigDecimal.valueOf(44).setScale(2));
        assertThat(shopPromoIdToPercent.get("ttt1")).isEqualTo(BigDecimal.valueOf(45).setScale(2));
        assertThat(shopPromoIdToPercent.get("ttt2")).isEqualTo(BigDecimal.valueOf(46).setScale(2));
    }

    @Test
    @DisplayName("Несколько итераций с ограничением на orderId")
    @DbUnitDataSet(
            before = "PromoTransferTaskTest.iterations.before.csv",
            after = "PromoTransferTaskTest.iterations.after.csv")
    public void iterations() throws Exception {
        Map<String, String> options = Map.of(
                TASK_NAME, "promo",
                TASK_INPUT, new ObjectMapper().writeValueAsString(input(0, 63997603))
        );

        transferOraToPgCommand.execute(
                new CommandInvocation("", new String[0], options), null);
    }

    @Test
    @DisplayName("cpa_order_item_promo ссылается на правильную запись в cpa_order_promo")
    @DbUnitDataSet(before = "PromoTransferTaskTest.iterations.before.csv")
    public void keys() {
        transferOraToPgCommand.execute(
                new CommandInvocation("", new String[0], Map.of(TASK_NAME, "promo")), null);

        String select = "" +
                "select market_promo_id " +
                "from market_billing.cpa_order_item_promo as ip " +
                "join market_billing.cpa_order_promo as op on ip.promo_id = op.id " +
                "where ip.order_id = :order_id";
        List<Map<String, Object>> result = new NamedParameterJdbcTemplate(pgJdbcTemplate).queryForList(
                select,
                new MapSqlParameterSource().addValue("order_id", 63997594L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("market_promo_id")).isEqualTo("qqq2");
    }

    private JsonNode input(long fromOrderId, long toOrderId) {
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        node.put("from_id", fromOrderId);
        node.put("to_id", toOrderId);
        node.put("batch_size", 5L);
        return node;
    }
}
