'use strict';

const PO = require('./OrgCollections.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    type: 'Подборки мест',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'пиццерия R14 спб',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_collections',
            srcrwr: 'GEOV:bering.man.yp-c.yandex.net:8004:99999',
        }, PO.orgCollections());
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.orgCollections());
    });

    it('Проверка ссылки на персональную подборку мест', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.orgCollections.firstItem(),
            target: '_self',
            message: 'Неверная ссылка на персональную подборку мест',
            url: {
                queryValidator: query => {
                    assert.equal(query.text, 'Куда сходить', 'Ошибка в параметре text');
                    assert.equal(query['serp-reload-from'], 'companies', 'Ошибка в параметре serp-reload-from');

                    return true;
                },
                ignore: ['protocol', 'hostname', 'pathname'],
            },
            baobab: {
                path: '/$page/$main/$result/composite/org-collections/scroller/collection[@type="chz_promo"]',
            },
        });
    });

    it('Проверка ссылки на подборку лучших мест', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.orgCollections.secondItem(),
            target: '_self',
            message: 'Неверная ссылка на подборку лучших мест',
            url: {
                queryValidator: query => {
                    assert.equal(query.text, 'Лучшие Пиццерии', 'Ошибка в параметре text');
                    assert.equal(query['serp-reload-from'], 'companies', 'Ошибка в параметре serp-reload-from');

                    return true;
                },
                ignore: ['protocol', 'hostname', 'pathname'],
            },
            baobab: {
                path: '/$page/$main/$result/composite/org-collections/scroller/collection[@type="bests_promo"]',
            },
        });
    });
});
