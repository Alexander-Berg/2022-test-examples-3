specs({
    feature: 'withVisibilityActions',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=withvisibilityactions/default.json')
            .yaWaitForVisible(PO.visibleVisibilityActions(), 'Текст не появился')
            .assertView('default', PO.visibleVisibilityActions())
            .click(PO.lcButtonList.button1())
            .assertView('in_moscow', PO.visibleVisibilityActions())
            .click(PO.lcButtonList.button2())
            .assertView('in_piter', PO.visibleVisibilityActions());
    });
});
