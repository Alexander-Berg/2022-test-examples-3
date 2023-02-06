'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/?text=Россия%2C%20Москва%2C%20улица%20Льва%20Толстого%2C%2016' +
        '&oll=37.587093%2C55.733969';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Конкретный адрес'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: 'льва толстого 16' });
    });

    runCommonTests(url);
});
