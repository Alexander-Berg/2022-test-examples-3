package ru.yandex.market.abo.web.controller.resupply.resupply;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.resupply.entity.ResupplyEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttrInclusionEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyStatus;
import ru.yandex.market.abo.core.resupply.repo.ResupplyItemAttrInclusionRepository;
import ru.yandex.market.abo.core.resupply.repo.ResupplyItemRepository;
import ru.yandex.market.abo.core.resupply.repo.ResupplyRepository;
import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;
import ru.yandex.market.checkout.checkouter.order.SupplierType;

/**
 * @author imelnikov
 * @since 01.11.2021
 */
class ResupplyRepositoryTest extends EmptyTest {

    @Autowired
    EntityManager entityManager;
    @Autowired
    ResupplyRepository resupplyRepo;
    @Autowired
    ResupplyItemRepository itemRepo;
    @Autowired
    ResupplyItemAttrInclusionRepository attrRepo;


    @Test
    public void repo() {
        var resupply = new ResupplyEntity();
        resupply.setStatus(ResupplyStatus.DRAFT);
        resupply.setUid(1L);
        resupply = resupplyRepo.save(resupply);

        var item = ResupplyItemEntity.builder()
                .resupply(resupply)
                .orderId(1L)
                .orderItemId(2L)
                .supplierId(3L)
                .marketSku(4L)
                .shopSku("shopSku")
                .categoryId(5)
                .title("title")
                .price(BigDecimal.valueOf(6L))
                .createdAt(LocalDateTime.now())
                .resupplyStock(ResupplyStock.GOOD)
                .supplierTypeId(SupplierType.FIRST_PARTY.getId())
                .build();
        item = itemRepo.save(item);

        attrRepo.saveAndFlush(new ResupplyItemAttrInclusionEntity(ResupplyItemAttr.DEFORMED, item));
        entityManager.clear();
        attrRepo.findByItemId(item.getId());
    }
}
