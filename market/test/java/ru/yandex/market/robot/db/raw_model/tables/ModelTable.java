package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.bind.Binder;
import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.ACTUAL;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ALIASES;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ANNOUNCE_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CATEGORY;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CREATE_VERSION_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CREATE_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.DELETED;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.DESCRIPTION;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.FIRST_VERSION_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.FIRST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.FULL_NAME;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.IN_STOCK_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.LAST_VERSION_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.LAST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.RAW_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SOURCE_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.TYPE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.VENDOR;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.VENDOR_CODE;

/**
 * @author jkt on 20.12.17.
 */
public class ModelTable {

    public static final String NAME = "model";


    public static Stream<Insert> entryFor(RawModel model) {
        return Stream.of(
            Insert.into(NAME)
                .row()
                .column(ID, model.getId())
                .column(SOURCE_ID, model.getSourceId())
                .column(VENDOR, model.getVendor())
                .column(RAW_ID, model.getRawId())
                .column(CREATE_VERSION_NUMBER, model.getCreateVersionNumber())
                .column(CREATE_VERSION_DATE, model.getCreateVersionDate())
                .column(FIRST_VERSION_NUMBER, model.getFirstVersionNumber())
                .column(FIRST_VERSION_DATE, model.getFirstVersionDate())
                .column(LAST_VERSION_NUMBER, model.getLastVersionNumber())
                .column(LAST_VERSION_DATE, model.getLastVersionDate())
                .column(DELETED, model.isDeleted())
                .column(CATEGORY, model.getCategory())
                .column(Columns.NAME, model.getName())
                .column(VENDOR_CODE, model.getVendorCode())
                .column(DESCRIPTION, model.getDescription())
                .column(ANNOUNCE_DATE, model.getAnnounceDate())
                .column(IN_STOCK_DATE, model.getInStockDate())
                .column(ACTUAL, model.isActual())
                .column(FULL_NAME, model.getFullName())
                .column(
                    ALIASES,
                    model.getAliases().stream()
                        .map(Objects::toString)
                        .collect(
                            Collectors.joining(RawModelStorage.ALIAS_SEPARATOR)
                        )
                )
                .column(TYPE, model.getType())
                .end()
                .build()
        );
    }
}
