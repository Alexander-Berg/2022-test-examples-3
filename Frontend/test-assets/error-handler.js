(function(window, prevOnErrorHandler) {
    var jsErrors = window['hermione-jserrors'] = [];

    function pushError(message, url, line, col, err) {
        jsErrors.push({
            message: message,
            url: url,
            line: line,
            col: col,
            error: err
        });
    }

    window.onerror = function(message, url, line, col, err) {
        pushError(message, url, line, col, err);
        return prevOnErrorHandler && prevOnErrorHandler(message, url, line, col, err);
    };
})(window, window.onerror);
