specs({
    feature: 'LcButton',
}, () => {
    it('Рендер LcButton с LcPhone', function() {
        return this.browser
            .url('/turbo?stub=lcbutton/with-lc-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPhoneContent(), 'Кнопка не загрузилась')
            .assertView('plain', PO.lcPhoneContent())
            .click(PO.lcPhoneContent())
            .yaWaitForVisible(PO.lcPhoneModal(), 'Модал не открылся');
    });
});
