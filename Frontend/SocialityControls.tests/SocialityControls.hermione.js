specs({
    feature: 'SocialityControls',
    experiment: 'Альтернативные контролы социальности',
}, () => {
    hermione.only.notIn('safari13');
    it('Проверка счётчиков', function() {
        const path = '$page.$main.$result.cover.description.sociality-controls.sociality-control';
        return this.browser
            .url('/turbo?stub=socialitycontrols/article.json&exp_flags=alternative-sociality-controls=1&hermione_commentator=stub')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.socialityControlsIdle())
            .yaCheckBaobabServerCounter({
                path,
                attrs: {
                    type: 'like',
                    pos: 0,
                },
            })
            .yaCheckBaobabServerCounter({
                path,
                attrs: {
                    type: 'comments',
                    pos: 1,
                },
            })
            .yaCheckBaobabCounter(PO.socialityControls.like(), {
                path,
                attrs: {
                    type: 'like',
                    pos: 0,
                },
            })
            .yaCheckBaobabCounter(PO.socialityControls.comments(), {
                path,
                attrs: {
                    type: 'comments',
                    pos: 1,
                },
            });
    });
});
