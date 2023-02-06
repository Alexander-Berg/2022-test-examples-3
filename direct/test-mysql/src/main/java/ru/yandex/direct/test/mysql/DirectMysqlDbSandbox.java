package ru.yandex.direct.test.mysql;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FileUtils;

import ru.yandex.direct.mysql.MySQLDockerContainer;
import ru.yandex.direct.mysql.MySQLServer;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.process.Processes;
import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.io.TempDirectory;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

public class DirectMysqlDbSandbox {
    private static final int SANDBOX_RESOURCE_TTL_DAYS = 366;
    private static final String MYSQL_ERROR_LOG_FILE = "mysql.err";
    // сейчас initialized всегда используется под локом, тем не менее объявлена volatile "на всякий случай"
    private static volatile boolean initialized = false;
    private static volatile RuntimeException initializationException = null;

    private final TestMysqlConfig config;

    public DirectMysqlDbSandbox(TestMysqlConfig config) {
        this.config = config;
    }

    public MySQLServer start() {
        Path mysqlServerPath;
        try {
            Path sandboxPath = getSandboxResourcesRoot().resolve(config.getDataResourceName()).toRealPath();
            Path localPath = Paths.get(ru.yandex.devtools.test.Paths.getBuildPath("my-sql/"));

            if (!localPath.toFile().exists()) {
                localPath.toFile().mkdir();
                FileUtils.copyDirectory(sandboxPath.toFile(), localPath.toFile());
                setPermissions(localPath.toFile());
            }

            mysqlServerPath = localPath;
        } catch (IOException exc) {
            throw new Checked.CheckedException(exc);
        }

        assertFirstStart();

        try {
            return useSandboxMysqlServer(new MySQLServerBuilder())
                    .setDataDir(mysqlServerPath)
                    .addExtraArgs(MySQLServer.MYSQL_OPTIMIZATION_PARAMS)
                    .addExtraArgs(MySQLServer.MYSQL_SERVER_PARAMS)
                    .addExtraArgs("--default-time-zone=" + MOSCOW_TIMEZONE)
                    .start();
        } catch (RuntimeException e) {
            initializationException = e;
            throw e;
        }
    }

    public MySQLServerBuilder useSandboxMysqlServer(MySQLServerBuilder builder) {
        Path mysqlServerPath;
        try {
            mysqlServerPath = getSandboxResourcesRoot().resolve(config.getBinaryResourceName()).toRealPath();
        } catch (IOException exc) {
            throw new Checked.CheckedException(exc);
        }

        if (mysqlServerPath.resolve("ld-linux-x86-64.so.2").toFile().exists()) {
            builder
                    .setMysqldBinary(Arrays.asList(
                            mysqlServerPath.resolve("ld-linux-x86-64.so.2").toString(),
                            "--inhibit-cache",
                            "--library-path", mysqlServerPath.toString(),
                            mysqlServerPath.resolve("sbin/mysqld").toString()
                    ))
                    .setMysqlInstallDbBinary(mysqlServerPath.resolve("bin/mysql_install_db"))
                    .addExtraArgs(
                            "--basedir", mysqlServerPath.toString(),
                            "--lc-messages-dir", mysqlServerPath.toString()
                    );
        } else {
            builder
                    .addEnvironment("LD_LIBRARY_PATH", mysqlServerPath.toString())
                    .addEnvironment("PATH", mysqlServerPath.toString())
                    .setMysqldBinary(mysqlServerPath.resolve("sbin/mysqld"))
                    .setMysqlInstallDbBinary(mysqlServerPath.resolve("bin/mysql_install_db"))
                    .addExtraArgs(
                            "--basedir", mysqlServerPath.toString(),
                            "--lc-messages-dir", mysqlServerPath.toString()
                    );
        }
        String needStrace = System.getProperty("ru.yandex.direct.test.mysql.DirectMysqlDbSandbox.needStrace", "false");
        if (Boolean.valueOf(needStrace)) {
            builder.withNeedStrace(true);
        }
        builder.setErrorLog(getTestOutputsRoot().resolve(MYSQL_ERROR_LOG_FILE));
        // Часто mysql не запускался в тестах c ошибкой:
        // [ERROR] InnoDB: Cannot initialize AIO sub-system
        // Поэтому для тестов асинхронная подсистема ввода/вывода отключена
        builder.disableAio();
        return builder;
    }

    private static synchronized void assertFirstStart() {
        // При работе с MySQL из Sandbox ресурса мы пока не умеем поднимать второй инстанс БД,
        // а если первый запуск не удался - добавляем его ошибку как suppressed
        if (initialized) {
            IllegalStateException ex = new IllegalStateException("Can't start second MySQL DB from Sandbox resource");
            if (initializationException != null) {
                ex.addSuppressed(initializationException);
            }
            throw ex;
        }
        initialized = true;
    }

