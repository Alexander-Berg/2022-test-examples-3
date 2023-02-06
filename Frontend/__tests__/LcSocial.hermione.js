specs({
    feature: 'LcSocial',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычные социальные сети', function() {
        return this.browser
            .url('/turbo?stub=lcsocial/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Социальные сети маленького размера с белыми иконками', function() {
        return this.browser
            .url('/turbo?stub=lcsocial/smallWhite.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
    hermione.only.notIn('safari13');
    it('Социальные сети среднего размера с цветными иконками', function() {
        return this.browser
            .url('/turbo?stub=lcsocial/mediumColorful.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
    hermione.only.notIn('safari13');
    it('Социальные сети большого размера с черными иконками', function() {
        return this.browser
            .url('/turbo?stub=lcsocial/bigBlack.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
