package ru.yandex.autotests.market.stat.logbroker;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.market.logbroker.push.PushSession;
import ru.yandex.market.logbroker.push.models.DataSendResult;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.joining;

/**
 * Created by kateleb on 25.08.17.
 */
public class LogbrokerWriter {
    private static final AtomicLong SEQNO = new AtomicLong(1);
    private static final String FILE = "some_dummy_filename.log";
    private final LogbrokerConfig config;
    private PushSession writeSession;


    public LogbrokerWriter(LogbrokerConfig config) {
        this.config = config;
    }

    private long next() {
        return SEQNO.getAndIncrement();
    }

    @Step("Write lines to Lb")
    public void write(List<String> lines) {
        checkWriteSession();
        Attacher.attach("Data to send", lines);

        for (List<String> part : Lists.partition(lines, config.getChunkSize())) {
            writeChunk(part.stream().collect(joining("\n")));
        }
    }

    @Step
    public void writeChunk(String text) {
        Attacher.attachAction("Will write chunk of data with lengh: " + text.length());
        checkWriteSession();
        sendData(text, next());
    }

    private void sendData(String text, long seqno) {
        try {
            ListenableFuture<DataSendResult> future = writeSession.sendData(text.getBytes(), seqno);
            future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalArgumentException("Can't write data to logbroker, error occurred. \ncaused by: \n" + e.getMessage());
        }
    }


    @Step
    public void openSession(String logtype) throws InterruptedException {
        closeSession();
        writeSession = PushSession.builder()
            .server(config.getMetaHost())
            .dc(config.getDc())
            .ident(config.getIdent())
            .logType(logtype)
            .file(FILE)
            .sourceId(UUID.randomUUID().toString())
            .build();

        writeSession.start();

        long startTime = Instant.now().getEpochSecond();

        while (Instant.now().getEpochSecond() - startTime < 30 && !writeSession.isAlive()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        Preconditions.checkState(writeSession.isAlive(), String.format("Can't open write session for logtype %s\nident: %s, dc: %s, host: %s",
            logtype, config.getIdent(), config.getDc(), config.getMetaHost()));
    }

    @Step
    private void checkWriteSession() {
        if (writeSession == null || !writeSession.isAlive()) {
            throw new IllegalArgumentException("No open write session found!");
        }
    }

    public void closeSession() {
        if (writeSession != null && writeSession.isAlive()) {
            try {
                writeSession.stop();
            } catch (Exception e) {
                Attacher.attachWarning("Can't close write session! Caused by: \n" + e.getMessage());
            }
        }
    }

    @Step
    public String getSessionStatus() {
        String status = writeSession.getStatus();
        Attacher.attach("Session status", status);
        return status;
    }
}
