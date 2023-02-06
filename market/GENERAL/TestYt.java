package ru.yandex.market.mbo.yt;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.acl.YtAcl;
import ru.yandex.inside.yt.kosher.files.YtFiles;

/**
 * @author s-ermakov
 */
public class TestYt implements Yt {

    private TestYtTransactions testYtTransactions;
    private TestCypress testCypress;
    private TestYtOperations testYtOperations;
    private TestYtTables testYtTables;
    private TestYtFiles testYtFiles;

    public TestYt() {
        init();
    }

    private void init() {
        testCypress = new TestCypress(this);
        testYtTables = new TestYtTables(this);
        testYtOperations = new TestYtOperations(this);
        testYtTransactions = new TestYtTransactions(this);
        testYtFiles = new TestYtFiles(this);
    }

    @Override
    public TestCypress cypress() {
        return testCypress;
    }

    @Override
    public YtAcl acl() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public YtFiles files() {
        return testYtFiles;
    }

    @Override
    public TestYtTables tables() {
        return testYtTables;
    }

    @Override
    public TestYtOperations operations() {
        return testYtOperations;
    }

    @Override
    public TestYtTransactions transactions() {
        return testYtTransactions;
    }
}
