package dto.requests.checkouter;

import dto.requests.checkouter.checkout.AddressRequest;

public enum Address {
    ADDRESS(
        new AddressRequest(
            "Россия",
            "107045",
            "Москва",
            "Пражская",
            "Сретенка",
            "14",
            "1",
            "404",
            "007",
            "8",
            "303",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    LAVKA(
        new AddressRequest(
            "Россия",
            "115172",
            "Москва",
            "Таганская",
            "Малые Каменщики",
            "4",
            "",
            "404",
            "007",
            "8",
            "303",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    LAVKA_WITH_PVZ(
        new AddressRequest(
            "Россия",
            "117321",
            "Москва",
            "Коломенская",
            "Андропова",
            "50",
            "",
            "404",
            "007",
            "8",
            "303",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    PVZ_ON_DEMAND(
        new AddressRequest(
            "Россия",
            "119021",
            "Москва",
            "Парк Культуры",
            "Льва Толстого",
            "16",
            "",
            "404",
            "007",
            "8",
            "303",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    EKB_DARKSTORE(
        new AddressRequest(
            "Россия",
            "115172",
            "Екатеринбург",
            null,
            "Малышева",
            "170",
            null,
            "12",
            "007",
            "8",
            "303",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    LAVKA_HOUR_SLOT(
        new AddressRequest(
            "Россия",
            "119048",
            "Москва",
            null,
            "Кооперативная",
            "2",
            "14",
            "1",
            "1",
            "1",
            "1",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    LAVKA_HOUR_SLOT_CAMEN(
        new AddressRequest(
            "Россия",
            "119048",
            "Москва",
            null,
            "Большие Каменщики",
            "9сБ",
            "14",
            "1",
            "1",
            "1",
            "1",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    LAVKA_HOUR_SLOT_SHARIK(
        new AddressRequest(
            "Россия",
            "115088",
            "Москва",
            null,
            "Шарикоподшипниковская",
            "22",
            "14",
            "1",
            "1",
            "1",
            "1",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    S7GO(
        new AddressRequest(
            "Россия",
            "630009",
            "Новосибирск",
            "",
            "Большевистская улица",
            "101",
            "",
            "404",
            "007",
            "8",
            "14",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    S7GO2(
        new AddressRequest(
            "Россия",
            "630123",
            "Новосибирск",
            "",
            "улица Гастелло",
            "13",
            "",
            "213",
            "112",
            "3",
            "99",
            "Иван Павлов",
            "+78887776655"
        )
    ),
    S7GO3(
        new AddressRequest(
            "Россия",
            "630123",
            "Новосибирск",
            "",
            "улица Аэропорт",
            "2Б",
            "",
            "404",
            "007",
            "5",
            "13",
            "Иван Иванов",
            "+78887776655"
        )
    ),
    MANUAL_COURIER_VORONEZH(
        new AddressRequest(
            "Россия",
            "394018",
            "Воронеж",
            null,
            "улица 9 Января",
            "30",
            null,
            null,
            null,
            null,
            null,
            "Иван Анастасиев",
            "+79101991199"
        )
    );

    private final AddressRequest address;

    Address(AddressRequest address) {
        this.address = address;
    }

    public AddressRequest getAddress() {
        return address;
    }
}
