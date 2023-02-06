specs({
    feature: 'Карточка ТОПа Спорта',
}, () => {
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=newscardsporttop/desktop.json')
            .yaWaitForVisible(PO.blocks.newsCardSportTop(), 'Страница не загрузилась')
            .moveToObject('.news-carousel__control_side_right')
            .assertView('plain', PO.blocks.newsCardSportTop());
    });
});
