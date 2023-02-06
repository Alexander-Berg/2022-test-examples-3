specs({
    feature: 'LcScreen',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид экрана', function() {
        return this.browser
            .url('/turbo?stub=lcscreen/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не отобразилась')
            .assertView('plain', PO.lcScreen());
    });
});
