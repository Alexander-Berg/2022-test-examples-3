specs({
    feature: 'Компонент темы Яндекс.Новости на турбо-платформе',
}, () => {
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=card%2Fnews-theme.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .setViewportSize({ width: 1020, height: 768 })
            .assertView('news-theme', '.card.card_theme_news-theme');
    });
});
