const PO = require('../../../../page-objects');
const START_URL_RESOLUTION = '/filter?goal=39455';
const START_URL_POPUP_VS = '/filter?goal=33900';
const START_URL_POPUP_UMB = '/filter?goal=57408';
const START_URL_POPUP_OUTLINE = '/filter?goal=57433';

const vs_descr_text = 'описание\nраз\nдва \nтри\n\n**Сценарий:**\n90\n\n**HeadCount:**\n123\n\n**Чего хотим добиться ' +
    '(Objective):**\nbla\n\n**Как определим успешность (Key-Result):**\n* key result 1\n* key result 2';
const umb_descr_text = 'описание могло быть тут ваще\n\n**Сценарий:**\n90\n\n**HeadCount:**\n5\n\n**Чего хотим ' +
    'добиться (Objective):**\nхотим\n\n**Как определим успешность (Key-Result):**\n* не влезло\n* не фартануло' +
    '\n* не зашло';
const outline_descr_text = '**Сценарий:**\n120\n\n**Как определим успешность (Key-Result):**\n* влаодыова\n* ' +
    'длывоалдоыва';

const vs_descr_text_default = 'описание\nраз\nдва \nтри';
const umb_descr_text_default = 'описание могло быть тут ваще';
const outline_descr_text_default = '';

const popupParts = [
    { name: 'header', selector: PO.goalEditForm.typeSelectPopup.Header() },
    { name: 'section', selector: PO.goalEditForm.typeSelectPopup.Section() },
    { name: 'buttons', selector: PO.goalEditForm.typeSelectPopup.Buttons() },
];

describe('Редактирование', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.loginToGoals();
    });

    it('Внешний вид резолюции', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-with-resolution', START_URL_RESOLUTION);
        await browser.waitForVisible(PO.goal.info());
        await browser.moveToObject(PO.goal.info(), 10, 10);
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.status());
        await browser.yaSelectChooseItem(PO.goalEditForm.status(), 'Достигнута');
        await browser.waitForVisible(PO.goalEditForm.resolutionFormField());
        await browser.assertView('edit-form-resolution', PO.goalEditForm.resolutionFormField());
    });

    // VALUE STREAM
    it('Всплывающее окно при смене типа цели с value stream', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-vs', START_URL_POPUP_VS);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());

        for (const part of popupParts) {
            await browser.assertView(`type-select-popup-${part.name}-vs`, part.selector);
        }
    });

    it('Перенос данных при смене типа цели с value stream', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-vs', START_URL_POPUP_VS);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.confirmButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), vs_descr_text,
            'Wrong description text after switching from VS to Goal');
    });

    it('Отмена переноса данных при смене типа цели с value stream', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-vs', START_URL_POPUP_VS);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.cancelButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), vs_descr_text_default,
            'Wrong description text after canceling the switching from VS to Goal');
    });

    // UMBRELLA
    it('Всплывающее окно при смене типа цели с зонта', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-umb', START_URL_POPUP_UMB);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());

        for (const part of popupParts) {
            await browser.assertView(`type-select-popup-${part.name}-umb`, part.selector);
        }
    });

    it('Перенос данных при смене типа цели с зонта', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-umb', START_URL_POPUP_UMB);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.confirmButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), umb_descr_text,
            'Wrong description text after switching from Umbrella to Goal');
    });

    it('Отмена переноса данных при смене типа цели с зонта', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-umb', START_URL_POPUP_UMB);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.cancelButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), umb_descr_text_default,
            'Wrong description text after canceling the switching from Umbrella to Goal');
    });

    // OUTLINE
    it('Всплывающее окно при смене типа цели с контура', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-outline', START_URL_POPUP_OUTLINE);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());

        for (const part of popupParts) {
            await browser.assertView(`type-select-popup-${part.name}-outline`, part.selector);
        }
    });

    it('Перенос данных при смене типа цели с контура', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-outline', START_URL_POPUP_OUTLINE);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.confirmButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), outline_descr_text,
            'Wrong description text after switching from Outline to Goal');
    });

    it('Отмена переноса данных при смене типа цели с контура', async function() {
        const browser = this.browser;

        await browser.preparePage('selected-goal-type-outline', START_URL_POPUP_OUTLINE);
        await browser.waitForVisible(PO.goal.info.editButton());
        await browser.click(PO.goal.info.editButton());
        await browser.waitForVisible(PO.goalEditForm.dropdownMenuLink());
        await browser.yaTypeSelectMenuChooseItem(1);
        await browser.waitForVisible(PO.goalEditForm.typeSelectPopup());
        await browser.click(PO.goalEditForm.typeSelectPopup.cancelButton());
        await browser.yaWaitForHidden(PO.goalEditForm.typeSelectPopup());
        await browser.yaAssertText(PO.goalEditForm.descriptionFormField.control(), outline_descr_text_default,
            'Wrong description text after canceling the switching from Outline to Goal');
    });
});
