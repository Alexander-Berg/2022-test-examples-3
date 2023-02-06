package ru.yandex.market.logistics.util.client.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author avetokhin 2019-03-12.
 */
public class CreateTestResourceRequest {
    private final String name;

    @JsonCreator
    public CreateTestResourceRequest(@JsonProperty("name") final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CreateTestResourceRequest{" +
            "name='" + name + '\'' +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateTestResourceRequest)) {
            return false;
        }
        final CreateTestResourceRequest that = (CreateTestResourceRequest) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
