package ru.yandex.direct.ansiblejuggler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.io.TempDirectory;

public class AnsibleWrapperTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNonExistentBinary() throws AnsibleWrapper.PlaybookProcessingException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("ansiblePlaybookCommand not found");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(dir.getPath().resolve("non-existent-binary").toString())
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testNonSpecifiedBinary() throws AnsibleWrapper.PlaybookProcessingException {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("ansiblePlaybookCommand is not specified in configuration");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testNonSpecifiedTimeout() throws AnsibleWrapper.PlaybookProcessingException {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("executionTimeout is not specified in configuration");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withAnsiblePlaybookCmd(dir.getPath().resolve("non-existent-binary").toString())
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testNegativeTimeout() throws AnsibleWrapper.PlaybookProcessingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("executionTimeout must be positive");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(-10))
                            .withAnsiblePlaybookCmd(dir.getPath().resolve("non-existent-binary").toString())
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testZeroTimeout() throws AnsibleWrapper.PlaybookProcessingException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("executionTimeout must be positive");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ZERO)
                            .withAnsiblePlaybookCmd(dir.getPath().resolve("non-existent-binary").toString())
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testNonExecutableBinary() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        thrown.expect(AnsibleWrapper.PlaybookProcessingException.class);
        thrown.expectCause(IsInstanceOf.instanceOf(Checked.CheckedException.class));

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(Files.createFile(dir.getPath().resolve("ansible")).toString())
                            .build(),
                    Files.createFile(dir.getPath().resolve("playbook")).toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testNonExistentPlaybook() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("playbook file must exists");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(Files.createFile(dir.getPath().resolve("ansible")).toString())
                            .build(),
                    dir.getPath().resolve("non-existent-playbook").toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testBadExitStatus() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        thrown.expect(AnsibleWrapper.PlaybookProcessingException.class);

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            Path binary = Files.createFile(dir.getPath().resolve("ansible"));
            Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.write(binary, "#!/bin/sh\nexit 1".getBytes());

            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(binary.toString())
                            .build(),
                    Files.createFile(dir.getPath().resolve("playbook")).toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testExecTimeout() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        thrown.expect(AnsibleWrapper.PlaybookProcessingException.class);
        thrown.expectMessage("Timed out waiting for ansible output");

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            Path binary = Files.createFile(dir.getPath().resolve("ansible"));
            Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.write(binary, "#!/bin/sh\nexec sleep 30".getBytes());

            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(1))
                            .withAnsiblePlaybookCmd(binary.toString())
                            .build(),
                    Files.createFile(dir.getPath().resolve("playbook")).toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testBadRecap() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        thrown.expect(AnsibleWrapper.PlaybookProcessingException.class);

        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            Path binary = Files.createFile(dir.getPath().resolve("ansible"));
            Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.write(binary, ("#!/bin/sh\necho 'PLAY RECAP ***'; echo blablabla").getBytes());

            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(binary.toString())
                            .build(),
                    Files.createFile(dir.getPath().resolve("playbook")).toFile()
            ).syncPlaybook();
        }
    }

    @Test
    public void testGoodRecap() throws AnsibleWrapper.PlaybookProcessingException, IOException {
        try (TempDirectory dir = new TempDirectory("ansible-wrapper-test")) {
            Path binary = Files.createFile(dir.getPath().resolve("ansible"));
            Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.write(binary, ("#!/bin/sh\n" +
                    "echo 'PLAY RECAP ***'; " +
                    "echo 'host : ok=9 changed=1 unreachable=2 failed=5'"
            ).getBytes());

            new AnsibleWrapper(
                    new AnsibleWrapperConfiguration.Builder()
                            .withExecutionTimeout(Duration.ofSeconds(10))
                            .withAnsiblePlaybookCmd(binary.toString())
                            .build(),
                    Files.createFile(dir.getPath().resolve("playbook")).toFile()
            ).syncPlaybook();
        }
    }
}
