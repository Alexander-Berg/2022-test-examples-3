package ru.yandex.market.logshatter.config.storage.json;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceLogBrokerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourcesEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JavaParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ProtoConverterEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class LogShatterConfigFileJsonWithProtoConverterLoaderTest {
    @Test
    public void ProtoConverterLoadingCheck() {
        check(new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("protoConverter", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(Arrays.asList("megamind--error-log"), "*", "**", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("ru.yandex.market.logshatter.parser.front.errorBooster.universal.ErrorsParser"),
                null,
                null,
                null
            ),
            new ProtoConverterEntity("ru.yandex.market.logshatter.parser.front.errorBooster.universal" +
                ".ErrorsProtoConverter"),
            new TableEntity(
                "db",
                "first"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    private static void check(LogshatterConfigVersionEntity expected) {
        String configDir = ClassLoader.getSystemResource("configs/protoConverter/").getPath();
        assertThat(new LogshatterConfigFileJsonLoader(configDir).load())
            .hasSize(1)
            .first().isEqualToComparingFieldByFieldRecursively(expected);
    }
}
