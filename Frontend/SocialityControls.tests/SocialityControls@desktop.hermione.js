specs({
    feature: 'SocialityControls',
    experiment: 'Альтернативные контролы социальности',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=socialitycontrols/default.json&exp_flags=alternative-sociality-controls=1&hermione_commentator=stub')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.socialityControlsIdle())
            .moveToObject(PO.socialityControls(), 0, 400)
            .assertView('plain', PO.socialityControls())
            .moveToObject(PO.socialityControls.comments())
            .assertView('comments-hovered', PO.socialityControls())
            .moveToObject(PO.socialityControls.like())
            .assertView('like-hovered', PO.socialityControls())
            .click(PO.socialityControls.like())
            .assertView('like-active-hovered', PO.socialityControls())
            .moveToObject(PO.socialityControls(), 0, 400)
            .assertView('like-active', PO.socialityControls());
    });
});
