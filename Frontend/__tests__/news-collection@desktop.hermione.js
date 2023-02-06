specs({
    feature: 'Карточка коллекций',
}, () => {
    it('Внешний вид коллекции', function() {
        return this.browser
            .url('/turbo?stub=newscollection/default.json')
            .assertView('sport', '.hermione__sport-collection');
    });
});
