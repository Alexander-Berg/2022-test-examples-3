package ru.yandex.market.deepdive.domain;
 
import java.util.ArrayList;
import java.util.List;
 
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;
 
public class PickupPointControllerTest {
    PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
    PickupPointService service = new PickupPointService(repository, new PickupPointMapper());
 
    List<PickupPoint> pickupPoints = new ArrayList<PickupPoint>();
 
    PickupPointController controller;
 
    @Test
    void getPickUpPointsTest() {
        pickupPoints.add(
                PickupPoint.builder()
                        .id(0L)
                        .name("a")
                        .active(false)
                        .build());
        pickupPoints.add(
                PickupPoint.builder()
                        .id(1L)
                        .name("b")
                        .active(true)
                        .build());
        pickupPoints.add(
                PickupPoint.builder()
                        .id(2L)
                        .name("c")
                        .active(true)
                        .build());
        Mockito.when(repository.findAll()).thenReturn(pickupPoints);
        controller = new PickupPointController(service);
        Assert.assertArrayEquals(pickupPoints.toArray(), controller.getPickupPoints().toArray());
    }
 
}
