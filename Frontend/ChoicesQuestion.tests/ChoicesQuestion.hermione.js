describe('forms-components_ChoicesQuestion', () => {
    it('view default', function() {
        return this.browser
            .openComponent('forms-components', 'choicesquestion', 'default')
            .assertView('default', ['.ChoicesQuestion'])

            .moveToObject('.Choice-Text')
            .assertView('choice-hover', ['.ChoicesQuestion'])
            .moveToObject('body', -1, -1)

            .click('.ChoicesQuestion-Choice:first-of-type .Radiobox')
            .assertView('checked-radio', ['.ChoicesQuestion'])

            .click('.Checkbox.BaseQuestion-HeaderControl')
            .click('.Select2.BaseQuestion-HeaderControl')
            .assertView('type-popup', ['.ChoicesQuestion'])

            .moveToObject('.Popup2 .Menu-Item:nth-child(2) .Menu-Text')
            .buttonDown()
            .buttonUp()
            .assertView('popup-select-2', ['.ChoicesQuestion'])

            .click('.ChoicesQuestion-Choice:first-of-type .Checkbox')
            .assertView('checked-checkbox', ['.ChoicesQuestion']);
    });
});
