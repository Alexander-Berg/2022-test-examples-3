'use strict';

const yaGoUrlHelper = require('./helpers/go-url.hermione-helper'),
    runCommonTests = require('./helpers/base.hermione-helper'),
    url = 'https://yandex.ru/maps/?text=Россия%2C%20Москва%2C%20Манежная%20улица%2C%202-10соор2' +
        '&oll=37.612748%2C55.75244';

specs({
    feature: 'Колдунщик топонимов',
    type: 'Координаты'
}, function() {
    beforeEach(function() {
        return yaGoUrlHelper(this.browser, { text: '55°45′07″  37°36′56″' });
    });

    runCommonTests(url);
});
