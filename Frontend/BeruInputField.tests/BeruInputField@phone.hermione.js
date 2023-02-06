specs({
    feature: 'beruInputField',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=beruinputfield/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Очищение поля ввода по кнопке', function() {
        return this.browser
            .url('/turbo?stub=beruinputfield/functionality.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.beruInputField())
            .hasFocus(PO.blocks.beruInputField.inputField())
            .then(isFocused => assert.equal(isFocused, true, 'Поле ввода должно быть в фокусе'))
            .setValue(`${PO.blocks.beruInputField()} input`, 'тест')
            .yaWaitForVisible(PO.blocks.beruInputField.clearButton(), 'Кнопка очистки поля должна быть видна')
            .click(PO.blocks.beruInputField.clearButton())
            .getValue(PO.blocks.beruInputField.inputField())
            .then(text => assert.equal(text, '', 'Поле ввода должно быть пустым'))
            .hasFocus(PO.blocks.beruInputField.inputField())
            .then(isFocused => assert.equal(isFocused, true, 'Поле ввода должно остаься в фокусе'));
    });

    hermione.only.notIn('safari13');
    it('Фокус должен убираться с поля ввода при клике снаружи', function() {
        this.browser
            .url('/turbo?stub=beruinputfield/functionality.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.beruInputField())
            .hasFocus(PO.blocks.beruInputField.inputField())
            .then(isFocused => assert.equal(isFocused, true, 'Поле ввода должно быть в фокусе'))
            .click('body')
            .hasFocus(PO.blocks.beruInputField.inputField())
            .then(isFocused => assert.equal(isFocused, false, 'Поле ввода должно быть не в фокусе'));
    });
});
