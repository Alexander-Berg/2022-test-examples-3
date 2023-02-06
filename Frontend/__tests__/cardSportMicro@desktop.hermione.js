specs({
    feature: 'card',
}, () => {
    it('Внешний вид микро-карточки Спорта', function() {
        return this.browser
            .url('/turbo?stub=card/sport-micro.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.hermione__text')
            .assertView('picture-card', '.hermione__picture');
    });
});
