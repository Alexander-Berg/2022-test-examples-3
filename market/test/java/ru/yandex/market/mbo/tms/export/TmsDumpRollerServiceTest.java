package ru.yandex.market.mbo.tms.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.yandex.market.mbo.utils.OsUtils;

/**
 * @author sergeymironov@yandex-team.ru , date 24.06.2019
 */
public class TmsDumpRollerServiceTest {

    private static final long TIME_OUT_IN_SECONDS = 18000;

    /**
     * Datetime formatter for linux tool touch.
     */
    private static final DateTimeFormatter TOUCH_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmm.ss")
        .toFormatter()
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path tmpDir;
    private Path archiveDir;
    private final AtomicLong now = new AtomicLong();
    private final long timeIncrement = TimeUnit.MINUTES.toMillis(1);

    private final List<Path> listPathToDelete = new ArrayList<>();
    private final List<Path> listPathToRemain = new ArrayList<>();
    private final List<Path> listPathToTar = new ArrayList<>();

    @Before
    public void setUp() throws IOException {
        org.junit.Assume.assumeFalse(OsUtils.isWindows());

        this.now.set(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));

        tmpDir = folder.newFolder("tms-exportdump-test-" + UUID.randomUUID()).toPath();
        updateTime(tmpDir);

        archiveDir = createDir("archive");

        listPathToRemain.add(archiveDir);
        listPathToRemain.add(createDir("clusters"));
        listPathToRemain.add(createDir("fast"));

        listPathToDelete.add(createDir("20190601_1200"));
        createFile("20190601_1200", "test-file");

        listPathToDelete.add(createDir("20190602_1200"));
        listPathToRemain.add(createDir("20190603_1200"));

        Path recentDir = createDir("20190604_1200");
        listPathToRemain.add(recentDir);
        listPathToRemain.add(createDir("fresh"));

        Path recentLink = tmpDir.resolve("recent");
        // will probably fail on Windows systems
        Files.createSymbolicLink(recentLink, recentDir);
        updateTime(recentLink);
    }

    private Path createDir(@NotNull String... dir) throws IOException {
        Path dest = Arrays.stream(dir).reduce(tmpDir, Path::resolve, Path::resolve);
        Path path = Files.createDirectories(dest);
        updateTime(path);
        return path;
    }

    private Path createFile(@NotNull String... dir) throws IOException {
        Path dest = Arrays.stream(dir).reduce(tmpDir, Path::resolve, Path::resolve);
        FileUtils.writeStringToFile(dest.toFile(), "test data", StandardCharsets.UTF_8);
        updateTime(dest);
        return dest;
    }

    private void updateTime(Path path) throws IOException {
        do {
            FileTime fileTime = FileTime.fromMillis(now.addAndGet(timeIncrement));

            Files.setLastModifiedTime(path, fileTime);
            path = path.getParent();
        } while (path != null && path.startsWith(tmpDir));
    }

    /**
     * tests if dumps are rolled successfully.
     */
    @Test
    public void testDumpDirectoriesDeleted() throws InterruptedException, IOException {

        executeDumpRoller("development");

        List<Path> notDeleted = listPathToDelete.stream().filter(Files::exists).collect(Collectors.toList());
        Assertions.assertThat(notDeleted).isEmpty();
    }

    @Test
    public void testDumpDirectoriesRemained() throws InterruptedException, IOException {

        executeDumpRoller("development");

        List<Path> remained = listPathToRemain.stream().filter(Files::exists).collect(Collectors.toList());
        Assertions.assertThat(remained).isEqualTo(listPathToRemain);
    }

    @Test
    public void testFilesTared() throws InterruptedException, IOException {

        executeDumpRoller("development");

        listPathToTar.add(archiveDir.resolve("20190601_1200.tar"));
        listPathToTar.add(archiveDir.resolve("20190602_1200.tar"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archiveDir)) {
            stream.forEach(listPathToTar::remove);
        }

        Assertions.assertThat(listPathToTar).isEmpty();
    }

    /**
     * tests if dumps are rolled successfully.
     */
    @Test
    public void testDumpDirectoriesDeletedWithFallback() throws InterruptedException, IOException {
        //add fallback session
        listPathToRemain.add(createDir("20190600_1200"));
        createFile("20190600_1200", "switch-to-prev-file");

        executeDumpRoller("development");

        List<Path> notDeleted = listPathToDelete.stream().filter(Files::exists).collect(Collectors.toList());
        Assertions.assertThat(notDeleted).isEmpty();
    }

    @Test
    @Ignore
    public void testDumpDirectoriesRemainedWithFallback() throws InterruptedException, IOException {
        //add fallback session
        listPathToRemain.add(createDir("20190600_1200"));
        createFile("20190600_1200", "switch-to-prev-file");

        executeDumpRoller("development");

        List<Path> remained = listPathToRemain.stream().filter(Files::exists).collect(Collectors.toList());
        Assertions.assertThat(remained).isEqualTo(listPathToRemain);
    }

    @Test
    public void testFilesTaredWithFallback() throws InterruptedException, IOException {
        //add fallback session
        listPathToRemain.add(createDir("20190600_1200"));
        createFile("20190600_1200", "switch-to-prev-file");

        executeDumpRoller("development");

        listPathToTar.add(archiveDir.resolve("20190601_1200.tar"));
        listPathToTar.add(archiveDir.resolve("20190602_1200.tar"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archiveDir)) {
            stream.forEach(listPathToTar::remove);
        }

        Assertions.assertThat(listPathToTar).isEmpty();
    }

    @Test
    public void testTestingEnvironment() throws InterruptedException, IOException {

        executeDumpRoller("testing");

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(archiveDir)) {
            Assert.assertFalse(dirStream.iterator().hasNext());
        }
    }

    @Test
    public void testDirectToS3() throws InterruptedException, IOException {

        executeDumpRoller("development", true);

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(archiveDir)) {
            Assert.assertFalse(dirStream.iterator().hasNext());
        }
    }

    void executeDumpRoller(String environment, boolean directToS3) throws IOException, InterruptedException {
        TmsDumpRollerService.doRollDump(tmpDir.toString(),
            archiveDir.toString(), environment, 1, TIME_OUT_IN_SECONDS, directToS3);
        TmsDumpRollerService.doRollDump(Paths.get(tmpDir.toString(), "clusters").toString(),
            archiveDir.toString(), environment, 1, TIME_OUT_IN_SECONDS, directToS3);
        TmsDumpRollerService.doRollDump(Paths.get(tmpDir.toString(), "fast").toString(),
            archiveDir.toString(), environment, 1, TIME_OUT_IN_SECONDS, directToS3);
    }

    void executeDumpRoller(String environment) throws IOException, InterruptedException {
        executeDumpRoller(environment, false);
    }

}
