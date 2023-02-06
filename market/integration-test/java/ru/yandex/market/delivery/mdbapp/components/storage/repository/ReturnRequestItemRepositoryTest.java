package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestItem;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnType;

@Sql(value = {"/data/repository/returnRequestItem/cleanup.sql", "/data/repository/returnRequest/cleanup.sql"})
public class ReturnRequestItemRepositoryTest extends MockContextualTest {

    @Autowired
    private ReturnRequestItemRepository repository;
    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Test
    public void testSave() {
        // given:
        ReturnRequest returnRequest = returnRequestRepository.saveAndFlush(ReturnRequestRepositoryTest.returnRequest());

        // when:
        ReturnRequestItem actual = repository.saveAndFlush(returnRequestItem(returnRequest));

        // then:
        ReturnRequestItem expected = returnRequestItem(returnRequest).setId(1L);
        ReturnRequest expectedReturnRequest = ReturnRequestRepositoryTest.returnRequest().setId(1L);
        expectedReturnRequest.addReturnRequestItem(expected);
        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    static ReturnRequestItem returnRequestItem(ReturnRequest returnRequest) {
        ReturnRequestItem returnRequestItem = new ReturnRequestItem()
            .setName("Лабутены")
            .setReturnType(ReturnType.UNSUITABLE)
            .setReturnReason("Натирают стопу, что невозможно ходить")
            .setPrice(BigDecimal.valueOf(13758.97))
            .setCount(1)
            .setOperatorComment("Повреждений не имеют, неношенные");
        returnRequest.addReturnRequestItem(returnRequestItem);
        return returnRequestItem;
    }
}
