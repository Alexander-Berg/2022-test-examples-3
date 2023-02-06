specs({
    feature: 'beruReasons',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=berureasons/default.json')
            .yaWaitForVisible(PO.blocks.beruReasons(), 'Блок не появился')
            .assertView('plain', PO.blocks.beruReasons());
    });

    hermione.only.notIn('safari13');
    it('Вид блока с одной причиной', function() {
        return this.browser
            .url('/turbo?stub=berureasons/one.json')
            .yaWaitForVisible(PO.blocks.beruReasons(), 'Блок не появился')
            .assertView('one', PO.blocks.beruReasons());
    });

    hermione.only.notIn('safari13');
    it('Вид блока с несколькими характеристиками. Отображается как одна причина.', function() {
        return this.browser
            .url('/turbo?stub=berureasons/consumer-factors.json')
            .yaWaitForVisible(PO.blocks.beruReasons(), 'Блок не появился')
            .assertView('consumer-factors', PO.blocks.beruReasons());
    });

    hermione.only.notIn('safari13');
    it('Вид блока с причинами "смотрели" и "купили"', function() {
        return this.browser
            .url('/turbo?stub=berureasons/viewed-and-bought.json')
            .yaWaitForVisible(PO.blocks.beruReasons(), 'Блок не появился')
            .assertView('viewed-and-bought', PO.blocks.beruReasons());
    });
});
