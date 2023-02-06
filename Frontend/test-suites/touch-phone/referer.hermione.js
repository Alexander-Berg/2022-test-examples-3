'use strict';

const PO = require('../../page-objects/common/index').PO;

specs('Проверка передачи реферера', function() {
    beforeEach(function() {
        return this.browser.yaOpenSerp({ text: 'ya.ru/white site:ya.ru' }, PO.organic());
    });

    it('При переходе по ссылке результата выдачи корректно передаётся реферер', function() {
        return this.browser
            .click(PO.organic.title.link())
            .getTabIds()
            .then(tabsId => {
                let tabId = tabsId[tabsId.length - 1];

                return this.browser.switchTab(tabId);
            })
            .yaWaitForVisible('body', 'Ошибка при открытии страницы при клике на заголовок сниппета')
            .execute(function() {
                return window.document.referrer;
            })
            .then(function(result) {
                assert.isOk(result.value, 'Реферер не передался при клике на заголовок сниппета');
            });
    });

    it('При переходе по ссылке результата выдачи отправляется счётчик с реферером вида host+path', function() {
        return this.browser
            .yaExecute(function() {
                return window.location.href.split('?')[0];
            }).then(location =>
                this.browser.yaCheckBaobabCounter(PO.organic.title.link(), {
                    path: '/$page/$main/$result/title',
                    raw: {
                        HTTP_REFERER: location.value
                    }
                })
            );
    });
});
