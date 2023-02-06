package ru.yandex.market.logistics.replica.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.replica.AbstractTest;

@DisplayName("Тесты на динамический DataSource")
public class DynamicDataSourceTest extends AbstractTest {
    private final DynamicDataSource dynamicDataSource = new DynamicDataSource();

    @Test
    @DisplayName("Установка похода в реплику")
    void getReplicaDataSource() {
        DynamicDataSource.setReplicaRoute();
        softly.assertThat(dynamicDataSource.determineCurrentLookupKey())
            .isEqualTo(DynamicDataSource.Route.REPLICA);
        DynamicDataSource.clearReplicaRoute();
    }

    @Test
    @DisplayName("Установка похода в datasource по умолчанию после обработки аннотации")
    void getDefaultDataSourceAfterAnnotationProcessing() {
        DynamicDataSource.setReplicaRoute();
        DynamicDataSource.clearReplicaRoute();
        softly.assertThat(dynamicDataSource.determineCurrentLookupKey())
            .isEqualTo(DynamicDataSource.Route.PRIMARY);
    }

    @Test
    @DisplayName("Установка похода в datasource по умолчанию")
    void getDefaultDataSource() {
        softly.assertThat(dynamicDataSource.determineCurrentLookupKey())
            .isEqualTo(DynamicDataSource.Route.PRIMARY);
    }
}
