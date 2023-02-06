package ru.yandex.market.mbi.tariffs.service;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.security.SecManager;

/**
 * Заглушка для {@link SecManager}
 */
@ParametersAreNonnullByDefault
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
