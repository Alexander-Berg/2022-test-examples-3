package ru.yandex.direct.logicprocessor.processors.bsexport.mobilegoals;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.bstransport.yt.repository.goals.MobileGoalsExternalTrackerYtRepository;
import ru.yandex.direct.common.log.service.LogBsExportEssService;
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsExternalTrackerRepository;
import ru.yandex.direct.ess.logicobjects.bsexport.mobilegoals.BsExportMobileGoalsExternalTrackerObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BsExportMobileGoalsExternalTrackerServiceTest {

    private BsExportMobileGoalsExternalTrackerService bsExportMobileGoalsExternalTrackerService;
    private MobileAppGoalsExternalTrackerRepository mobileAppGoalsExternalTrackerRepository;
    private MobileAppRepository mobileAppRepository;
    private MobileGoalsExternalTrackerYtRepository mobileGoalsExternalTrackerYtRepository;
    private LogBsExportEssService logBsExportEssService;

    @BeforeEach
    void before() {
        mobileAppGoalsExternalTrackerRepository = mock(MobileAppGoalsExternalTrackerRepository.class);
        mobileAppRepository = mock(MobileAppRepository.class);
        mobileGoalsExternalTrackerYtRepository = mock(MobileGoalsExternalTrackerYtRepository.class);
        logBsExportEssService = mock(LogBsExportEssService.class);
        bsExportMobileGoalsExternalTrackerService = new BsExportMobileGoalsExternalTrackerService(
                mobileAppGoalsExternalTrackerRepository, mobileAppRepository, mobileGoalsExternalTrackerYtRepository,
                logBsExportEssService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void modifiedGoalTest() {
        var goalId = 123L;
        long mobileAppId = 345L;
        String bundleId = "bundle222";
        ExternalTrackerEventName eventName = ExternalTrackerEventName.PURCHASED;
        MobileAppStoreType storeType = MobileAppStoreType.APPLEAPPSTORE;

        var bsExportMobileGoalsExternalTrackerObject = new BsExportMobileGoalsExternalTrackerObject(goalId);

        var feedFromDb = new MobileExternalTrackerEvent()
                .withId(goalId)
                .withMobileAppId(mobileAppId)
                .withEventName(eventName);
        when(mobileAppGoalsExternalTrackerRepository.getEventsByIds(anyInt(), anyCollection()))
                .thenReturn(List.of(feedFromDb));

        MobileApp mobileApp = new MobileApp()
                .withId(mobileAppId)
                .withStoreType(storeType)
                .withMobileContent(new MobileContent().withBundleId(bundleId));
        when(mobileAppRepository.getMobileApps(anyInt(), any(), anyCollection())).thenReturn(List.of(mobileApp));

        bsExportMobileGoalsExternalTrackerService.processGoals(1, List.of(bsExportMobileGoalsExternalTrackerObject));

        ArgumentCaptor<Collection> modifyGoalsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(mobileGoalsExternalTrackerYtRepository).modify(modifyGoalsCaptor.capture());
        verify(mobileGoalsExternalTrackerYtRepository, never()).deleteByIds(anyCollection());

        var gotModifiedGoals = (List<ru.yandex.adv.direct.goals.GoalExternalTracker>)
                modifyGoalsCaptor.getValue();

        var expectedGoal = ru.yandex.adv.direct.goals.GoalExternalTracker.newBuilder()
                .setGoalId(goalId)
                .setMobileAppId(mobileAppId)
                .setBundleId(bundleId)
                .setEventName(eventName.toString())
                .setStoreType("AppleAppStore")
                .build();

        assertThat(gotModifiedGoals).hasSize(1);
        assertEquals(gotModifiedGoals.get(0), expectedGoal);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletedGoalTest() {
        var goalId = 123L;
        var bsExportMobileGoalsExternalTrackerObject = new BsExportMobileGoalsExternalTrackerObject(goalId, true);
        bsExportMobileGoalsExternalTrackerService.processGoals(1, List.of(bsExportMobileGoalsExternalTrackerObject));

        ArgumentCaptor<Collection> goalIdsToDeleteCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(mobileGoalsExternalTrackerYtRepository, never()).modify(anyCollection());
        verify(mobileGoalsExternalTrackerYtRepository).deleteByIds(goalIdsToDeleteCaptor.capture());

        var gotGoalIds = (List<Long>) goalIdsToDeleteCaptor.getValue();
        assertThat(gotGoalIds)
                .containsExactlyInAnyOrder(goalId);
    }
}
