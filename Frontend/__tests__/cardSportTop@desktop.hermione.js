specs({
    feature: 'card',
}, () => {
    it('Внешний вид карточки Топа Спорта с картинкой', function() {
        return this.browser
            .url('/turbo?stub=card/sport-top.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .moveToObject('.stub')
            .assertView('plain', '.hermione__picture')
            .moveToObject('.hermione__picture')
            .assertView('hovered', '.hermione__picture')
            .yaMockFetch({
                urlDataMap: {
                    '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                    '/collections/api/v0.1/link-status': '',
                },
            });
    });
});
