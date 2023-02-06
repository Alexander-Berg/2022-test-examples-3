'use strict';

specs({
    feature: 'Колдунщик Работы'
}, function() {
    const PO = require('../../../page-objects/touch-phone').PO;
    const rabota = PO.rabota;

    const RABOTA_URL = 'https://m.rabota.yandex.ru';
    const COUNTER_PREFIX = '/$page/$main/$result[@wizard_name="rabota"]';

    beforeEach(function() {
        return this.browser
            .yaOpenSerp({
                text: 'работа в екатеринбурге'
            })
            .yaWaitForVisible(rabota(), 'колдунщик Работы не появился');
    });

    it('Ссылка в заголовке', function() {
        const title = rabota.title.link;

        return this.browser
            .assertView('plain', rabota())
            .yaCheckBaobabCounter(title(), {
                path: `${COUNTER_PREFIX}/title`
            })
            .then(counterData => this.browser.yaCheckURL(counterData[0].url, `${RABOTA_URL}/search`, {
                skipProtocol: true,
                skipPathnameTrail: true,
                skipQuery: true
            }));
    });

    it('Ссылка в гринурле', function() {
        const greenurl = rabota.path.item;

        return this.browser
            .yaCheckBaobabCounter(greenurl(), {
                path: `${COUNTER_PREFIX}/path/greenurl`
            })
            .then(counterData => this.browser.yaCheckURL(counterData[0].url, `${RABOTA_URL}`, {
                skipProtocol: true,
                skipPathnameTrail: true,
                skipQuery: true
            }));
    });
});
