'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Форма смены региона', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=test')
            .yaWaitForVisible(PO.regionFooter.link(), 'Ссылка на форму региона не появилась');
    });

    afterEach(function() {
        return this.browser
            .deleteCookie('yp')
            .deleteCookie('yandex_gid');
    });

    it('Проверка ссылки и сохранения региона', function() {
        const city = 'Тверь';

        return this.browser
            .getText(PO.regionFooter.link()).then(text =>
                assert.notEqual(text, city, 'Изначальный регион совпадает с тем, на который будем менять')
            )
            .yaCheckLink(PO.regionFooter.link(), { target: '' }).then(url => this.browser
                .yaCheckURL(url, 'http://tune.yandex.ru/region/', 'Сломана ссылка на tune.yandex.ru', {
                    skipProtocol: true,
                    skipQuery: true
                }))
            .yaCheckBaobabCounter(PO.regionFooter.link(), { path: '/$page/$main/region-change/link' })
            // Уходим на другой запрос, чтобы отличать дамп "до" от дампа "после" изменения региона
            .yaWaitUntilPageReloaded(() => this.browser.yaOpenSerp('text=another+test'))
            .click(PO.regionFooter.link())
            .yaWaitForVisible(PO.geoForm(), 'Сломался переход на страницу геолокации')
            .setValue(PO.geoForm.input(), city)
            .yaWaitForVisible(PO.geoAutocompletePopup(), 'Не появился автокомплит региона')
            .getText(PO.geoAutocompletePopup.firstItem.title()).then(text =>
                // проверка не относится к region-change, но если упадёт, то будет понятнее, что случилось
                assert.equal(text, city, `В автокомплите региона первый элемент не "${city}"`)
            )
            // В мобильной версии автокомплит реагирует только на тачовые события,
            // простой клик не триггерит выбор элемента из списка.
            // Мы не умеем тач события, поэтому используем клавиатурные действия, чтобы выбрать первый пункт.
            .yaKeyPress('ARROW_DOWN')
            .yaKeyPress('ENTER')
            // После выбора пункта сабмит автоматический
            .yaWaitForVisible(PO.regionFooter.link(), 'Сломалось возвращение обратно на серп')
            .getText(PO.regionFooter.link()).then(text =>
                assert.equal(text, city, 'Регион не изменился')
            );
    });
});
