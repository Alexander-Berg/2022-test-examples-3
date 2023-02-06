package ru.yandex.chemodan.app.psbilling.core.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.misc.db.embedded.SchemaResolver;
import ru.yandex.misc.db.embedded.SchemaResolverFS;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

import static ru.yandex.misc.test.Assert.assertEquals;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MigrationsTest extends AbstractPsBillingDBTest {
    private final static Logger logger = LoggerFactory.getLogger(MigrationsTest.class);

    @Test
    @SneakyThrows
    public void migrationsNum() {
        String path = "ps_billing_db/migrations/";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ListF<String> migrationFiles = Cf.toList(Collections.list(cl.getResources(path)))
                .flatMap(x1 -> {
                    try {
                        return loadScriptsFromResource(path, x1);
                    } catch (Exception e) {
                        throw new IllegalStateException();
                    }
                }).sorted();

        SchemaResolverFS resolverFS = new SchemaResolverFS("ps_billing_db");
        ListF<SchemaResolver.ResolvedMigration> resolvedMigrations =
                Cf.toList(resolverFS.resolveQueries()).filter(x -> x.getVersion() != null);

        assertEquals(resolvedMigrations.size(), migrationFiles.length());
        MapF<Integer, Integer> duplicateVersions = resolvedMigrations.map(SchemaResolver.ResolvedMigration::getVersion)
                .groupBy(x -> x).mapValues(ListF::length).filterValues(count -> count > 1);
        Assert.isEmpty(duplicateVersions.keys(), "duplicate migration nums found");

        for (SchemaResolver.ResolvedMigration migration : resolvedMigrations) {
            String script = migration.getScript();
            Pattern ruLettersPattern = Pattern.compile("[а-яА-Я]");
            if (ruLettersPattern.matcher(script).find()) {
                Assert.isTrue(
                        String.format("migration №%d \"%s\" should contain \"/* pgmigrate-encoding: utf-8 */\"",
                                migration.getVersion(), migration.getDescription()),
                        script.contains("/* pgmigrate-encoding: utf-8 */"));
            }
        }
    }

    private ListF<String> loadScriptsFromResource(String path, URL resource) throws IOException {
        String fullPath = resource.getFile();
        logger.info("FullPath to script {} {}", path, fullPath);
        try {
            File directory = new File(resource.toURI());
            if (directory.exists()) {
                return loadScriptsFromDir(directory);
            }
        } catch (Exception e) {
            logger.info("checking jar {}", e.getMessage());
        }
        return loadScriptsFromJar(fullPath, path);
    }

    private ListF<String> loadScriptsFromJar(String fullPath, String path) throws IOException {
        ListF<String> files = new ArrayListF<>();
        String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(path) && entryName.length() > (path.length() + "/".length())) {
                    String scriptName = entryName.substring(entryName.lastIndexOf("/") + 1);
                    logger.info("JarEntry: {}, script: {}", entryName, scriptName);
                    files.add(scriptName);
                }
            }
        }
        return files;
    }

    private ListF<String> loadScriptsFromDir(File directory) {
        ListF<String> files = new ArrayListF<>();
        // Get the list of the files contained in the package
        File[] allScripts = directory.listFiles();
        assert allScripts != null;
        for (File s : allScripts) {
            logger.info("resolved script on FS {}", s);
            files.add(s.getName());
        }
        return files;
    }
}
