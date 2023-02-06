package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.enums.CustomerReturnStatus;
import ru.yandex.market.ff.model.entity.CustomerReturn;
import ru.yandex.market.ff.model.entity.CustomerReturnItem;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Интеграционный тест для {@link CustomerReturnRepository}.
 *
 * @author avetokhin 01/02/18.
 */
public class CustomerReturnRepositoryTest extends IntegrationTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2017, 10, 10, 10, 10, 10);
    private static final long SUPPLIER_ID = 15L;

    @Autowired
    private CustomerReturnRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/customer-return/before.xml")
    @ExpectedDatabase(value = "classpath:repository/customer-return/before.xml", assertionMode = NON_STRICT)
    @Transactional
    public void read() {
        final String id = "return1";
        final CustomerReturn customerReturn = repository.findOne(id);

        assertThat(customerReturn, notNullValue());
        assertThat(customerReturn.getId(), equalTo(id));
        assertThat(customerReturn.getCreatedAt(), equalTo(CREATED_AT));
        assertThat(customerReturn.getStatus(), equalTo(CustomerReturnStatus.PROCESSED));

        final List<CustomerReturnItem> items = customerReturn.getItems();
        assertThat(items, notNullValue());
        assertThat(items, hasSize(1));

        final CustomerReturnItem item = items.get(0);
        assertThat(item.getArticle(), equalTo("article1"));
        assertThat(item.getSupplierId(), equalTo(SUPPLIER_ID));
    }

    @Test
    @DatabaseSetup("classpath:repository/customer-return/before.xml")
    @ExpectedDatabase(value = "classpath:repository/customer-return/after-write.xml", assertionMode = NON_STRICT)
    public void write() {
        final CustomerReturn customerReturn = new CustomerReturn();
        customerReturn.setId("return2");
        customerReturn.setCreatedAt(CREATED_AT);
        customerReturn.setStatus(CustomerReturnStatus.NEW);
        customerReturn.setItems(Collections.singletonList(new CustomerReturnItem(SUPPLIER_ID, "article2")));

        repository.save(customerReturn);
    }

}
