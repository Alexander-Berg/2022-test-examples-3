specs({
    feature: 'LcEventsSpeakerInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Информация о спикере', function() {
        return this.browser
            .url('/turbo?stub=lceventsspeakerinfo/default.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('plain', PO.lcEventsSpeakerInfo());
    });
});
