module.exports = {
    /**
     * Селекторы экранов вызова такси (поиск/ожидание/поездка/завершение поездки)
     */
    cancelButton: '[class*="OrderCancelButton_button"]',
    orderStatusView: '[class*="OrderStatusView"]',
    container: '[class*="OrderStatusView_container"]',
    containerHidden: '[class*="OrderStatusContainer_hidden"]',
    completeViewInfo: '[class*=CompleteView_order-info]',
    completeButton: '[class*="CompleteView_action-button"]',
    details: '[class*="OrderDetails_title"]',
    detailsModal: '[class*="OrderDetailsModal_content__"]',
    detailsModalScroll: '[class*="OrderDetailsModal_content__"] .VerticalScroll',
    detailsModalRules: '[class*="OrderDetailsModal_rules__"]',
    drivingViewInfo: '[class*="DrivingView_order-info"]',
    searchViewInfo: '[class*="SearchView_order-info"]',
    tripRating: '[class*="TripRating_container"]',
};
