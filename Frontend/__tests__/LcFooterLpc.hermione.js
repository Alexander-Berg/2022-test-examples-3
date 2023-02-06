specs({
    feature: 'LcFooterLpc',
}, () => {
    hermione.only.notIn('safari13');
    it('Default', function() {
        return this.browser
            .url('/turbo?stub=lcfooterlpc/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcFooterLpc());
    });

    hermione.only.notIn('safari13');
    it('С социальными сетями на всю ширину', function() {
        return this.browser
            .url('/turbo?stub=lcfooterlpc/withSocials.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('withSocials', PO.lcFooterLpc());
    });
});
