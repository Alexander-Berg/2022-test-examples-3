package ru.yandex.market.mboc.app.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;
import ru.yandex.market.springmvctots.codegen.DefinitionsController;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ConfiguredDefinitionsControllerTest extends BaseWebIntegrationTestClass {

    @Autowired
    DefinitionsController controller;

    /**
     * Checks if controller is capable of definition generation.
     * Protects from failure cases like: two classes have the same name, leading to generation failure
     */
    @Test
    public void tryDefinitionGeneration() throws Exception {
        assertNotNull(controller);

        DefinitionsController.Options options = new DefinitionsController.Options();
        options.setMaxArgs(100500);
        options.setMaxOptionalArgs(100500);
        options.setConvertArgsToObject(false);
        options.setIncludeCommonHeaders(false);

        String definitions = controller.getDefinitions(options);
        assertNotNull(definitions);
        assertFalse(definitions.isBlank());
    }
}
