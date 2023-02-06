/* globals browserLog */
window.console = (function(nativeConsole) {
    var newConsole = {},
        stack = [];

    newConsole.getLogStack = function() {
        return stack;
    };

    newConsole.emptyLogStack = function() {
        stack.length = 0;
    };

    function toJSON(data) {
        return data.map(function(item) {
            try {
                return JSON.stringify(item, 4);
            } catch (e) {
                nativeConsole.error(e.message);
                return '';
            }
        });
    }

    function hermioneLog(method, data) {
        stack.push({
            method: method,
            data: data,
            timestamp: Date.now()
        });
    }

    nativeConsole['browser-error'] = function() {
        browserLog('error', toJSON(Array.prototype.slice.call(arguments)));
    };

    ['error', 'log', 'info', 'warn', 'table', 'assert'].forEach(function(method) {
        newConsole[method] = function() {
            var data = toJSON(Array.prototype.slice.call(arguments));

            // Детектим console
            if (nativeConsole) {
                var logger = nativeConsole[method] ? nativeConsole[method] : nativeConsole.log;
                logger.apply(nativeConsole, arguments);
            }

            // Пробрасываем в тесты
            hermioneLog(method, data);
        };
    });

    return newConsole;
})(window.console);
