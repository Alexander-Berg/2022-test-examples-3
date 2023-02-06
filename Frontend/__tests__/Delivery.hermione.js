describe('Delivery', function() {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=delivery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.radioGroup())
            .click(PO.radioGroup.radioItem1())
            .getAttribute(PO.radioGroup.radioItem.inputChecked(), 'value')
            .then(value => assert.strictEqual(value, 'courier2'));
    });
});
