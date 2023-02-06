hermione.skip.in(['chrome-phone']);
specs({
    feature: 'LcPageGuides',
}, () => {
    hermione.only.notIn('safari13');
    it('Направляющие показываются на странице', function() {
        return this.browser
            .url('/turbo?stub=lcpageguides/default.json')
            .yaWaitForVisible(PO.lcPageGuides(), 'Направляющие не отобразились')
            .assertView('plain', PO.lcPageGuides(), { allowViewportOverflow: true });
    });
    hermione.only.notIn('safari13');
    it('Направляющие показываются на странице с контентом', function() {
        return this.browser
            .url('/turbo?stub=lcpageguides/withContent.json')
            .yaWaitForVisible(PO.lcPageGuides(), 'Направляющие не отобразились')
            .assertView('plain', PO.lcPageGuides(), { allowViewportOverflow: true });
    });
    hermione.only.notIn('safari13');
    it('Направляющие скрыты', function() {
        return this.browser
            .url('/turbo?stub=lcpageguides/hidden.json')
            .yaWaitForVisible(PO.hermioneContainer(), 'Контейнер не отобразился')
            .elements(PO.blocks.lcPageGuides())
            .then(elements => { assert.equal(elements.value.length, 0) });
    });
    hermione.only.notIn('safari13');
    it('Направляющие скрыты на странице с контентом', function() {
        return this.browser
            .url('/turbo?stub=lcpageguides/hiddenWithContent.json')
            .yaWaitForVisible(PO.hermioneContainer(), 'Контейнер не отобразился')
            .elements(PO.blocks.lcPageGuides())
            .then(elements => { assert.equal(elements.value.length, 0) });
    });
});
