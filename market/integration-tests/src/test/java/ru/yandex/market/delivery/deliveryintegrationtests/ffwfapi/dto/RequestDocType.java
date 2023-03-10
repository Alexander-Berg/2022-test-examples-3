package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto;

/**
 * Перечень разновидностей типов документов.
 *
 */
public enum RequestDocType {
    /**
     * Файл заявки на поставку.
     */
    SUPPLY(0, true),

    /**
     * Файл заявки изъятия.
     */
    WITHDRAW(1, true),

    /**
     * Копия МХ-1 (акт о приеме - передаче товарно-материальных ценностей на хранение) (стандартный формат).
     */
    MX_1_COPY(2, false),

    /**
     * Излишки.
     */
    SURPLUSES(3, false),

    /**
     * Пересортица.
     */
    REGRADING(4, false),

    /**
     * Файл с детальной информацией об ошибках валидации. По факту это копия основного документа заявки + дополнительная
     * колонка с описанием ошибок построчно.
     */
    VALIDATION_ERRORS(5, false),

    /**
     * Акт о приеме - передаче товарно-материальных ценностей на хранение (наш внутренний формат).
     */
    ACT_OF_RECEPTION_TRANSFER(6, false),

    /**
     * Акт вторичной приемки. Генерируется по фактически принятому количеству товара. Аналогичен акту
     * о приеме-передаче, но в колонке "количество" указывается фактически поставленное количество товара.
     */
    SECONDARY_RECEPTION_ACT(7, false),

    /**
     * Акт о расхождениях. Генерируется по фактически принятому количеству товара. Генерируется только в том случае,
     * если поставка закрыта с расхождениями, то есть хотя бы по 1-му товару есть расхождения и план не равен факту.
     * В строках акта указывается название товара, количество и тип расхождения.
     */
    ACT_OF_DIVERGENCE(8, false),

    /**
     * Этикетка для палеты.
     */
    PALLET_LABEL(9, false),

    /**
     * Акт о вывозе товаров. Генерируется по статусу заявки "Товары переданы".
     */
    ACT_OF_WITHDRAW(10, false),

    /**
     * Буклет с инструкцией проезда на склад для водителя (версия на DITA-шаблонах).
     */
    DRIVER_BOOKLET(111, false),

    /**
     * Буклет с инструкцией проезда на склад для водителя (версия на jasper-шаблонах).
     */
    DRIVER_BOOKLET_OLD(11, false),

    /**
     * Акт о приеме - передаче товарно-материальных ценностей на хранение (наш внутренний формат).
     * Дополнительный акт с информацией о незаявленных товарах.
     */
    ADDITIONAL_ACT_OF_RECEPTION_TRANSFER(12, false),

    /**
     * Акт вторичной приемки. Генерируется по фактически принятому количеству товара. Аналогичен акту
     * о приеме-передаче, но в колонке "количество" указывается фактически поставленное количество товара.
     * Дополнительный акт с информацией о незаявленных товарах.
     */
    ADDITIONAL_SECONDARY_RECEPTION_ACT(13, false),

    /**
     * Акт о расхождениях по незаявленным излишкам. Генерируется по фактически принятому количеству товара.
     * Генерируется только в том случае, если в излишках есть брак.
     */
    ADDITIONAL_ACT_OF_DIVERGENCE(14, false),

    /**
     * Драфт кроссдок поставки на сегодня.
     */
    CROSSDOCK_INBOUND_DRAFT_FOR_CURRENT_DAY(15, false),

    /**
     * Драфт кроссдок поставки на все дни.
     */
    CROSSDOCK_INBOUND_RESERVE(16, false),

    /**
     * Этикетка для паллеты с кроссдок товарами.
     */
    CROSSDOCK_PALLET_LABEL(17, false),

    /**
     * Транспортная накладная для кроссдок-поставки.
     */
    CROSSDOCK_INBOUND_TRANSPORT_WAYBILL(18, false),

    /**
     * Файл заявки изъятия с необязательными полями количества и названия.
     */
    SIMPLIFIED_WITHDRAW(19, true),

    /**
     * УПД
     */
    UTD(20, false),

    /**
     * Форма ТОРГ-2
     */
    TORG2(21, false),

    /**
     * Форма ТОРГ-12
     */
    TORG12(22, false),

    /**
     * Форма ТОРГ-13
     */
    TORG13(23, false),

    /**
     * Счет-фактура
     */
    FACTURE(24, false),

    /**
     * Файл перемещения товара между стоками.
     */
    TRANSFER(25, true),

    /**
     * Акт списания с ответственного хранения для утилизации.
     */
    ACT_OF_WITHDRAW_FROM_STORAGE(26, false),

    /**
     * Акт расхождений с учетом аномалий.
     */
    ACT_OF_DISCREPANCY(27, false),

    OTHER(100, false);

    private final int id;
    private final boolean isMain;

    RequestDocType(final int id, final boolean isMain) {
        this.id = id;
        this.isMain = isMain;
    }

    public Integer getId() {
        return id;
    }

    public boolean isMain() {
        return isMain;
    }
}
