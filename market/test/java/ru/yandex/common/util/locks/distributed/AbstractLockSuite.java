package ru.yandex.common.util.locks.distributed;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests, common for all locks.
 *
 * @author maxkar
 *
 */
public abstract class AbstractLockSuite {

    /**
     * Creates a new instance of the lock manager.
     *
     * @return new instance of the lock manager.
     * @throws Exception
     *             if lock manager can't be created.
     */
    protected abstract LockManager createLM() throws Exception;

    /**
     * Cleans up an existing instance of the lock manager.
     *
     * @param lm
     *            lock manager to cleanup.
     * @throws Exception
     *             if something goes wrong.
     */
    protected abstract void cleanup(LockManager lm) throws Exception;

    /**
     * Tests, that locks can be acquired.
     *
     * @throws Exception
     *             if something goes wrong.
     */
    @Test
    public void testBasicLock() throws Exception {
        final LockManager lockManager = createLM();
        try {
            lockManager.lock("abc");
            try {
                System.out.println("Abc locked");
                lockManager.lock("def");
                try {
                    System.out.println("Def locked");
                } finally {
                    lockManager.unlock("def");
                }
            } finally {
                lockManager.unlock("abc");
            }
        } finally {
            cleanup(lockManager);
        }
    }

    /**
     * Tests, that locks can be acquired in sequential manner
     *
     * @throws Exception
     *             if something goes wrong.
     */
    @Test
    public void seqLockTest() throws Exception {
        final LockManager lockManager = createLM();
        try {
            lockManager.lock("abc");
            try {
                System.out.println("Abc locked");
                lockManager.lock("def");
                try {
                    System.out.println("Def locked");
                } finally {
                    lockManager.unlock("def");
                }
            } finally {
                lockManager.unlock("abc");
            }
            lockManager.lock("abc");
            lockManager.unlock("abc");
            lockManager.lock("abc");
            lockManager.lock("def");
            lockManager.unlock("abc");
            lockManager.unlock("def");
        } finally {
            cleanup(lockManager);
        }
    }

    /**
     * Tests, that same lock can't be acquired twice (with different
     * combinations of a lock managers).
     *
     * @throws Exception
     *             if something goes wrong.
     */
    @Test
    public void testIntersects() throws Exception {
        final LockManager lm1 = createLM();
        try {
            final LockManager lm2 = createLM();
            try {
                testIntersects(lm1, lm1);
                testIntersects(lm1, lm2);
            } finally {
                cleanup(lm2);
            }
        } finally {
            cleanup(lm1);
        }
    }

    /**
     * Checks intercestion of locks.
     *
     * @param lm1
     *            first lock manager.
     * @param lm2
     *            second lock manager.
     * @throws LockingException
     *             if something goes wrong.
     * @throws InterruptedException
     *             if test was interrupted.
     */
    protected void testIntersects(LockManager lm1, final LockManager lm2)
            throws LockingException, InterruptedException {
        final AtomicBoolean sc = new AtomicBoolean();
        final AtomicBoolean fail = new AtomicBoolean();
        lm1.lock("abc");
        final Thread sexec;
        try {
            sexec = new Thread(new Runnable() {
                public void run() {
                    try {
                        lm2.lock("abc");
                        try {
                            sc.set(true);
                        } finally {
                            lm2.unlock("abc");
                        }
                    } catch (LockingException e) {
                        fail.set(true);
                    }
                }
            });
            sexec.start();
            Thread.sleep(5000);
            Assert.assertFalse(sc.get());
        } finally {
            lm1.unlock("abc");
        }
        sexec.join();
        Assert.assertFalse(fail.get());
        Assert.assertTrue(sc.get());
    }

}