package ru.yandex.market.mboc.common.masterdata.repository;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;

/**
 * @author moskovkin@yandex-team.ru
 * @since 20.02.19
 */
public class CargoTypeRepositoryImplTest extends MdmBaseIntegrationTestClass {
    public static final int SEED = 42;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();

    @Autowired
    private CargoTypeRepository cargoTypeRepository;

    @Test
    public void whenInsertingShouldWriteCorrectValues() {
        CargoType newCargoType = RANDOM.nextObject(CargoType.class);

        cargoTypeRepository.insertOrUpdate(newCargoType);
        CargoType storedCargoType = cargoTypeRepository.findById(newCargoType.getId());

        Assertions.assertThat(storedCargoType).isEqualToComparingFieldByField(newCargoType);
    }

    @Test
    public void whenUpdatingShouldWriteCorrectValues() {
        CargoType newCargoType = RANDOM.nextObject(CargoType.class);
        cargoTypeRepository.insertOrUpdate(newCargoType);

        newCargoType.setDescription("Changed description");
        cargoTypeRepository.insertOrUpdate(newCargoType);

        CargoType storedCargoType = cargoTypeRepository.findById(newCargoType.getId());
        Assertions.assertThat(storedCargoType).isEqualToComparingFieldByField(newCargoType);
    }

    @Test
    public void whenSearchingByMboParameterIdShouldReturnCorrectValue() {
        CargoType newCargoType = RANDOM.nextObject(CargoType.class);
        cargoTypeRepository.insertOrUpdate(newCargoType);

        CargoType storedCargoType = cargoTypeRepository.findByMboParameterId(newCargoType.getMboParameterId());
        Assertions.assertThat(storedCargoType).isEqualToComparingFieldByField(newCargoType);
    }
}
