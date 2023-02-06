'use strict';

const PO = require('../../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Смартбаннер', function() {
    [{
        type: 'default',
        foreverdata: 1402660465
    }, {
        type: 'features',
        foreverdata: 2412136857
    }].forEach(item => {
        describe('Общие проверки ' + item.type, function() {
            beforeEach(function() {
                return this.browser
                    .yaOpenSerp({
                        text: 'киска',
                        foreverdata: item.foreverdata
                    })
                    .yaWaitForVisible(PO.smartInfo(), 'Смартбаннер не появился на странице', 1000);
            });

            it('Проверка внешнего вида', function() {
                return this.browser
                    .yaDisableBoxShadow(PO.smartInfo())
                    .assertView('plain', PO.smartInfo());
            });

            it('Скрытие при клике в поле поиска', function() {
                return this.browser
                    .click(PO.search.input())
                    .yaWaitForHidden(PO.smartInfo(), 'Смартбаннер должен исчезнуть');
            });

            it('Проверка кнопки установки приложения', function() {
                return this.browser
                    .yaCheckBaobabCounter(PO.smartInfo.installButton(), {
                        path: '/$page/smart-info/ok'
                    })
                    .yaWaitForHidden(PO.smartInfo(), 'Смартбаннер должен исчезнуть');
            });

            it('Проверка кнопки отмены установки', function() {
                return this.browser
                    .yaCheckBaobabCounter(PO.smartInfo.closeButton(), {
                        path: '/$page/smart-info/close'
                    })
                    .yaWaitForHidden(PO.smartInfo(), 'Смартбаннер должен исчезнуть');
            });
        });
    });
});
