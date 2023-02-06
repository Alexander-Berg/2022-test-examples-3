package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;
import ru.yandex.market.tsum.registry.v2.RandomFieldsGenerator;

import java.util.List;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 30/07/2018
 */
public class TestInstallationBuilder {

    private final Installation installation;

    public TestInstallationBuilder() {
        installation = new Installation();
    }
    
    public Installation build() {
        return installation;
    }

    public TestInstallationBuilder withRandomName() {
        installation.setName(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestInstallationBuilder withName(String name) {
        installation.setName(name);
        return this;
    }

    public TestInstallationBuilder withRandomDescription() {
        installation.setDescription(RandomFieldsGenerator.getRandomString());
        return this;
    }

    public TestInstallationBuilder withDescription(String description) {
        installation.setDescription(description);
        return this;
    }

    public TestInstallationBuilder withComponentId(String componentId) {
        installation.setComponentId(componentId);
        return this;
    }

    public TestInstallationBuilder withEnvironment(Environment environment) {
        installation.setEnvironment(environment);
        return this;
    }

    public TestInstallationBuilder withNannyServices(List<String> nannyServices) {
        this.installation.setNannyServices(nannyServices);
        return this;
    }
}
