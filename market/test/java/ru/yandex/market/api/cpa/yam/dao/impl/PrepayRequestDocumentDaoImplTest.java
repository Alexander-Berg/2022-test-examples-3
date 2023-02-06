package ru.yandex.market.api.cpa.yam.dao.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDocumentDao;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequestDocument;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class PrepayRequestDocumentDaoImplTest extends FunctionalTest {

    @Autowired
    private PrepayRequestDocumentDao prepayRequestDocumentDao;

    @DisplayName("Устаревшими документами должны считаться старые (старше 6 месяцев) документы, на которых нет" +
            "заявок в потенциально активных статусах.")
    @Test
    @DbUnitDataSet(before = "getObsoleteDocuments.before.csv")
    void getObsoleteDocuments() {
        List<PrepayRequestDocument> docs = prepayRequestDocumentDao.getObsoleteDocuments(50);

        Set<Long> expectedIds = set(102L);
        Set<Long> actualIds = docs.stream().map(PrepayRequestDocument::getId).collect(Collectors.toSet());
        MatcherAssert.assertThat(actualIds, equalTo(expectedIds));
    }
}
