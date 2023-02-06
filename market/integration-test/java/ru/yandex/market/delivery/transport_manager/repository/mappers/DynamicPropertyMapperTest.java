package ru.yandex.market.delivery.transport_manager.repository.mappers;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DynamicProperty;

public class DynamicPropertyMapperTest extends AbstractContextualTest {
    @Autowired
    private DynamicPropertyMapper mapper;

    @Test
    @DatabaseSetup("/repository/property/property.xml")
    void testGetByName() {
        DynamicProperty property = mapper.getByName("ENABLE_INTERWAREHOUSE_CALENDARING");
        softly.assertThat(property)
            .isEqualTo(new DynamicProperty().setId(1L).setValue("true").setKey("ENABLE_INTERWAREHOUSE_CALENDARING"));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/property/property.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInsert() {
        mapper.persist("ENABLE_INTERWAREHOUSE_CALENDARING", "true");
    }

    @Test
    @DatabaseSetup("/repository/property/property.xml")
    @ExpectedDatabase(
        value = "/repository/property/updated_property.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        mapper.persist("ENABLE_INTERWAREHOUSE_CALENDARING", "false");
    }
}
