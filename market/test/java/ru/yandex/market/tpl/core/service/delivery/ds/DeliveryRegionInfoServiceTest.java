package ru.yandex.market.tpl.core.service.delivery.ds;

import java.time.Clock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.api.model.usershift.location.DSRegionYtDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.region.TplRegionService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.DeliveryRegionInfoService;
import ru.yandex.market.tpl.core.domain.usershift.location.RegionBordersService;
import ru.yandex.market.tpl.core.service.user.UserDtoMapper;

public class DeliveryRegionInfoServiceTest {

    private final Clock clock = Mockito.mock(Clock.class);
    private final RegionBordersService regionBordersService = Mockito.mock(RegionBordersService.class);
    private final DsRepository dsRepository = Mockito.mock(DsRepository.class);

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final UserDtoMapper userDtoMapper = Mockito.mock(UserDtoMapper.class);
    private final MovementRepository movementRepository = Mockito.mock(MovementRepository.class);
    private final OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private final ConfigurationProviderAdapter configurationProviderAdapter =
            Mockito.mock(ConfigurationProviderAdapter.class);

    private final TplRegionService tplRegionService = Mockito.mock(TplRegionService.class);
    private final DeliveryServiceRegionRepository deliveryServiceRegionRepository =
            Mockito.mock(DeliveryServiceRegionRepository.class);
    private final SortingCenterService sortingCenterService = Mockito.mock(SortingCenterService.class);

    private DeliveryRegionInfoService deliveryRegionInfoService = new DeliveryRegionInfoService(clock,
            regionBordersService, dsRepository,
            userRepository, userDtoMapper, movementRepository, configurationProviderAdapter,
            tplRegionService, deliveryServiceRegionRepository,
            sortingCenterService, orderRepository);

    @Test
    public void getFullFalttenSubtreeNoException() {
        RegionTree regionTree = new RegionTree(new Region(1, "123", RegionType.REGION, null));
        Mockito.when(tplRegionService.getOrBuildRegionTree()).thenReturn(regionTree);
        Mockito.when(tplRegionService.getRegionTree()).thenReturn(regionTree);
        DSRegionYtDto regionDto = new DSRegionYtDto(0, 0, true, true);
        Assertions.assertDoesNotThrow(() -> deliveryRegionInfoService.getFullFlattenSubtree(regionDto));
    }
}
