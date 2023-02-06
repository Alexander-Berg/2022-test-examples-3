package dto.responses.tm.admin;

public enum TransportationTaskStatus {
    NEW,
    ENRICHING,
    ENRICHED,
    VALIDATING,
    VALID,
    INVALID,
    STOCK_AVAILABILITY_CHECKING,
    STOCK_AVAILABILITY_CHECKED,
    STOCK_AVAILABILITY_CHECK_FAILED,
    PREPARING,
    PREPARE_FAILED,
    PALLETS_CREATED,
    TRANSPORTATIONS_CREATED,
    PREPARED,
    PROCESSING,
    COMPLETED,
    ERROR
}