package ru.yandex.market.core.supplier;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.contact.InnerRole;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "SupplierContactServiceTest.before.csv")
class SupplierContactServiceTest extends FunctionalTest {

    @Autowired
    private SupplierContactService tested;

    @Test
    void testGetSupplierContacts() {
        Map<Long, List<Long>> contacts = tested.getSupplierContacts(InnerRole.SHOP_ADMIN, asList(1L, 2L));

        assertThat(contacts)
                .hasSize(2)
                .hasEntrySatisfying(1L, v -> assertThat(v).containsExactlyInAnyOrder(177893L, 177894L))
                .hasEntrySatisfying(2L, v -> assertThat(v).containsExactlyInAnyOrder(177895L));
    }
}
