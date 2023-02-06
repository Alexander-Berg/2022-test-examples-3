specs({
    feature: 'beru-age-disclaimer',
}, () => {
    function checkIframeRedirect(type) {
        return function() {
            return this.browser
                .yaOpenInIframe(`?stub=beruagedisclaimer/type-${type}.json`)
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(PO.blocks.beruAgeDisclaimer.rejectButton())
                .getTabIds()
                .then(ids => assert.lengthOf(ids, 1), 'Таб должен быть один').getUrl()
                .then(url => {
                    assert.include(url, 'm.pokupki.market.yandex.ru', 'Неверная ссылка');
                });
        };
    }

    function checkRejectButton(type) {
        return function() {
            return this.browser
                .url(`/turbo?stub=beruagedisclaimer/type-${type}.json`)
                .yaWaitForVisible(PO.blocks.beruAgeDisclaimer(), 'Страница не загрузилась')
                .click(PO.blocks.beruAgeDisclaimer.rejectButton())
                .getUrl()
                .then(url => {
                    assert.include(url, 'm.pokupki.market.yandex.ru', 'Неверная ссылка');
                });
        };
    }

    beforeEach(function() {
        this.browser.deleteCookie();
    });

    describe('Тип modal', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид блока', function() {
            return this.browser
                .url('/turbo?stub=beruagedisclaimer/type-modal.json')
                .yaWaitForVisible(PO.blocks.beruAgeDisclaimer(), 'Страница не загрузилась')
                .assertView('modal', PO.blocks.turboModalBeru());
        });

        hermione.only.notIn('safari13');
        it('Проверка кнопки "Есть 18"', function() {
            return this.browser
                .url('/turbo?stub=beruagedisclaimer/type-modal.json')
                .yaWaitForVisible(PO.blocks.beruAgeDisclaimer(), 'Страница не загрузилась')
                .click(PO.blocks.beruAgeDisclaimer.acceptButton())
                .yaWaitForHidden(PO.blocks.beruAgeDisclaimer(), 'Должен быть виден текст "Lorem Ipsum ...."')
                .getCookie('pokupkiAdult')
                .then(cookie => assert.ok(cookie, 'Кука "pokupkiAdult" должна быть выставлена'))
                .assertView('modal-text', PO.page.content.beruText());
        });

        hermione.only.notIn('safari13');
        it('Проверка кнопки "Нет"', checkRejectButton('modal'));

        hermione.only.notIn('safari13');
        it('Проверка закрытия окна через крестик в углу', function() {
            return this.browser
                .url('/turbo?stub=beruagedisclaimer/type-modal.json')
                .yaWaitForVisible(PO.blocks.beruAgeDisclaimer(), 'Страница не загрузилась')
                .click(PO.blocks.turboModalBeru.close())
                .getUrl()
                .then(url => {
                    assert.include(url, 'm.pokupki.market.yandex.ru', 'Неверная ссылка');
                });
        });

        hermione.only.notIn('safari13');
        it('Клик по кнопке "Нет" не должен приводить к открытию нового таба в iframe', checkIframeRedirect('modal'));
    });

    describe('Тип inline', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид блока', function() {
            return this.browser
                .url('/turbo?stub=beruagedisclaimer/type-inline.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('inline', PO.blocks.beruAgeDisclaimer());
        });

        hermione.only.notIn('safari13');
        it('Клик по кнопке "Есть 18" должен выставлять куку и редиректить на запрашиваемую страницу', function() {
            return this.browser
                .url('/turbo?stub=beruagedisclaimer/type-inline.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .click(PO.blocks.beruAgeDisclaimer.acceptButton())
                .getCookie('pokupkiAdult')
                .then(cookie => assert.isOk(cookie, 'Кука "pokupkiAdult" должна быть выставлена'))
                .getUrl()
                .then(url => (
                    assert.include(url, 'stub=beruagedisclaimer', 'Ссылка не должрна измениться')
                ));
        });

        hermione.only.notIn('safari13');
        it('Клик по кнопке "Нет" должен открывать главную страницу сервиса', checkRejectButton('inline'));

        hermione.only.notIn('safari13');
        it('Клик по кнопке "Нет" не должен приводить к открытию нового таба в iframe', checkIframeRedirect('inline'));
    });
});
