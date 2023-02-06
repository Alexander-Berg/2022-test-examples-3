specs({
    feature: 'YandexComments',
}, function() {
    hermione.only.notIn('safari13');
    it('Отступы от основного контента', function() {
        return this.browser
            .url('/turbo?stub=yandexcomments/default.json&hermione_commentator=stub')
            .yaWaitForVisible(PO.cmntMain())
            .assertView('plain', PO.page.result());
    });
});
