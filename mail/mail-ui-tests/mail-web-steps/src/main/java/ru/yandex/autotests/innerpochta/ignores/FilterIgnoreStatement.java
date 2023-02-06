package ru.yandex.autotests.innerpochta.ignores;

import org.junit.runners.model.Statement;

import static java.lang.String.format;
import static org.junit.Assume.assumeTrue;

/**
 * @author pavponn
 */
public class FilterIgnoreStatement extends Statement implements IgnoreStatement {

    private String filter;

    public FilterIgnoreStatement(final String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public void evaluate() {
        assumeTrue(format("Ignored by FilterRunRule\nTest doesn't satisfy filter: %s", getFilter()), false);
    }
}
