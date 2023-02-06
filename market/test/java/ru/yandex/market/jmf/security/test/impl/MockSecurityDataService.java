package ru.yandex.market.jmf.security.test.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.security.SecurityConstants;
import ru.yandex.market.jmf.security.SecurityDataService;

public class MockSecurityDataService implements SecurityDataService {

    private Entity currentEmployee;
    private Entity initialEmployee;
    private List<String> currentUserProfiles;

    public MockSecurityDataService() {
        this.reset();
    }

    public void reset() {
        currentEmployee = null;
        initialEmployee = null;
        currentUserProfiles = List.of(SecurityConstants.Profiles.SUPERUSER_PROFILE_ID);
    }

    @Override
    public Stream<String> getCurrentUserProfiles() {
        return currentUserProfiles.stream();
    }

    public void setCurrentUserProfiles(List<String> profiles) {
        currentUserProfiles = profiles;
    }

    public void setCurrentUserProfiles(String... profiles) {
        currentUserProfiles = Arrays.asList(profiles);
    }

    public void setCurrentUserProfile(String profile) {
        this.setCurrentUserProfiles(List.of(profile));
    }

    @Override
    public <T extends Entity> T getCurrentEmployee() {
        return (T) currentEmployee;
    }

    public <T extends Entity> void setCurrentEmployee(T employee) {
        currentEmployee = employee;
    }

    @Override
    public <T extends Entity> T getCurrentNonRobotEmployee() {
        return (T) initialEmployee;
    }

    public <T extends Entity> void setInitialEmployee(T employee) {
        initialEmployee = employee;
    }

}
