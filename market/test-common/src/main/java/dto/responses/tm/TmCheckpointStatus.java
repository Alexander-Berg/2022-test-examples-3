package dto.responses.tm;

public enum TmCheckpointStatus {

    //Movement statuses
    MOVEMENT_CANCELLED(3),
    MOVEMENT_CANCELLED_BY_PARTNER(4),
    MOVEMENT_CONFIRMED(95),
    MOVEMENT_COURIER_FOUND(100),
    MOVEMENT_OUTBOUND_WAREHOUSE_REACHED(130),
    MOVEMENT_HANDED_OVER(150),
    MOVEMENT_DELIVERING(200),
    MOVEMENT_INBOUND_WAREHOUSE_REACHED(230),
    MOVEMENT_DELIVERED(250),

    //Outbound statuses
    OUTBOUND_CANCELLED(2),
    OUTBOUND_ASSEMBLING(310),
    OUTBOUND_ASSEMBLED(320),
    OUTBOUND_TRANSFERRED(330),

    //Inbound statuses
    INBOUND_CREATED(1),
    INBOUND_ARRIVED(20),
    INBOUND_ACCEPTANCE(30),
    INBOUND_ACCEPTED(40);

    private final int id;

    TmCheckpointStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
