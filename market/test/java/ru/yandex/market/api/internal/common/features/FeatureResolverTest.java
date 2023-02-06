package ru.yandex.market.api.internal.common.features;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

public class FeatureResolverTest extends UnitTestBase {
    private final class MockFeatureProvider implements FeatureProvider {
        private final Map<String, String> result;

        public MockFeatureProvider(Map<String, String> result) {
            this.result = result;
        }

        @Override
        public Map<String, String> provide() {
            return result;
        }
    }

    private static final Map<Feature, String> DEFAULT_FEATURES = ImmutableMap.<Feature, String>builder()
            .put(Feature.LOG_AUTHORIZATION_TYPE, Feature.LOG_AUTHORIZATION_TYPE.getDefaultValue())
            .put(Feature.LOG_MEMCACHED_IN_TRACE, Feature.LOG_MEMCACHED_IN_TRACE.getDefaultValue())
            .put(Feature.DISABLE_LOG_INCOMING_SERVICE_QUERY_IN_TRACE, Feature.DISABLE_LOG_INCOMING_SERVICE_QUERY_IN_TRACE.getDefaultValue())
            .put(Feature.MEMCACHED_CLIENT_PROTOCOL, Feature.MEMCACHED_CLIENT_PROTOCOL.getDefaultValue())
            .put(Feature.PERSONAL_ENABLED, Feature.PERSONAL_ENABLED.getDefaultValue())
            .build();

    @Test
    public void nullProviders() {
        FeatureResolver resolver = new FeatureResolver(null);
        Assert.assertEquals(DEFAULT_FEATURES, resolver.resolve());
    }

    @Test
    public void emptyListProviders() {
        FeatureResolver resolver = new FeatureResolver(Collections.emptyList());
        Assert.assertEquals(DEFAULT_FEATURES, resolver.resolve());
    }

    @Test
    public void oneValueChangedInOneProvider() {
        MockFeatureProvider provider = new MockFeatureProvider(
            ImmutableMap.<String, String>builder()
                    .put(Feature.DISABLE_LOG_INCOMING_SERVICE_QUERY_IN_TRACE.getFeatureName(), "/order")
                    .build()
        );
        FeatureResolver resolver = new FeatureResolver(Collections.singletonList(provider));
        Map<Feature, String> features = Maps.newHashMap(DEFAULT_FEATURES);
        features.put(Feature.DISABLE_LOG_INCOMING_SERVICE_QUERY_IN_TRACE, "/order");

        Assert.assertEquals(features, resolver.resolve());
    }
}
