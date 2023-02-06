package ru.yandex.market.health.ui.features.clickphite_config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.ui.features.clickphite_config.view_model.ClickphiteConfigGroupVersionViewModel;
import ru.yandex.market.health.ui.features.clickphite_config.view_model.ClickphiteConfigGroupViewModel;
import ru.yandex.market.health.ui.features.common.view_model.VersionedConfigIdViewModel;

public class ClickphiteConfigControllerBaseTest {
    protected MockMvc mockMvc;

    @Autowired
    protected ClickphiteConfigDao dao;

    @Autowired
    protected ObjectMapper jacksonMapper;

    @Autowired
    protected ClickphiteConfigController controller;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getName()).thenReturn("user42");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    protected static ClickphiteConfigGroupEntity config(String id) {
        return config(id, null);
    }

    protected static ClickphiteConfigGroupEntity config(String id, Instant createdTime) {
        return new ClickphiteConfigGroupEntity(
            id,
            null,
            "title1",
            "description1",
            createdTime,
            null,
            null,
            null
        );
    }

    protected static ClickphiteConfigGroupVersionEntity version(String id) {
        return version(id, -1);
    }

    protected static ClickphiteConfigGroupVersionEntity version(String id, long version) {
        return new ClickphiteConfigGroupVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id(id, version),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            "owner1",
            null,
            null
        );
    }


    protected static ClickphiteConfigGroupViewModel configViewModel(String id) {
        ClickphiteConfigGroupViewModel result = new ClickphiteConfigGroupViewModel();
        result.setId(id);
        return result;
    }

    protected static ClickphiteConfigGroupVersionViewModel configVersionViewModel(String id) {
        VersionedConfigIdViewModel idViewModel = new VersionedConfigIdViewModel();
        idViewModel.setConfigId(id);
        ClickphiteConfigGroupVersionViewModel result = new ClickphiteConfigGroupVersionViewModel();
        result.setId(idViewModel);
        result.setOwner("owner1");
        return result;
    }


    protected String serializeConfig(ClickphiteConfigGroupViewModel configViewModel) throws JsonProcessingException {
        return jacksonMapper.writeValueAsString(configViewModel);
    }

    protected String serializeConfigVersion(
        ClickphiteConfigGroupVersionViewModel configVersionViewModel
    ) throws JsonProcessingException {
        return jacksonMapper.writeValueAsString(configVersionViewModel);
    }

    protected List<ClickphiteConfigGroupViewModel> deserializeConfigs(String json) throws IOException {
        return jacksonMapper.readValue(
            json,
            new TypeReference<List<ClickphiteConfigGroupViewModel>>() {
            }
        );
    }

    protected ClickphiteConfigGroupViewModel deserializeConfig(String json) throws IOException {
        return jacksonMapper.readValue(json, ClickphiteConfigGroupViewModel.class);
    }
}
