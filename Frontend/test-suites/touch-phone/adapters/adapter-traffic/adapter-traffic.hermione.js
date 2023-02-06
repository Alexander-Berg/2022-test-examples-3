'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Колдунщик пробок', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=пробки в москве')
            .yaWaitForVisible(PO.traffic(), 'Не появился колдунщик пробок');
    });

    it('Проверка ссылки и счётчика карты', function() {
        return this.browser
            .yaCheckLink(PO.traffic.map())
            .yaCheckBaobabCounter(PO.traffic.map(), {
                path: '/$page/$main/$result[@wizard_name="traffic"]/open'
            });
    });
});
