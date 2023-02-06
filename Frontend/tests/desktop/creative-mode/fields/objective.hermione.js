const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';
const KEYS_TO_ENTER = ['56', 'ENTER', 'new objective'];
const ADDITIONAL_VALUE = '56\nnew objective';
const SAVE_HOTKEY = 'Shift+ENTER';

describe('Поля режима креатива', function() {
    describe('objective', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 1700, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-objective', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.objective())
                .assertView(
                    'umb-objective-plain',
                    PO.creativeMode.table.headerRow.objective(),
                )
                .doubleClick(PO.creativeMode.table.headerRow.objective())
                .assertView(
                    'umb-objective-editable',
                    PO.creativeMode.table.headerRow.objective(),
                )
                .assertView(
                    'outline-objective-plain',
                    PO.creativeMode.table.contentRow.objective(),
                )
                .doubleClick(PO.creativeMode.table.contentRow.objective())
                .assertView(
                    'outline-objective-editable',
                    PO.creativeMode.table.contentRow.objective(),
                );
        });

        it('поле редактируется в зонтике', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-objective', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.objective())
                .$(PO.creativeMode.secondTable.headerRow.objective())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.headerRow.objective())
                .waitForVisible(PO.creativeMode.secondTable.headerRow.objectiveEditable())
                .yaKeyPress(KEYS_TO_ENTER)
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.objective(),
                    currentValue + ADDITIONAL_VALUE,
                    'Текст должен вводиться в input',
                )
                .yaKeyPress(SAVE_HOTKEY)
                .yaWaitForHidden(PO.creativeMode.secondTable.headerRow.objectiveEditable())
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.objective(),
                    currentValue + ADDITIONAL_VALUE,
                    'Текст должен сохраниться',
                );
        });

        it('поле редактируется в контуре', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-objective', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.objective())
                .$(PO.creativeMode.secondTable.contentRow.objective())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.contentRow.objective())
                .waitForVisible(PO.creativeMode.secondTable.contentRow.objectiveEditable())
                .yaKeyPress(KEYS_TO_ENTER)
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.objective(),
                    currentValue + ADDITIONAL_VALUE,
                    'Текст должен вводиться в input',
                )
                .yaKeyPress(SAVE_HOTKEY)
                .yaWaitForHidden(PO.creativeMode.secondTable.contentRow.objectiveEditable())
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.objective(),
                    currentValue + ADDITIONAL_VALUE,
                    'Текст должен сохраниться',
                );
        });

        it('редактирование сбрасывается по escape', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-objective', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.objective())
                .$(PO.creativeMode.secondTable.headerRow.objective())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.headerRow.objective())
                .waitForVisible(PO.creativeMode.secondTable.headerRow.objectiveEditable())
                .yaKeyPress(KEYS_TO_ENTER)
                .yaKeyPress('ESC')
                .yaWaitForHidden(PO.creativeMode.secondTable.headerRow.objectiveEditable())
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.objective(),
                    currentValue,
                    'Изменение текста должно сброситься',
                );
        });

        it('в инпуте незаполненного поля пустая строка', async function() {
            await this.browser
                .preparePage('creative-mode-objective', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.lastContentRow.objective())
                .doubleClick(PO.creativeMode.secondTable.lastContentRow.objective())
                .waitForVisible(PO.creativeMode.secondTable.lastContentRow.objectiveEditable())
                .yaAssertText(
                    PO.creativeMode.secondTable.lastContentRow.objective(),
                    '',
                    'Текст должен вводиться в input',
                );
        });
    });
});
