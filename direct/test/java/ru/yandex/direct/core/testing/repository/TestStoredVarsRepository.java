package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.sessionvariables.repository.StoredVarsRepository;
import ru.yandex.direct.dbschema.monitor.enums.StoredVarsStatuslocked;
import ru.yandex.direct.dbschema.monitor.tables.records.StoredVarsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.monitor.tables.StoredVars.STORED_VARS;

/**
 * Работа с сессиоными переменными в тестах
 */
@ParametersAreNonnullByDefault
@Repository
public class TestStoredVarsRepository extends StoredVarsRepository {

    @Autowired
    public TestStoredVarsRepository(DslContextProvider dslContextProvider) {
        super(dslContextProvider);
    }

    /**
     * Создает запись в таблице stored_vars с указанными параметрами
     *
     * @param cid ID кампании
     * @param uid ID пользователя
     * @return ID созданной записи
     */
    public Long createStoredVar(long cid, long uid, ULong name, byte[] yamldata, LocalDateTime logdate,
                                StoredVarsStatuslocked statuslocked) {
        StoredVarsRecord storedVarsRecord = dslContextProvider.monitor()
                .insertInto(STORED_VARS)
                .set(STORED_VARS.CID, cid)
                .set(STORED_VARS.UID, uid)
                .set(STORED_VARS.NAME, name)
                .set(STORED_VARS.YAML_DATA, yamldata)
                .set(STORED_VARS.LOGDATE, logdate)
                .set(STORED_VARS.STATUS_LOCKED, statuslocked)
                .returning(STORED_VARS.ID).fetchOne();
        return storedVarsRecord.getId();
    }

    /**
     * Возвращает список ID всех записей в таблице stored_vars
     *
     * @return список ID всех записей
     */
    public List<Long> getAllStoredVarsIds() {
        return dslContextProvider.monitor().select(STORED_VARS.ID).from(STORED_VARS).fetch(STORED_VARS.ID);
    }

    /**
     * Удаляет все записи из таблицы stored_vars
     */
    public void clearAllStoredVars() {
        dslContextProvider.monitor().truncate(STORED_VARS).execute();
    }
}
