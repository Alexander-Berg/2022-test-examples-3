specs({
    feature: 'card',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид базовой карточки Спорта', function() {
        return this.browser
            .url('/turbo?stub=card/sport.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('text-card', '.hermione__text')
            .assertView('picture-card', '.hermione__picture')
            .assertView('video-card', '.hermione__video')
            .assertView('footer-filled', '.hermione-card-footer-filled .card__footer')
            .yaMockFetch({
                urlDataMap: {
                    '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                    '/collections/api/v0.1/link-status': '',
                },
            });
    });
});
