'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;
const SERVICES = [
    {
        name: 'Картинки',
        selector: PO.search.services.imagesLink(),
        url: 'http://yandex.ru/images/touch/search?text=test&parent-reqid=REQID&source=tabbar&lite=1',
        baobab: '/$page/$header/$navigation/link[@type="images"]'
    },
    {
        name: 'Видео',
        selector: PO.search.services.videoLink(),
        url: 'http://yandex.ru/video/touch/search?text=test&source=tabbar',
        baobab: '/$page/$header/$navigation/link[@type="video"]'
    },
    {
        name: 'Карты',
        selector: PO.search.services.mapsLink(),
        url: 'http://yandex.ru/maps/?source=serp_navig&text=test',
        baobab: '/$page/$header/$navigation/link[@type="maps"]',
        target: '_blank'
    },
    {
        name: 'Маркет',
        selector: PO.search.services.marketLink(),
        url: 'http://m.market.yandex.ru/search?cvredirect=2&text=test&source=tabbar',
        baobab: '/$page/$header/$navigation/link[@type="market"]',
        target: '_blank'
    },
    {
        name: 'Новости',
        selector: PO.search.services.newsLink(),
        url: 'http://m.news.yandex.ru/yandsearch?text=test&rpt=nnews2&grhow=clutop&source=tabbar',
        baobab: '/$page/$header/$navigation/link[@type="news"]'
    },
    {
        name: 'Все',
        selector: PO.search.services.allLink(),
        url: 'http://yandex.ru/all?text=test&source=tabbar',
        baobab: '/$page/$header/$navigation/link[@type="all"]'
    }
];

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Поисковая стрелка', function() {
    const firstRequest = 'test';
    const secondRequest = 'testqz';

    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=' + firstRequest)
            .yaWaitForVisible(PO.search(), 'Стрелка не появилась');
    });

    it('Проверка BackSpace', function() {
        return this.browser
            .setValue(PO.search.input(), 'qz')
            .yaKeyPress('BACKSPACE')
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, 'q', 'Поле запроса содержит неверное значение')
            );
    });

    it('Очистка инпута', function() {
        return this.browser
            .click(PO.search.clear())
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, '', 'Поле запроса не очищено после клика на крестик')
            );
    });

    it('Выполняется перезапрос с другим запросом', function() {
        return this.browser
            .setValue(PO.search.input(), secondRequest)
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, secondRequest, 'Не удалось поменять запрос')
            )
            .yaWaitUntilPageReloaded(() => this.browser.click(PO.search.button()))
            .yaParseUrl().then(url =>
                assert.equal(url.query.text, secondRequest, 'Значение параметра text не изменилось')
            );
    });

    hermione.only.notIn('winphone', 'фича не актуальна в winphone');
    it("Запрос меняется при переходе по кнопке браузера 'назад'", function() {
        return this.browser
            .setValue(PO.search.input(), secondRequest)
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, secondRequest, 'Не удалось поменять запрос')
            )
            .yaWaitUntilPageReloaded(() => this.browser.click(PO.search.button()))
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, secondRequest, 'Не удалось поменять запрос')
            )
            .back()
            .yaWaitUntil('Запрос не стал прежним при переходе назад',
                () => this.browser.getValue(PO.search.input())
                    .then(text => text === firstRequest)
            )
            .yaWaitUntil('Расхлопушка не схлопнулась',
                () => this.browser.getAttribute('body', 'class')
                    .then(bodyCls => bodyCls === '')
            )
            .getAttribute(PO.search.input(), 'class').then(inputCls => {
                assert(inputCls.indexOf(' f') === -1, 'Отображение стрелки не изменилось');
            })
            .yaShouldNotBeVisible(PO.search.button());
    });

    it('Выполняется перезапрос с пустым запросом', function() {
        return this.browser
            .click(PO.search.clear())
            .yaWaitUntilPageReloaded(() => this.browser.click(PO.search.button()))
            .yaParseUrl().then(url =>
                assert.equal(url.query.text, '', 'Значение параметра text не изменилось')
            );
    });

    it('Проверка ссылок и счётчиков сервисов', function() {
        return SERVICES.reduce((browser, service) => {
            return browser
                .yaCheckLink(service.selector, { target: service.target || '' }).then(url => {
                    if (url.query['parent-reqid']) { url.query['parent-reqid'] = 'REQID' }

                    return browser.yaCheckURL(url, service.url, `Некорректный URL в ссылке "${service.name}"`, {
                        skipProtocol: true
                    });
                })
                .yaMockExternalUrl(service.selector)
                .yaCheckBaobabCounter(service.selector, { path: service.baobab })
                .execute(function(selector) {
                    // Скрываем ссылку, после того, как её проверили.
                    // Если этого не делать, то на ссылках за скролом будет ошибка
                    // Error: An element command could not be completed because the element is not visible on the page.
                    /* jshint browser: true */
                    document.querySelector(selector).style = 'display:none';
                }, service.selector);
        }, this.browser);
    });
});
