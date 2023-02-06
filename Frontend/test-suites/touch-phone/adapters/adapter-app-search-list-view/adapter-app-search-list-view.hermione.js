'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Вьюпорт списка приложений', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=приложение доставки еды&srcskip=ATOM_PROXY')
            .yaWaitForVisible(PO.appSearchListView(), 'Не появился список приложений');
    });

    it('Проверка ссылки и счётчика названия приложения', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.appSearchListView.app.title.link(), {
                path: '/$page/$main/$result[@wizard_name="app_search_list_view"]/app/applistcard-title'
            })
            .yaCheckLink(PO.appSearchListView.app.title.link());
    });

    it('Проверка ссылки и счётчика названия кнопки покупки', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.appSearchListView.app.button(), {
                path: '/$page/$main/$result[@wizard_name="app_search_list_view"]/app/applistcard-buy'
            })
            .yaCheckLink(PO.appSearchListView.app.button());
    });
    hermione.only.notIn(/winphone|iphone/, 'кнопка Все приложения показывается только на андроидах');
    it('Проверка ссылки и счётчика на кнопке все приложения', function() {
        return this.browser
            .yaCheckLink(PO.appSearchListView.allAppsButton());
    });
});
