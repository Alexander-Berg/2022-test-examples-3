package ru.yandex.market.health.ui.features.logshatter_config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourcesEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JavaParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ParserEntity;
import ru.yandex.market.health.ui.features.common.view_model.VersionedConfigIdViewModel;
import ru.yandex.market.health.ui.features.logshatter_config.view_model.LogshatterConfigVersionViewModel;
import ru.yandex.market.health.ui.features.logshatter_config.view_model.LogshatterConfigViewModel;

public class LogshatterConfigControllerBaseTest {
    protected MockMvc mockMvc;

    @Autowired
    protected LogshatterConfigDao dao;

    @Autowired
    protected ObjectMapper jacksonMapper;

    @Autowired
    protected LogshatterConfigController controller;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    protected static LogshatterConfigEntity config(String id) {
        return config(id, null);
    }

    protected static LogshatterConfigEntity config(String id, Instant createdTime) {
        return new LogshatterConfigEntity(
            id,
            "test",
            "title1",
            "description1",
            createdTime,
            null,
            null,
            null
        );
    }

    protected static LogshatterConfigVersionEntity version(String id) {
        return version(id, -1);
    }

    protected static LogshatterConfigVersionEntity version(String id, long version) {
        return new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id(id, version),
            VersionedConfigSource.CODE,
            null,
            new DataSourcesEntity(null, null, null),
            new ParserEntity(
                new JavaParserEntity("ru.yandex.market.logshatter.parser.trace.TraceLogParser"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                "default",
                "test_table"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }


    protected static LogshatterConfigViewModel configViewModel(String id) {
        LogshatterConfigViewModel result = new LogshatterConfigViewModel();
        result.setId(id);
        return result;
    }

    protected static LogshatterConfigVersionViewModel configVersionViewModel(String id) {
        VersionedConfigIdViewModel idViewModel = new VersionedConfigIdViewModel();
        idViewModel.setConfigId(id);
        LogshatterConfigVersionViewModel result = new LogshatterConfigVersionViewModel();
        result.setId(idViewModel);
        return result;
    }


    protected String serializeConfig(LogshatterConfigViewModel configViewModel) throws JsonProcessingException {
        return jacksonMapper.writeValueAsString(configViewModel);
    }

    protected String serializeConfigVersion(
        LogshatterConfigVersionViewModel configVersionViewModel
    ) throws JsonProcessingException {
        return jacksonMapper.writeValueAsString(configVersionViewModel);
    }

    protected List<LogshatterConfigViewModel> deserializeConfigs(String json) throws IOException {
        return jacksonMapper.readValue(
            json,
            new TypeReference<List<LogshatterConfigViewModel>>() {
            }
        );
    }

    protected LogshatterConfigViewModel deserializeConfig(String json) throws IOException {
        return jacksonMapper.readValue(json, LogshatterConfigViewModel.class);
    }
}
