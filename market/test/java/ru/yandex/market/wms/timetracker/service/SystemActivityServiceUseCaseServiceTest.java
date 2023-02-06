package ru.yandex.market.wms.timetracker.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.wms.timetracker.config.TestAsyncConfig;
import ru.yandex.market.wms.timetracker.dto.NotificationMessage;
import ru.yandex.market.wms.timetracker.dto.enums.WarehouseName;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.SystemActivityAssignRequest;
import ru.yandex.market.wms.timetracker.response.WmsTaskRouterAssignTaskRequest;
import ru.yandex.market.wms.timetracker.service.impl.SystemActivityServiceImpl;
import ru.yandex.market.wms.timetracker.service.impl.SystemActivityServiceTaskRouterAssign;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TestAsyncConfig.class,
        SystemActivityServiceUseCaseService.class,
        SystemActivityServiceImpl.class,
        SystemActivityServiceTaskRouterAssign.class,
        SystemActivityWithNotificationService.class,
        SystemActivityServiceCompleteIndirectActivity.class,

})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SystemActivityServiceUseCaseServiceTest {

    @Autowired
    private SystemActivityServiceUseCaseService useCaseService;

    @MockBean
    public SystemActivityServiceImpl systemActivityServiceImpl;

    @MockBean
    public WmsTaskRouterClient wmsTaskRouterClient;

    @MockBean
    public WarehouseService warehouseService;

    @MockBean
    public NotificationSender notificationSender;

    @MockBean
    public WmsCoreClient wmsCoreClient;

    @BeforeEach
    void init() {
        Mockito.reset(wmsTaskRouterClient, wmsCoreClient, notificationSender, systemActivityServiceImpl);
    }

    @Test
    public void assign() {

        Mockito.when(warehouseService.warehouseNameEnum(Mockito.any(String.class)))
                .thenReturn(Optional.of(WarehouseName.SOF));

        useCaseService.assign(
                "testWarehouseName",
                EmployeeStatus.CONSOLIDATION,
                SystemActivityAssignRequest.builder()
                        .assigner("assigner")
                        .zone("testZone")
                        .users(List.of("testUser"))
                        .build());

        Mockito.verify(notificationSender, Mockito.times(1)).sendNotification(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(NotificationMessage.class)
        );

        Mockito.verify(wmsTaskRouterClient, Mockito.times(1)).assignTask(
                Mockito.any(WmsTaskRouterAssignTaskRequest.class),
                Mockito.any(WarehouseName.class)
        );

        Mockito.verify(systemActivityServiceImpl, Mockito.times(1)).assign(
                Mockito.any(String.class),
                Mockito.any(EmployeeStatus.class),
                Mockito.any(SystemActivityAssignRequest.class)
        );
    }

    @Test
    void finish() {

        Mockito.when(warehouseService.warehouseNameEnum(Mockito.any(String.class)))
                .thenReturn(Optional.of(WarehouseName.SOF));

        useCaseService.finish("", List.of("test"));

        Mockito.verify(wmsCoreClient, Mockito.times(1)).completeIndirectActivity(
                Mockito.anyCollection(),
                Mockito.any(WarehouseName.class)
        );

        Mockito.verify(systemActivityServiceImpl, Mockito.times(1)).finish(
                Mockito.any(String.class),
                Mockito.anyCollection());
    }
}
