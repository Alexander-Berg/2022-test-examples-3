package ru.yandex.market.logistics.util.client.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author avetokhin 2019-03-12.
 */
public class TestResourceResponse {
    private final ResourceId resourceId;
    private final String name;

    @JsonCreator
    public TestResourceResponse(@JsonProperty("resourceId") final ResourceId resourceId,
                                @JsonProperty("name") final String name) {
        this.resourceId = resourceId;
        this.name = name;
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TestResourceResponse{" +
            "resourceId=" + resourceId +
            ", name='" + name + '\'' +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestResourceResponse)) {
            return false;
        }
        final TestResourceResponse that = (TestResourceResponse) o;
        return Objects.equals(resourceId, that.resourceId) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, name);
    }
}
