package ru.yandex.direct.web.core.entity.bidmodifiers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.web.core.entity.bidmodifiers.model.BidModifierInventoryWeb;

import static org.junit.Assert.assertEquals;

public class InventoryModifierConverterTest {

    private static final String INVENTORY_TYPE_FILE = "classpath:///bidmodifiers/inventory_type.json";

    private static String serialize(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    @Test
    public void convert_ReturnCorrectResult() throws JsonProcessingException {

        LocalDate date = LocalDate.parse("2017-06-22");
        LocalDateTime time = date.atTime(20, 12);
        BidModifierInventoryAdjustment first = new BidModifierInventoryAdjustment()
                .withId(474159311L)
                .withInventoryType(InventoryType.INSTREAM_WEB)
                .withLastChange(time)
                .withPercent(200);

        BidModifierInventoryAdjustment second = new BidModifierInventoryAdjustment()
                .withId(474159311L)
                .withInventoryType(InventoryType.INPAGE)
                .withLastChange(time)
                .withPercent(50);

        BidModifierInventory inventory =
                TestBidModifiers.createEmptyInventoryModifier()
                        .withId(474159306L)
                        .withLastChange(time)
                        .withInventoryAdjustments(Arrays.asList(first, second));

        BidModifierInventoryWeb converted = InventoryModifierConverter.convertToWeb(inventory);
        String serialized = serialize(converted);
        String expected = LiveResourceFactory.get(INVENTORY_TYPE_FILE).getContent();
        assertEquals(expected, serialized);
    }
}
