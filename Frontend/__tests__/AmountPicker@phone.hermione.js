hermione.only.in(['iphone', 'chrome-phone', 'searchapp']);
specs({
    feature: 'Amount-Picker',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/default.json')
            .yaWaitForVisible(PO.blocks.amountPicker())
            .assertView('default', PO.blocks.amountPicker());
    });

    hermione.only.notIn('safari13');
    it('Количество уменьшается на 1 при нажатии на "-"', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/default.json')
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '2'))
            .click(PO.blocks.amountPicker.decrButton())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '1'));
    });

    hermione.only.notIn('safari13');
    it('Количество увеличивается на 1 при нажатии на "+"', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/default.json')
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '2'))
            .click(PO.blocks.amountPicker.incrButton())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '3'));
    });

    // исходим из заданного минимального количества 0 и начального значения 2
    hermione.only.notIn('safari13');
    it('По достижении минимального количества кнопка "-" не нажимается', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/default.json')
            .click(PO.blocks.amountPicker.decrButton())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '1'))
            .click(PO.blocks.amountPicker.decrButton())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '0'))
            .getAttribute(PO.blocks.amountPicker.decrButton(), 'disabled')
            .then(value => assert.strictEqual(value, 'true'));
    });

    // исходим из заданного максимального количества 999 и начального значени 998
    hermione.only.notIn('safari13');
    it('По достижении максимального количества кнопка "+" не нажимается', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/big-value.json')
            .click(PO.blocks.amountPicker.incrButton())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '999'))
            .getAttribute(PO.blocks.amountPicker.incrButton(), 'disabled')
            .then(value => assert.strictEqual(value, 'true'))
            .assertView('incrButton-disabled', PO.blocks.amountPicker());
    });

    hermione.only.notIn('safari13');
    it('Если оставить инпут пустым и убрать курсор, в инпут вписывается 1', function() {
        return this.browser
            .url('/turbo?stub=amountpicker/test.json')
            .click(PO.blocks.amountPicker.input())
            .keys('Backspace')
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, ''))
            .click(PO.blocks.button())
            .getValue(PO.blocks.amountPicker.input())
            .then(value => assert.strictEqual(value, '1'));
    });
});
