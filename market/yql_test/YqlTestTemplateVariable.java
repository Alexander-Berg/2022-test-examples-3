package ru.yandex.market.yql_test;

import ru.yandex.market.yql_query_service.YqlTemplateVariable;

/**
 * Чтобы создать или переопределить переменную, которая будет подставляться в <i>freemarker template</i>,
 * необходимо создать <b>@Bean</b>, который будет возвращать класс реализовывающий этот интерфейс
 * @see YqlTemplateVariable
 */
public interface YqlTestTemplateVariable extends YqlTemplateVariable {
}
