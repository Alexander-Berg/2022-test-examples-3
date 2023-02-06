specs({
    feature: 'LcEventsSpeaker',
}, () => {
    hermione.only.notIn('safari13');
    it('Cписок спикеров', function() {
        return this.browser
            .url('/turbo?stub=lceventsspeaker/default.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('plain', PO.lcEventsSpeaker());
    });
});
