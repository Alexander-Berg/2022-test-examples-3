'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/' +
        '?text=Россия%2C%20Московская%20область%2C%20городской%20округ%20Химки,%20аэропорт%20Шереметьево' +
        '&oll=37.415685%2C55.966786';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Геообъекты'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: 'шереметьево - аносино маршрут' });
    });

    runCommonTests(url, 3);
});
