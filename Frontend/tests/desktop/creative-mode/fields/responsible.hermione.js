const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';
const ORIGINAL_VALUE = 'Александр Шлейко';
const VALUE_TO_ENTER = {
    value: 'annvas',
    item: 'Анна Стаховская (Васильева) (@annvas)',
    text: 'Анна Стаховская (Васильева)',
};

describe('Поля режима креатива', function() {
    describe('responsible', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 2500, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.responsible())
                .assertView('umb-responsible-plain', PO.creativeMode.table.headerRow.responsible())
                .doubleClick(PO.creativeMode.table.headerRow.responsible())
                .waitForVisible(
                    PO.creativeMode.table.headerRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .assertView(
                    'umb-responsible-editable',
                    PO.creativeMode.table.headerRow.responsible(),
                )
                .assertView(
                    'outline-responsible-plain',
                    PO.creativeMode.table.contentRow.responsible(),
                )
                .doubleClick(PO.creativeMode.table.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.table.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .assertView(
                    'outline-responsible-editable',
                    PO.creativeMode.table.contentRow.responsible(),
                );
        });

        it('поле редактируется в зонтике', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.headerRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.headerRow.responsible.suggest())
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.headerRow.responsible.inputFocused(),
                    PO.creativeMode.secondTable.headerRow.responsible.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.responsible(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .click(PO.creativeMode.secondTable.headerRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.headerRow.responsible.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.headerRow.responsible())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('поле редактируется в контуре', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.contentRow.responsible.suggest())
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.contentRow.responsible.inputFocused(),
                    PO.creativeMode.secondTable.contentRow.responsible.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.responsible(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .click(PO.creativeMode.secondTable.contentRow.hc())
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.responsible())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('редактирование сбрасывается по escape', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.contentRow.responsible.suggest())
                .yaSuggestChooseItem(
                    PO.creativeMode.secondTable.contentRow.responsible.inputFocused(),
                    PO.creativeMode.secondTable.contentRow.responsible.suggestPopup(),
                    VALUE_TO_ENTER.value,
                    VALUE_TO_ENTER.item,
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.responsible(),
                    VALUE_TO_ENTER.item,
                    'Значение должно выбраться в саджесте',
                )
                .yaKeyPress(['ESC'])
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не скрылся',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.responsible(),
                    ORIGINAL_VALUE,
                    'Значение не должно сохраниться',
                );
        });

        it('значение добавляется по пробелу', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.contentRow.responsible.suggest())
                .yaKeyPress(VALUE_TO_ENTER.value)
                .yaKeyPress(' ')
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.responsible())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('значение добавляется по запятой', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.contentRow.responsible.suggest())
                .yaKeyPress('@' + VALUE_TO_ENTER.value)
                .yaKeyPress(',')
                .waitForHidden(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не скрылся',
                )
                .yaWaitUntil('Значение должно сохраниться', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.responsible())
                        .getText()
                        .then(text => text.trim() === VALUE_TO_ENTER.text);
                }, 10000);
        });

        it('ответственный зонтика не пополняется с помощью запятой, если логин не существует', function() {
            return this.browser
                .preparePage('creative-mode-responsible', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.responsible())
                .doubleClick(PO.creativeMode.secondTable.contentRow.responsible())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.responsible.suggest(),
                    'Саджест не появился',
                )
                .click(PO.creativeMode.secondTable.contentRow.responsible.suggest())
                .yaKeyPress('dusty1dusty')
                .yaKeyPress(',')
                .yaWaitUntil('Должен остаться несуществующий логин', () => {
                    return this.browser.$(PO.creativeMode.secondTable.contentRow.responsible.inputControl())
                        .getValue()
                        .then(text => text.trim() === 'dusty1dusty,');
                }, 10000);
        });
    });
});
