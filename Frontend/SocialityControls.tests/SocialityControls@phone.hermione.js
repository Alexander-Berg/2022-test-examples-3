specs({
    feature: 'SocialityControls',
    experiment: 'Альтернативные контролы социальности',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=socialitycontrols/default.json&exp_flags=alternative-sociality-controls=1&hermione_commentator=stub')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.socialityControlsIdle())
            .moveToObject(PO.socialityControls(), 0, 400)
            .assertView('plain', PO.socialityControls())
            .click(PO.socialityControls.like())
            .assertView('like-active-hovered', PO.socialityControls());
    });
});
