(function() {
    window.Ya.SAVED_PAYMENT_REQUESTS = [];
    var shouldMockRequest = window.location.href.indexOf('mock_payment_requests') !== -1;
    var shouldThrow = window.location.href.indexOf('mock_payment_requests_throw') !== -1;
    var shouldThrowNotStarted = window.location.href.indexOf('mock_payment_requests_throw_not_started') !== -1;

    if (shouldMockRequest) {
        window.PaymentRequest = function(methodData, details, options) {
            window.Ya.SAVED_PAYMENT_REQUESTS.push([methodData, details, options]);
        };

        window.PaymentRequest.prototype.canMakePayment = function() { return Promise.resolve(true) };
        window.PaymentRequest.prototype.show = function() {
            return shouldThrow ?
                Promise.reject(shouldThrowNotStarted ? Error('[NOT_STARTED]: ') : 'oops') :
                Promise.resolve({ complete: function() {} });
        };
    }
})();
