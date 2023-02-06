package ru.yandex.market.antifraud.orders.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.detector.OrderFraudDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetector;
import ru.yandex.market.antifraud.orders.storage.dao.RoleDao;
import ru.yandex.market.antifraud.orders.storage.entity.DetectorDescription;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RoleServiceTest {

    @Mock
    private RoleDao roleDao;
    private List<FraudDetector> detectors;
    private RoleService roleService;

    @Before
    public void init() {
        LoyaltyDetector loyaltyDetector = mockDetector("loyalty_detector", LoyaltyDetector.class);
        OrderFraudDetector orderDetector = mockDetector("order_detector", OrderFraudDetector.class);
        detectors = Arrays.asList(loyaltyDetector, orderDetector);
        roleService = new RoleService(roleDao, detectors);
        List<DetectorDescription> descriptions =
                detectors.stream().map(FraudDetector::getDescription).collect(Collectors.toList());
        when(roleDao.saveDetectorDescriptionIfAbsent(any(DetectorDescription.class))).then(i -> i.getArguments()[0]);
        when(roleDao.getDetectorDescriptions())
                .thenReturn(descriptions);
        roleService.init();
    }


    @Test
    public void testInit() {
        verify(roleDao, times(2)).saveDetectorDescriptionIfAbsent(any(DetectorDescription.class));
        verify(roleDao).getDetectorDescriptions();
    }

    @Test
    public void getRoleByUid() {
        when(roleDao.getRoleByUid(anyString())).thenReturn(Optional.of(
                BuyerRole.builder()
                        .name("test_role")
                        .detectorConfigurations(ImmutableMap.of("loyalty_detector", new BaseDetectorConfiguration(false)))
                        .build()
        ));
        Optional<BuyerRole> roleO = roleService.getRoleByUid("1234");
        assertThat(roleO).isPresent();
        BuyerRole role = roleO.get();
        assertThat(role.getConfigurationForRule("loyalty_detector").isEnabled())
                .isEqualTo(false);
        assertThat(role.getConfigurationForRule("order_detector").isEnabled())
                .isEqualTo(true);
    }

    private <T extends FraudDetector> T mockDetector(String name, Class<T> detectorClass) {
        T detector = mock(detectorClass);
        when(detector.getUniqName()).thenReturn(name);
        when(detector.getDetectorType()).thenCallRealMethod();
        when(detector.defaultConfiguration()).thenCallRealMethod();
        when(detector.getDescription()).thenCallRealMethod();
        return detector;
    }

}