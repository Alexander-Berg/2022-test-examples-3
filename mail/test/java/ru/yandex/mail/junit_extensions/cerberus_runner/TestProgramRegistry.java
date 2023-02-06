package ru.yandex.mail.junit_extensions.cerberus_runner;

import lombok.SneakyThrows;
import lombok.val;
import ru.yandex.mail.junit_extensions.program_runner.ProgramOptions;
import ru.yandex.mail.junit_extensions.program_runner.ProgramRegistry;
import ru.yandex.mail.pglocal.junit_jupiter.Constants;

import java.net.ServerSocket;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class TestProgramRegistry implements ProgramRegistry {
    static final String DB_NAME = "cerberusdb";
    static final int CERBERUS_PORT = getRandomPort();

    private static final String DB_HOST = "localhost";
    private static final String DB_USER = Constants.DATABASE_USER;
    private static final String DB_PASSWORD = "";

    @SneakyThrows
    private static int getRandomPort() {
        try (val socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public Optional<ProgramOptions> findProgramOptions(String id) {
        if (id.equals(Cerberus.NAME)) {
            val dbPort = requireNonNull(System.getProperty(Constants.DATABASE_PORT_PROPERTY));
            val dbHost = DB_HOST + ':' + dbPort;
            return Optional.of(Cerberus.options(CERBERUS_PORT, singletonList(dbHost), DB_NAME, DB_USER, DB_PASSWORD));
        } else {
            return Optional.empty();
        }
    }
}
