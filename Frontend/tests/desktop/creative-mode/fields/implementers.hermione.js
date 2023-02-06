const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';
const VALUE_TO_ENTER = {
    value: 'annvas',
    item: 'Анна Стаховская (Васильева) (@annvas)',
    text: 'Анна Стаховская (Васильева)',
};

describe('Поля режима креатива', function() {
    describe('implementers', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 2500, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.implementers())
                .assertView('umb-implementers-plain', PO.creativeMode.table.headerRow.implementers())
                .doubleClick(PO.creativeMode.table.headerRow.implementers())
                .waitForVisible(
                    PO.creativeMode.table.headerRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.table.headerRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .assertView(
                    'umb-implementers-editable',
                    PO.creativeMode.table.headerRow.implementers(),
                )
                .assertView(
                    'outline-implementers-plain',
                    PO.creativeMode.table.contentRow.implementers(),
                )
                .doubleClick(PO.creativeMode.table.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.table.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.table.contentRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .assertView(
                    'outline-implementers-editable',
                    PO.creativeMode.table.contentRow.implementers(),
                );
        });

        it('поле редактируется в зонтике', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.headerRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.headerRow.implementers.inputFocused(),
                    PO.creativeMode.secondTable.headerRow.implementers.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.implementers(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .click(PO.creativeMode.secondTable.headerRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.headerRow.implementers.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.headerRow.implementers())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000)
                .doubleClick(PO.creativeMode.secondTable.headerRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaKeyPress(['BACKSPACE'])
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.implementers(),
                    '',
                    'Значение должно удалиться',
                )
                .click(PO.creativeMode.secondTable.headerRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.headerRow.implementers.suggest(),
                    'Саджест не скрылся',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.implementers(),
                    '',
                    'Пустое значение должно сохраниться',
                );
        });

        it('поле редактируется в контуре', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.contentRow.implementers.inputFocused(),
                    PO.creativeMode.secondTable.contentRow.implementers.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.implementers(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .click(PO.creativeMode.secondTable.contentRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.implementers())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000)
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaKeyPress(['BACKSPACE'])
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.implementers(),
                    '',
                    'Значение должно удалиться',
                )
                .click(PO.creativeMode.secondTable.contentRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не скрылся',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.implementers(),
                    '',
                    'Пустое значение должно сохраниться',
                );
        });

        it('редактирование сбрасывается по escape', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.contentRow.implementers.inputFocused(),
                    PO.creativeMode.secondTable.contentRow.implementers.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.implementers(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .yaKeyPress(['ESC'])
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не скрылся',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.implementers(),
                    '',
                    'Значение не должно сохраниться',
                );
        });

        it('значение добавляется по пробелу', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .yaKeyPress(VALUE_TO_ENTER.value)
                .yaKeyPress(' ')
                .yaWaitUntil('Значение исчезло из input', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.implementers.inputControl())
                        .getValue()
                        .then(text => text.trim() === '');
                }, 10000)
                .yaWaitUntil('Значение должно добавиться в саджест', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.implementers.suggest())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('значение добавляется по запятой', async function() {
            await this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .yaKeyPress('@' + VALUE_TO_ENTER.value)
                .yaKeyPress(',');

            await this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.contentRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .yaKeyPress('@' + VALUE_TO_ENTER.value)
                .yaKeyPress(',')
                .yaWaitUntil('Значение исчезло из input', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.implementers.inputControl())
                        .getValue()
                        .then(text => text.trim() === '');
                }, 10000)
                .yaWaitUntil('Значение должно добавиться в саджест', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.implementers.suggest())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('команда зонтика не пополняется с помощью запятой, если логин не существует', function() {
            return this.browser
                .preparePage('creative-mode-implementers', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.implementers())
                .doubleClick(PO.creativeMode.secondTable.headerRow.implementers())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.implementers.suggest(),
                    'Саджест не появился',
                )
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.implementers.inputFocused(),
                    'Поле ввода в саджесте не получило фокус',
                )
                .yaKeyPress('dusty1dusty')
                .yaKeyPress(',')
                .yaWaitUntil('Значение исчезло из input', () => {
                    return this.browser.$(PO.creativeMode.secondTable.headerRow.implementers.inputControl())
                        .getValue()
                        .then(text => text.trim() === 'dusty1dusty');
                }, 10000);
        });
    });
});
