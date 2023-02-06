package ru.yandex.market.mbo.reactui.definitions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.springmvctots.codegen.AppDefinitionsGenerator;
import ru.yandex.market.springmvctots.codegen.CustomSettings;
import ru.yandex.market.springmvctots.codegen.CustomSettingsJsonConverter;

public class CheckDefinitionsTest {
    private static final Logger log = LoggerFactory.getLogger(CheckDefinitionsTest.class);

    @Test
    public void checkDefinitions() throws IOException, ClassNotFoundException {
        String currentDefinitions = generateDefinitions();

        String definitions = readResourceString("definitions.ts");
        if (!Objects.equals(StringUtils.strip(currentDefinitions), StringUtils.strip(definitions))) {
            Assert.assertEquals("Definitions are different, just main() in this test to fix it " +
                    "(or mbo-react-ui/src/definitions-update/update-definitions.sh)",
                currentDefinitions, definitions);
        }
    }

    private String generateDefinitions() throws IOException, ClassNotFoundException {
        CustomSettings settings = CustomSettingsJsonConverter.convertFromJson(
            readResourceString("ts_codegen_config.json"));

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        settings.classLoader = classLoader;

        AppDefinitionsGenerator definitionsGenerator = new AppDefinitionsGenerator(settings, classLoader);
        String currentDefinitions = definitionsGenerator.generateDefinitions();
        return currentDefinitions;
    }

    @NotNull
    private String readResourceString(String resourceName) throws IOException {
        return new String(
            getClass().getClassLoader().getResourceAsStream(resourceName).readAllBytes(),
            StandardCharsets.UTF_8
        );
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new CheckDefinitionsTest().updateDefinitions();
    }

    private void updateDefinitions() throws IOException, ClassNotFoundException {
        String arcadia = System.getenv("ARCADIA");
        if (arcadia == null) {
            arcadia = System.getenv("ARCADIA_ROOT");
        }
        if (arcadia == null) {
            System.out.println("Please set ARCADIA or ARCADIA_ROOT environment variable");
            System.exit(-1);
        }

        Path definitionsPath =
            Paths.get(arcadia, "market/mbo/mbo-catalog/mbo-react-ui/src/definitions/src/definitions.ts");
        if (!Files.exists(definitionsPath)) {
            System.out.println("Can't find definitions.ts at " + definitionsPath);
            System.exit(-1);
        }

        System.out.println("Updating " + definitionsPath);
        Files.write(definitionsPath, generateDefinitions().getBytes(StandardCharsets.UTF_8));
        System.out.println("Done updating " + definitionsPath);
    }
}
