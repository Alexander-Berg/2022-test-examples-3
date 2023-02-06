package dto.requests.checkouter;

public enum OrderComment {
    /**
     * Такси автоматически запускает флоу доставки пользователю.
     */
    FIND_COURIER_FASTER("speed-1800,wait-3"),
    /**
     * Такси имитирует, будто курьер не смог вручить заказ и вернулся в дропшип.
     */
    COURIER_RETURN("cargo-return-on-point-B,speed-1800,wait-3"),
    /**
     * Создание и успешная доставка со 2й попытки заказа.
     */
    FIND_COURIER_2_ATTEMPT("$$$ { \"operator_comments\" : { \"taxi-external\" : " +
        "{ \"visit_order_0_0\" : \"reject-0,speed-1800,wait-3\", " +
        "\"visit_order_0_1\" : \"speed-1800,wait-3\"} } }"),
    /**
     * Такси имитирует, будто курьер не нашелся 2 раза. На 3 раз нашелся.
     */
    FIND_COURIER_3_ATTEMPT("$$$ { \"operator_comments\" : { \"taxi-external\" : " +
        "{ \"visit_order_0_0\" : \"reject-0,speed-1800,wait-3\", " +
        "\"visit_order_0_1\" : \"reject-0,speed-1800,wait-3\", " +
        "\"visit_order_0_2\" : \"speed-1800,wait-3\" } } }"),
    /**
     * Создание и успешная доставка с 4й попытки 2й статус водитель не мог доставить заказ.
     */
    FIND_COURIER_4_ATTEMPT_2_CARGO("$$$ { \"operator_comments\" : { \"taxi-external\" : { " +
        "\"visit_order_0_0\" : \"reject-0,speed-1800,wait-3\"," +
        " \"visit_order_0_1\" : \"cargo-return-on-point-B,speed-1800,wait-3\"," +
        " \"visit_order_2_1\" : \"reject-0,speed-1800,wait-3\"," +
        " \"visit_order_2_2\" : \"speed-1800,wait-3\" } } } "),
    /**
     * Создание и успешная доставка с 4й попытки 3й статус водитель не мог доставить заказ.
     */
    FIND_COURIER_4_ATTEMPT_3_CARGO("$$$ { \"operator_comments\" : { \"taxi-external\" : { " +
        "\"visit_order_0_0\" : \"reject-0,speed-1800,wait-3\", " +
        "\"visit_order_0_1\" : \"reject-0,speed-1800,wait-3\", " +
        "\"visit_order_0_2\" : \"cargo-return-on-point-B,speed-1800,wait-3\", " +
        "\"visit_order_2_2\" : \"speed-1800,wait-3\" } } } "),
    /**
     * Такси имитирует, будто не смогли найти курьера за 3 попытки.
     */
    CANT_FIND_COURIER("reject-0,speed-1800,wait-3");

    private final String comment;

    OrderComment(String comment) {
        this.comment = comment;
    }

    public String getValue() {
        return comment;
    }
}
