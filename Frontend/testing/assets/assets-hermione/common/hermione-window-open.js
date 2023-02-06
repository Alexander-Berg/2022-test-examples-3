/**
 * Подменяет оригинальную функцию window.open и запоминает параметры вызова
 */
(function(window, BEM) {
    'use strict';

    var calledWith,
        originalWindowOpen = window.open;

    BEM.decl('hermione-spy-window-open', {}, {
        calledWith: function() {
            return calledWith;
        },
        spy: function() {
            calledWith = undefined;
            window.open = function(url, target) {
                calledWith = { url: url.trim(), target: target };
                window.open = originalWindowOpen;
                return null;
            };
        }
    });
})(window, BEM);