    private Path getSandboxResourcesRoot() {
        if (ru.yandex.devtools.test.Paths.getSandboxResourcesRoot() == null) {
            throw new IllegalStateException("Sandbox resources are not available");
        }
        return Paths.get(ru.yandex.devtools.test.Paths.getSandboxResourcesRoot());
    }

    private Path getTestOutputsRoot() {
        if (ru.yandex.devtools.test.Paths.getTestOutputsRoot() == null) {
            throw new IllegalStateException("Sandbox test outputs root are not available");
        }
        return Paths.get(ru.yandex.devtools.test.Paths.getTestOutputsRoot());
    }

    public void uploadMysqlServer(Path arcadiaRoot, MySQLDockerContainer mysql)
            throws IOException, InterruptedException {
        try (TempDirectory tmpDir = new TempDirectory(config.getBinaryResourceName())) {
            Path mysqlServerPath = Files.createDirectory(tmpDir.getPath().resolve("mysql-server"));
            Files.createDirectory(mysqlServerPath.resolve("sbin"));

            String image = mysql.getImage();
            String[] imageParts = image.split(":");
            if (imageParts.length != 2) {
                throw new IllegalStateException("Can't extract version from image name: " + image);
            }
            Version mysqlVersion = Version.valueOf(imageParts[1]);

            if (MySQLServerBuilder.requiresMysqlInstallDb(mysqlVersion)) {
                Files.createDirectory(mysqlServerPath.resolve("bin"));
                Files.createDirectory(mysqlServerPath.resolve("share"));

                mysql.copyFromContainer(
                        Paths.get("/usr/bin/mysql_install_db"),
                        mysqlServerPath.resolve("bin/mysql_install_db")
                );
                mysql.copyFromContainer(
                        Paths.get("/usr/bin/my_print_defaults"),
                        mysqlServerPath.resolve("bin/my_print_defaults")
                );

                for (String name : Arrays.asList(
                        "fill_help_tables.sql",
                        "mysql_system_tables.sql",
                        "mysql_system_tables_data.sql",
                        "mysql_security_commands.sql"
                )) {
                    mysql.copyFromContainer(
                            Paths.get("/usr/share/mysql", name),
                            mysqlServerPath.resolve("share").resolve(name)
                    );
                }
            }

            mysql.copyFromContainer(
                    Paths.get("/usr/sbin/mysqld"),
                    mysqlServerPath.resolve("sbin/mysqld")
            );
            mysql.copyFromContainer(
                    Paths.get("/usr/share/mysql/english/errmsg.sys"),
                    mysqlServerPath.resolve("errmsg.sys")
            );

            if (mysqlVersion.lessThan(Version.forIntegers(5, 7))) {
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libaio.so.1.0.1"),
                        mysqlServerPath.resolve("libaio.so.1"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libwrap.so.0.7.6"),
                        mysqlServerPath.resolve("libwrap.so.0"));
            } else if (mysqlVersion.lessThan(Version.forIntegers(5, 7, 33))) {
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libpthread-2.19.so"),
                        mysqlServerPath.resolve("libpthread.so.0"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libaio.so.1.0.1"),
                        mysqlServerPath.resolve("libaio.so.1"));
                mysql.copyFromContainer(Paths.get("/usr/lib/x86_64-linux-gnu/libnuma.so.1.0.0"),
                        mysqlServerPath.resolve("libnuma.so.1"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libwrap.so.0.7.6"),
                        mysqlServerPath.resolve("libwrap.so.0"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libcrypt-2.19.so"),
                        mysqlServerPath.resolve("libcrypt.so.1"));
                mysql.copyFromContainer(Paths.get("/usr/lib/x86_64-linux-gnu/libssl.so.1.0.0"),
                        mysqlServerPath.resolve("libssl.so.1.0.0"));
                mysql.copyFromContainer(Paths.get("/usr/lib/x86_64-linux-gnu/libcrypto.so.1.0.0"),
                        mysqlServerPath.resolve("libcrypto.so.1.0.0"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libdl-2.19.so"),
                        mysqlServerPath.resolve("libdl.so.2"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libz.so.1.2.8"),
                        mysqlServerPath.resolve("libz.so.1"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/librt-2.19.so"),
                        mysqlServerPath.resolve("librt.so.1"));
                mysql.copyFromContainer(Paths.get("/usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.20"),
                        mysqlServerPath.resolve("libstdc++.so.6"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libm-2.19.so"),
                        mysqlServerPath.resolve("libm.so.6"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libgcc_s.so.1"),
                        mysqlServerPath.resolve("libgcc_s.so.1"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libc-2.19.so"),
                        mysqlServerPath.resolve("libc.so.6"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/ld-2.19.so"),
                        mysqlServerPath.resolve("ld-linux-x86-64.so.2"));
                mysql.copyFromContainer(Paths.get("/lib/x86_64-linux-gnu/libnsl-2.19.so"),
                        mysqlServerPath.resolve("libnsl.so.1"));
            } else {
                var libs = List.of(
                        // ldd /usr/sbin/mysqld|grep '=>'|perl -lane 'print qq!"$F[0]", "$F[2]",!'
                        "libpthread.so.0", "/lib64/libpthread-2.28.so",
                        "libaio.so.1", "/lib64/libaio.so.1.0.1",
                        "libnuma.so.1", "/lib64/libnuma.so.1.0.0",
                        "libcrypt.so.1", "/lib64/libcrypt.so.1.1.0",
                        "libssl.so.1.1", "/lib64/libssl.so.1.1.1g",
                        "libcrypto.so.1.1", "/lib64/libcrypto.so.1.1.1g",
                        "libdl.so.2", "/lib64/libdl-2.28.so",
                        "libz.so.1", "/lib64/libz.so.1.2.11",
                        "librt.so.1", "/lib64/librt.so.1",
                        "libstdc++.so.6", "/lib64/libstdc++.so.6",
                        "libm.so.6", "/lib64/libm.so.6",
                        "libgcc_s.so.1", "/lib64/libgcc_s.so.1",
                        "libc.so.6", "/lib64/libc.so.6",

                        // hardcoded
                        "libc.so.6", "/lib64/libc.so.6",
                        "ld-linux-x86-64.so.2", "/lib64/ld-2.28.so",
                        "libnsl.so.2", "/lib64/libnsl.so.2.0.0"
                );
                for (int i = 0; i < libs.size(); i += 2) {
                    mysql.copyFromContainer(Paths.get(libs.get(i + 1)),
                            mysqlServerPath.resolve(libs.get(i)));
                }
            }

            long sbr = uploadToSandbox(
                    arcadiaRoot,
                    mysqlServerPath,
                    SANDBOX_RESOURCE_TTL_DAYS,
                    "linux",
                    "mysqld binary for Ya.Direct tests"
            );

            Path mysqlServerIncPath =
                    arcadiaRoot.resolve(config.getIncludesPath()).resolve("mysql-server.inc");
            String newMysqlServerInc = "IF(HOST_OS_LINUX)\n    DATA(sbr://" + sbr + ")\n    DISABLE_DATA_VALIDATION()\nENDIF()\n";
            Files.write(mysqlServerIncPath, newMysqlServerInc.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void uploadMysqlData(Path arcadiaRoot, MySQLDockerContainer mysql)
            throws IOException, InterruptedException {
        try (TempDirectory tmpDir = new TempDirectory("mysql-data")) {
            Path tmpMysqlData = tmpDir.getPath().resolve(config.getDataResourceName());
            mysql.copyFromContainer(Paths.get(DirectMysqlDb.MYSQL_DATA_DIR), tmpMysqlData);
            long sbr = uploadToSandbox(
                    arcadiaRoot,
                    tmpMysqlData,
                    SANDBOX_RESOURCE_TTL_DAYS,
                    "linux",
                    "mysql data dir for Ya.Direct tests"
            );

            Path mysqlDataIncPath =
                    arcadiaRoot.resolve(config.getIncludesPath()).resolve("mysql-data.inc");
            String newMysqlDataInc = "IF(HOST_OS_LINUX)\n    DATA(sbr://" + sbr + ")\n    DISABLE_DATA_VALIDATION()\nENDIF()\n";
            Files.write(mysqlDataIncPath, newMysqlDataInc.getBytes(StandardCharsets.UTF_8));
        }
    }

    private long uploadToSandbox(Path arcadiaRoot, Path path, int ttl, String arch, String description)
            throws InterruptedException {
        Processes.CommunicationResult result = Processes.communicateSafe(new ProcessBuilder(
                arcadiaRoot.resolve("ya").toString(),
                "upload",
                "--description=" + description,
                "--arch=" + arch,
                "--ttl=" + Integer.toString(ttl),
                path.toString()
        ));

        if (!result.getStdout().trim().isEmpty()) {
            throw new IllegalStateException("Unexpected output to stdout from ya: " + result.getStdout());
        }

        Matcher matcher = Pattern
                .compile("^Created resource id is (\\d+)$", Pattern.MULTILINE)
                .matcher(result.getStderr());

        if (matcher.find()) {
            return Long.valueOf(matcher.group(1));
        } else {
            throw new IllegalStateException("Failed sandbox upload, stderr: " + result.getStderr());
        }
    }

    private void setPermissions(File localPath) throws IOException {
        for (String pathname : localPath.list()) {
            var file = localPath.toPath().resolve(pathname).toFile();
            var perms = new HashSet<PosixFilePermission>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);

            if (file.isDirectory()) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
            }

            Files.setPosixFilePermissions(file.toPath(), perms);

            if (file.isDirectory()) {
                setPermissions(file);
            }
        }
    }
}
