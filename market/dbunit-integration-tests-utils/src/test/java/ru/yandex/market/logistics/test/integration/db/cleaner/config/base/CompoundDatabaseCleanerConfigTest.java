package ru.yandex.market.logistics.test.integration.db.cleaner.config.base;

import java.util.Arrays;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase.LiquibaseSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

class CompoundDatabaseCleanerConfigTest extends SoftAssertionSupport {
    @Test
    void getSchemas() {

        CompoundDatabaseCleanerConfig config = new CompoundDatabaseCleanerConfig(Arrays.asList(
            SchemaCleanerConfigProvider
                .builder()
                .schema("public").truncateAllExcept("IGNORE_1", "IGNORE_2")
                .schema("someSchema").truncateAllExcept("IGNORE_4", "IGNORE_3")
                .build(),

            new LiquibaseSchemaCleanerConfigProvider("public"),
            SchemaCleanerConfigProvider
                .builder()
                .schema("public").truncateAllExcept("IGNORE_5", "IGNORE_6")
                .schema("public2").truncateOnly("TRUNCATE_2", "TRUNCATE_1")

                .build()

        ));
        softly.assertThat(config.getSchemas()).hasSize(3).isEqualTo(ImmutableSet.of("public", "public2", "someSchema"));
        SchemaCleanerConfig publicSchema = config.getConfigForSchema("public");
        SchemaCleanerConfig public2Schema = config.getConfigForSchema("public2");

        softly.assertThat(publicSchema.getSchemaName()).isEqualTo("public");
        softly.assertThat(publicSchema.resetSequences()).isNull();
        softly.assertThat(publicSchema.shouldNotBeIgnored()).isEqualTo(
            ImmutableSet.builder()
                .add("IGNORE_1", "IGNORE_2", "IGNORE_5", "IGNORE_6")
                .addAll(LiquibaseSchemaCleanerConfigProvider.DO_NOT_DELETE_TABLES)
                .build()
        );
        softly.assertThat(public2Schema.shouldBeTruncated()).isEqualTo(ImmutableSet.of("TRUNCATE_1", "TRUNCATE_2"));
    }

}
