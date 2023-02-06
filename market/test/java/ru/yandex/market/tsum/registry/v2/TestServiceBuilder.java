package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.registry.v2.dao.model.Service;

import static ru.yandex.market.tsum.registry.v2.RandomFieldsGenerator.getRandomString;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 25/07/2018
 */
public class TestServiceBuilder {

    private final Service service;

    public TestServiceBuilder() {
        service = new Service();
    }

    public Service build() {
        return service;
    }

    public TestServiceBuilder withRandomName() {
        service.setName(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestServiceBuilder withName(String name) {
        service.setName(name);
        return this;
    }

    public TestServiceBuilder withRandomDescription() {
        service.setDescription(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestServiceBuilder withDescription(String description) {
        service.setDescription(description);
        return this;
    }

    public TestServiceBuilder withRandomStarTrekQueue() {
        service.setStarTrekQueue(getRandomString());
        return this;
    }

    public TestServiceBuilder withRandomProjectId() {
        service.setProjectId(getRandomString());
        return this;
    }

    public TestServiceBuilder withRandomOwner() {
        service.setOwner(getRandomString());
        return this;
    }

    public TestServiceBuilder withRandomAbcSlug() {
        service.setAbcSlug(getRandomString());
        return this;
    }
}
