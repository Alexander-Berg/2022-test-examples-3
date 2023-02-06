package ru.yandex.search.salo;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import ru.yandex.json.dom.ValueContentHandler;

public class TestLock {
    private final String name;
    private long operationId;
    private String token;
    private long deadline;
    private List<Object> envelopes = Collections.emptyList();

    public TestLock(final String name, final long operationId) {
        this.name = name;
        this.operationId = operationId;
        this.deadline = 0;
    }

    public synchronized List<Object> envelopes() {
        return envelopes;
    }

    public synchronized long operationId() {
        return operationId;
    }

    public synchronized String tryGetLock(final long millis) {
        long currentTime = System.currentTimeMillis();
        if (currentTime < deadline) {
            return null;
        }
        deadline = currentTime + millis;
        token = name + 'x' + deadline;
        return token;
    }

    private static <T> String unexpected(
        final String name,
        final T expect,
        final T actual)
    {
        return name + ": expect " + expect + ", but " + actual;
    }

    // CSOFF: ParameterNumber
    public synchronized boolean compareAndSet(
        final long update,
        final String token,
        final Reader response,
        final Logger logger)
    {
        boolean ok = true;
        if (System.currentTimeMillis() >= deadline) {
            ok = false;
            logger.info("expired");
        }
        if (ok && !this.token.equals(token)) {
            ok = false;
            logger.info(unexpected("token", this.token, token));
        }
        if (!ok) {
            return false;
        }
        operationId = update;
        List<Object> envelopes = new ArrayList<>(this.envelopes);
        try (BufferedReader reader = new BufferedReader(response)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                logger.info("line: " + line);
                if (!line.isEmpty()) {
                    envelopes.add(ValueContentHandler.parse(line));
                }
            }
        } catch (Exception e) {
            envelopes.add(e);
        }
        this.envelopes = Collections.unmodifiableList(envelopes);
        return true;
    }
    // CSON: ParameterNumber
}
