package ru.yandex.crypta.graph2.dao.yt.local.fastyt.testdata;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingIterator<E> implements Iterator<E> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingIterator.class);

    private final Iterator<E> delegate;
    private final String name;
    private final int logFreq;
    private final boolean logRec;
    private int currentIndex = 0;
    private boolean finalIndexLogged = false;

    public LoggingIterator(Iterator<E> delegate, String name, int logFreq, boolean logRec) {
        this.delegate = delegate;
        this.name = name == null ? "" : name;
        this.logFreq = logFreq;
        this.logRec = logRec;
    }

    public LoggingIterator(Iterator<E> delegate, String name, int logFreq) {
        this(delegate, name, logFreq, false);
    }

    public LoggingIterator(Iterator<E> delegate, int logFreq) {
        this(delegate, "", logFreq, false);
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = delegate.hasNext();
        if (!hasNext && logFreq != 0 && !finalIndexLogged) {
            LOG.info("Reading {}: {}", name, currentIndex);
            finalIndexLogged = true;
        }
        return hasNext;
    }

    @Override
    public E next() {
        E rec = delegate.next();
        if (logFreq != 0) {
            if (currentIndex % logFreq == 0) {
                if (logRec) {
                    LOG.info("Reading {}: {} {}", name, currentIndex, rec);
                } else {
                    LOG.info("Reading {}: {}", name, currentIndex);
                }
            }
            currentIndex++;
        }
        return rec;
    }

    public static <E> Stream<E> logStream(Stream<E> source, String name, int logFreq, boolean logRec) {
        final AtomicLong currentIndex = new AtomicLong();
        return source.peek(rec -> {
            long index = currentIndex.incrementAndGet();
            if (index % logFreq == 0) {
                if (logRec) {
                    LOG.info("Reading {}: {} {}", name, index, rec);
                } else {
                    LOG.info("Reading {}: {}", name, index);
                }
            }
        });
    }
}
