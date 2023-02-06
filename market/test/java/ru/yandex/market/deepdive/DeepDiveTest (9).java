package ru.yandex.market.deepdive;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;

import ru.yandex.market.deepdive.configuration.DeepDiveDatasourceConfiguration;
import ru.yandex.market.deepdive.configuration.DeepDiveDbConfiguration;
import ru.yandex.market.deepdive.configuration.DeepDiveLiquibaseConfiguration;
import ru.yandex.market.deepdive.configuration.QuartzTasksConfiguration;
import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.mockito.Mockito.when;

@Import({
        DeepDiveDatasourceConfiguration.class,
        DeepDiveLiquibaseConfiguration.class,
        DeepDiveDbConfiguration.class,
        QuartzTasksConfiguration.class,
})
public class DeepDiveTest {

    @Test
    public void pickupPointControllerTest() {
        PickupPointController pickupPointController =
                new PickupPointController(
                        when(Mockito.mock(PickupPointService.class).getPickupPoints()).thenReturn(
                                List.of(
                                        PickupPointDto.builder().id(0).name("name0").build(),
                                        PickupPointDto.builder().id(1).name("name1").build(),
                                        PickupPointDto.builder().id(2).name("name2").build()
                                )
                        ).getMock()
                );
        Assert.assertEquals(
                pickupPointController.getPickupPoints(),
                List.of(
                        PickupPointDto.builder().id(0).name("name0").build(),
                        PickupPointDto.builder().id(1).name("name1").build(),
                        PickupPointDto.builder().id(2).name("name2").build()
                )
        );
    }

    @Test
    public void test() {
        //Just a test example
        Assert.assertTrue(true);
    }
}
