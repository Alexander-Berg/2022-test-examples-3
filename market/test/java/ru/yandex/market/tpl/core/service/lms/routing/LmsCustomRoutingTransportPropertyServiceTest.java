package ru.yandex.market.tpl.core.service.lms.routing;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.tpl.api.model.user.TplUserPropertyDto;
import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.core.domain.lms.routing.LmsCustomRoutingConfigCommand;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportConfigEntity;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportPropertyEnum;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportPropertyRepository;
import ru.yandex.market.tpl.core.domain.routing.custom.transport.CustomRoutingTransportRepository;
import ru.yandex.market.tpl.core.domain.routing.custom.validator.VehicleCostExpressionValidator;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

public class LmsCustomRoutingTransportPropertyServiceTest extends TplAbstractTest {


    @Autowired
    private CustomRoutingTransportRepository transportRepository;

    @Autowired
    private LmsCustomRoutingConfigService service;

    @Autowired
    private LmsCustomRoutingTransportService transportService;

    @Autowired
    private LmsCustomRoutingTransportPropertyService transportPropsService;

    @Autowired
    private CustomRoutingTransportPropertyRepository transportPropertyRepository;

    private CustomRoutingTransportConfigEntity transportConfig;

    @SpyBean(name = "vehicleCostExpressionValidator")
    private VehicleCostExpressionValidator vehicleCostExpressionValidator;

    @BeforeEach
    public void init() {
        String configName = "Test_config";
        var dto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(dto);

        var transportConfigName = "TConfig1";
        var transportDto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingTransportCreateDto(transportConfigName);
        var tId = transportService.createTransportsConfig(id, transportDto);
        transportConfig = transportRepository.findByIdOrThrow(tId);
    }

    @Test
    public void shouldCreateTransportProperty() {
        var propDto = new TplUserPropertyDto();
        propDto.setName(CustomRoutingTransportPropertyEnum.TAGS.getName());
        propDto.setValue("delivery");
        propDto.setType("STRING");
        var id = transportPropsService.createProperty(transportConfig.getId(), propDto);
        var property = transportPropertyRepository.findByIdOrThrow(id);
        Assertions.assertEquals(property.getName(), propDto.getName());
        Assertions.assertEquals(property.getValue(), propDto.getValue());
    }

    @Test
    public void shouldUpdateTransportProperty() {
        var propDto = new TplUserPropertyDto();
        propDto.setName(CustomRoutingTransportPropertyEnum.TAGS.getName());
        propDto.setValue("delivery");
        propDto.setType("STRING");
        var id = transportPropsService.createProperty(transportConfig.getId(), propDto);
        propDto.setValue("dropship");
        propDto.setId(id);
        transportPropsService.updateProperty(propDto);
        var property = transportPropertyRepository.findByIdOrThrow(id);
        Assertions.assertEquals(property.getName(), propDto.getName());
        Assertions.assertEquals(property.getValue(), propDto.getValue());
    }

    @Test
    public void shouldDeleteTransportProperty() {
        var propDto = new TplUserPropertyDto();
        propDto.setName(CustomRoutingTransportPropertyEnum.TAGS.getName());
        propDto.setValue("delivery");
        propDto.setType("STRING");
        var id = transportPropsService.createProperty(transportConfig.getId(), propDto);
        var property = transportPropertyRepository.findById(id);
        Assertions.assertTrue(property.isPresent());
        var idsDto = new IdsDto();
        idsDto.setIds(List.of(id));
        transportPropsService.deleteProperties(idsDto);
        property = transportPropertyRepository.findById(id);
        Assertions.assertTrue(property.isEmpty());
    }

    @Test
    public void shouldValidateVehicleCostExprProperty_whenCreateProperty() {
        String costExpression = "x^2 + y^2";

        Mockito.doNothing().when(vehicleCostExpressionValidator).validate(costExpression);

        var propertyDto = new TplUserPropertyDto();
        propertyDto.setName(CustomRoutingTransportPropertyEnum.SPECIAL_VEHICLE_COST_EXPRESION.getName());
        propertyDto.setType("STRING");
        propertyDto.setValue(costExpression);

        Long propertyId = transportPropsService.createProperty(transportConfig.getId(), propertyDto);

        Mockito.verify(vehicleCostExpressionValidator, Mockito.times(1)).validate(costExpression);

        propertyDto.setId(propertyId);
        transportPropsService.updateProperty(propertyDto);

        Mockito.verify(vehicleCostExpressionValidator, Mockito.times(2)).validate(costExpression);
        Mockito.reset(vehicleCostExpressionValidator);
    }

}
