package ru.yandex.market.delivery.rupostintegrationapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.health.PickupPointCountChecker;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest extends BaseTest {

    @Mock
    private PickuppointRepository repository;

    private PickupPointCountChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PickupPointCountChecker(repository);
    }

    @Test
    void testAllOk() {
        Mockito.when(repository.countEnabledPickupPoints()).thenReturn(21000L);
        softly.assertThat(checker.check()).as("Result should be ok")
            .isEqualTo("0;OK");
    }

    @Test
    void testFailure() {
        Mockito.when(repository.countEnabledPickupPoints()).thenReturn(19000L);
        softly.assertThat(checker.check()).as("Result should be ok")
            .isEqualTo("2;Current amount of pickup points in db; 19000");
    }
}
