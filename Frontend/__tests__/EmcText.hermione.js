specs({
    feature: 'EmcText',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный текст', function() {
        return this.browser
            .url('/turbo?stub=emctext/default.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });

    hermione.only.notIn('safari13');
    it('Текст, выровненный по центру', function() {
        return this.browser
            .url('/turbo?stub=emctext/center.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });

    hermione.only.notIn('safari13');
    it('Текст, выровненный по правому краю, с отступами и фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emctext/right.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });

    hermione.only.notIn('safari13');
    it('Текст с заголовком и описанием. В описании присутствует html вёрстка', function() {
        return this.browser
            .url('/turbo?stub=emctext/title-description-html.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });

    hermione.only.notIn('safari13');
    it('Обычный текст (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emctext/default-columns.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });

    hermione.only.notIn('safari13');
    it('Текст, выровненный по правому краю, с отступами и фоном секции (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emctext/right-columns.json')
            .yaWaitForVisible(PO.emcText(), 'Текст не появился')
            .assertView('emctext', PO.emcText());
    });
});
