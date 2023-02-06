'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

function assertUrlIsCorrect(elemName, url, queryParams) {
    assert.equal(url.hostname + url.pathname, 'yandex.ru/maps/', elemName + 'содержит неверный хост');

    queryParams.forEach(param => {
        assert.isOk(url.query[param], elemName + ' не содержит необходимый параметр ' + param);
    });
}

specs({
    feature: 'Адресный',
    type: 'Сети/рубрики'
}, function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ text: 'аптеки в москве' })
            .yaWaitForVisible(PO.companies(), 'Гео-колдунщик не появился');
    });

    it('Проверка ссылок и счётчиков', function() {
        return this.browser
            .yaCheckLink(PO.companiesList.title.link()).then(url => {
                assertUrlIsCorrect('ссылка на тайтле', url, ['source', 'text', 'll', 'sll']);
            })
            .yaCheckBaobabCounter(PO.companiesList.title.link(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/title'
            })
            .yaCheckLink(PO.companiesList.map()).then(url => {
                assertUrlIsCorrect('ссылка на карте', url, ['source', 'text', 'll', 'sll']);
            })
            .yaCheckBaobabCounter(PO.companiesList.map(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/teaser'
            })
            .yaCheckLink(PO.companiesList.more()).then(url => {
                assertUrlIsCorrect('ссылка «Еще адреса»', url, ['source', 'text', 'll', 'sll']);
            })
            .yaCheckBaobabCounter(PO.companiesList.more(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/"list/more"'
            })
            .yaCheckBaobabCounter(PO.companiesList.firstMinibadge.link(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/minibadge/link'
            })
            .yaCheckLink(PO.companiesList.firstMinibadge.phoneButton(), { target: '' }).then(url =>
                assert.equal(url.protocol, 'tel:', 'Ссылка на телефонном номере без протокола tel:')
            )
            .yaCheckBaobabCounter(PO.companiesList.firstMinibadge.phoneButton(), {
                path: '/$page/$main/$result[@wizard_name="companies"]/minibadge/link/phone_button'
            });
    });
});
