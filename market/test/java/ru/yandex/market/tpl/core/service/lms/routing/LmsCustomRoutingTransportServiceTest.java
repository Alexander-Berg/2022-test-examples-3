package ru.yandex.market.tpl.core.service.lms.routing;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.core.domain.lms.routing.LmsCustomRoutingConfigCommand;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingConfig;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingConfigRepository;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;


public class LmsCustomRoutingTransportServiceTest extends TplAbstractTest {

    @Autowired
    private CustomRoutingConfigRepository repository;

    @Autowired
    private CustomRoutingTransportRepository transportRepository;

    @Autowired
    private LmsCustomRoutingConfigService service;

    @Autowired
    private LmsCustomRoutingTransportService transportService;

    private CustomRoutingConfig config;

    @BeforeEach
    public void init() {
        String configName = "Test_config";
        var dto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(dto);
        config = repository.findByIdOrThrow(id);
    }

    @Test
    public void shouldCreateTransportConfig() {
        var configName = "TConfig1";
        var transportDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingTransportCreateDto(configName);
        var id = transportService.createTransportsConfig(config.getId(), transportDto);
        var tConfig = transportRepository.findByIdOrThrow(id);
        Assertions.assertEquals(tConfig.getName(), configName);
    }

    @Test
    public void shouldUpdateTransportConfig() {
        var configName = "TConfig2";
        var transportDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingTransportCreateDto(configName);
        var id = transportService.createTransportsConfig(config.getId(), transportDto);
        var tConfig = transportRepository.findByIdOrThrow(id);
        Assertions.assertEquals(tConfig.getName(), configName);
        configName = "TConfig3";
        var updateDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingTransportUpdateDto(
                configName, false, null, null, null, null, null, null, null, null, null, null
        );
        transportService.updateTransportsConfig(id, updateDto);
        tConfig = transportRepository.findByIdOrThrow(id);
        Assertions.assertEquals(tConfig.getName(), configName);
    }

    @Test
    public void shouldDeleteTransportConfig() {
        var configName = "TConfig1";
        var transportDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingTransportCreateDto(configName);
        var id = transportService.createTransportsConfig(config.getId(), transportDto);
        var tConfig = transportRepository.findByIdOrThrow(id);
        Assertions.assertEquals(tConfig.getName(), configName);
        var idsDto = new IdsDto();
        idsDto.setIds(List.of(id));
        transportService.deleteTransportsConfig(idsDto);
        tConfig = transportRepository.findByIdOrThrow(id);
        Assertions.assertTrue(tConfig.getDeleted());
    }

}
