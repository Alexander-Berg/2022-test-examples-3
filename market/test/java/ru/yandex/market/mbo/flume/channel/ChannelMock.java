package ru.yandex.market.mbo.flume.channel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleState;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.08.17
 */
public class ChannelMock implements Channel {
    private String name;
    private Queue<Event> events = new LinkedList<>();

    public ChannelMock() {
    }

    public ChannelMock(String name) {
        this.name = name;
    }

    public void addEvents(Collection<? extends Event> events) {
        this.events.addAll(events);
    }

    @Override
    public void put(Event event) throws ChannelException {
    }

    @Override
    public Event take() throws ChannelException {
        return events.poll();
    }

    @Override
    public Transaction getTransaction() {
        return null;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public LifecycleState getLifecycleState() {
        return null;
    }
}
