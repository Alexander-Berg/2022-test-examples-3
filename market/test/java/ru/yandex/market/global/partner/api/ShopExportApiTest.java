package ru.yandex.market.global.partner.api;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.global.partner.BaseApiTest;
import ru.yandex.market.global.partner.domain.shop.model.ExtendedShopModel;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.market.global.partner.mapper.EntityMapper;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.mj.generated.server.model.ListShopsExportResponseDto;
import ru.yandex.mj.generated.server.model.ShopExportDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopExportApiTest extends BaseApiTest {

    private final ShopExportApiService shopExportApiService;

    private final TestPartnerFactory testPartnerFactory;



    private ExtendedShopModel fromShopModel(ShopModel shop) {
        return EntityMapper.MAPPER.toExtendedShopModel(shop.getShop(), shop.getLegalEntity(), shop.getSchedule(),
                shop.getPermissions(), List.of());
    }

    @Test
    public void testAllShop() {
        ShopModel shop1 = testPartnerFactory.createShopAndAllRequired();
        ShopModel shop2 = testPartnerFactory.createShopAndAllRequired();
        ShopModel shop3 = testPartnerFactory.createShopAndAllRequired();

        ResponseEntity<ListShopsExportResponseDto> rsp = shopExportApiService.apiV1ShopExportAllGet();
        Assertions.assertThat(rsp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(rsp.getBody()).isNotNull();
        Assertions.assertThat(rsp.getBody().getItems()).containsAll(List.of(
                EntityMapper.MAPPER.toShopExportDto(fromShopModel(shop1)),
                EntityMapper.MAPPER.toShopExportDto(fromShopModel(shop2)),
                EntityMapper.MAPPER.toShopExportDto(fromShopModel(shop3))
        ));
    }

    @Test
    public void testOneShop() {
        ShopModel shop1 = testPartnerFactory.createShopAndAllRequired();
        ShopModel shop2 = testPartnerFactory.createShopAndAllRequired();
        ShopModel shop3 = testPartnerFactory.createShopAndAllRequired();

        ResponseEntity<ShopExportDto> rsp = shopExportApiService.apiV1ShopExportGet(shop2.getShop().getId());
        Assertions.assertThat(rsp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(rsp.getBody()).isNotNull().usingRecursiveComparison().isEqualTo(
                EntityMapper.MAPPER.toShopExportDto(fromShopModel(shop2))
        );
    }
}
