specs({
    feature: 'Custom-HTML [React]',
}, () => {
    beforeEach(function() {
        return this.browser
            .url('?stub=custom%2Fcustom-html-react.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .assertView('plain', PO.blocks.custom());
    });

    hermione.only.notIn('safari13');
    it('CSS классы', function() {
        return this.browser
            .yaIndexify(PO.blocks.custom())
            .getAttribute(PO.blocks.firstCustom(), 'class')
            .then(className => assert.equal(className, 'custom good-old-ul'))
            .getAttribute(PO.blocks.thirdCustom(), 'class')
            .then(className => assert.equal(className, 'custom'))
            .getAttribute(PO.blocks.fifthCustom(), 'class')
            .then(className => assert.equal(className, 'custom bold-text'));
    });
});
