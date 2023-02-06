package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.bind.Binder;
import org.postgresql.util.PGobject;
import ru.yandex.market.robot.shared.raw_model.Status;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author jkt on 11.12.17.
 */
public class ModelStatusBinder implements Binder{

    @Override
    public void bind(PreparedStatement statement, int param, Object value) throws SQLException {
        statement.setObject(param, modelStatus(value));
    }

    private PGobject modelStatus(Object value) throws SQLException {
        if (! (value instanceof Status)) {
            throw new IllegalStateException(value + " is not a model status. Can not generate database data");
        }

        Status statusValue = (Status) value;

        PGobject modelStatus = new PGobject();
        modelStatus.setType("model_status");
        modelStatus.setValue(statusValue.name());

        return modelStatus;
    }
}
