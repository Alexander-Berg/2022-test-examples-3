package ru.yandex.market.mbo.mdm.common.service;

import java.util.Collection;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmMonitoringResult;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmBusinessMigrationMonitoringService;

public class MdmBusinessMigrationMonitoringServiceMock implements MdmBusinessMigrationMonitoringService {

    @Override
    public void addLockedBusinessesWithSaveRequest(Collection<Integer> supplier) {

    }

    @Override
    public void incrementLockedCounter(Integer value) {
        return;
    }

    @Override
    public void decrementLockedCounter(Integer value) {
        return;
    }

    @Override
    public void incrementOffersToMergeCounter(Integer value) {
        return;
    }

    @Override
    public void decrementOffersToMergeCounter(Integer value) {
        return;
    }

    @Override
    public void removeLockedBusinessesWithSaveRequest(Collection<Integer> supplier) {
        return;
    }

    @Override
    public MdmMonitoringResult getDatacampOffersLogbrokerMonitoringResult() {
        return null;
    }

    @Override
    public MdmMonitoringResult getSaveByLockedBusinessMonitoringResult() {
        return null;
    }

    @Override
    public MdmMonitoringResult getOldLockedSuppliersIsOverThresholdMonitoringResult() {
        return null;
    }

    @Override
    public MdmMonitoringResult getOldRequestedMigrationOffersIsOverThresholdMonitoringResult() {
        return null;
    }

    @Override
    public MdmMonitoringResult getMbiPartnersMonitoringResult() {
        return null;
    }
}
