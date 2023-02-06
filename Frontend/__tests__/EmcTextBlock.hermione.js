specs({
    feature: 'EmcTextBlock',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный текст', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/default.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });

    hermione.only.notIn('safari13');
    it('Текст с выравниванием по центру, размером 24, начертанием regular, цветом 1166ff, и ссылкой', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/acenter-s24-tregular.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });

    hermione.only.notIn('safari13');
    it('Текст с выравниванием по левому краю, размером 20, начертанием bold, цветом 000099', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/aleft-s20-tbold.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });

    hermione.only.notIn('safari13');
    it('Текст с выравниванием по правому краю, размером 24, начертанием medium, цветом 1166ff, интерлиньяжем 32, трекингом 4', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/aleft-s24-tmedium.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });

    hermione.only.notIn('safari13');
    it('Текст с выравниванием по правому краю, размером 24, начертанием light, цветом 1166ff', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/aright-s24-tlight.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });

    hermione.only.notIn('safari13');
    it('Текст, содержащий html вёрстку', function() {
        return this.browser
            .url('/turbo?stub=emctextblock/html.json')
            .yaWaitForVisible(PO.emcTextBlock(), 'Текст не появился')
            .assertView('emctextblock', PO.emcTextBlock());
    });
});
