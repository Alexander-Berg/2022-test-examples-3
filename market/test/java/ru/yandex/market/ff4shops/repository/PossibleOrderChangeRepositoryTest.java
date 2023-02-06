package ru.yandex.market.ff4shops.repository;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.PossibleOrderChangeEntity;
import ru.yandex.market.ff4shops.model.entity.PossibleOrderChangeEntity.PossibleOrderChangeKey;
import ru.yandex.market.ff4shops.model.enums.PossibleOrderChangeMethod;
import ru.yandex.market.ff4shops.model.enums.PossibleOrderChangeType;

public class PossibleOrderChangeRepositoryTest extends FunctionalTest {
    @Autowired
    private PossibleOrderChangeRepository possibleOrderChangeRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "PossibleOrderChangeRepositoryTest.insert.after.csv")
    void insert() {
        transactionTemplate.execute(ignored -> possibleOrderChangeRepository.save(possibleOrderChange()));
    }

    @Nonnull
    private PossibleOrderChangeEntity possibleOrderChange() {
        return new PossibleOrderChangeEntity()
            .setKey(
                new PossibleOrderChangeKey()
                    .setType(PossibleOrderChangeType.ORDER_ITEMS)
                    .setMethod(PossibleOrderChangeMethod.PARTNER_API)
                    .setPartnerId(107L)
            )
            .setEnabled(true);
    }
}
