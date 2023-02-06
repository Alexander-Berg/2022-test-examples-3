specs({
    feature: 'LcHeader',
}, () => {
    it('Внешний вид блока c большим количеством пунктов меню', function() {
        return this.browser
            .url('/turbo?stub=lcheader/many-options.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader())
            .moveToObject(PO.lcHeader.moreButton())
            .assertView('openned', PO.lcHeader.moreMenu());
    });

    it('Внешний вид блока с полупрозрачным фоном', function() {
        return this.browser
            .url('/turbo?stub=lcheader/custom-background.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader())
            .moveToObject(PO.lcHeader.moreButton())
            .assertView('menu-opened', PO.lcHeader.moreMenu())
            .click(PO.lcHeader.cartButton())
            .assertView('cart-opened', PO.lcHeader.cartPopup());
    });

    it('Внешний вид с LcPhone', function() {
        return this.browser
            .url('/turbo?stub=lcheader/with-lc-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPhoneButton(), 'Кнопка LcPhone не появилась')
            .assertView('plain', PO.lcPhoneButton())
            .click(PO.lcPhoneButton())
            .yaWaitForVisible(PO.lcPhoneModal(), 'Модал телефона не открылся')
            .assertView('modal-opened', PO.lcPhoneModal())
            .click(PO.lcPhoneModalClose())
            .yaWaitForHidden(PO.lcPhoneModal(), 'Модал телефона не закрылся');
    });

    it('Внешний вид с LcPhone небольшой размер', function() {
        return this.browser
            .url('/turbo?stub=lcheader/with-small-lc-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPhoneButton(), 'Кнопка LcPhone не появилась')
            .assertView('plain', PO.lcPhoneButton());
    });

    it('Внешний вид с LcPhone несколько телефонов', function() {
        return this.browser
            .url('/turbo?stub=lcheader/with-lc-phone-several.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPhoneButton(), 'Кнопка LcPhone не появилась')
            .assertView('plain', PO.lcPhoneButton())
            .click(PO.lcPhoneButton())
            .yaWaitForVisible(PO.lcPhoneModal(), 'Модал телефона не открылся')
            .assertView('modal-opened', PO.lcPhoneModal());
    });
});
