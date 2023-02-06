'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/' +
        '?text=Россия%2C%20Москва%2C%20Центральный%20административный%20округ%2C%20район%20Хамовники' +
        '&oll=37.574525%2C55.729199';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Район'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: 'хамовники район москвы' });
    });

    runCommonTests(url);
});
