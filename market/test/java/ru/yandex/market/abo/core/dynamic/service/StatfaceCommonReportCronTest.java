package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.dynamic.model.StatfaceCommonReport;

/**
 * @author artemmz
 * @date 21.05.18.
 */
public class StatfaceCommonReportCronTest extends DynamicTaskCronTest<StatfaceCommonReport> {
    @Autowired
    private StatfaceCommonReportRepository statfaceCommonReportRepository;

    @Override
    List<StatfaceCommonReport> tasks() {
        return statfaceCommonReportRepository.findAll();
    }
}
