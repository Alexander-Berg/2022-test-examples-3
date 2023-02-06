/**
 * Подменяет оригинальные функции alert/confirm и запоминает количество вызовов
 */
(function(window) {
    window.hermione.alertCalls = 0;
    window.hermione.confirmCalls = 0;
    window.hermione.confirmReturnValue = false;

    window.alert = function() {
        window.hermione.alertCalls++;
    };

    window.confirm = function() {
        window.hermione.confirmCalls++;
        return window.hermione.confirmReturnValue;
    };
})(window);
