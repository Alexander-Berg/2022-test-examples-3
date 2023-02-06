'use strict';

const PO = require('./Route.page-object');

specs({
    feature: 'Колдунщик маршрутов',
}, function() {
    hermione.also.in('iphone-dark');
    it('Проверка внешнего вида', async function() {
        await this.browser.yaOpenSerp({
            text: 'как добраться',
            data_filter: 'route',
        }, PO.route());

        await this.browser.assertView('transport', PO.route());
    });

    it('Проверка ссылок и счетчиков', async function() {
        await this.browser.yaOpenSerp({
            text: 'как добраться',
            data_filter: 'route',
        }, PO.route());

        await this.browser.yaCheckLink2({
            selector: PO.route.organic.title.link(),
            url: {
                href: 'https://yandex.ru/maps/',
                ignore: ['protocol', 'query'],
            },
            target: '_blank',
            baobab: {
                path: '/$page/$main/$result/title',
            },
            message: 'Неправильная ссылка на тайтле',
        });
    });

    it('Проверка кнопок с простым запросом', async function() {
        await this.browser.yaOpenSerp({
            text: 'как добраться',
            data_filter: 'route',
        }, PO.route());

        const url1 = await this.browser.yaCheckLink2({
            selector: PO.route.goAuto(),
            baobab: {
                path: '/$page/$main/$result/actions/go-auto',
            },
            message: 'Неправильная ссылка на машине',
            target: '_blank',
        });

        assert.equal(url1.host, 'yandex.ru', 'Авто - Неправильный host');
        assert.equal(url1.pathname, '/maps/', 'Авто - Неправильный pathname');
        assert.equal(url1.query.rtext, '~', 'Авто - Неправильный rtext');
        assert.equal(url1.query.rtt, 'auto', 'Авто - Передан неправильный вид транспорта');

        const url2 = await this.browser.yaCheckLink2({
            selector: PO.route.goMt(),
            baobab: {
                path: '/$page/$main/$result/actions/go-mt',
            },
            message: 'Неправильная ссылка на транспоорте',
            target: '_blank',
        });

        assert.equal(url2.host, 'yandex.ru', 'ОТ - Неправильный host');
        assert.equal(url2.pathname, '/maps/', 'Авто - Неправильный pathname');
        assert.equal(url2.query.rtext, '~', 'Авто - Неправильный rtext');
        assert.equal(url2.query.rtt, 'mt', 'ОТ - Передан неправильный вид транспорта');
    });

    it('Проверка кнопок с указанием точек маршрута', async function() {
        await this.browser.yaOpenSerp({
            text: 'маршрут из балашихи в москву',
            data_filter: 'route',
        }, PO.route());

        const url3 = await this.browser.yaCheckLink2({
            selector: PO.route.goAuto(),
            baobab: {
                path: '/$page/$main/$result/actions/go-auto',
            },
            message: 'Неправильная ссылка на машине',
            target: '_blank',
        });

        assert.equal(url3.host, 'yandex.ru', 'Авто - Неправильный host');
        assert.equal(url3.pathname, '/maps/', 'Авто - Неправильный pathname');
        assert.equal(url3.query.rtext, 'Балашиха~Москва', 'Авто - Неправильный rtext');
        assert.equal(url3.query.rtt, 'auto', 'Авто - Передан неправильный вид транспорта');

        const url4 = await this.browser.yaCheckLink2({
            selector: PO.route.goMt(),
            baobab: {
                path: '/$page/$main/$result/actions/go-mt',
            },
            message: 'Неправильная ссылка на транспоорте',
            target: '_blank',
        });

        assert.equal(url4.host, 'yandex.ru', 'ОТ - Неправильный host');
        assert.equal(url4.pathname, '/maps/', 'Авто - Неправильный pathname');
        assert.equal(url4.query.rtext, 'Балашиха~Москва', 'Авто - Неправильный rtext');
        assert.equal(url4.query.rtt, 'mt', 'ОТ - Передан неправильный вид транспорта');
    });
});
