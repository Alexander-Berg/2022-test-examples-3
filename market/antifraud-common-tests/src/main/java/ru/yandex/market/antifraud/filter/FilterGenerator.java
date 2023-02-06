package ru.yandex.market.antifraud.filter;

import java.util.List;

/**
 * Created by oroboros on 14.08.17.
 */
public interface FilterGenerator {
    List<TestClick> generate();
}
