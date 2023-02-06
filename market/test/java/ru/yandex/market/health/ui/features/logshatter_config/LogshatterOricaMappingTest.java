package ru.yandex.market.health.ui.features.logshatter_config;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.TimestampFormat;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserColumnEntity;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserConditionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceDirectoryEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceLogBrokerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceTrackerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourcesEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JavaParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JsonParserColumnEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JsonParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.TableEngineEntity;
import ru.yandex.market.health.ui.config.internal.OrikaMappingConfig;
import ru.yandex.market.health.ui.features.logshatter_config.view_model.LogshatterConfigViewModel;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrikaMappingConfig.class)
public class LogshatterOricaMappingTest {
    @Autowired
    private MapperFacade oricaMapperFacade;

    @Test
    public void roundTrip() {
        LogshatterConfigEntity original = new LogshatterConfigEntity(
            "id",
            "test",
            "title",
            "description",
            Instant.now(),
            Instant.now(),
            new LogshatterConfigVersionEntity(
                new VersionedConfigEntity.VersionEntity.Id("id", -1L),
                VersionedConfigSource.CODE,
                VersionStatus.PUBLIC,
                new DataSourcesEntity(
                    new DataSourceLogBrokerEntity(
                        Collections.singletonList("topic"),
                        "hostGlob",
                        "pathGlob",
                        null
                    ),
                    Collections.singletonList(new DataSourceTrackerEntity(
                        "queue"
                    )),
                    new DataSourceDirectoryEntity(
                        Collections.singletonList("directoryPath"),
                        "hostGlob", "pathGlob")
                ),
                new ParserEntity(
                    new JavaParserEntity(
                        "className"
                    ),
                    new JsonParserEntity(
                        "dateFormat1",
                        "dateField1",
                        "dateJsonPath1",
                        "timestampField1",
                        "timestampJsonPath1",
                        false,
                        Arrays.asList(
                            new JsonParserColumnEntity(
                                "column1",
                                "type1",
                                null,
                                null,
                                null,
                                null,
                                null
                            ),
                            new JsonParserColumnEntity(
                                "column2",
                                "type2",
                                "field2",
                                "jsonPath2",
                                "defaultValue2",
                                "defaultExpr2",
                                "codec2"
                            )
                        ),
                        new TableEngineEntity(
                            "partitionBy1",
                            Collections.singletonList("orderBy1"),
                            "sampleBy1"
                        )
                    ),
                    new AutoParserEntity(
                        "dateFormat1",
                        Collections.singletonMap("column2", "dateFormat2"),
                        TimestampFormat.UNIXTIME,
                        Arrays.asList("column1", "column2"),
                        "separator1",
                        Arrays.asList("column3", "column4"),
                        Collections.singletonMap("column1", "pattern1"),
                        true,
                        Collections.singletonList(new AutoParserConditionEntity(
                            "condition1",
                            Collections.singletonMap("column1", "value1"),
                            Collections.singletonMap("column2", "value2")
                        )),
                        Collections.singletonMap("column3", Collections.singletonList("requirement3")),
                        Collections.singletonMap("column4", "value4"),
                        false,
                        false,
                        false,
                        Collections.singletonMap("column5", "column6"),
                        Collections.singletonList(new AutoParserColumnEntity(
                            "column7",
                            "type1",
                            "default1",
                            "defaultExpr1",
                            "codec1"
                        )),
                        new TableEngineEntity(
                            "partitionBy1",
                            Collections.singletonList("orderBy1"),
                            "sampleBy1"
                        )
                    ),
                    Collections.singletonMap(
                        "key",
                        "value"
                    )
                ),
                null,
                new TableEntity(
                    "database",
                    "table"
                ),
                5,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null
        );

        LogshatterConfigEntity mapped = oricaMapperFacade.map(
            oricaMapperFacade.map(original, LogshatterConfigViewModel.class),
            original.getClass()
        );

        assertThat(mapped).usingRecursiveComparison().isEqualTo(original);
    }
}
