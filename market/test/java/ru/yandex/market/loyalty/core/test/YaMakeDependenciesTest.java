package ru.yandex.market.loyalty.core.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Только для локального запуска
 * <p>
 * Подготовка к тестам - выполнить команду 'ya java dependency-tree >> file_name' на двух разных ветках,
 * зависимости которых нужно сравнить.
 * Полученные файлы сравнивать глазами, например через View -> Compare With... в IDEA
 */
@Disabled
public class YaMakeDependenciesTest {
    private static final String OLD_TREE_FILE = "/Users/mr-dm1try/Documents/YaMake/OldYaTree.txt";
    private static final String NEW_TREE_FILE = "/Users/mr-dm1try/Documents/YaMake/NewYaTree.txt";

    @Test
    public void printAllDepsFromTree() throws IOException {
        var oldDeps = "/Users/mr-dm1try/Documents/YaMake/OldDeps.txt";
        var newDeps = "/Users/mr-dm1try/Documents/YaMake/NewDeps.txt";

        var oldDependencies = treeToDependencies(OLD_TREE_FILE, false);
        var newDependencies = treeToDependencies(NEW_TREE_FILE, false);

        dependenciesToFile(oldDependencies, oldDeps);
        dependenciesToFile(newDependencies, newDeps);
    }

    @Test
    public void printUsedDepsFromTreeByProject() throws IOException {
        var oldDepsUsed = "/Users/mr-dm1try/Documents/YaMake/OldDepsUsedByProject.txt";
        var newDepsUsed = "/Users/mr-dm1try/Documents/YaMake/NewDepsUsedByProject.txt";

        var oldDeps = treeToDependenciesByProject(OLD_TREE_FILE, true);
        var newDeps = treeToDependenciesByProject(NEW_TREE_FILE, true);

        diffDependenciesByProjectToFiles(oldDeps, newDeps, oldDepsUsed);
        diffDependenciesByProjectToFiles(newDeps, oldDeps, newDepsUsed);
    }

    @Test
    public void printUsedDepsFromTree() throws IOException {
        var oldDepsUsed = "/Users/mr-dm1try/Documents/YaMake/OldDepsUsed.txt";
        var newDepsUsed = "/Users/mr-dm1try/Documents/YaMake/NewDepsUsed.txt";

        var oldDeps = treeToDependencies(OLD_TREE_FILE, true);
        var newDeps = treeToDependencies(NEW_TREE_FILE, true);

        diffDependencies(oldDeps, newDeps, oldDepsUsed);
        diffDependencies(newDeps, oldDeps, newDepsUsed);
    }

    private void diffDependencies(
            Map<String, Set<String>> writeMap, Map<String, Set<String>> diffMap, String fileName
    ) throws IOException {
        try (var writer = new FileWriter(fileName);) {
            writer.write("");

            writeMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(kv -> !diffMap.getOrDefault(kv.getKey(), Collections.emptySet())
                            .equals(kv.getValue())
                    )
                    .forEachOrdered(kv -> writeDependency(kv, writer, 0));
        }
    }

    private void diffDependenciesByProjectToFiles(
            Map<String, Map<String, Set<String>>> writeMap,
            Map<String, Map<String, Set<String>>> diffMap,
            String fileName
    ) throws IOException {
        try (var writer = new FileWriter(fileName);) {
            writer.write("");

            writeMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(projectKv -> {
                        try {
                            writer.append(projectKv.getKey()).append("\n"); // project name
                            projectKv.getValue().entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .filter(kv -> !diffMap.getOrDefault(projectKv.getKey(), Collections.emptyMap())
                                            .getOrDefault(kv.getKey(), Collections.emptySet())
                                            .equals(kv.getValue())
                                    )
                                    .forEachOrdered(kv -> writeDependency(kv, writer, 1));
                        } catch (IOException e) {
                        }
                    });
        }
    }

    private Map<String, Map<String, Set<String>>> treeToDependenciesByProject(
            String fileName, boolean usedOnly
    ) throws IOException {
        var dependencies = new HashMap<String, Map<String, Set<String>>>();
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            var currentProject = new AtomicReference<>("");
            stream.forEachOrdered(line -> {
                if (!line.startsWith("|")) {
                    var lastSlashIndex = line.lastIndexOf("/");
                    currentProject.set(line.substring(lastSlashIndex + 1));
                    dependencies.put(currentProject.get(), new HashMap<>());
                    return;
                } else if (usedOnly && !line.contains("(*)")) {
                    return;
                }

                addDependencyFromLine(dependencies.get(currentProject.get()), line);
            });
        }
        return dependencies;
    }

    private Map<String, Set<String>> treeToDependencies(String fileName, boolean usedOnly) throws IOException {
        var dependencies = new HashMap<String, Set<String>>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.filter(line -> !usedOnly || line.contains("(*)"))
                    .forEach(line -> addDependencyFromLine(dependencies, line));
        }

        return dependencies;
    }

    private void dependenciesToFile(Map<String, Set<String>> deps, String fileName) throws IOException {
        try (var writer = new FileWriter(fileName)) {
            writer.write("");
            deps.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(kv -> writeDependency(kv, writer, 0));
        }
    }

    private void writeDependency(Map.Entry<String, Set<String>> dependency, Writer writer, int additionalTabCount) {
        var tabs = "\t".repeat(additionalTabCount);
        var newLine = "\n\t" + tabs;
        try {
            writer.append(tabs).append(dependency.getKey()).append(newLine)
                    .append(
                            dependency.getValue().stream()
                                    .sorted()
                                    .collect(Collectors.joining(newLine))
                    ).append("\n");
        } catch (IOException e) {
        }
    }

    private void addDependencyFromLine(Map<String, Set<String>> map, String treeLine) {
        var tempStr = treeLine.replaceFirst(".*contrib/java/", "")
                .replaceFirst(".*\\|-->", "");
        var lastSlashIndex = tempStr.lastIndexOf("/");
        var depName = tempStr.substring(0, lastSlashIndex);
        var depVer = tempStr.substring(lastSlashIndex + 1);

        map.merge(depName, Sets.newHashSet(depVer), (a, b) -> {
            a.addAll(b);
            return a;
        });
    }
}
