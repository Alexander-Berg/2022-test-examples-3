specs({
    feature: 'YandexComments',
}, () => {
    it('Проверка отображения списка комментариев', function() {
        return this.browser
            .url('/turbo?stub=yandexcomments/default.json')
            .yaWaitForVisible(PO.cmntList());
    });

    it('Скролл до компонента', function() {
        return this.browser
            .url('/turbo?stub=yandexcomments/article.json&cmnt_login=comment')
            .yaWaitForVisible(PO.page())
            .yaWaitForVisibleWithinViewport(PO.cmntList(), 'Страница не была доскроллена до компонента');
    });
});
