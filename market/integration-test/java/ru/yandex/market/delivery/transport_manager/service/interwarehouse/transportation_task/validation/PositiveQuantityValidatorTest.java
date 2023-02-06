package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.validation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationTaskMapper;


@DatabaseSetup("/repository/transportation_task/task_with_transportation.xml")
public class PositiveQuantityValidatorTest extends AbstractContextualTest {

    @Autowired
    PositiveQuantityValidator positiveQuantityValidator;

    @Autowired
    TransportationTaskMapper transportationTaskMapper;

    @DatabaseSetup("/repository/register/single_register_single_fit.xml")
    @Test
    void validRegister() {
        var result = positiveQuantityValidator.isValid(transportationTaskMapper.getById(2L));
        softly.assertThat(result.getErrorMessage()).isEmpty();
        softly.assertThat(result.isInvalid()).isFalse();
    }

    @DatabaseSetup("/repository/register/single_register_zero_fit.xml")
    @Test
    void invalidRegister() {
        var result = positiveQuantityValidator.isValid(transportationTaskMapper.getById(1L));
        softly.assertThat(result.getErrorMessage()).isEqualTo("В регистре есть товар с нулевым количеством; ");
        softly.assertThat(result.isInvalid()).isTrue();
    }

    @DatabaseSetup("/repository/register/single_register_wrong_fit.xml")
    @Test
    void invalidRegisterWithWrongFit() {
        var result = positiveQuantityValidator.isValid(transportationTaskMapper.getById(1L));
        softly.assertThat(result.getErrorMessage()).isEqualTo("Пустой регистр; ");
        softly.assertThat(result.isInvalid()).isTrue();
    }
}
