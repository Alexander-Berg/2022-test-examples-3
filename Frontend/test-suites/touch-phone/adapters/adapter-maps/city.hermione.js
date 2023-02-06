'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/?text=Россия%2C%20Республика%20Татарстан%2C%20Казань&oll=49.108795%2C55.796289';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Город'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: 'казань на карте' });
    });

    runCommonTests(url, 2);
});
