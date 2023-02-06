package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.gen.model.GeneratorProfile;

/**
 * @author komarovns
 * @date 31.07.19
 */
class GeneratorsCronTest extends DynamicTaskCronTest<GeneratorProfile> {
    @Autowired
    private GeneratorRepository generatorRepository;

    @Override
    List<GeneratorProfile> tasks() {
        return generatorRepository.findAllByActiveTrue();
    }
}
