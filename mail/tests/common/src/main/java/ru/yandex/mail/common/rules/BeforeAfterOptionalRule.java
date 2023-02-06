package ru.yandex.mail.common.rules;

import org.junit.rules.ExternalResource;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.07.15
 * Time: 15:49
 */
public abstract class BeforeAfterOptionalRule<T extends BeforeAfterOptionalRule> extends ExternalResource {
    private boolean before = false;
    private boolean after = true;

    @Override
    protected void before() throws Exception {
        if (before) {
            call();
        }
    }

    @Override
    protected void after() {
        if (after) {
            call();
        }
    }

    public T before(boolean before) {
        this.before = before;
        return (T) this;
    }

    public T after(boolean after) {
        this.after = after;
        return (T) this;
    }


    //Override this
    public abstract void call();
}