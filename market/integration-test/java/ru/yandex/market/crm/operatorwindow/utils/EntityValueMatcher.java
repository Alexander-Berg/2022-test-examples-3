package ru.yandex.market.crm.operatorwindow.utils;

import java.util.function.Function;

import org.mockito.ArgumentMatcher;

import ru.yandex.market.jmf.entity.EntityValue;

public class EntityValueMatcher<TEntity> implements ArgumentMatcher<EntityValue> {
    private final Function<TEntity, Boolean> matcher;

    public EntityValueMatcher(Function<TEntity, Boolean> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(EntityValue argument) {
        return matcher.apply((TEntity) argument.unwrapAdaptee());
    }
}
