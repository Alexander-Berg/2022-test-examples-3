'use strict';

const PO = require('../../page-objects/desktop').PO,
    YABS_BG_COUNTER = 'yabs.yandex.ru/count/',
    BS_COUNTER = '/clck/safeclick/',
    YABS_COUNTER_ERROR = 'yabs-счётчик не содержит ожидаемый url',
    BS_COUNTER_ERROR = 'bs-счётчик не содержит ожидаемый url';

specs('Счётчик учёта видимости рекламы', function() {
    hermione.only.notIn('ie8', 'nth-of-type не работает в ie8-');
    it('Присутствует в премиуме', function() {
        return this.browser
            .yaOpenSerp({ text: 'окна пвх' })
            .yaWaitForVisible(PO.premium(), 'Рекламный сниппет (премиум) не появился')
            .getAttribute(PO.premium.visibilityCounter(), 'style')
            .then(style => {
                assert.include(style, YABS_BG_COUNTER, YABS_COUNTER_ERROR);
            })
            .getAttribute(PO.premium.blockstatCounter(), 'style')
            .then(style => {
                assert.include(style, BS_COUNTER, BS_COUNTER_ERROR);
            });
    });

    hermione.only.notIn('ie8', 'nth-of-type не работает в ie8-');
    it('Присутствует в халфпремиуме', function() {
        return this.browser
            .yaOpenSerp({ text: 'окна пвх' })
            .yaWaitForVisible(PO.halfpremium(), 'Рекламный сниппет (халфпремиум) не появился')
            .getAttribute(PO.halfpremium.visibilityCounter(), 'style')
            .then(style => {
                assert.include(style, YABS_BG_COUNTER, YABS_COUNTER_ERROR);
            })
            .getAttribute(PO.halfpremium.blockstatCounter(), 'style')
            .then(style => {
                assert.include(style, BS_COUNTER, BS_COUNTER_ERROR);
            });
    });

    it('Присутствует в гарантии', function() {
        return this.browser
            .yaOpenSerp({ text: 'bonprix' })
            .yaWaitForVisible(PO.adv(), 'Рекламный сниппет (гарантия) не появился')
            .getAttribute(PO.rightColumn.adv.visibilityCounter(), 'style')
            .then(style => {
                assert.include(style, YABS_BG_COUNTER, YABS_COUNTER_ERROR);
            })
            .getAttribute(PO.rightColumn.adv.blockstatCounter(), 'style')
            .then(style => {
                assert.include(style, BS_COUNTER, BS_COUNTER_ERROR);
            });
    });
});
