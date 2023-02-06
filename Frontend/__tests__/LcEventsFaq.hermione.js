specs({
    feature: 'LcEventsFaq',
}, () => {
    hermione.only.notIn('safari13');
    it('Cписок вопросов-ответов', function() {
        return this.browser
            .url('/turbo?stub=lceventsfaq/default.json')
            .yaWaitForVisible(PO.page(), 'Cписок вопросов-ответов не появился')
            .assertView('plain', PO.lcEventsFaq());
    });
});
