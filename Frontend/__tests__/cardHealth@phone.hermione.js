specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид карточки статьи', function() {
        return this.browser
            .url('/turbo?stub=card/article.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardHealth());
    });
    hermione.only.notIn('safari13');
    it('Внешний вид карточки энциклопедии', function() {
        return this.browser
            .url('/turbo?stub=card/encyclopedia.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.cardEncyclopedia());
    });
    hermione.only.notIn('safari13');
    it('Внешний вид карточек статей в статистике', function() {
        return this.browser
            .url('/turbo?stub=card/healthStat-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthArticles());
    });
});
