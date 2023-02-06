specs({
    feature: 'Карточка коллекций',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=newscollection/default.json')
            .assertView('sport', '.hermione__sport-collection')
            .execute(function() {
                document.getElementsByClassName('turbo-native-scroll__items-list')[0].scroll(10000, 0);
            })
            .assertView('scroll', PO.blocks.newsCollection());
    });

    hermione.only.notIn('safari13');
    it('обрезает длинный заголовок', function() {
        return this.browser
            .url('/turbo?stub=newscollection/withLongTitle.json')
            .yaWaitForVisible(PO.blocks.newsCollection(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.newsCollection.title());
    });
});
