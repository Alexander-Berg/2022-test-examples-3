package ru.yandex.market.clickphite.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Sergey Novichkov <a href="mailto:sereja589@yandex-team.ru"></a>
 * @date 04.09.19
 */
public class ReportConfigGeneratorTest {
    private static final String PROJECT_PATH = "market/infra/market-health/config-cs-clickphite/";

    private static final Logger log = LogManager.getLogger();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testThatReportConfigsIsGenerated() throws Exception {
        String pathToGenerator = ru.yandex.devtools.test.Paths.getSourcePath(
            PROJECT_PATH + "tools/report_config_gen.py"
        );
        String pathToConfigs = ru.yandex.devtools.test.Paths.getSourcePath(PROJECT_PATH + "src/conf.d");
        File generatedConfigsDir = temporaryFolder.newFolder();

        Process generationProcess = new ProcessBuilder(pathToGenerator, "--output-dir",
            generatedConfigsDir.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
        List<String> processOutput = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(generationProcess.getInputStream(),
            StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processOutput.add(line);
            }
        }
        final int genProcessExitCode = generationProcess.waitFor();
        if (genProcessExitCode != 0) {
            log.error("Generation process output:");
            for (String line : processOutput) {
                log.error(line);
            }
            throw new RuntimeException("Generation process finished with error code: " + genProcessExitCode);
        }

        for (String config : Objects.requireNonNull(generatedConfigsDir.list())) {
            byte[] generated = Files.readAllBytes(Paths.get(generatedConfigsDir.toString(), config));
            byte[] source = Files.readAllBytes(Paths.get(pathToConfigs, config));
            Assert.assertArrayEquals(generated, source);
        }
    }
}
