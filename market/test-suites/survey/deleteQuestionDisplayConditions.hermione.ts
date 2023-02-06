import 'hermione';
import {expect} from 'chai';

import {login, waitForReactRootLoaded} from '../../helpers';
import {TIMEOUT_MS, CHECK_INTERVAL, LONG_TIMEOUT_MS} from '../../constants';

const PAGE_URL = 'survey/admin/survey@216319784';
const TITLE_DISPLAY_CONDITIONS = 'Вопрос для тестирования условий показа';

/**
 * Проверяем, что для вопроса можно создать, а затем убрать условие его показа
 */
describe('ocrm-1151: Удаление условий показа вопроса', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('должно работать корректно', async function() {
        await waitForReactRootLoaded(this.browser);

        const pageHeader = await this.browser.react$('PageHeader');

        await pageHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: 'Не дождались появления заголовка',
        });

        const questionTitleItem = await this.browser.$(`div=${TITLE_DISPLAY_CONDITIONS}`);

        const questionTitleIsDisplayed = await questionTitleItem.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления вопроса',
        });

        expect(questionTitleIsDisplayed).to.equal(true, 'Нет вопроса для проверки условий отображения');

        const questionLiParent = await (await questionTitleItem.parentElement()).parentElement();

        const questionDisplayButton = await questionLiParent.$('[title="Настроить условия показа"]');

        await questionDisplayButton.isEnabled();
        await questionDisplayButton.click();

        await this.browser.react$('ModalControls').waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });

        const displayConditionButton = await this.browser.$('span=при условии');

        await displayConditionButton.isEnabled();
        await displayConditionButton.click();

        const inputSelect = this.browser.$('span=выберите');

        inputSelect.isExisting();
        inputSelect.click();

        const selectValue = await this.browser.$('[data-ow-test-select-option="Ответ на вопрос"]');

        await selectValue.isExisting();
        const selectValueParent = await selectValue.parentElement();

        await selectValueParent.click();

        const saveButton = await this.browser.react$('ModalControls').$('button=Сохранить');

        await saveButton.click();

        const displayIconIsExisting = await questionTitleItem.$('[title="Вопрос с условием показа"]').waitForExist();

        expect(displayIconIsExisting).to.equal(true, 'Рядом с вопросом не появился значок наличия условия отображения');

        await questionDisplayButton.isEnabled();
        await questionDisplayButton.click();
        const displayAlwaysButton = await this.browser.$('span=всегда');

        await displayAlwaysButton.isEnabled();
        await displayAlwaysButton.click();
        const saveButton2 = await this.browser.react$('ModalControls').$('button=Сохранить');

        await saveButton2.click();

        await questionTitleItem.$('[title="Вопрос с условием показа"]').waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Рядом с вопросом не пропал значок наличия условия отображения',
        });
    });
});
