package ru.yandex.direct.dbschemagen;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Duration;
import java.util.Collections;
import java.util.stream.Stream;

import org.jooq.meta.jaxb.ForcedType;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.mysql.MySQLDockerContainer;
import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.process.Docker;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.utils.io.TempDirectory;

import static org.junit.Assume.assumeTrue;

/**
 * Проверяет, что генерируемая 'игрушечная' схема не отличается от эталонной (хранится в ресурсах). При перенастройке
 * или обновлении jooq могут появиться отличия. В этом случае следует запустить {@link TestGen#main(String[])} и,
 * убедившись в корректности новой схемы, закоммитить ее.
 * <p>
 * N.B.: Новая схема генерируется в текущем каталоге, так что при запуске из Idea файлы будут лежать в каталоге с
 * проектом Idea(~/IdeaProjects/[proj_dir]), а не в каталоге репозитория.
 * Не забудь скопировать файлы в корректный каталог!
 * </p>
 */
@YaIgnore
@Ignore("Для запуска вручную. Вообще тест не должен работать, если посмотреть на schema1_generated_code")
public class TestGen {
    public static final Docker DOCKER = new Docker();
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    public static void main(String[] args) throws Exception {
        try (MySQLDockerContainer mysql = new MySQLDockerContainer(DOCKER)) {
            generateWithUnderscores(
                    createDbSchemaGen(new JooqDbInfo(mysql)),
                    Paths.get("."),
                    mysql
            );
        }
    }

    @Test
    public void testGen() throws Exception {
        assumeTrue(DOCKER.isAvailable());
        try (
                TempDirectory generatedCodeDir = new TempDirectory("DbSchemaGenTest");
                MySQLDockerContainer mysql = new MySQLDockerContainer(DOCKER)
        ) {
            generateWithUnderscores(
                    createDbSchemaGen(new JooqDbInfo(mysql)),
                    generatedCodeDir.getPath(),
                    mysql
            );
            TestUtils.assertEqualDirs(
                    Paths.get(TestGen.class.getClassLoader().getResource("schema1_generated_code").toURI()),
                    generatedCodeDir.getPath()
            );
        }
    }

    private static DbSchemaGen createDbSchemaGen(JooqDbInfo db) throws URISyntaxException {
        return new DbSchemaGen("com.example", db)
                .withForcedTypes(Collections.singletonList(
                        new ForcedType().withName("bigint").withExpression("schema1.table1.ulong_val")
                ));
    }

    private static void generateWithUnderscores(DbSchemaGen generator, Path targetDir, MySQLInstance connector)
            throws Exception {
        Path schemaDir = Paths.get(TestGen.class.getClassLoader().getResource("schema1").toURI());

        for (Path schemaSqlDir : Collections.singletonList(schemaDir)) {
            String schemaName = schemaSqlDir.getFileName().toString();
            try (Connection conn = connector.connect(CONNECT_TIMEOUT)) {
                DbSchemaGen.createSchema(conn, schemaName);
                conn.setCatalog(schemaName);
                DbSchemaGen.populate(schemaSqlDir, conn, true);
                generator.generate(schemaName, targetDir);
            }
        }

        try (Stream<Path> files = Files.walk(targetDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (file.toString().endsWith(".java")) {
                    // yatool не различает директории с исходниками и ресурсами,
                    // поэтому, если не переименовыввать *.java файлы, yatool
                    // решит, что это исходники.
                    Files.move(file, Paths.get(file.toString() + "_"));
                }
            }
        }
    }
}
