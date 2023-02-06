package ru.yandex.market.abo.core.resupply;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.resupply.entity.ResupplyEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyOperator;
import ru.yandex.market.abo.core.resupply.entity.ResupplyType;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.repo.ResupplyItemRepository;
import ru.yandex.market.abo.core.resupply.repo.ResupplyOperatorRepo;
import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;
import ru.yandex.market.abo.util.SpecBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResupplyServiceTest extends EmptyTest {

    @Autowired
    ResupplyService resupplyService;
    @Autowired
    ResupplyOperatorRepo resupplyOperatorRepo;
    @Autowired
    ResupplyItemRepository resupplyItemRepo;
    @Autowired
    EntityManager entityManager;

    @Test
    public void spawn() {
        long userId = getUserId();

        ResupplyEntity resupply = resupplyService.createResupply(userId);
        ResupplyItemEntity resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 1L, 2L);
        resupplyItem.setResupplyStock(ResupplyStock.GOOD);
        resupplyService.saveResupplyItem(resupplyItem);

        resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 4L, 5L);
        resupplyItem.setResupplyStock(ResupplyStock.BAD_3P);
        resupplyService.saveResupplyItem(resupplyItem);

        ResupplyEntity newResupply = resupplyService.spawnNewResupply(resupply.getId(), ResupplyType.BAD);
        assertFalse(newResupply.getId().equals(resupply.getId()));

        List<ResupplyItemEntity> items = resupplyService.findItemsByResupplyId(newResupply.getId());
        assertEquals(1, items.size());
        assertEquals(resupplyItem.getId(), items.get(0).getId());
    }

    private long getUserId() {
        long userId = 3L;
        ResupplyOperator settings = new ResupplyOperator();
        settings.setUserId(userId);
        settings.setWarehouse(Warehouse.TOMILINO);
        resupplyOperatorRepo.save(settings);
        return userId;
    }

    @Test
    public void report() {
        long userId = getUserId();
        ResupplyEntity resupply = resupplyService.createResupply(userId);
        ResupplyItemEntity resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 1L, 2L);

        Specification<ResupplyItemEntity> spec = new SpecBuilder<ResupplyItemEntity>()
                .withCondition(userId, (acc, cb) -> cb.equal(acc.join("resupply").get("uid"), userId))
                .build();
        resupplyItemRepo.findOne(spec);
    }

    @NotNull
    protected static OrderItem orderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setSupplierId(2L);
        orderItem.setSupplierType(SupplierType.FIRST_PARTY);
        orderItem.setOrderId(3L);
        orderItem.setShopSku("");
        orderItem.setMsku(4L);
        orderItem.setCategoryId(5);
        orderItem.getPrices().setBuyerPriceBeforeDiscount(BigDecimal.ONE);
        orderItem.setOfferName("title");
        return orderItem;
    }

    @Test
    public void deleteAttr() {
        long userId = getUserId();

        ResupplyEntity resupply = resupplyService.createResupply(userId);
        ResupplyItemEntity resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 1L, 2L);
        resupplyService.saveResupplyItem(resupplyItem);

        resupplyService.setResupplyItemAttributes(resupplyItem.getId(), Set.of(ResupplyItemAttr.DEFORMED));
        resupplyService.setResupplyItemAttributes(resupplyItem.getId(), Set.of(ResupplyItemAttr.MISSING_PARTS));
        var attrs = resupplyService.getResupplyItemAttributes(resupplyItem.getId());
        assertEquals(1, attrs.size());

        resupplyService.setResupplyItemAttributes(resupplyItem.getId(), Set.of(ResupplyItemAttr.DEFORMED, ResupplyItemAttr.PACKAGE_HOLES));
        attrs = resupplyService.getResupplyItemAttributes(resupplyItem.getId());
        assertEquals(2, attrs.size());
    }

    @Test
    public void deleteItem() {
        long userId = getUserId();

        ResupplyEntity resupply = resupplyService.createResupply(userId);
        ResupplyItemEntity resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 1L, 2L);

        resupplyService.deleteResupplyItem(resupplyItem.getId());
        entityManager.clear();
        assertFalse(resupplyService.findResupplyItem(resupplyItem.getId()).isPresent());
        assertTrue(resupplyService.findItemsByResupplyId(resupply.getId()).isEmpty());
        assertTrue(resupplyService.findResupplyItem(resupply.getId(), resupplyItem.getId()).isEmpty());
    }

}
