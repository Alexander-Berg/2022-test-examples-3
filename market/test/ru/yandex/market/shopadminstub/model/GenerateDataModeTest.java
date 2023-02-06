package ru.yandex.market.shopadminstub.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenerateDataModeTest {
    @Test
    public void shouldReturnOffIfPropertyKeyIsNullOrEmpty() {
        Assertions.assertEquals(GenerateDataMode.OFF, GenerateDataMode.getByPropertyKey(null));
        Assertions.assertEquals(GenerateDataMode.OFF, GenerateDataMode.getByPropertyKey(""));
        Assertions.assertEquals(GenerateDataMode.OFF, GenerateDataMode.getByPropertyKey("off"));
    }

    @Test
    public void shouldReturnAutoIfPropertyKeyIsAnyExceptInventory() {
        Assertions.assertEquals(GenerateDataMode.AUTO, GenerateDataMode.getByPropertyKey("true"));
        Assertions.assertEquals(GenerateDataMode.AUTO, GenerateDataMode.getByPropertyKey("on"));
        Assertions.assertEquals(GenerateDataMode.AUTO, GenerateDataMode.getByPropertyKey("yes"));
    }

    @Test
    public void shouldReturnInventoryIfPropertyKeyIsInvetory() {
        Assertions.assertEquals(GenerateDataMode.INVENTORY, GenerateDataMode.getByPropertyKey("inventory"));
    }
}
