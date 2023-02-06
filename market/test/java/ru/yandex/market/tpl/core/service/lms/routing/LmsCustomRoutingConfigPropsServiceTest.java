package ru.yandex.market.tpl.core.service.lms.routing;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.tpl.api.model.user.TplUserPropertyDto;
import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.core.domain.lms.routing.LmsCustomRoutingConfigCommand;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingConfig;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingConfigRepository;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingPropertyEnum;
import ru.yandex.market.tpl.core.domain.routing.custom.CustomRoutingPropertyRepository;
import ru.yandex.market.tpl.core.domain.routing.custom.validator.VehicleCostExpressionValidator;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

public class LmsCustomRoutingConfigPropsServiceTest extends TplAbstractTest {

    @Autowired
    private CustomRoutingConfigRepository repository;

    @Autowired
    private CustomRoutingPropertyRepository propertyRepository;

    @Autowired
    private LmsCustomRoutingConfigService service;

    @Autowired
    private LmsCustomRoutingConfigPropsService propsService;

    private CustomRoutingConfig config;

    @SpyBean(name = "vehicleCostExpressionValidator")
    private VehicleCostExpressionValidator vehicleCostExpressionValidator;

    @BeforeEach
    public void init() {
        String configName = "Test_config";
        var dto = new LmsCustomRoutingConfigCommand.LmsCustomRoutingConfigCreateDto(configName);
        var id = service.createCustomRoutingConfig(dto);
        config = repository.findByIdOrThrow(id);
    }

    @Test
    public void shouldCreateProperty() {
        var propDto = new TplUserPropertyDto();
        propDto.setName(CustomRoutingPropertyEnum.AVOID_TOLLS.getName());
        propDto.setValue("true");
        propDto.setType("BOOLEAN");
        var id = propsService.createProperty(config.getId(), propDto);
        var property = propertyRepository.findByIdOrThrow(id);
        Assertions.assertEquals(property.getName(), propDto.getName());
        Assertions.assertEquals(property.getValue(), propDto.getValue());
    }

    @Test
    public void shouldUpdateProperty() {
        var propDto = new TplUserPropertyDto();
        propDto.setName(CustomRoutingPropertyEnum.AVOID_TOLLS.getName());
        propDto.setValue("true");
        propDto.setType("BOOLEAN");
        var id = propsService.createProperty(config.getId(), propDto);
        propDto.setValue("false");
        propDto.setId(id);
        propsService.updateProperty(propDto);
        var property = propertyRepository.findByIdOrThrow(id);
        Assertions.assertEquals(property.getName(), propDto.getName());
        Assertions.assertEquals(property.getValue(), propDto.getValue());
    }

    @Test
    public void shouldDeleteProperty() {
        var propertyDto = new TplUserPropertyDto();
        propertyDto.setName(CustomRoutingPropertyEnum.AVOID_TOLLS.getName());
        propertyDto.setType("BOOLEAN");
        propertyDto.setValue("true");
        var id = propsService.createProperty(config.getId(), propertyDto);
        var property = propertyRepository.findById(id);
        Assertions.assertTrue(property.isPresent());
        var idsDto = new IdsDto();
        idsDto.setIds(List.of(id));
        propsService.deleteProperties(idsDto);
        property = propertyRepository.findById(id);
        Assertions.assertTrue(property.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideTestCustomProperty")
    public void shouldValidateVehicleCostExpr_whenCreateOrUpdateProperty(CustomRoutingPropertyEnum property) {
        String costExpression = "x^2 + y^2";

        Mockito.doNothing().when(vehicleCostExpressionValidator).validate(costExpression);

        var propertyDto = new TplUserPropertyDto();
        propertyDto.setName(property.getName());
        propertyDto.setType("STRING");
        propertyDto.setValue(costExpression);

        Long propertyId = propsService.createProperty(config.getId(), propertyDto);

        Mockito.verify(vehicleCostExpressionValidator, Mockito.times(1)).validate(costExpression);

        propertyDto.setId(propertyId);
        propsService.updateProperty(propertyDto);

        Mockito.verify(vehicleCostExpressionValidator, Mockito.times(2)).validate(costExpression);
        Mockito.reset(vehicleCostExpressionValidator);
    }

    private static Stream<Arguments> provideTestCustomProperty() {
        return Stream.of(
                Arguments.of(CustomRoutingPropertyEnum.REGULAR_CARGO_COST_EXPRESSION),
                Arguments.of(CustomRoutingPropertyEnum.BULKY_CARGO_COST_EXPRESSION)
        );
    }


}
