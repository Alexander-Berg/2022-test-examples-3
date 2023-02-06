package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.bind.Binder;
import com.ninja_squad.dbsetup.operation.Insert;
import org.postgresql.util.PGobject;
import ru.yandex.market.robot.shared.raw_model.Picture;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.DELETED;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.DOWNLOAD_ERROR;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.DOWNLOAD_STATUS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.DOWNLOAD_TIMESTAMP;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.FIRST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.INDEX;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.LAST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.MODEL_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.PICTURE_HASH;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SOURCE_URL;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.STATUS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.TYPE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.URL;

/**
 * @author jkt on 20.12.17.
 */
public class ModelPictureTable {
    public static final String NAME = "model_picture";

    private ModelPictureTable() {
    }

    public static Stream<Insert> entryFor(RawModel model) {
        return model.getPictureUrls().stream().map(picture -> entryFor(model.getId(), picture, true));
    }

    public static Insert entryFor(int modelId, Picture picture, boolean includeVersionColumns) {
        Insert.Builder insert = Insert.into(NAME);
        Insert.RowBuilder row = insert.row()
            .column(MODEL_ID, modelId)
            .column(PICTURE_HASH, picture.getPictureHash())
            .column(SOURCE_URL, picture.getSourceUrl())
            .column(STATUS, picture.getStatus())
            .column(URL, picture.getUrl())
            .column(DELETED, picture.isDeleted());
        if (includeVersionColumns) {
            row
                .column(FIRST_VERSION_NUMBER, picture.getFirstVersionNumber())
                .column(LAST_VERSION_NUMBER, picture.getLastVersionNumber());
        }
        row
            .column(TYPE, picture.getType())
            .column(INDEX, picture.getIndex())
            .column(DOWNLOAD_TIMESTAMP, picture.getDownloadTimestamp())
            .column(DOWNLOAD_STATUS, picture.getDownloadStatus())
            .column(DOWNLOAD_ERROR, picture.getDownloadError())
            .end();
        return insert
            .withBinder(new ModelStatusBinder(), STATUS)
            .withBinder(new DownloadStatusBinder(), DOWNLOAD_STATUS)
            .build();
    }

    private static class DownloadStatusBinder implements Binder {
        @Override
        public void bind(PreparedStatement statement, int param, Object value) throws SQLException {
            if (value == null) {
                return;
            }
            if (!(value instanceof Picture.DownloadStatus)) {
                throw new IllegalArgumentException(value + " is not a Picture.DownloadStatus");
            }
            PGobject pgValue = new PGobject();
            pgValue.setType("model_picture_download_status");
            pgValue.setValue(((Picture.DownloadStatus) value).name());

            statement.setObject(param, pgValue);
        }
    }
}
