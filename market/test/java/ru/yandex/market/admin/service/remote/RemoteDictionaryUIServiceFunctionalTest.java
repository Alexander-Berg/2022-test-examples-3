package ru.yandex.market.admin.service.remote;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.dic.DictionaryTerm;
import ru.yandex.market.admin.ui.model.dic.LongDictionaryTerm;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Функциональные тесты для {@link RemoteDictionaryUIService}.
 */
class RemoteDictionaryUIServiceFunctionalTest extends FunctionalTest {
    private static final List<LongDictionaryTerm> EXPECTED_MANAGERS = new ImmutableList.Builder<LongDictionaryTerm>()
            .add(new LongDictionaryTerm(1L, "user 1 (1)"))
            .add(new LongDictionaryTerm(4L, "user 1 (4)"))
            .add(new LongDictionaryTerm(2L, "user 2"))
            .add(new LongDictionaryTerm(3L, "user 3"))
            .build();

    @Autowired
    private RemoteDictionaryUIService remoteDictionaryUIService;

    @Test
    @DbUnitDataSet(before = "RemoteDictionaryUIServiceFunctionalTest.before.csv")
    void testGetManagers() {
        List<DictionaryTerm> managers = remoteDictionaryUIService.getManagers();
        Assertions.assertEquals(4, managers.size());
        for (int i = 0; i < managers.size(); ++i) {
            Assertions.assertEquals(EXPECTED_MANAGERS.get(i).getCode(), managers.get(i).getCode());
            Assertions.assertEquals(EXPECTED_MANAGERS.get(i).getTerm(), managers.get(i).getTerm());
        }
    }
}
