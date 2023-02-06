package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.services.model.MarketDataSyncAddress;

import static ru.yandex.market.pers.address.factories.AddressDtoFactory.TOLSTOGO_STREET;

public class DataSyncAddressFactory {
    public static MarketDataSyncAddress.Builder tolstogo() {
        return MarketDataSyncAddress.builder()
            .setZip("119021")
            .setFloor("3")
            .setEntrance("2")
            .setIntercom("235")
            .setRegionId("213")
            .setMetro("Парк Культуры")
            .setCountry("Россия")
            .setCity("Москва")
            .setStreet(TOLSTOGO_STREET)
            .setSuite("Оранжевое лето")
            .setBuilding("16")
            .setEntrance("2")
            .setFlat("Оранжевое лето")
            .setFloor("2")
            .setIntercom("849*5")
            .setCargolift("no")
            .setEmail("user@yandex-team.ru")
            .setComment("some comment")
            .setFirstName("Иван")
            .setFathersName("Иванович")
            .setLastName("Иванов")
            .setTitle("Работа")
            .setPhone("+796514578945")
            .setPhoneExtra("+796514578123");
    }

    public static MarketDataSyncAddress.Builder defaultContact() {
        return MarketDataSyncAddress.builder()
            .setCountry("")
            .setCity("")
            .setBuilding("")
            .setEmail("user@yandex-team.ru")
            .setComment("some comment")
            .setFirstName("Иван")
            .setFathersName("Иванович")
            .setLastName("Иванов")
            .setTitle("Работа")
            .setPhone("+796514578945")
            .setPhoneExtra("+796514578123");
    }
}
