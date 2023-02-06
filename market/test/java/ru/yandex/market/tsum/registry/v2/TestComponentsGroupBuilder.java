package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.registry.v2.dao.model.ComponentsGroup;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 25/07/2018
 */
public class TestComponentsGroupBuilder {

    private final ComponentsGroup componentsGroup;

    public TestComponentsGroupBuilder() {
        componentsGroup = new ComponentsGroup();
    }

    public ComponentsGroup build() {
        return componentsGroup;
    }

    public TestComponentsGroupBuilder withRandomName() {
        componentsGroup.setName(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestComponentsGroupBuilder withName(String name) {
        componentsGroup.setName(name);
        return this;
    }


    public TestComponentsGroupBuilder withRandomDescription() {
        componentsGroup.setDescription(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestComponentsGroupBuilder withDescription(String description) {
        componentsGroup.setDescription(description);
        return this;
    }

    public TestComponentsGroupBuilder withServiceId(String serviceId) {
        componentsGroup.setServiceId(serviceId);
        return this;
    }

    public TestComponentsGroupBuilder withParentComponentsGroupId(String parentComponentsGroupId) {
        componentsGroup.setParentComponentsGroupId(parentComponentsGroupId);
        return this;
    }
}
