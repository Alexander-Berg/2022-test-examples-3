package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.dynamic.model.ExpImpTask;

/**
 * @author artemmz
 * @date 21.05.18.
 */
public class ExpImpCronTest extends DynamicTaskCronTest<ExpImpTask> {
    @Autowired
    private ExpImpTaskRepository expImpTaskRepository;

    @Override
    List<ExpImpTask> tasks() {
        return expImpTaskRepository.findAll();
    }
}
