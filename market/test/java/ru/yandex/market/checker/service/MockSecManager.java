package ru.yandex.market.checker.service;

import ru.yandex.market.security.SecManager;

public class MockSecManager implements SecManager {
    @Override
    public boolean canDo(String operationName, Object data) {
        return true;
    }

    @Override
    public boolean hasAuthority(String authority, String param, Object data) {
        return true;
    }
}
