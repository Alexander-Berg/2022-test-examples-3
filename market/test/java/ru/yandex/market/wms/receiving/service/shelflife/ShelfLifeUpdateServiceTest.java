package ru.yandex.market.wms.receiving.service.shelflife;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.mbo.http.MdmShelfLife;
import ru.yandex.market.mbo.http.MdmShelfLifeService;
import ru.yandex.market.wms.common.model.dto.SkuShelfLifeInfoHolder;
import ru.yandex.market.wms.common.service.ShelfLifeService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.ShelfLifeDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.exception.ShelfLifeControlChangeProhibitedException;
import ru.yandex.market.wms.common.spring.service.GoldSkuService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelfLifeUpdateServiceTest extends BaseTest {
    @Mock
    private ShelfLifeService shelfLifeService;
    @Mock
    private SecurityDataProvider securityDataProvider;
    @Mock
    private ShelfLifeDao shelfLifeDao;
    @Mock
    private GoldSkuService goldSkuService;
    @Mock
    private MdmShelfLifeService mdmShelfLifeService;

    @Mock
    private SkuDaoImpl skuDao;

    private ShelfLifeUpdateService service;

    @BeforeEach
    void before() {
        this.service = new ShelfLifeUpdateService(
                shelfLifeService,
                securityDataProvider,
                shelfLifeDao,
                goldSkuService,
                mdmShelfLifeService,
                skuDao);
    }

    @Test
    void validateCanChangeShelfLifeIndicatorSwapToTrueProhibited() {
        SkuShelfLifeInfoHolder existed = SkuShelfLifeInfoHolder.builder()
                .shelfLifeControl(false)
                .manufacturerSku("SKU")
                .storerKey("123")
                .build();
        SkuShelfLifeInfoHolder updated = SkuShelfLifeInfoHolder.builder()
                .shelfLifeControl(true)
                .manufacturerSku("SKU")
                .storerKey("123")
                .build();

        when(mdmShelfLifeService
                .allowToUpdateShelfLifeRequiredFlag(any(MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.class)))
                .thenReturn(MdmShelfLife.AllowToUpdateShelfLifeRequiredResponse.newBuilder()
                        .setAllowToSetTrue(false)
                        .setAllowToSetFalse(false)
                        .build());

        assertions.assertThatThrownBy(() -> service.validateCanChangeShelfLifeIndicator(existed, updated))
                .isExactlyInstanceOf(ShelfLifeControlChangeProhibitedException.class);
    }

    @Test
    void validateCanChangeShelfLifeIndicatorSwapToFalseProhibited() {
        SkuShelfLifeInfoHolder existed = SkuShelfLifeInfoHolder.builder()
                .shelfLifeControl(true)
                .manufacturerSku("SKU")
                .storerKey("123")
                .build();
        SkuShelfLifeInfoHolder updated = SkuShelfLifeInfoHolder.builder()
                .shelfLifeControl(false)
                .manufacturerSku("SKU")
                .storerKey("123")
                .build();

        when(mdmShelfLifeService
                .allowToUpdateShelfLifeRequiredFlag(any(MdmShelfLife.AllowToUpdateShelfLifeRequiredRequest.class)))
                .thenReturn(MdmShelfLife.AllowToUpdateShelfLifeRequiredResponse.newBuilder()
                        .setAllowToSetTrue(false)
                        .setAllowToSetFalse(false)
                        .build());

        assertions.assertThatThrownBy(() -> service.validateCanChangeShelfLifeIndicator(existed, updated))
                .isExactlyInstanceOf(ShelfLifeControlChangeProhibitedException.class);
    }
}
