package ru.yandex.chemodan.app.dataapi.test;

/**
 * @author Denis Bakharev
 *
 * http://stackoverflow.com/questions/1064596/what-is-javas-equivalent-of-manualresetevent
 */

public class ManualResetEvent {

    private final Object monitor = new Object();
    private volatile boolean open = false;

    public ManualResetEvent(boolean open) {
        this.open = open;
    }

    public void waitOne() throws InterruptedException {
        synchronized (monitor) {
            while (!open) {
                monitor.wait();
            }
        }
    }

    public boolean waitOne(long milliseconds) throws InterruptedException {
        synchronized (monitor) {
            if (open)
                return true;
            monitor.wait(milliseconds);
            return open;
        }
    }

    public void set() {
        synchronized (monitor) {
            open = true;
            monitor.notifyAll();
        }
    }

    public void reset() {
        open = false;
    }
}


