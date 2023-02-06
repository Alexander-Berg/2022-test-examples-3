package ru.yandex.market.jmf.security.test.impl;

import java.util.List;

import org.springframework.security.core.Authentication;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.security.AuthRunnerService;

public class MockAuthRunnerService implements AuthRunnerService {

    private boolean isCurrentUserSuperUser;

    public MockAuthRunnerService() {
        this.reset();
    }

    @Override
    public boolean isCurrentUserSuperUser() {
        return isCurrentUserSuperUser;
    }

    public void setCurrentUserSuperUser(boolean isCurrentUserSuperUser) {
        this.isCurrentUserSuperUser = isCurrentUserSuperUser;
    }

    public void reset() {
        this.isCurrentUserSuperUser = true;
    }

    @Override
    public void runAsSuperUser(Exceptions.TrashRunnable action) {
        doAsSuperUser(() -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T doAsSuperUser(Exceptions.TrashSupplier<T> action) {
        boolean oldValue = isCurrentUserSuperUser;
        try {
            isCurrentUserSuperUser = true;
            return action.get();
        } finally {
            isCurrentUserSuperUser = oldValue;
        }
    }

    @Override
    public void runAsLastRealUser(Exceptions.TrashRunnable action) {
        action.run();
    }

    @Override
    public <T> T doAsLastRealUser(Exceptions.TrashSupplier<T> action) {
        return action.get();
    }

    @Override
    public List<Authentication> getAuthenticationsFromInitialToCurrent() {
        return null;
    }

}

