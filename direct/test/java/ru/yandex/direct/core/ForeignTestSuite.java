package ru.yandex.direct.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AllModelPropsCorrespondToBeanProps;
import ru.yandex.direct.model.AllModelsCanBeInitialized;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AllModelsCanBeInitialized.class
})
public class ForeignTestSuite {
    public static class AllModelPropsCorrespondToBeanPropsDescendant extends AllModelPropsCorrespondToBeanProps {
        private static final Map<Class<?>, Supplier<?>> CUSTOM_CLASS_FACTORIES = new HashMap<>();

        static {
            CUSTOM_CLASS_FACTORIES.put(AdGroup.class, TextAdGroup::new);
            CUSTOM_CLASS_FACTORIES.put(Percent.class, () -> Percent.fromPercent(BigDecimal.TEN));
            CUSTOM_CLASS_FACTORIES.put(ClientId.class, () -> ClientId.fromNullableLong(42L));
        }

        @Override
        protected Map<Class<?>, Supplier<?>> getCustomClassFactories() {
            return CUSTOM_CLASS_FACTORIES;
        }
    }
}
