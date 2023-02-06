specs({
    feature: 'LcTextarea',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная текстареа', function() {
        return this.browser
            .url('/turbo?stub=lctextarea/default.json')
            .yaWaitForVisible(PO.lcTextarea(), 'Текстареа не появилась')
            .assertView('plain', PO.lcTextarea());
    });

    hermione.only.notIn('safari13');
    it('Текстареа с контентом', function() {
        return this.browser
            .url('/turbo?stub=lctextarea/filled.json')
            .yaWaitForVisible(PO.lcTextarea(), 'Текстареа не появилась')
            .assertView('filled', PO.lcTextarea());
    });

    hermione.only.notIn('safari13');
    it('Невалидная текстареа', function() {
        return this.browser
            .url('/turbo?stub=lctextarea/invalid.json')
            .yaWaitForVisible(PO.lcTextarea(), 'Текстареа не появилась')
            .assertView('invalid', PO.lcTextarea());
    });
});
