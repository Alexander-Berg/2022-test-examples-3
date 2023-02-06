'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/?text=Беларусь&oll=27.701393%2C52.858248';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Страна'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: 'Беларусь на карте' });
    });

    runCommonTests(url, 2);
});
