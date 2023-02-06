package ru.yandex.market.wms.common.spring.service.unit;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.dto.SkuAndPackDTO;
import ru.yandex.market.wms.common.model.dto.SkuShelfLifeInfoHolder;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.ShelfLifeDao;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.service.ExpirationDateOnlyShelfLifeCalculator;
import ru.yandex.market.wms.common.spring.service.SkuService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest extends IntegrationTest {
    @Mock
    SkuDaoImpl skuDao;
    @Mock
    ExpirationDateOnlyShelfLifeCalculator expirationDateOnlyShelfLifeCalculator;
    @Mock
    ShelfLifeDao shelfLifeDao;
    @Mock
    SecurityDataProvider securityDataProvider;

    // в том случае если List<SkuAndPackDTO> skus содержит записи с одинаковыми
    //sku, storer дубли не должны записываться в БД
    @Test
    public void updateSkuTest() {
        SkuService skuService = new SkuService(skuDao, skuDao, expirationDateOnlyShelfLifeCalculator,
                shelfLifeDao, securityDataProvider);

        List<SkuAndPackDTO> skus = new ArrayList<>();
        SkuAndPackDTO skuAndPackDTO1 = new SkuAndPackDTO();
        skuAndPackDTO1.setSku("SKU_1");
        skuAndPackDTO1.setStorerkey("STORER_KEY_1");
        skuAndPackDTO1.setShelflifeindicator("N");
        skuAndPackDTO1.setShelflifecodetype("M");

        SkuAndPackDTO skuAndPackDTO2 = new SkuAndPackDTO();
        skuAndPackDTO2.setSku("SKU_1");
        skuAndPackDTO2.setStorerkey("STORER_KEY_1");
        skuAndPackDTO2.setShelflifeindicator("N");
        skuAndPackDTO2.setShelflifecodetype("M");

        SkuAndPackDTO skuAndPackDTO3 = new SkuAndPackDTO();
        skuAndPackDTO3.setSku("SKU_2");
        skuAndPackDTO3.setStorerkey("STORER_KEY_2");
        skuAndPackDTO3.setShelflifeindicator("N");
        skuAndPackDTO3.setShelflifecodetype("M");

        skus.add(skuAndPackDTO1);
        skus.add(skuAndPackDTO2);
        skus.add(skuAndPackDTO3);

        SkuShelfLifeInfoHolder skuShelfLifeInfoHolder1 = new SkuShelfLifeInfoHolder();
        skuShelfLifeInfoHolder1.setSku("SKU_1");
        skuShelfLifeInfoHolder1.setStorerKey("STORER_KEY_1");

        SkuShelfLifeInfoHolder skuShelfLifeInfoHolder2 = new SkuShelfLifeInfoHolder();
        skuShelfLifeInfoHolder2.setSku("SKU_2");
        skuShelfLifeInfoHolder2.setStorerKey("STORER_KEY_2");

        SkuId skuId1 = new SkuId("STORER_KEY_1", "SKU_1");
        SkuId skuId2 = new SkuId("STORER_KEY_2", "SKU_2");

        Mockito.when(shelfLifeDao.getShelflifeInfoBySkuId(any())).thenAnswer(invocation -> {
            SkuId skuId = (SkuId) invocation.getArguments()[0];
            if (skuId.equals(skuId1)) {
                return skuShelfLifeInfoHolder1;
            }

            if (skuId.equals(skuId2)) {
                return skuShelfLifeInfoHolder2;
            }

            throw new RuntimeException("Unknown skuId");
        });

        Mockito.when(securityDataProvider.getUser()).thenReturn("test_user");

        skuService.updateSku(skus, "test");

        Mockito.verify(shelfLifeDao, Mockito.times(2))
                .writeChangeSkuHistory(any(), any(), any());
        Mockito.verify(skuDao, Mockito.times(2))
                .updateSkus(any(), any());
    }


}
