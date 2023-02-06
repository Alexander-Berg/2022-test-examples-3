package ru.yandex.market.core.post.dao;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.post.model.ContractOfferEntity;
import ru.yandex.market.core.post.model.ContractOfferValidationError;
import ru.yandex.market.core.post.model.PostContractStatus;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.core.post.model.ContractOfferValidationError.ValidationErrorCode.INVALID;
import static ru.yandex.market.core.post.model.ContractOfferValidationError.ValidationErrorCode.MISSING;

@DbUnitDataSet(before = "ContractOfferDaoTest.before.csv")
class ContractOfferDaoTest extends FunctionalTest {

    @Autowired
    private ContractOfferDao contractOfferDao;

    @Test
    void it_must_return_empty_for_unknown_shop_id() {
        assertTrue(contractOfferDao.getByShopId(0).isEmpty());
    }

    @Test
    void it_must_return_expected_value_for_known_shop_id_when_contract_is_signed() {
        final ContractOfferEntity actual = contractOfferDao.getByShopId(1).orElseThrow();
        final ContractOfferEntity expected = ContractOfferEntity.newBuilder()
                .withShopId(1L)
                .withUserId(0L)
                .withSubmittedForm(null)
                .withFormErrors(null)
                .withStatus(PostContractStatus.NEW)
                .withUpdatedAt(Instant.parse("2018-01-01T00:00:00.00Z"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void it_must_return_expected_value_for_known_shop_id_when_contract_is_not_signed() {
        final ContractOfferEntity actual = contractOfferDao.getByShopId(2).orElseThrow();
        final ContractOfferEntity expected = ContractOfferEntity.newBuilder()
                .withShopId(2L)
                .withUserId(0L)
                .withSubmittedForm("{}")
                .withStatus(PostContractStatus.SENT)
                .withUpdatedAt(Instant.parse("2019-01-01T00:00:00.00Z"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(after = "ContractOfferDaoTest.markUnsigned.after.csv")
    void testMarkContractAsNonSigned() {
        contractOfferDao.setSubmitStatus(
                2, 0, PostContractStatus.VALIDATION_FAILED,
                asList(new ContractOfferValidationError("field1", "reason1", INVALID),
                        new ContractOfferValidationError("field2", "reason2", MISSING)));
    }
}
