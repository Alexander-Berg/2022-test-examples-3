package ru.yandex.market.tpl.core.service.lms.routing;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.core.domain.lms.routing.LmsCustomRoutingConfigCommand;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingConfigRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

public class LmsCustomRoutingConfigServiceTest extends TplAbstractTest {

    @Autowired
    CustomRoutingConfigRepository repository;

    @Autowired
    LmsCustomRoutingConfigService service;


    @Test
    public void shouldCreateCustomConfig() {
        String configName = "Test_config";
        var dto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(dto);
        var config = repository.findByIdOrThrow(id);
        Assertions.assertEquals(config.getName(), configName);
    }

    @Test
    public void shouldUpdateCustomConfig() {
        String configName = "Test_config";
        String updatedName = "Test_config2";
        var createDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(createDto);
        var updateDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigUpdateDto(updatedName);
        service.updateCustomRoutingConfig(id, updateDto);
        var config = repository.findByIdOrThrow(id);
        Assertions.assertEquals(config.getName(), updatedName);
    }

    @Test
    public void shouldDeleteCustomConfig() {
        String configName = "Test_config";
        var createDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(createDto);
        var idsDto = new IdsDto();
        idsDto.setIds(List.of(id));
        service.deleteCustomRoutingConfig(idsDto);
        var config = repository.findByIdOrThrow(id);
        Assertions.assertEquals(config.getDeleted(), true);
    }

}
