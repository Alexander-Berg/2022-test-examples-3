package ru.yandex.market.global.index.domain.index;

import java.net.URI;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.global.common.elastic.IndexSupplier;
import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.index.config.properties.MigrationSourceProperties;

@Slf4j
@RequiredArgsConstructor
public class TestingIndexMigrationService implements IndexMigrationService {
    private final IndexingService indexingService;
    private final MigrationSourceProperties migrationSourceProperties;

    @Override
    @SneakyThrows
    public void migrate(@SuppressWarnings("rawtypes") IndexSupplier... indexSuppliers) {
        URI migrationSourceUri = new URI(migrationSourceProperties.getUrl());
        Arrays.stream(indexSuppliers).forEach(indexSupplier -> indexingService.reindexFromOtherCluster(
                indexSupplier,
                migrationSourceUri,
                migrationSourceProperties.getUsername(),
                migrationSourceProperties.getPassword()
        ));
    }
}
