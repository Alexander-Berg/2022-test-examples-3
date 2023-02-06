specs({
    feature: 'radio-group',
}, () => {
    hermione.only.notIn('safari13');
    it('Группа кнопок выбора цвета', function() {
        return this.browser
            .url('/turbo?stub=radiogroup/color.json')
            .yaWaitForVisible(PO.radioGroup())
            .assertView('plain', PO.radioGroup())
            .click(PO.radioGroup.radioItem0())
            .getAttribute(PO.radioGroup.radioItem.inputChecked(), 'value')
            .then(value => assert.strictEqual(value, 'black'))
            .click(PO.radioGroup.radioItem1())
            .getAttribute(PO.radioGroup.radioItem.inputChecked(), 'value')
            .then(value => assert.strictEqual(value, 'gray'))
            .assertView('click', PO.radioGroup());
    });

    hermione.only.notIn('safari13');
    it('Группа радио-кнопок голубой темы', function() {
        return this.browser
            .url('/turbo?stub=radiogroup/default.json')
            .yaWaitForVisible(PO.radioGroup())
            .assertView('plain', PO.radioGroup())
            .click(PO.radioGroup.radioItem0())
            .getAttribute(PO.radioGroup.radioItem.inputChecked(), 'value')
            .then(value => assert.strictEqual(value, 'courier1'))
            .click(PO.radioGroup.radioItem1())
            .getAttribute(PO.radioGroup.radioItem.inputChecked(), 'value')
            .then(value => assert.strictEqual(value, 'courier2'))
            .assertView('click', PO.radioGroup());
    });

    hermione.only.notIn('safari13');
    it('Группа заблокированных радио-кнопок голубой темы', function() {
        return this.browser
            .url('/turbo?stub=radiogroup/disabled.json')
            .yaWaitForVisible(PO.radioGroup())
            .assertView('plain', PO.radioGroup())
            .click(PO.radioGroup.radioItem1())
            .assertView('click', PO.radioGroup());
    });

    hermione.only.notIn('safari13');
    it('Группа радио-кнопок кастомные цвета', function() {
        return this.browser
            .url('/turbo?stub=radiogroup/custom-theme.json')
            .yaWaitForVisible(PO.radioGroup())
            .assertView('plain', PO.radioGroup());
    });
});
