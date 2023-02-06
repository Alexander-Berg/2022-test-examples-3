package ru.yandex.market.mbi.logprocessor;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.logprocessor.storage.yt.model.PushApiLogEntity;

import static org.junit.jupiter.api.Assertions.*;

class TestUtilTest {

    @Test
    void readBean() {
        List<PushApiLogEntity> beans = TestUtil.readBeanFromCsv("push-api-entities.csv", PushApiLogEntity.class);
        assertEquals(2, beans.size());
    }
}
