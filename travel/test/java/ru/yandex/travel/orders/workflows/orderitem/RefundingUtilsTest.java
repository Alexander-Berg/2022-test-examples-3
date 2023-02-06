package ru.yandex.travel.orders.workflows.orderitem;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.MoneyMarkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.travel.orders.TestOrderObjects.fiscalItem;
import static ru.yandex.travel.orders.TestOrderObjects.moneyMarkup;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class RefundingUtilsTest {
    @Test
    public void calculateDefaultTargetMoneyMarkup() {
        List<FiscalItem> items = List.of(
                fiscalItem(1, 1000, 500, FiscalItemType.EXPEDIA_HOTEL),
                fiscalItem(2, 800, 500, FiscalItemType.EXPEDIA_HOTEL)
        );

        MoneyMarkup markup = RefundingUtils.calculateDefaultTargetMoneyMarkup(items, rub(300));

        assertThat(markup).isEqualTo(moneyMarkup(300, 0));
    }

    @Test
    public void calculateDefaultTargetMoneyMarkup_integerPlusPoints() {
        List<FiscalItem> items = List.of(fiscalItem(1, 1001.23, 500, FiscalItemType.EXPEDIA_HOTEL));

        MoneyMarkup markup1 = RefundingUtils.calculateDefaultTargetMoneyMarkup(items, rub(600));
        assertThat(markup1).isEqualTo(moneyMarkup(501, 99));

        MoneyMarkup markup2 = RefundingUtils.calculateDefaultTargetMoneyMarkup(items, rub(602.56));
        assertThat(markup2).isEqualTo(moneyMarkup(500.56, 102));
    }

    @Test
    public void calculateDefaultTargetMoneyMarkup_notEnoughMoney() {
        assertThatThrownBy(() -> RefundingUtils.calculateDefaultTargetMoneyMarkup(List.of(), rub(100)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough fiscal item money to provide target sum");
    }

    @Test
    public void calculateTargetFiscalItems() {
        List<FiscalItem> items = List.of(
                fiscalItem(1, 1000, 500, FiscalItemType.EXPEDIA_HOTEL),
                fiscalItem(2, 800, 500, FiscalItemType.EXPEDIA_HOTEL)
        );

        TargetFiscalItems targetItems = RefundingUtils.calculateTargetFiscalItems(
                items, rub(300), moneyMarkup(200, 100));

        assertThat(targetItems.getPrices()).isEqualTo(Map.of(1L, rub(300), 2L, rub(0)));
        assertThat(targetItems.getPricesMarkup()).isEqualTo(Map.of(
                1L, moneyMarkup(200, 100),
                2L, moneyMarkup(0, 0)));
    }

    @Test
    public void calculatTargetFiscalItemsGroupingByType() {
        List<FiscalItem> items = List.of(
                fiscalItem(1, 1000, 100, FiscalItemType.EXPEDIA_HOTEL),
                fiscalItem(2, 100, 10, FiscalItemType.HOTEL_MEAL),
                fiscalItem(3, 500, 0, FiscalItemType.EXPEDIA_HOTEL),
                fiscalItem(4, 50, 0, FiscalItemType.HOTEL_MEAL)
        );
        TargetFiscalItems targetItems = RefundingUtils.calculateTargetFiscalItemsGroupingByType(
                items, rub(825), null);
        assertThat(targetItems).isNotNull();
    }
}
