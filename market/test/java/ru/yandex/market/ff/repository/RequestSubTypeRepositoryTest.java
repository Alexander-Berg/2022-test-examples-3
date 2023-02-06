package ru.yandex.market.ff.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.SlotSize;
import ru.yandex.market.ff.model.entity.SlotSizeProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RequestSubTypeRepositoryTest extends IntegrationTest {

    @Autowired
    private RequestSubTypeRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/request_subtype/slot-size-before.xml")
    public void slotSizeMappingTest() {
        RequestSubTypeEntity entity = repository.findByTypeAndSubtype(RequestType.SUPPLY, "IMPORT");
        SlotSizeProperty expectedValue =
                new SlotSizeProperty(true,
                        List.of(new SlotSize(3, 60), new SlotSize(4, 90)));
        assertEquals(expectedValue, entity.getSlotSizeProperty());

        assertThrows(IllegalArgumentException.class, () ->
                repository.findByTypeAndSubtype(RequestType.SHADOW_SUPPLY, "IMPORT")
        );
    }
}
