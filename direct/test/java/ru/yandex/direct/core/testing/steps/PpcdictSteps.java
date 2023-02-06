package ru.yandex.direct.core.testing.steps;

import java.sql.Timestamp;

import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

@Component
@SuppressWarnings("WeakerAccess")
public class PpcdictSteps {
    @Autowired
    private DslContextProvider dslContextProvider;


    /**
     * Возвращает текущее время из базы ppcdict
     *
     * @return текущее время
     */
    public Timestamp getTimestamp() {
        return dslContextProvider.ppcdict()
                .select(DSL.currentTimestamp())
                .fetchOne()
                .value1();
    }
}
