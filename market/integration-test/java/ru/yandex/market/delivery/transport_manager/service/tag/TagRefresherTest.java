package ru.yandex.market.delivery.transport_manager.service.tag;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;

class TagRefresherTest extends AbstractContextualTest {
    @Autowired
    private TagRefresher tagRefresher;

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/tag/tags_multiple.xml",
    })
    @ExpectedDatabase(
        value = "/repository/tag/after/tags_multiple_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void setOrReplaceSingleTag() {
        tagRefresher.setOrReplaceSingleTag(2L, TagCode.AXAPTA_MOVEMENT_ORDER_ID, "Зпер-0002");
    }
}
