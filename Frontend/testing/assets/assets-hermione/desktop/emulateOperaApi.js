(function(window) {
    window.opr = {
        searchEnginesPrivate: {
            Engine: {
                YANDEX: 'YANDEX'
            },
            canAskToSetAsDefault: function(name, callback) {
                callback(true);
            }
        }
    };
})(window);
