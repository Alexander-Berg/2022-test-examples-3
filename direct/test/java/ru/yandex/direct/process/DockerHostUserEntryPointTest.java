package ru.yandex.direct.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.utils.io.TempDirectory;

@Ignore("Тест достаточно тяжёлый и специфичный. Предполагается, что нужно помнить про тест и запускать его вручную.")
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class DockerHostUserEntryPointTest {
    private static final String[] SUPPORTED_IMAGES = {"alpine:latest", "ubuntu:trusty", "ubuntu:xenial"};
    private static Docker docker;
    private TempDirectory directory;
    private Path volume;

    @BeforeClass
    public static void setUpDocker() throws InterruptedException {
        docker = new Docker();
        Assume.assumeTrue("This test requires running Docker", docker.isAvailable());
        for (String image : SUPPORTED_IMAGES) {
            if (docker.checkOutput("images", "--quiet", image).trim().isEmpty()) {
                throw new RuntimeException(String.format("You should run `docker pull %s` before this test", image));
            }
        }
    }

    @Before
    public void createTempDirectory() throws IOException {
        directory = new TempDirectory(Paths.get("."), "host_user_entrypoint_test");
        volume = directory.getPath().resolve("volume");
        Files.createDirectory(volume);
    }

    @After
    public void removeTempDirectory() {
        directory.close();
    }

    public List<String> parametersForAll() {
        return Arrays.asList(SUPPORTED_IMAGES);
    }

    /**
     * Если используется DockerHostUserEntryPoint, то все файлы, которые пишутся в контейнере в volume, должны быть
     * доступны на чтение и на запись пользователю с хоста. Имя пользователя на хосте и в контейнере тоже должно
     * совпадать.
     */
    @Test
    @Parameters(method = "parametersForAll")
    public void producesWriteableFiles(String image) throws InterruptedException, IOException {
        Optional<String> containerLogs;
        try (DockerContainer container = new DockerContainer(new DockerHostUserEntryPoint(directory, null)
                .apply(new DockerRunner(docker, image))
                .withVolume(volume, Paths.get("/volume"), false)
                .withCmd("/bin/sh", "-x", "-c", "whoami > /volume/test_file"))) {
            container.waitAndReturnExitCode();
            containerLogs = container.tryReadStderr(DockerContainer.Tail.ALL, Duration.ofSeconds(3));
        }

        String hostUidGid = Processes.checkOutput("whoami");

        try {
            Assertions.assertThat(volume.resolve("test_file"))
                    .isRegularFile()
                    .isReadable()
                    .isWritable()
                    .hasContent(hostUidGid);
        } catch (AssertionError e) {
            e.addSuppressed(new RuntimeException("Container logs:\n" + containerLogs.orElse("<none>")));
            throw e;
        }
    }

    /**
     * Возможность передать свою, сложную команду не должна ломаться при использовании DockerHostUserEntryPoint.
     * Проверяется путём вызова {@code sh -c <script>}, где скрипт содержит кавычки и бекслеши.
     */
    @Test
    @Parameters(method = "parametersForAll")
    public void cmdQuoting(String image) throws IOException, InterruptedException {
        Optional<String> containerLogs;
        String script;
        try (InputStream sourceStream = DockerHostUserEntryPointTest.class.getResourceAsStream("quoting_payload.sh")) {
            script = new BufferedReader(new InputStreamReader(sourceStream)).lines()
                    .collect(Collectors.joining("\n"));
        }
        try (DockerContainer container = new DockerContainer(new DockerHostUserEntryPoint(directory, null)
                .apply(new DockerRunner(docker, image))
                .withHostname("docker_test")
                .withVolume(volume, Paths.get("/volume"), false)
                .withCmd("sh", "-x", "-c", script))) {
            container.waitAndReturnExitCode();
            containerLogs = container.tryReadStderr(DockerContainer.Tail.ALL, Duration.ofSeconds(3));
        }

        try {
            Assertions.assertThat(volume.resolve("test_file"))
                    .hasContent(String.format(""
                                    + "x\\ \"'1'\" %1$s %2$s\n"
                                    + "x\\ \"'2'\" %1$s %2$s\n",
                            "docker_test", System.getProperty("user.name")));
        } catch (AssertionError e) {
            e.addSuppressed(new RuntimeException("Container logs:\n" + containerLogs.orElse("<none>")));
            throw e;
        }
    }
}

