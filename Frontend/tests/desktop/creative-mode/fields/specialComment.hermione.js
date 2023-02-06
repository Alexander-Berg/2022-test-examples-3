const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/57452';
const VALUE_TO_ENTER = '1';

describe('Поля режима креатива', function() {
    describe('specialComment', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 3000, height: 2000 })
                .loginToGoals();
        });

        it('внешний вид', function() {
            return this.browser
                .preparePage('creative-mode-specialComment', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.specialComment())
                .assertView(
                    'umb-specialComment-plain',
                    PO.creativeMode.table.headerRow.specialComment(),
                )
                .doubleClick(PO.creativeMode.table.headerRow.specialComment())
                .waitForVisible(
                    PO.creativeMode.table.headerRow.specialCommentEditable(),
                    'Должна появиться textarea',
                )
                .assertView(
                    'umb-specialComment-editable',
                    PO.creativeMode.table.headerRow.specialComment(),
                )
                .assertView(
                    'outline-specialComment-plain',
                    PO.creativeMode.table.contentRow.specialComment(),
                )
                .doubleClick(PO.creativeMode.table.contentRow.specialComment())
                .waitForVisible(
                    PO.creativeMode.table.contentRow.specialCommentEditable(),
                    'Должна появиться textarea',
                )
                .assertView(
                    'outline-specialComment-editable',
                    PO.creativeMode.table.contentRow.specialComment(),
                );
        });

        it('поле редактируется в зонтике', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-specialComment', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.specialComment())
                .$(PO.creativeMode.secondTable.headerRow.specialComment())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.headerRow.specialComment())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.specialCommentEditable(),
                    'Должна появиться textarea',
                )
                .yaKeyPress(VALUE_TO_ENTER)
                .yaAssertValue(
                    PO.creativeMode.secondTable.headerRow.specialCommentEditable(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен вводиться в textarea',
                )
                .yaKeyPress('Shift+ENTER')
                .yaWaitForHidden(
                    PO.creativeMode.secondTable.headerRow.specialCommentEditable(),
                    'Textarea должен скрыться',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.specialComment(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен сохраниться',
                );
        });

        it('поле редактируется в контуре', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-specialComment', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.contentRow.specialComment())
                .$(PO.creativeMode.secondTable.contentRow.specialComment())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.contentRow.specialComment())
                .waitForVisible(
                    PO.creativeMode.secondTable.contentRow.specialCommentEditable(),
                    'Должна появиться textarea',
                )
                .yaKeyPress(VALUE_TO_ENTER)
                .yaAssertValue(
                    PO.creativeMode.secondTable.contentRow.specialCommentEditable(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен вводиться в textarea',
                )
                .yaKeyPress('Shift+ENTER')
                .yaWaitForHidden(
                    PO.creativeMode.secondTable.contentRow.specialCommentEditable(),
                    'Textarea должен скрыться',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.contentRow.specialComment(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен сохраниться',
                );
        });

        it('редактирование сбрасывается по escape', async function() {
            const currentValue = await this.browser
                .preparePage('creative-mode-specialComment', START_URL)
                .waitForVisible(PO.creativeMode.secondTable.headerRow.specialComment())
                .$(PO.creativeMode.secondTable.headerRow.specialComment())
                .getText();

            await this.browser
                .doubleClick(PO.creativeMode.secondTable.headerRow.specialComment())
                .waitForVisible(
                    PO.creativeMode.secondTable.headerRow.specialCommentEditable(),
                    'Должна появиться textarea',
                )
                .yaKeyPress(VALUE_TO_ENTER)
                .yaAssertValue(
                    PO.creativeMode.secondTable.headerRow.specialCommentEditable(),
                    currentValue + VALUE_TO_ENTER,
                    'Текст должен вводиться в textarea',
                )
                .yaKeyPress('ESC')
                .yaWaitForHidden(
                    PO.creativeMode.secondTable.contentRow.specialCommentEditable(),
                    'Textarea должен скрыться',
                )
                .yaAssertText(
                    PO.creativeMode.secondTable.headerRow.specialComment(),
                    currentValue,
                    'Изменение текста должно сброситься',
                );
        });
    });
});
