package steps.orderSteps.itemSteps;

import java.util.HashSet;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.cart.ItemChange;

public class ChangesSteps {
    private static final ItemChange ITEM_CHANGE = ItemChange.DELIVERY;

    private ChangesSteps() {
    }

    public static Set<ItemChange> getChangeSet() {
        HashSet<ItemChange> itemChangeSet = new HashSet<>();
        itemChangeSet.add(ITEM_CHANGE);
        return itemChangeSet;
    }
}
