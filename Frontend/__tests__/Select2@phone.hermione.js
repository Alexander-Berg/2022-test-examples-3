specs({
    feature: 'Select2',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=select2/default.json')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Выбор значения', function() {
        return this.browser
            .url('/turbo?stub=select2/default.json')
            .yaIndexify(PO.select2())
            .yaIndexify(PO.select2.option())

            .click(PO.select21())
            .click(PO.select21.option3())
            .assertView('long-value', PO.select21())

            .click(PO.select21())
            .click(PO.select21.option2())
            .getValue(PO.select21.control()).then(function(value) {
                assert.strictEqual(value, '0', 'значение не изменилось на выбранное');
            })

            .click(PO.select21())
            .click(PO.select21.option1())
            .getValue(PO.select21.control()).then(function(value) {
                assert.strictEqual(value, '0', 'значение изменилось, хотя не должно было');
            })

            .click(PO.select2Disabled())
            .yaShouldNotBeVisible(PO.select2Disabled.option1(), 'появился заблокированный селект');
    });
});
