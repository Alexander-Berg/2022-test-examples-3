package ru.yandex.market.api.util.httpclient.spi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import ru.yandex.market.api.util.ApiCollections;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by tesseract on 27.09.16.
 */
public class MockExternal {

    private Collection<String> ignoredParams = Sets.newHashSet();
    private Map<String, MockResource> index = Maps.newHashMap();

    public Collection<String> getIgnoredParams() {
        return ignoredParams;
    }

    public void setIgnoredParams(List<String> ignoredParams) {
        this.ignoredParams = ignoredParams;
    }

    public MockResource getResource(String uri) {
        return index.get(uri);
    }

    public void setIndex(Map<String, MockResource> index) {
        this.index = index;
    }

    public Map<String, MockResource> getIndex() {
        return index;
    }

    public void addResources(String key, MockResource resource) {
        index.put(key, resource);
    }
}
