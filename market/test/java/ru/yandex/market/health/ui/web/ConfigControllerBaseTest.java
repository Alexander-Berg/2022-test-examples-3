package ru.yandex.market.health.ui.web;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.ui.features.auth.PermissionsValidator;
import ru.yandex.market.health.ui.features.common.view_model.ConfigShortViewModel;
import ru.yandex.market.health.ui.features.logshatter_config.LogshatterConfigController;
import ru.yandex.market.health.ui.features.logshatter_config.NonAdminValidationHelper;
import ru.yandex.market.health.ui.features.logshatter_config.service.LogshatterConfigService;

import static ru.yandex.market.health.ui.web.ConfigControllerBase.ARCHIVE_PROJECT;
import static ru.yandex.market.health.ui.web.ConfigControllerBase.PROJECT_IS_UNDEFINED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigControllerBaseTest {

    @BeforeAll
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void updateConfig() {
        LogshatterConfigService lserv = Mockito.mock(LogshatterConfigService.class);
        LogshatterConfigDao ldao = Mockito.mock(LogshatterConfigDao.class);
        ClickphiteConfigDao cdao = Mockito.mock(ClickphiteConfigDao.class);
        PermissionsValidator pv = Mockito.mock(PermissionsValidator.class);
        NonAdminValidationHelper vh = Mockito.mock(NonAdminValidationHelper.class);
        Mockito.when(vh.getPermissionsValidator()).thenReturn(pv);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        MapperFacade mapperFacade = Mockito.mock(MapperFacade.class);
        String yqlLink = "https://yql.yandex-team.ru/?query_type=CLICKHOUSE&query=use%20marketclickhousetesting;%0A%0A";

        var logshatterConfigController = new LogshatterConfigController(lserv,
            objectMapper,
            ldao,
            mapperFacade,
            vh,
            cdao,
            yqlLink);
        var model = new ConfigShortViewModel();

        checkIfDescProjectArchiveAllowToUpdate(logshatterConfigController, model);
        checkIfDescProjectNotArchiveDontAllowToUpdate(logshatterConfigController, model);
        checkIfSourceProjectIsDefinedDontAllowToUpdateIfNoPermissions(ldao, logshatterConfigController, model);
        checkIfSourceProjectUndefinedAllowToChangeItToPermittedOneForUser(pv, logshatterConfigController, model);
    }

    private void checkIfSourceProjectUndefinedAllowToChangeItToPermittedOneForUser(
        PermissionsValidator pv,
        LogshatterConfigController logshatterConfigController,
        ConfigShortViewModel model) {

        Mockito.when(pv.isAllowed(Mockito.anyString())).thenReturn(true);
        var responseEntity = logshatterConfigController.updateConfig(model);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    private void checkIfSourceProjectIsDefinedDontAllowToUpdateIfNoPermissions(
        LogshatterConfigDao ldao,
        LogshatterConfigController logshatterConfigController,
        ConfigShortViewModel model) {

        var entity = Mockito.mock(LogshatterConfigEntity.class);
        Mockito.when(entity.getProjectId()).thenReturn("some_source_project", PROJECT_IS_UNDEFINED);
        Mockito.when(ldao.getOptionalConfig(Mockito.any())).thenReturn(Optional.of(entity));
        var responseEntity = logshatterConfigController.updateConfig(model);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    }

    private void checkIfDescProjectNotArchiveDontAllowToUpdate(
        LogshatterConfigController logshatterConfigController,
        ConfigShortViewModel model) {
        model.setProjectId("some_desc_project");
        var responseEntity = logshatterConfigController.updateConfig(model);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    }

    private void checkIfDescProjectArchiveAllowToUpdate(LogshatterConfigController logshatterConfigController,
                                                        ConfigShortViewModel model) {
        model.setProjectId(ARCHIVE_PROJECT);
        ResponseEntity<?> responseEntity = logshatterConfigController.updateConfig(model);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
