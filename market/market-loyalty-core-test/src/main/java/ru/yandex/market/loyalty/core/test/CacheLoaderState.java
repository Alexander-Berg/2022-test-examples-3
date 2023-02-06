package ru.yandex.market.loyalty.core.test;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CacheLoaderState {
    private volatile boolean cacheLoaded = false;

    public void cacheIsLoaded() {
        cacheLoaded = true;
    }

    public boolean isCacheLoaded() {
        return cacheLoaded;
    }
}
