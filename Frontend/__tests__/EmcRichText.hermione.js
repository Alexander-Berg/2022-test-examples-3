specs({
    feature: 'EmcRichText',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный текст', function() {
        return this.browser
            .url('/turbo?stub=emcrichtext/default.json')
            .yaWaitForVisible(PO.emcRichText(), 'Текст не загрузился')
            .assertView('emcrichtext', PO.emcRichText());
    });

    hermione.only.notIn('safari13');
    it('К текстовому элементу применены все возможные настройки', function() {
        return this.browser
            .url('/turbo?stub=emcrichtext/all-settings.json')
            .yaWaitForVisible(PO.emcRichText(), 'Текст не загрузился')
            .assertView('emcrichtext', PO.emcRichText());
    });

    hermione.only.notIn('safari13');
    it('В тексте присутствует html вёрстка', function() {
        return this.browser
            .url('/turbo?stub=emcrichtext/html.json')
            .yaWaitForVisible(PO.emcRichText(), 'Текст не загрузился')
            .assertView('emcrichtext', PO.emcRichText());
    });
});
