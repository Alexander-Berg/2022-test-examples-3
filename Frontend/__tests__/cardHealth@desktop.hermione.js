specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    it('Внешний вид карточки статьи', function() {
        return this.browser
            .url('/turbo?stub=card/article.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardHealth());
    });
    it('Внешний вид карточки статьи без картинки', function() {
        return this.browser
            .url('/turbo?stub=card/articleText.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardHealth());
    });
    it('Внешний вид широкой карточки статьи', function() {
        return this.browser
            .url('/turbo?stub=card/doubleArticle.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardHealth());
    });
    it('Внешний вид широкой карточки статьи без картинки', function() {
        return this.browser
            .url('/turbo?stub=card/doubleTextArticle.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardHealth());
    });
    it('Внешний вид карточки энциклопедии', function() {
        return this.browser
            .url('/turbo?stub=card/encyclopedia.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardEncyclopedia());
    });
    it('Внешний вид карточек статей в статистике', function() {
        return this.browser
            .url('/turbo?stub=card/healthStat-desktop.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthArticles());
    });
});
