'use strict';

specs('Серверные счетчики /tech/userlocation', function() {
    it('Записываются при открытии страницы', function() {
        return this.browser
            .yaOpenSerp({
                text: 'test',
                exp_flags: 'update_geolocation'
            })
            .yaCheckServerCounter(['on', 'off', 'granted', 'denied', 'prompt'].map(name => {
                return { path: `/tech/userlocation/${name}`, vars: {} };
            }));
    });
});
