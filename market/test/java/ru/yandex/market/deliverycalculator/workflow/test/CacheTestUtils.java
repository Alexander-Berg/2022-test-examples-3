package ru.yandex.market.deliverycalculator.workflow.test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.ApplicationContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.ReflectionUtils;

import ru.yandex.market.deliverycalculator.workflow.RegionCache;
import ru.yandex.market.deliverycalculator.workflow.TariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.WarehouseCache;
import ru.yandex.market.deliverycalculator.workflow.service.MetaDataCacheService;
import ru.yandex.market.deliverycalculator.workflow.service.SenderSettingsCacheService;
import ru.yandex.market.deliverycalculator.workflow.service.ShopSettingsCacheService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonTestUtil;
import ru.yandex.market.deliverycalculator.workflow.util.cache.VersionedCacheImpl;

/**
 * Утилитный класс для сброса кэш-данных в Spring'овых компонентах.
 */
@ParametersAreNonnullByDefault
public final class CacheTestUtils {

    private CacheTestUtils() {
        throw new UnsupportedOperationException();
    }

    /*
     * Так как ДБюнит накатывается после создания бинов, то вызываемый после создания бина afterPropertiesSet()
     * рефрешит кэш значениями из пустой базы. Чтобы рефрэшнуть значениями из наполненной базы - повторно вызываем
     * afterPropertiesSet()
     */
    public static void cleanDictionaryCache(ApplicationContext context) {
        cleanRegionCache(context);
        cleanWarehouseCache(context);
    }

    public static void cleanSolomonCache(ApplicationContext context) {
        BoilingSolomonService bean = context.getBean(BoilingSolomonService.class);
        BoilingSolomonTestUtil.clearSensors(bean);
    }

    public static void cleanRegionCache(ApplicationContext context) {
        context.getBean(RegionCache.class).refresh();
    }

    private static void cleanWarehouseCache(ApplicationContext context) {
        context.getBean(WarehouseCache.class).refresh();
    }

    public static void cleanTariffCaches(ApplicationContext context) {
        // Очистка кэша search engine, чтобы тесты не влияли друг на друга
        Map<String, ?> beans = context.getBeansOfType(TariffWorkflow.class);
        for (Object bean : beans.values()) {
            setValue(bean, "tariffCache", new VersionedCacheImpl<>());
            setValue(bean, "actualDeliveryInfoCache", new ConcurrentHashMap<>());
        }
    }

    public static void cleanMetaDataCacheService(ApplicationContext context) {
        cleanMetaDataCacheService(context, ShopSettingsCacheService.class);
        cleanMetaDataCacheService(context, SenderSettingsCacheService.class);
    }

    public static void cleanFeedParserWorkflowService(ApplicationContext context) {
        // Очистка счетчиков поколений, чтобы тесты не влияли друг на друга
        Object bean = AopTestUtils.getTargetObject(context.getBean("feedParserWorkflowService"));
        List<String> atomicFieldNames = Arrays.asList(
                "activeExternalGenerationId", "nextGenerationId", "nextOutdatedExternalGenerationId");

        for (String fieldName : atomicFieldNames) {
            setValue(bean, fieldName, new AtomicLong());
        }

        setValue(bean, "feedSourceCache", new VersionedCacheImpl<>());
    }

    private static void setValue(Object bean, String fieldName, Object value) {
        try {
            Class<?> clazz = bean.getClass();
            Field field = ReflectionUtils.findField(clazz, fieldName);

            if (field == null) {
                throw new RuntimeException(String.format("Could not find field %s in %s", fieldName, clazz));
            }

            field.setAccessible(true);
            field.set(bean, value);
        } catch (final Exception ex) {
            throw new RuntimeException(String.format("Could not setup %s to %s in %s", value, bean, fieldName), ex);
        }
    }

    private static void cleanMetaDataCacheService(
            ApplicationContext context, Class<? extends MetaDataCacheService<?, ?>> clazz
    ) {
        var bean = AopTestUtils.getTargetObject(context.getBean(clazz));
        setValue(bean, "cache", new VersionedCacheImpl<>());
    }

}
