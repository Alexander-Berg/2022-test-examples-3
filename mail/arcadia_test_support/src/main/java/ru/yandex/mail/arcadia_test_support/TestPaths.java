package ru.yandex.mail.arcadia_test_support;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class TestPaths {
    private static final String ARCADIA_ROOT_MARKER = ".arcadia.root";
    private static final Path ARCADIA_YA_CONF_PATH = Paths.get("devtools", "ya", "ya.conf.json");
    private static final Path ARCADIA_ROOT = findArcadiaRoot();

    private static boolean isExists(Path path) {
        return path.toFile().exists();
    }

    // see https://a.yandex-team.ru/arc/trunk/arcadia/library/python/find_root/__init__.py
    private static Path findArcadiaRoot() {
        var cwd = Paths.get("").toAbsolutePath();

        while (cwd != null) {
            if (isExists(cwd.resolve(ARCADIA_ROOT_MARKER)) || isExists(cwd.resolve(ARCADIA_YA_CONF_PATH))) {
                return cwd;
            }

            cwd = cwd.getParent();
        }

        return null;
    }

    private static boolean isUnderYaMake() {
        return !getYaMakeSourcePath("").startsWith("null");
    }

    private static String getYaMakeSourcePath(String path) {
        return ru.yandex.devtools.test.Paths.getSourcePath(path);
    }

    private static String getYaMakeBuildPath(String path) {
        return ru.yandex.devtools.test.Paths.getBuildPath(path);
    }

    private static Path fallback(Path path) {
        if (ARCADIA_ROOT == null) {
            throw new RuntimeException("Can't resolve arcadia root");
        }

        return ARCADIA_ROOT.resolve(path);
    }

    public static Path getSourcePath(String first, String... more) {
        val tailPath = Paths.get(first, more);
        if (isUnderYaMake()) {
            return Paths.get(getYaMakeSourcePath(tailPath.toString()));
        } else {
            return fallback(tailPath);
        }
    }

    public static Path getBuildPath(String first, String... more) {
        val tailPath = Paths.get(first, more);
        if (isUnderYaMake()) {
            return Paths.get(getYaMakeBuildPath(tailPath.toString()));
        } else {
            return fallback(tailPath);
        }
    }
}
