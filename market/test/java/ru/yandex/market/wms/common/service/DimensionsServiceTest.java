package ru.yandex.market.wms.common.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.dao.PackDao;
import ru.yandex.market.wms.common.dao.SkuDao;
import ru.yandex.market.wms.common.model.dto.SaveDimensionsDTO;
import ru.yandex.market.wms.common.pojo.Dimensions;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DimensionsServiceTest {

    @Mock
    private PackDao packDao;

    @Mock
    private PackDao enterprisePackDao;

    @Mock
    private SkuDao skuDao;

    @Mock
    private SkuDao enterpriseSkuDao;

    private DimensionsService service;

    @BeforeEach
    public void init() {
        service = new DimensionsService(packDao, enterprisePackDao, skuDao, enterpriseSkuDao);
    }

    @Test
    public void correctDimensionsRounding() {
        String packKey = "PACK";
        when(packDao.checkExistence(packKey)).thenReturn(false);
        SaveDimensionsDTO saveDimensionsDTO = new SaveDimensionsDTO.SaveDimensionsDTOBuilder()
            .sku("SKU")
            .storerKey("STORER")
            .width(1.23456789f)
            .height(2.5555555555f)
            .length(4.11111111f)
            .weight(5.333333333f)
            .build();
        service.updatePackAndSku(saveDimensionsDTO, packKey, "user");

        Dimensions expectedDimensions = new Dimensions.DimensionsBuilder()
            .width(BigDecimal.valueOf(1.2346))
            .height(BigDecimal.valueOf(2.5556))
            .length(BigDecimal.valueOf(4.1111))
            .weight(BigDecimal.valueOf(5.3333))
            .cube(BigDecimal.valueOf(12.9711))
            .build();
        verify(packDao).checkExistence(packKey);
        verify(enterprisePackDao).copyFromStdPack(packKey, expectedDimensions);
        verify(packDao).copyFromStdPack(packKey, expectedDimensions);
        verify(enterpriseSkuDao).updateSkuPack("STORER", "SKU", packKey, expectedDimensions, "user");
        verify(skuDao).updateSkuPack("STORER", "SKU", packKey, expectedDimensions, "user");
    }
}
