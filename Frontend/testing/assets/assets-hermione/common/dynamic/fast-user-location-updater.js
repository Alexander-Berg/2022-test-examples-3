/**
 * Для некоторых тестов нужно ускорить обновление геопозиции
 * Передаём параметр fast_user_location_updater
 */
BEM.DOM.decl('user-location-updater', {
    onSetMod: {
        js: {
            inited: function() {
                this.params.period = 1;
                this.params.delay = 1;

                this.__base.apply(this, arguments);
            }
        }
    }
});
