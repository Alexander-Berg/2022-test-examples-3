package ru.yandex.chemodan.uploader.av.drweb;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.uploader.av.AntivirusResult;
import ru.yandex.chemodan.uploader.av.AntivirusTestUtils;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ShellAntivirusTest extends AbstractTest {

    @Value("${antivirus.max.file.size}")
    private DataSize maxFileSize;
    @Value("${antivirus.only.file}")
    private boolean supportFileOnly;
    @Value("${shell.antivirus.command}")
    private String command;
    @Value("${shell.antivirus.only.stdin:-false}")
    private boolean useOnlyStdin;

    private ShellAntivirus shellAntivirus;

    @Before
    public void before() {
        shellAntivirus = create();
    }

    @Test
    @Ignore("Shell antivirus not used more in disk, it was removed from agent")
    public void testOk() {
        Assert.equals(AntivirusResult.HEALTHY, shellAntivirus.check(AntivirusTestUtils.healthyInputStreamSource()));
    }

    @Test
    @Ignore("Shell antivirus not used more in disk, it was removed from agent")
    public void testVirus() {
        Assert.equals(AntivirusResult.INFECTED, shellAntivirus.check(AntivirusTestUtils.infectedInputStreamSource()));
    }

    @Test
    @Ignore("Shell antivirus not used more in disk, it was removed from agent")
    public void testFileOk() {
        testWithTempFile(AntivirusTestUtils.healthyInputStreamSource(),
                tmpFile -> Assert.equals(AntivirusResult.HEALTHY, shellAntivirus.check(tmpFile)));
    }

    @Test
    @Ignore("Shell antivirus not used more in disk, it was removed from agent")
    public void testFileVirus() {
        testWithTempFile(AntivirusTestUtils.infectedInputStreamSource(),
                tmpFile -> Assert.equals(AntivirusResult.INFECTED, shellAntivirus.check(tmpFile)));
    }

    @Test
    public void maxSize() {
        Assert.isTrue(shellAntivirus.isEnabledForFileSize(Option.empty()));
        Assert.isTrue(shellAntivirus.isEnabledForFileSize(Option.of(DataSize.fromKiloBytes(5))));
        Assert.isTrue(shellAntivirus.isEnabledForFileSize(Option.of(DataSize.MEGABYTE)));
        Assert.isFalse(shellAntivirus.isEnabledForFileSize(Option.of(DataSize.fromGigaBytes(3))));
    }

    private void testWithTempFile(InputStreamSource input, Function1V<File2> test) {
        File2.withNewTempFile(tmpFile -> {
            IoUtils.copy(input.getInput(), tmpFile.asOutputStreamTool().getOutput());
            test.apply(tmpFile);
        });
    }

    private ShellAntivirus create() {
        return new ShellAntivirus(command, useOnlyStdin, true, maxFileSize, supportFileOnly);
    }

}
