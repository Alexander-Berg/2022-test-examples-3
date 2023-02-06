package ru.yandex.direct.binlogclickhouse;

import java.sql.SQLException;

import ru.yandex.direct.binlog.reader.BinlogStateSnapshotter;
import ru.yandex.direct.mysql.MySQLBinlogState;
import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.db.MySQLConnector;

public class IntrusiveSnapshotter implements BinlogStateSnapshotter {
    interface Intruder {
        void call() throws SQLException;
    }

    private BinlogStateSnapshotter snapshotter;
    private Intruder[] intruders;
    private int count;

    public IntrusiveSnapshotter(BinlogStateSnapshotter snapshotter, Intruder... intruders) {
        this.snapshotter = snapshotter;
        this.intruders = intruders;
        this.count = 0;
    }

    @Override
    public MySQLBinlogState snapshot(MySQLConnector mysql) {
        MySQLBinlogState state = snapshotter.snapshot(mysql);
        if (count < intruders.length) {
            try {
                intruders[count].call();
            } catch (SQLException exc) {
                throw new Checked.CheckedException(exc);
            }
            count++;
        } else {
            throw new IllegalStateException(
                    "Too many calls to " + this + ": " + count + " (> " + intruders.length + ")"
            );
        }
        return state;
    }
}
