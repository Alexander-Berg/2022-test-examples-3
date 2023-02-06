specs({
    feature: 'beruRecomendations',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=berurecomendations/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaWaitForVisible(PO.blocks.beruRecomendations(), 'Компонент слайдера с рекомендациями не загрузился')
            .assertView('plain', PO.blocks.beruRecomendations());
    });

    hermione.only.notIn('safari13');
    it('Cниппеты должны проматываться в iframe', function() {
        return this.browser
            .yaOpenInIframe('?stub=berurecomendations/default.json')
            .yaWaitForVisible(PO.blocks.beruRecomendations(), 'Страница не загрузилась')
            .yaTouchScroll(PO.blocks.beruRecomendations(), 100)
            .assertView('iframe-scroll', PO.blocks.beruRecomendations());
    });
});
