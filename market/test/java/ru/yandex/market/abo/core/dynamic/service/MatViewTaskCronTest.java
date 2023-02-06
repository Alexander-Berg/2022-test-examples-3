package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.dynamic.model.MatViewTask;

/**
 * @author artemmz
 * @date 21.05.18.
 */
public class MatViewTaskCronTest extends DynamicTaskCronTest<MatViewTask> {
    @Autowired
    private MatViewTaskRepository matViewTaskRepository;

    @Override
    List<MatViewTask> tasks() {
        return matViewTaskRepository.findAll();
    }
}
