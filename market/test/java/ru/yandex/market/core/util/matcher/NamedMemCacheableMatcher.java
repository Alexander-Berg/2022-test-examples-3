package ru.yandex.market.core.util.matcher;

import java.util.Objects;

import org.mockito.ArgumentMatcher;

import ru.yandex.common.cache.memcached.cacheable.NamedMemCacheable;

/**
 * Именной {@link ArgumentMatcher} для {@link NamedMemCacheable}.
 *
 * @author avetokhin 17/01/17.
 */
public class NamedMemCacheableMatcher implements ArgumentMatcher<NamedMemCacheable> {

    private final String name;

    private NamedMemCacheableMatcher(final String name) {
        this.name = name;
    }

    /**
     * Создает матчер, который матчится, если проверяемое имя эквивалентно имени Cacheable
     * <code>operand</code>.
     * <br/>
     * Пример:
     * <pre>
     * assertThat(myCacheable, hasName("test"));
     * </pre>
     */
    public static NamedMemCacheableMatcher hasName(final String operand) {
        return new NamedMemCacheableMatcher(operand);
    }

    @Override
    public boolean matches(final NamedMemCacheable cacheable) {
        final String cacheableName = cacheable.name();
        return Objects.equals(cacheableName, this.name);
    }
}
