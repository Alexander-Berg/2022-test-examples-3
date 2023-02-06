package steps.ordersteps.ordersubsteps;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.delivery.entities.common.Item;
import ru.yandex.market.delivery.entities.common.Korobyte;

public class ItemSteps {

    private ItemSteps() {
        throw new UnsupportedOperationException();
    }

    public static Item getItem() {
        Item item = new Item();
        Korobyte korobyte = new Korobyte();
        korobyte.setWeightGross(BigDecimal.valueOf(10));

        item.setName("name");
        item.setCount(1);
        item.setPrice(BigDecimal.valueOf(100));
        item.setKorobyte(korobyte);

        return item;
    }

    public static List<Item> getItemList() {
        return Collections.singletonList(getItem());
    }
}
