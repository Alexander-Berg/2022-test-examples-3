package ru.yandex.autotests.market.stat.logbroker;

import org.apache.commons.io.IOUtils;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logbroker.pull.LogBrokerOffset;
import ru.yandex.market.logbroker.pull.LogBrokerSession;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by kateleb on 25.08.17.
 */
public class LogbrokerReader {
    private final LogbrokerConfig config;
    private LogBrokerSession readSession;
    private LogBrokerClient reader;
    private String topic;

    public LogbrokerReader(LogbrokerConfig config) {
        this.config = config;
        createReader(config);
    }

    private void createReader(LogbrokerConfig config) {
        try {
            this.reader = new LogBrokerClient(
                "http://" + config.getMetaHost() + ":" + config.getPort(),
                config.getClientId(),
                config.getDc()
            );
            reader.afterPropertiesSet();

        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Can't create logbroker reader!", e);
        }
    }

    @Step
    public void openSession(String logtype) throws IOException {
        closeSession();
        this.topic = config.getTopic(logtype);
        readSession = reader.openSession(reader.getSuggestPartitions(topic).get(0));
    }

    @Step
    public String readFromLastPosition() throws IOException {
        checkReadSession();
        List<String> list = new ArrayList<>();
        reader.read(readSession, (session1, meta, inputStream, receiveTimeMillis)
            -> list.add(IOUtils.toString(inputStream, "utf-8")));
        return list.stream().collect(joining("\n"));
    }

    @Step
    public void moveReadCursorToTheEnd() throws Exception {
        checkReadSession();
        LogBrokerOffset offset = getOffsets().stream().findFirst().orElseThrow(RuntimeException::new);
        reader.commit(readSession, offset.getLogEnd() - 1);
    }

    @Step
    public List<LogBrokerOffset> getOffsets() throws IOException {
        List<LogBrokerOffset> offsets = reader.getOffsets(config.getIdent()).stream()
            .filter(x -> x.getPartition().equals(topic)).collect(Collectors.toList());
        Attacher.attach("LB offsets found", offsets);
        return offsets;
    }

    @Step
    private void checkReadSession() {
        if (readSession == null || !readSession.isActive()) {
            throw new IllegalArgumentException("No read session opened!");
        }
    }

    @Step
    public void closeSession() {
        if (readSession != null) {
            try {
                readSession.close();
                topic = null;
            } catch (IOException e) {
                Attacher.attachWarning("Can't close read session! Caused by: \n" + e.getMessage());
            }
        }
    }
}
