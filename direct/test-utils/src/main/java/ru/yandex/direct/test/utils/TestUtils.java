package ru.yandex.direct.test.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;

import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class TestUtils {
    private static final Random RANDOM = new Random();

    public static void assumeThat(Consumer<SoftAssertions> matcher) {
        try {
            SoftAssertions.assertSoftly(matcher);
        } catch (AssertionError e) {
            throw new CustomAssumptionException(e);
        }
    }

    /**
     * Создаёт случайную строку. Случайные символы соответствуют [0-9a-v].
     *
     * @param prefix      Строка, которая будет подставлена перед случайными символами.
     * @param totalLength Желаемая длина результирующей строки.
     * @param suffix      Строка, которая будет подставлена после случайных символов.
     */
    public static String randomName(String prefix, int totalLength, String suffix) {
        int bytesNeed = totalLength - prefix.length() - suffix.length();
        if (bytesNeed <= 0) {
            throw new IllegalArgumentException("Prefix and suffix are too long for specified total length");
        }
        return prefix + new BigInteger(5 * bytesNeed, RANDOM).toString(32) + suffix;
    }

    /**
     * {@link #randomName(String, int, String)} с пустым суффиксом.
     */
    public static String randomName(String prefix, int totalLength) {
        return randomName(prefix, totalLength, "");
    }

    public static <T> void assumeThat(String message, T actual, Matcher<T> matcher) {
        try {
            Assertions.assertThat(actual).as(message).is(matchedBy(matcher));
        } catch (AssertionError e) {
            throw new CustomAssumptionException(e);
        }
    }

    public static <T> void assumeThat(T actual, Matcher<T> matcher) {
        assumeThat("", actual, matcher);
    }

    public static void assertEqualDirsWithDiff(Path origDir, Path newDir, String charset) throws IOException {
        Set<Path> newDirFiles = getDirFiles(newDir).collect(Collectors.toSet());
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(getDirFiles(newDir).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(origDir);

        for (Path path : newDirFiles) {
            sa.assertThat(new String(Files.readAllBytes(newDir.resolve(path)), charset))
                    .as("File contents changed: " + path)
                    .isEqualTo(new String(Files.readAllBytes(origDir.resolve(path)), charset));
        }
        sa.assertAll();
    }

    public static void assertEqualDirs(Path origDir, Path newDir) throws IOException {
        Set<Path> newDirFiles = getDirFiles(newDir).collect(Collectors.toSet());

        SoftAssertions sa = new SoftAssertions();

        sa.assertThat(getDirFiles(origDir).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(newDirFiles);

        for (Path path : newDirFiles) {
            sa.assertThat(newDir.resolve(path).toFile())
                    .as("File contents changed: " + path)
                    .hasSameTextualContentAs(origDir.resolve(path).toFile());
        }
        sa.assertAll();
    }

    public static Stream<Path> getDirFiles(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .map(dir::relativize);
    }
}
