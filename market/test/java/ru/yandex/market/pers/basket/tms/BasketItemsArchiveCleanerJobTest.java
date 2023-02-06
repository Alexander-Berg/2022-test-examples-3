package ru.yandex.market.pers.basket.tms;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 03.12.2021
 */
public class BasketItemsArchiveCleanerJobTest extends PersBasketTest {
    @Autowired
    private BasketItemsArchiveCleanerJob job;

    @Autowired
    private BasketService basketService;

    @Test
    public void testCleaning() {
        BasketOwner owner = BasketOwner.fromUid(1);
        Long ownerId = basketService.getOrAddOwnerId(owner);

        BasketReferenceItem[] items = {
            AbstractBasketControllerTest.generateItem(WHITE, ReferenceType.PRODUCT, "1"),
            AbstractBasketControllerTest.generateItem(WHITE, ReferenceType.PRODUCT, "2"),
        };

        for (BasketReferenceItem item : items) {
            item.setOwnerId(ownerId);
            basketService.addItem(item, owner);
        }

        basketService.bulkDelete(
            ownerId, WHITE,
            Stream.of(items).map(BasketReferenceItem::getId).collect(Collectors.toList()),
            owner, null
        );

        pgaasJdbcTemplate.update(
            "update basket_items_archive\n" +
                "set del_time = now() - interval '6' month - interval '1' day\n" +
                "where id = ?",
            items[0].getId()
        );
        pgaasJdbcTemplate.update(
            "update basket_items_archive\n" +
                "set del_time = now() - interval '6' month + interval '1' day\n" +
                "where id = ?",
            items[1].getId()
        );


        job.cleanOldArchiveItems();

        assertEquals(
            List.of(items[1].getId()),
            pgaasJdbcTemplate.queryForList("select id from basket_items_archive order by id", Long.class)
        );
    }
}
