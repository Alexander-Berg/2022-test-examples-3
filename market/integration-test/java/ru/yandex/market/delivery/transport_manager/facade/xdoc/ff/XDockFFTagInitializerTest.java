package ru.yandex.market.delivery.transport_manager.facade.xdoc.ff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;

class XDockFFTagInitializerTest extends AbstractContextualTest {
    @Autowired
    private XDockFFTagInitializer tagInitializer;

    @DatabaseSetup("/repository/transportation/after/xdoc_to_ff_transportation_1p.xml")
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_rc_to_ff_transportation_tag_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTags1p() {
        tagInitializer.createTags(
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            new Transportation().setId(1L)
        );
    }

    @DatabaseSetup("/repository/transportation/after/xdoc_to_ff_transportation_3p.xml")
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_rc_to_ff_transportation_tag_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTags3p() {
        tagInitializer.createTags(
            XDocTestingConstants.X_DOC_CREATE_DATA_3P,
            new Transportation().setId(1L)
        );
    }
}
