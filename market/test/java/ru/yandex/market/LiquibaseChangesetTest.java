package ru.yandex.market;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource({
        "classpath:liquibase"
})
public class LiquibaseChangesetTest {

    private static final String PATH_TO_CHANGESETS_YA_MAKE = "classpath:liquibase/*";
    private static final Set<String> PERMITTED_EXTENSIONS = Set.of(".xml", ".sql");

    @Test
    @DisplayName("Проверка, что ченджлоги содержат все файлы с ченджсетами и других файлов в ченджлогах нет.")
    public void checkAllSqlFilesAreInChangelogs() throws Exception {
        PathMatchingResourcePatternResolver res = new PathMatchingResourcePatternResolver();
        Resource[] resources = res.getResources(PATH_TO_CHANGESETS_YA_MAKE);
        for (Resource resource : resources) {
            if (resource.isFile()) {
                File file = resource.getFile();
                if (file.isDirectory()) {
                    recursiveCompareFiles(file);
                }
            }
        }
    }

    /**
     * Провести сравнение ченджлога и существующих файлов для выбранной директории и всех вложенных.
     * Каталоги обходятся рекурсивно для случая годовых папок, в которых внутри еще есть кварталы.
     * Однако файлики из всех кварталов лежат в общем годовом ченджлоге.
     * Именно поэтому продакшн в исключении, т.к. там три уровня и есть общий ченджлог, который читать не надо.
     *
     * @param dir - директория, в которой ведем сравнение.
     */
    private void recursiveCompareFiles(File dir) throws Exception {
        Map<String, File> filesMap = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .collect(Collectors.toMap(File::getName, f -> f));

        if (filesMap.containsKey("changelog.xml") && !dir.getName().equals("production")) {
            Set<String> existingFilesInDir = findExistingFilesInDir(dir, 0);
            Set<String> changelogFileNames = findChangelogFileNames(filesMap.get("changelog.xml"));
            Assertions.assertEquals(existingFilesInDir, changelogFileNames,
                    "Changelog doesn't equal to files list in dir \"" + dir.getName() + "\"");
        } else {
            for (File file : filesMap.values()) {
                if (file.isDirectory()) {
                    recursiveCompareFiles(file);
                }
            }
        }
    }

    /**
     * Найти все файлы в заданном каталоге
     *
     * @param dir   - каталог, в котором ищем файлы
     * @param level - уровень, относительно начального каталога для создания относительного пути
     * @return множество файлов, найденных в каталоге.
     */
    private Set<String> findExistingFilesInDir(File dir, int level) {
        Set<String> res = new HashSet<>();
        try (Stream<Path> stream = Files.list(dir.toPath())) {
            stream.peek(path -> {
                File file = path.toFile();
                if (file.isDirectory()) {
                    res.addAll(findExistingFilesInDir(file, level + 1));
                } else if (PERMITTED_EXTENSIONS.contains(file.getName().substring(file.getName().length() - 4))) {
                    res.add(level == 0 ? file.getName() : dir.getName() + "/" + file.getName());
                }
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        res.remove("changelog.xml");
        return res;
    }


    /**
     * Найти все файлы ченджсетов, подгружаемые в данный файл ченджлога
     *
     * @param changelogFile - файл ченджлога, в котором ведется поиск
     * @return Список файлов ченджсетов из данного ченджлога.
     */
    private Set<String> findChangelogFileNames(File changelogFile) throws Exception {
        Set<String> changelogFileNames = new HashSet<>();
        String changelogText = String.join("\n", Files.readAllLines(changelogFile.toPath()));
        Pattern pattern = Pattern.compile("include file=\"(.*?)\"");
        Matcher matcher = pattern.matcher(changelogText);
        while (matcher.find()) {
            changelogFileNames.add(matcher.group(1));
        }

        return changelogFileNames;
    }
}
