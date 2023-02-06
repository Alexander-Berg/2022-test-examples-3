package ru.yandex.market.core.feed.assortment.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.assortment.model.AssortmentFeedValidationRequest;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationType;
import ru.yandex.market.core.misc.resource.RemoteResource;

public class AssortimentValidationDaoTest extends FunctionalTest {
    @Autowired
    private NamedParameterJdbcTemplate parameterJdbcTemplate;

    @Test
    @DbUnitDataSet(before = "AssortmentDao.before.csv")
    void smokeTest() {
        AssortmentValidationDao assortmentValidationDao = new AssortmentValidationDao(parameterJdbcTemplate);
        assortmentValidationDao.createValidation(new AssortmentFeedValidationRequest(
                new AssortmentFeedValidationRequest.Builder()
                        .setPartnerId(774L)
                        .setType(AssortmentValidationType.STOCKS)
                        .setResource(RemoteResource.of("test", "log", "pass", false))
        ));
    }
}
