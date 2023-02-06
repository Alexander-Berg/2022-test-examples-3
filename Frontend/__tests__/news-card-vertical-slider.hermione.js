specs({
    feature: 'Карта вертикального слайдера c доскролом в Новостях (news-card-vertical-slider)',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид вертикального слайдера Новостей', function() {
        return this.browser
            .url('/turbo?stub=newscardverticalslider/default.json')
            .yaWaitForVisible(PO.blocks.newsCardVerticalSlider(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.newsCardVerticalSlider());
    });

    hermione.only.notIn('safari13');
    it('Длинные названия изданий переносятся на новую строку со знаком ракеты', function() {
        return this.browser
            .url('/turbo?stub=newscardverticalslider/longAgencyName.json')
            .yaWaitForVisible(PO.blocks.newsCardVerticalSlider(), 'Страница не загрузилась')
            .assertView('plain', '.news-card-snippet__agency-info_is-turbo');
    });
});
