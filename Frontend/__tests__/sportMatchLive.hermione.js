hermione.only.in(['firefox']);

specs({
    feature: 'sportMatchLive',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=sportmatchlive/default.json')
            .yaWaitForVisible(PO.blocks.sportMatchLive(), 'Блок не появился')
            .assertView('plain', PO.blocks.sportMatchLive());
    });
    hermione.only.notIn('safari13');
    it('Статус - "Трансляция"', function() {
        return this.browser
            .url('/turbo?stub=sportmatchlive/live.json')
            .yaWaitForVisible(PO.blocks.sportMatchLive(), 'Блок не появился')
            .assertView('plain', PO.blocks.sportMatchLive());
    });
});
