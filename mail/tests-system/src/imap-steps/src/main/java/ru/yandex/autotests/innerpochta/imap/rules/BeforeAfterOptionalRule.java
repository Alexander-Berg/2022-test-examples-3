package ru.yandex.autotests.innerpochta.imap.rules;

import org.junit.runner.Description;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.14
 * Time: 16:54
 */
public abstract class BeforeAfterOptionalRule extends TestWatcherWithExceptions {
    private boolean before = false;
    private boolean after = false;

    @Override
    protected void starting(Description description) throws Exception {
        if (before) {
            call();
        }
    }

    @Override
    protected void finished(Description description) throws Exception {
        if (after) {
            call();
        }
    }


    public BeforeAfterOptionalRule before(boolean before) {
        this.before = before;
        return this;
    }

    public BeforeAfterOptionalRule after(boolean after) {
        this.after = after;
        return this;
    }


    //Override this
    public abstract void call();
}
