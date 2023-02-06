package ru.yandex.market.wms.picking.modules.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.achievement.model.metric.PickingAdultItemsMetric;
import ru.yandex.market.wms.achievement.model.metric.PickingElectronicsMetric;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.shared.libs.achievement.metrics.service.AchievementMetricsSenderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PickingCargotypeTrackingServiceTest extends IntegrationTest {

    @MockBean
    @Autowired
    private AchievementMetricsSenderService achievementMetricsSenderService;

    @Autowired
    private PickingCargotypeTrackingService underTest;

    private static final String USER_ID = "000";
    private static final TaskDetail TASK_DETAIL = TaskDetail.builder()
            .sku("TEST0000000000000000000")
            .storerKey("909090909")
            .build();

    @BeforeEach
    void beforeEach() {
        Mockito.reset(achievementMetricsSenderService);
    }

    @Test
    @DatabaseSetup(value = "/service/check-cargotype-taskdetail/1/before.xml")
    void tryCheckCargotypeTaskDetailEmpty() {

        underTest.checkCargotypeTaskDetail(USER_ID, TASK_DETAIL);

        Mockito.verify(achievementMetricsSenderService, Mockito.never())
                .sendAchievementMetrics(any(PickingElectronicsMetric.class), eq(USER_ID));
        Mockito.verify(achievementMetricsSenderService, Mockito.never())
                .sendAchievementMetrics(any(PickingAdultItemsMetric.class), eq(USER_ID));

    }

    @Test
    @DatabaseSetup(value = "/service/check-cargotype-taskdetail/2/before.xml")
    void tryCheckCargotypeTaskDetailOfElectronic() {

        underTest.checkCargotypeTaskDetail(USER_ID, TASK_DETAIL);

        Mockito.verify(achievementMetricsSenderService)
                .sendAchievementMetrics(any(PickingElectronicsMetric.class), eq(USER_ID));
        Mockito.verify(achievementMetricsSenderService, Mockito.never())
                .sendAchievementMetrics(any(PickingAdultItemsMetric.class), eq(USER_ID));

    }

    @Test
    @DatabaseSetup(value = "/service/check-cargotype-taskdetail/3/before.xml")
    void tryCheckCargotypeTaskDetailOfAdultItems() {

        underTest.checkCargotypeTaskDetail(USER_ID, TASK_DETAIL);

        Mockito.verify(achievementMetricsSenderService, Mockito.never())
                .sendAchievementMetrics(any(PickingElectronicsMetric.class), eq(USER_ID));
        Mockito.verify(achievementMetricsSenderService)
                .sendAchievementMetrics(any(PickingAdultItemsMetric.class), eq(USER_ID));

    }

    @Test
    @DatabaseSetup(value = "/service/check-cargotype-taskdetail/4/before.xml")
    void tryCheckCargotypeTaskDetailBothTypes() {

        underTest.checkCargotypeTaskDetail(USER_ID, TASK_DETAIL);

        Mockito.verify(achievementMetricsSenderService)
                .sendAchievementMetrics(any(PickingElectronicsMetric.class), eq(USER_ID));
        Mockito.verify(achievementMetricsSenderService)
                .sendAchievementMetrics(any(PickingAdultItemsMetric.class), eq(USER_ID));

    }

}
