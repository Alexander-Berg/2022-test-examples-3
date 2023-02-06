package dto.responses.lom;

import io.swagger.annotations.ApiModelProperty;

public enum S7PackOfOrdersStatus {

    @ApiModelProperty("Новый заказ")
    UNCONFIRMED,

    @ApiModelProperty("Назначили курьера")
    CONFIRMED,

    @ApiModelProperty("Заказ получен экспедитором")
    DELIVERY_STARTED,

    @ApiModelProperty("Заказ доставлен экспедитором")
    DELIVERY_PACKAGED,

    @ApiModelProperty("Заказ подготавливается к авиаперевозке")
    DELIVERY_PACKAGING_STARTED,

    @ApiModelProperty("Заказ подготовлен к авиаперевозке")
    DELIVERY_PACKAGING_ENDED,

    @ApiModelProperty("Груз вылетел")
    DELIVERY_FLIGHT_STARTED,

    @ApiModelProperty("Груз прилетел")
    DELIVERY_FLIGHT_ENDED,

    @ApiModelProperty("Заказ готов к выдаче")
    DELIVERY_ARRIVED_TO_CITY,

    @ApiModelProperty("Заказ выдан")
    COMPLETED,

    @ApiModelProperty("Заказ готов к обработке")
    POST_FLIGHT_DELIVERY_PACKAGING_STARTED,

    @ApiModelProperty("Заказ обработан")
    POST_FLIGHT_DELIVERY_PACKAGING_ENDED,

    @ApiModelProperty("Заказ передан для доставки на склад")
    POST_FLIGHT_DELIVERY_STARTED,

    @ApiModelProperty("Заказ ожидает консолидацию для возврата")
    DELIVERY_WAIT_FOR_CONSOLIDATION,
}
