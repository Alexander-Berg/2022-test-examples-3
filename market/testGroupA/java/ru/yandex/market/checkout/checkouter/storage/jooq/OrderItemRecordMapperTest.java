package ru.yandex.market.checkout.checkouter.storage.jooq;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkup;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkouter.jooq.tables.records.OrderItemRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author dzvyagin
 */
@Disabled
//this test was broken and skipped execution due to junit version mismatch
class OrderItemRecordMapperTest {

    private static final long UPDATE_TIME = 100500L;

    @Test
    public void fromEntityPartnerPrices() {
        OrderItemRecord record = new OrderItemRecord();
        record.setPartnerPrice(BigDecimal.valueOf(99L));
        List<PartnerPriceMarkupCoefficient> markups = Arrays.asList(
                new PartnerPriceMarkupCoefficient("markup1", new BigDecimal("1.21")),
                new PartnerPriceMarkupCoefficient("markup2", new BigDecimal("0.12"))
        );
        Gson gson = new Gson();
        record.setPartnerPriceMarkups(gson.toJson(markups));
        OrderItem orderItem = OrderItemRecordMapper.fromRecord(record);
        assertNotNull(orderItem.getPrices());
        ItemPrices itemPrices = orderItem.getPrices();
        assertThat(itemPrices.getPartnerPrice()).isEqualTo(new BigDecimal("99"));
        final List<PartnerPriceMarkupCoefficient> coefficients = itemPrices.getPartnerPriceMarkup().getCoefficients();
        assertThat(coefficients.size()).isEqualTo(2);
        assertThat(coefficients).contains(new PartnerPriceMarkupCoefficient("markup1", new BigDecimal("1.21")));
        assertThat(coefficients).contains(new PartnerPriceMarkupCoefficient("markup2", new BigDecimal("0.12")));
        assertThat(itemPrices.getPartnerPriceMarkup().getUpdateTime()).isEqualTo(UPDATE_TIME);
    }

    @Test
    public void toEntityPartnerPrices() {
        OrderItem orderItem = new OrderItem();
        List<PartnerPriceMarkupCoefficient> coefficients = Arrays.asList(
                new PartnerPriceMarkupCoefficient("markup1", new BigDecimal("1.21")),
                new PartnerPriceMarkupCoefficient("markup2", new BigDecimal("0.12"))
        );
        final PartnerPriceMarkup partnerPriceMarkup = new PartnerPriceMarkup();
        partnerPriceMarkup.setCoefficients(coefficients);
        partnerPriceMarkup.setUpdateTime(UPDATE_TIME);
        orderItem.setFeedId(1L);
        orderItem.setCategoryId(1);
        orderItem.getPrices().setPartnerPriceMarkup(partnerPriceMarkup);
        orderItem.getPrices().setPartnerPrice(new BigDecimal("99"));
        OrderItemRecord record = OrderItemRecordMapper.toRecord(orderItem);
        assertThat(record.getPartnerPrice()).isEqualTo(new BigDecimal("99"));
        Gson gson = new Gson();
        assertThat(record.getPartnerPriceMarkups()).isEqualTo(gson.toJson(coefficients));
    }

}
