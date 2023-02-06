specs({
    feature: 'LcButton',
}, () => {
    function checkSize(browser, size) {
        return browser.url(`/turbo?stub=lcbutton/size-${size}.json`)
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcButton(), 'На странице нет кнопки')
            .moveToObject(PO.lcButton(), 0, 400)
            .assertView('plain', PO.lcButton());
    }

    hermione.only.notIn('safari13');
    it('Размер XS', function() {
        return checkSize(this.browser, 'xs');
    });
    hermione.only.notIn('safari13');
    it('Размер S', function() {
        return checkSize(this.browser, 's');
    });
    hermione.only.notIn('safari13');
    it('Размер M', function() {
        return checkSize(this.browser, 'm');
    });
    hermione.only.notIn('safari13');
    it('Размер L', function() {
        return checkSize(this.browser, 'l');
    });
    hermione.only.notIn('safari13');
    it('Размер XL', function() {
        return checkSize(this.browser, 'xl');
    });
    hermione.only.notIn('safari13');
    it('Тема Action', function() {
        return this.browser.url('/turbo?stub=lcbutton/theme-action.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcButton(), 'На странице нет кнопки')
            .moveToObject(PO.lcButton(), 0, 400)
            .assertView('plain', PO.lcButton());
    });
    hermione.only.notIn('safari13');
    it('Тема BorderBlack', function() {
        return this.browser.url('/turbo?stub=lcbutton/theme-border-black.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcButton(), 'На странице нет кнопки')
            .moveToObject(PO.lcButton(), 0, 400)
            .assertView('plain', PO.lcButton());
    });
    hermione.only.notIn('safari13');
    it('Тема BorderWhite', function() {
        return this.browser.url('/turbo?stub=lcbutton/theme-border-white.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcButton(), 'На странице нет кнопки')
            .moveToObject(PO.lcButton(), 0, 400)
            .assertView('plain', PO.lcButton());
    });
    hermione.only.notIn('safari13');
    it('Тема Shadow', function() {
        return this.browser.url('/turbo?stub=lcbutton/theme-shadow.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .moveToObject(PO.lcButton(), 0, 400)
            .assertView('plain', PO.lcButton());
    });
    hermione.only.notIn('safari13');
    it('LPC link hover', function() {
        return this.browser.url('/turbo?stub=lcbutton/lpc-link.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .moveToObject(PO.lcButton())
            .assertView('plain', PO.lcButton());
    });
    hermione.only.notIn('safari13');
    it('LPC anchor hover', function() {
        return this.browser.url('/turbo?stub=lcbutton/lpc-anchor.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .moveToObject(PO.lcButton())
            .assertView('plain', PO.lcButton());
    });
});
