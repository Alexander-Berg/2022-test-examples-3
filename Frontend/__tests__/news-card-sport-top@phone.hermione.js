specs({
    feature: 'Карточка ТОПа Спорта',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=newscardsporttop/default.json')
            .yaWaitForVisible(PO.blocks.newsCardSportTop(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.newsCardSportTop());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с иконкой избранного', function() {
        return this.browser
            .url('/turbo?stub=newscardsporttop/default.json&exp_flags=yxnews_nerpa_sport_favorites=1')
            .yaWaitForVisible(PO.blocks.newsCardSportTop(), 'Страница не загрузилась')
            .assertView('favorites', PO.blocks.newsCardSportTop())
            .yaMockFetch({
                urlDataMap: {
                    '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                    '/collections/api/v0.1/link-status': '',
                },
            });
    });

    hermione.only.notIn('safari13');
    it('обрезает длинный источник', function() {
        return this.browser
            .url('/turbo?stub=newscardsporttop/withLongSource.json')
            .yaWaitForVisible(PO.blocks.newsCardSportTop(), 'Страница не загрузилась')
            .assertView('longSource', PO.blocks.newsCardSportTop.sourceName())
            .yaCheckClientErrors();
    });
});
