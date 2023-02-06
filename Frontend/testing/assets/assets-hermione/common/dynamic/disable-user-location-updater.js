/**
 * Для некоторых тестов нужно отключить обновление геопозиции
 * Передаём параметр disable_user_location_updater
 */
BEM.DOM.decl('user-location-updater', {
    onSetMod: {
        js: {
            inited: function() {}
        }
    }
});
