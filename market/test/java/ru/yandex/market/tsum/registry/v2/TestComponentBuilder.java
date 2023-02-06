package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.registry.v2.dao.model.Component;

import ru.yandex.market.tsum.registry.v2.RandomFieldsGenerator;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentStatus;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 30/07/2018
 */
public class TestComponentBuilder {

    private final Component component;

    public TestComponentBuilder() {
        component = new Component();
    }

    public Component build() {
        return component;
    }

    public TestComponentBuilder withRandomName() {
        component.setName(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestComponentBuilder withName(String name) {
        component.setName(name);
        return this;
    }

    public TestComponentBuilder withRandomDescription() {
        component.setDescription(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestComponentBuilder withDescription(String description) {
        component.setDescription(description);
        return this;
    }

    public TestComponentBuilder withServiceId(String serviceId) {
        component.setServiceId(serviceId);
        return this;
    }

    public TestComponentBuilder withRandomAbcSlug() {
        component.setAbcSlug(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestComponentBuilder withComponentsGroupId(String componentsGroupId) {
        component.setComponentsGroupId(componentsGroupId);
        return this;
    }

    public TestComponentBuilder withStatus(ComponentStatus status) {
        component.setStatus(status);
        return this;
    }
}
