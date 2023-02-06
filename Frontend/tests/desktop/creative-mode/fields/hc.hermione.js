const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';
const VALUE_TO_ENTER = '1';

describe('Поля режима креатива', function() {
    describe('hc', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 2500, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-hc', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.hc())
                .assertView(
                    'umb-hc-plain',
                    PO.creativeMode.table.headerRow.hc(),
                )
                .assertView(
                    'outline-hc-plain',
                    PO.creativeMode.table.contentRow.hc(),
                )
                .doubleClick(PO.creativeMode.table.contentRow.hc())
                .waitForVisible(
                    PO.creativeMode.table.contentRow.hcEditable(),
                    'Должен появиться input',
                )
                .assertView(
                    'outline-hc-editable',
                    PO.creativeMode.table.contentRow.hc(),
                );
        });

        it('headcount зонтика не редактируется', function() {
            return this.browser
                .preparePage('creative-mode-hc', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.hc())
                .doubleClick(PO.creativeMode.table.headerRow.hc())
                .yaShouldExist(
                    PO.creativeMode.table.headerRow.hcEditable(),
                    'HC в зонте нельзя редактировать',
                    false,
                );
        });

        it('поле редактируется в контуре', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-hc', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.hc())
                .$(PO.creativeMode.secondTable.contentRow.hc())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.contentRow.hc())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    'Должен появиться input',
                )
                .yaKeyPress(VALUE_TO_ENTER)
                .yaAssertValue(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен вводиться в input',
                )
                .yaKeyPress('ENTER')
                .yaWaitForHidden(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    'Input должен скрыться',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.hc(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен сохраниться',
                );
        });

        it('редактирование сбрасывается по escape', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-hc', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.hc())
                .$(PO.creativeMode.secondTable.contentRow.hc())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.contentRow.hc())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    'Должен появиться input',
                )
                .yaKeyPress(VALUE_TO_ENTER)
                .yaAssertValue(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен вводиться в input',
                )
                .yaKeyPress('ESC')
                .yaWaitForHidden(
                    PO.creativeMode.secondTable.contentRow.hcEditable(),
                    'Input должен скрыться',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.hc(),
                    currentValue,
                    'Изменение текста должно сброситься',
                );
        });
    });
});
