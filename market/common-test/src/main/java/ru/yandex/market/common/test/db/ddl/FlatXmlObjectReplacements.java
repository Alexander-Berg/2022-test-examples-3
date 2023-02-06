package ru.yandex.market.common.test.db.ddl;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 23.03.2022
 */
public final class FlatXmlObjectReplacements {
    private FlatXmlObjectReplacements() {
        throw new UnsupportedOperationException();
    }
    public static final Map<Object, Object> REPLACEMENTS = Collections.singletonMap("[sysdate]", new Date());
}
