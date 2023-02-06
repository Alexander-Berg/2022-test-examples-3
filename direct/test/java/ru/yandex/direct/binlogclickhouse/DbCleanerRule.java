package ru.yandex.direct.binlogclickhouse;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.direct.utils.Interrupts;
import ru.yandex.direct.utils.db.DbInstance;

public class DbCleanerRule<T extends DbInstance> implements TestRule {
    private Interrupts.InterruptibleSupplier<T> dbFactory;
    private T db;

    public DbCleanerRule(Interrupts.InterruptibleSupplier<T> dbFactory) {
        this.dbFactory = dbFactory;
        this.db = null;
    }

    public T getDb() {
        return db;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (description.isSuite()) {
            DbCleanerRule<T> rule = this;
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try (T mysql = dbFactory.get()) {
                        if (rule.db != null) {
                            throw new IllegalStateException("Double initialization");
                        }

                        rule.db = mysql;
                        try {
                            base.evaluate();
                        } finally {
                            rule.db = null;
                        }
                    }
                }
            };

        } else {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (db == null) {
                        throw new IllegalStateException("Must be annotated with both @Rule and @ClassRule");
                    }

                    try (DbCleaner ignored = new DbCleaner(db.connect())) {
                        base.evaluate();
                    }
                }
            };
        }
    }
}
