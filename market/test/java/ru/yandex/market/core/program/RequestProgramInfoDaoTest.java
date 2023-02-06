package ru.yandex.market.core.program;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link RequestProgramInfoDao}.
 */
public class RequestProgramInfoDaoTest extends FunctionalTest {

    private final long REQUEST_ID = 123L;
    @Autowired
    private RequestProgramInfoDao requestProgramInfoDao;

    /**
     * Сохраненние новых данных.
     */
    @DbUnitDataSet(after = "db/RequestProgramInfoDaoTest_save_whenCreation.after.csv")
    @Test
    public void test_createOrUpdate_when_newReord() {
        requestProgramInfoDao.createOrUpdate(REQUEST_ID);
    }

    /**
     * Сохраненние новых данных.
     */
    @DbUnitDataSet(after = "db/RequestProgramInfoDaoTest_save_whenUpdate.after.csv",
            before = "db/RequestProgramInfoDaoTest_save_whenUpdate.before.csv")
    @Test
    public void test_createOrUpdate_when_updateExistingRecord() {
        requestProgramInfoDao.createOrUpdate(REQUEST_ID);
    }

    /**
     * Проверка загрузки данных из базы:
     * - проверяется, что все существующие значения {@link ProgramIdentity#values()} корректно поднимаются из базы
     * - проверяется, что подмножество значений {@link ProgramIdentity} корректно поднимается из базы
     */
    @DbUnitDataSet(before = "db/RequestProgramInfoDaoTest_find.before.csv")
    @Test
    public void test_find_when_exists() {
        Map<Long, RequestProgramInfo> requestProgramInfoById = requestProgramInfoDao.find(
                ImmutableList.of(REQUEST_ID, 456L)
        ).stream().collect(
                Collectors.toMap(
                        RequestProgramInfo::getRequestId,
                        info -> info
                )
        );

        assertThat(requestProgramInfoById.size(), is(2));

        //должен содержать все доступные в энаме программы
        assertThat(
                requestProgramInfoById.get(REQUEST_ID).getProgramIdentities(),
                containsInAnyOrder(ProgramIdentity.values())
        );

        //неполный набор программ
        assertThat(
                requestProgramInfoById.get(456L).getProgramIdentities(),
                containsInAnyOrder(ProgramIdentity.POSTPAY, ProgramIdentity.DELIVERY)
        );
    }

    /**
     * Если ничего не найдно по запросу для одного идентификатору - возвращает null.
     */
    @DbUnitDataSet
    @Test
    public void test_find_when_nothingFoundForOneReq_then_returnsNull() {
        RequestProgramInfo requestProgramInfo = requestProgramInfoDao.find(REQUEST_ID);
        assertThat(requestProgramInfo, nullValue());
    }

    /**
     * Если ничего не найдно по запросу для набора идентификаторов - возвращает пустую коллекцию.
     */
    @DbUnitDataSet
    @Test
    public void test_find_when_nothingFoundForMultipleReqs_then_returnsNull() {
        Collection<RequestProgramInfo> requestProgramInfos = requestProgramInfoDao.find(ImmutableList.of(REQUEST_ID, 456L));
        assertThat(requestProgramInfos, hasSize(0));
    }

    /**
     * Если в базе содержится неизвестно именование программы то исключение.
     */
    @DbUnitDataSet(before = "db/RequestProgramInfoDaoTest_find.before.csv")
    @Test
    public void test_find_when_containsIllegalProgramName_then_throws() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> requestProgramInfoDao.find(789L)
        );
    }

}
