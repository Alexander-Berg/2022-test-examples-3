specs({
    feature: 'Компонент "Ещё по теме" Яндекс.Новости на турбо-платформе',
}, () => {
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=cardverticallist/related.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .setViewportSize({ width: 1920, height: 1080 })
            .assertView('plane-1920x1080', '.card-vertical-list')
            .setViewportSize({ width: 1365, height: 768 })
            .assertView('plane-1365x768', '.card-vertical-list');
    });
});
