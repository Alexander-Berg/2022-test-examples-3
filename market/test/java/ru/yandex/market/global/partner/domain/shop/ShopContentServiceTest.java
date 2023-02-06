package ru.yandex.market.global.partner.domain.shop;

import java.io.ByteArrayInputStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.LegalEntity;
import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.market.global.partner.util.TestPartnerFactory.CreateShopBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopContentServiceTest extends BaseLocalTest {
    private final ShopContentService shopContentService;
    private final TestPartnerFactory testPartnerFactory;

    @Test
    public void testUploadPicture() {
        ShopModel shopModel = testPartnerFactory.createShopAndAllRequired();
        shopContentService.updateShopPicture(
                shopModel.getShop().getId(),
                new ByteArrayInputStream("test".getBytes(UTF_8))
        );
    }

    @Test
    public void testUploadCatalog() {
        Business business = testPartnerFactory.createBusiness(b -> b
                .setId(11379943L)
        );

        LegalEntity legalEntity = testPartnerFactory.createLegalEntity(
                business.getId()
        );

        ShopModel shopModel = testPartnerFactory.createShop(
                business.getId(),
                legalEntity.getId(),
                CreateShopBuilder.builder()
                        .setupShop(s -> s.setId(11441388L))
                        .build()
        );

        shopContentService.updateOffers(
                shopModel.getShop().getId(),
                false,
                new ByteArrayInputStream((
                        "QQQ00012,smartphones,bebebe,some cool model,https://d3m9l0v76dty0.cloudfront.net/system/photos/7661138/original/b2424f62f0bf5e6ea76d5c1502c881eb.jpg,\"22,22\"\n" +
                        "QQQ00013,бетономешалки,bububu,some cool model,https://d3m9l0v76dty0.cloudfront.net/system/photos/7661138/original/b2424f62f0bf5e6ea76d5c1502c881eb.jpg,\"33,33$\"\n"
                ).getBytes(UTF_8))
        );
    }

    @Test
    public void testUpload() {
        shopContentService.updateShopPicture(
                123,
                new ByteArrayInputStream("test".getBytes(UTF_8))
        );
    }
}
