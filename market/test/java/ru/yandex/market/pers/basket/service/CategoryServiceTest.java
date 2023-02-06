package ru.yandex.market.pers.basket.service;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.Icon;
import ru.yandex.market.pers.basket.model.Nid;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.UserIdType;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;

import static org.junit.Assert.assertEquals;

/**
 * @author maratik
 */
public class CategoryServiceTest extends PersBasketTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BasketService basketv2Service;

    @Test
    public void testPostEntry() {
        Random rand = new Random();
        String userAnyId = String.valueOf(rand.nextLong());
        BasketOwner owner = BasketOwner.from(UserIdType.UID, userAnyId);
        Long ownerId = basketv2Service.getOrAddOwnerId(owner);

        Icon iconToSave = new Icon(10, 20, "http://example.com");
        Nid nidToSave = new Nid(1, "test", Collections.singletonList(iconToSave));
        List<Nid> savedNids = categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave)
        );
        checkEntries(iconToSave, nidToSave, savedNids);
    }

    @Test
    public void testPostOtherEntry() {
        Random rand = new Random();
        String userAnyId = String.valueOf(rand.nextLong());
        BasketOwner owner = BasketOwner.from(UserIdType.UID, userAnyId);
        Long ownerId = basketv2Service.getOrAddOwnerId(owner);

        Icon iconToSave = new Icon(10, 20, "http://example.com");
        Nid nidToSave1 = new Nid(1, "test", Collections.singletonList(iconToSave));
        categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave1)
        );

        Nid nidToSave2 = new Nid(2, "test", Collections.singletonList(iconToSave));
        List<Nid> savedNids = categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave2)
        );
        checkEntries(iconToSave, nidToSave2, savedNids);
    }

    @Test
    public void testPostSameEntry() {
        Random rand = new Random();
        String userAnyId = String.valueOf(rand.nextLong());
        BasketOwner owner = BasketOwner.from(UserIdType.UID, userAnyId);
        Long ownerId = basketv2Service.getOrAddOwnerId(owner);

        Icon iconToSave = new Icon(10, 20, "http://example.com");
        Nid nidToSave = new Nid(1, "test", Collections.singletonList(iconToSave));
        categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave)
        );
        List<Nid> savedNids = categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave)
        );
        checkEntries(iconToSave, nidToSave, savedNids);
    }

    @Test
    public void testGetEntry() {
        Random rand = new Random();
        String userAnyId = String.valueOf(rand.nextLong());
        BasketOwner owner = BasketOwner.from(UserIdType.UID, userAnyId);
        Long ownerId = basketv2Service.getOrAddOwnerId(owner);

        Icon iconToSave = new Icon(10, 20, "http://example.com");
        Nid nidToSave = new Nid(1, "test", Collections.singletonList(iconToSave));
        categoryService.postEntries(
            ownerId, MarketplaceColor.BLUE, Collections.singletonList(nidToSave)
        );
        List<Nid> savedNids = categoryService.getEntries(
            ownerId, MarketplaceColor.BLUE
        );
        checkEntries(iconToSave, nidToSave, savedNids);
    }

    public static void checkEntries(Icon iconToSave, Nid nidToSave, List<Nid> savedNids) {
        assertEquals(1, savedNids.size());
        Nid savedNid = savedNids.get(0);
        assertEquals(nidToSave.getId(), savedNid.getId());
        assertEquals(nidToSave.getFullName(), savedNid.getFullName());
        List<Icon> savedIcons = savedNid.getIcons();
        assertEquals(1, savedIcons.size());
        Icon savedIcon = savedIcons.get(0);
        assertEquals(iconToSave.getHeight(), savedIcon.getHeight());
        assertEquals(iconToSave.getWidth(), savedIcon.getWidth());
        assertEquals(iconToSave.getUrl(), savedIcon.getUrl());
    }
}
