package ru.yandex.market.delivery.rupostintegrationapp.service.implementation;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import steps.pickuppointsteps.PickupPointSteps;

import ru.yandex.market.delivery.entities.common.PickupPoint;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsGetReferencePickupPointsResponseContent;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;
import ru.yandex.market.delivery.rupostintegrationapp.service.converter.PickupPointConverter;
import ru.yandex.market.delivery.rupostintegrationapp.service.converter.RuPostPickupPointToLocationConverter;
import ru.yandex.market.delivery.rupostintegrationapp.service.converter.WorkTimeParser;

@ExtendWith(MockitoExtension.class)
class GetReferencePickupPointsServicePositiveTest extends BaseTest {
    @Mock
    private PickuppointRepository repository;

    private GetReferencePickupPointsService service;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        WorkTimeParser workTimeParser = new WorkTimeParser();
        RuPostPickupPointToLocationConverter locationConverter = new RuPostPickupPointToLocationConverter();
        PickupPointConverter converter = new PickupPointConverter(workTimeParser, locationConverter);
        service = new GetReferencePickupPointsService(repository, converter);

        Mockito
            .when(repository.findPickupPoints("bname", "fname", "fvalue", null, Collections.emptySet()))
            .thenReturn(PickupPointSteps.getRussianPostPickupPoints());
    }

    @Test
    void testService() {
        DsGetReferencePickupPointsResponseContent responseContent = service.doJob(PickupPointSteps.getRequest());
        List<PickupPoint> pickupPoints = responseContent.getPickupPoints();

        softly.assertThat(pickupPoints).as("Result has not correct size").hasSize(3);

        softly.assertThat(pickupPoints.get(0).getCode())
            .as("Result pickup point was not mapped correctly")
            .isEqualTo("1");

        softly.assertThat(pickupPoints.get(1).getCode())
            .as("Result pickup point was not mapped correctly")
            .isEqualTo("2");

        softly.assertThat(pickupPoints.get(2).getCode())
            .as("Result pickup point was not mapped correctly")
            .isEqualTo("3");
    }
}
