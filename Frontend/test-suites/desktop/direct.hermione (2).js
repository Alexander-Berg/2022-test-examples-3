'use strict';

const PO = require('../../page-objects/desktop').PO;

specs('Директ', function() {
    const YABS_COUNTER = 'http://yabs.yandex.ru/count/';

    it('Проверка блока директа', function() {
        return this.browser
            .yaOpenSerp({
                text: 'мир упаковки москва',
                foreverdata: '4235844571'
            })
            .yaWaitForVisible(PO.rightColumn.directHead(), 'Блок директа не появился')
            .yaCheckBaobabCounter(PO.rightColumn.directHeadLink(), { path: '/$page/$parallel/serp-adv-head/serplink' })
            .yaCheckLink(PO.rightColumn.directHeadLink()).then(url => this.browser
                .yaCheckURL(url, 'http://direct.yandex.ru', 'Сломана ссылка в заголовке "Яндекс.Директ"', {
                    skipProtocol: true,
                    skipQuery: true
                })
            );
    });

    it('Проверка блока директа на второй странице', function() {
        return this.browser
            .yaOpenSerp({
                text: 'мир упаковки москва',
                p: 1,
                foreverdata: '4235844571'
            })
            .yaWaitForVisible(PO.rightColumn.directHead(), 'Блок директа не появился');
    });

    it('Проверка сниппета директа с медицинским предупреждением', function() {
        return this.browser
            .yaOpenSerp({
                text: 'аптека ru',
                foreverdata: '4235844571'
            })
            .yaWaitForVisible(PO.rightColumn.directSnippet(), 'Рекламный сниппет не появился')
            .yaCheckLink(PO.rightColumn.directSnippet.title.link()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка в тайтле', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            )
            .yaCheckLink(PO.rightColumn.directSnippet.path.item()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка в гринурле', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            )
            .yaCheckLink(PO.rightColumn.directSnippet.contactLink()).then(url => this.browser
                .yaCheckURL(url, YABS_COUNTER, 'Сломана ссылка на контактную информацию', {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            );
    });

    it('Проверка сниппета директа с пометкой о возрастных ограничениях', function() {
        return this.browser
            .yaOpenSerp({
                text: 'мир упаковки москва',
                foreverdata: '3426667517'
            })
            .yaWaitForVisible(PO.rightColumn.directHead(), 'Блок директа не появился')
            .yaGetHTML(PO.rightColumn.directSnippet.text())
            .then(text => {
                assert.include(text, '18+', 'Сниппет рекламы не содержит возрастной метки');
            });
    });
});
