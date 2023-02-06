import 'hermione';
import {expect} from 'chai';

import {login, waitForReactRootLoaded} from '../../helpers';
import {TIMEOUT_MS, CHECK_INTERVAL, LONG_TIMEOUT_MS} from '../../constants';
import {archiveQuestion, createQuestion, editQuestion} from './helpers';

const PAGE_URL_OCRM_885 = 'survey/admin/survey@214345984';
const PAGE_URL_OCRM_825 = 'survey/admin/survey@214358887';
const PAGE_URL_OCRM_824 = 'survey/admin/survey@214358890';
const TITLE_SEVERAL_ANSWERS = 'Тестовый вопрос с выбором нескольких вариантов ответа';
const TITLE_ONE_ANSWER = 'Тестовый вопрос с выбором одного варианта ответа';
const TITLE_INPUT_FORM = 'Тестовый вопрос с формой ввода';
const EDIT_TITLE_TEXT = ' после редактирования';

/**
 * Проверяем, что в опросе вопрос с выбором нескольких вариантов
 * ответа успешно создаётся, редактируется и архивируется.
 */
describe('ocrm-885: Создание, изменение и архивация вопроса с выбором нескольких ответов', () => {
    beforeEach(function() {
        return login(PAGE_URL_OCRM_885, this);
    });

    it('должно работать корректно', async function() {
        await waitForReactRootLoaded(this.browser);
        const date = new Date().toLocaleString('ru');

        const pageHeader = await this.browser.react$('PageHeader');

        await pageHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: 'Не дождались появления заголовка',
        });

        const titleWithDate = `${TITLE_SEVERAL_ANSWERS} ${date}`;

        await createQuestion(this, 'Вопрос с выбором нескольких вариантов', titleWithDate);

        const questionTitle = await this.browser.$(`//div[contains(., "${titleWithDate}")]`);

        const questionTitleIsDisplayed = await questionTitle.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления вопроса',
        });

        expect(questionTitleIsDisplayed).to.equal(true, 'Вопрос не создан');

        await editQuestion(this, titleWithDate, EDIT_TITLE_TEXT);

        const titleAfterEdit = `${titleWithDate}${EDIT_TITLE_TEXT}`;

        const questionTitleAfterEdit = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        const questionTitleAfterEditIsDisplayed = await questionTitleAfterEdit.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления отредактированного вопроса',
        });

        expect(questionTitleAfterEditIsDisplayed).to.equal(true, 'Вопрос не отредактирован');

        await archiveQuestion(this, titleAfterEdit);

        const questionTitleAfterArchive = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        const questionIsArchived = await questionTitleAfterArchive.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Не дождались архивации вопроса',
        });

        expect(questionIsArchived).to.equal(true, 'Вопрос не архивирован');
    });
});

/**
 * Проверяем, что в опросе вопрос с выбором одного варианта ответа
 * успешно создаётся, редактируется и архивируется.
 */
describe('ocrm-825: Создание, изменение и архивация вопроса с выбором одного ответа', () => {
    beforeEach(function() {
        return login(PAGE_URL_OCRM_825, this);
    });

    it('должно работать корректно', async function() {
        await waitForReactRootLoaded(this.browser);
        const date = new Date().toLocaleString('ru');

        const pageHeader = await this.browser.react$('PageHeader');

        await pageHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: 'Не дождались появления заголовка',
        });

        const titleWithDate = `${TITLE_ONE_ANSWER} ${date}`;

        await createQuestion(this, 'Вопрос с выбором одного варианта', titleWithDate);

        const questionTitle = await this.browser.$(`//div[contains(., "${titleWithDate}")]`);

        const questionTitleIsDisplayed = await questionTitle.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления вопроса',
        });

        expect(questionTitleIsDisplayed).to.equal(true, 'Вопрос не создан');

        await editQuestion(this, titleWithDate, EDIT_TITLE_TEXT);

        const titleAfterEdit = `${titleWithDate}${EDIT_TITLE_TEXT}`;

        const questionTitleAfterEdit = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        const questionTitleAfterEditIsDisplayed = await questionTitleAfterEdit.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления отредактированного вопроса',
        });

        expect(questionTitleAfterEditIsDisplayed).to.equal(true, 'Вопрос не отредактирован');

        await archiveQuestion(this, titleAfterEdit);

        const questionTitleAfterArchive = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        const questionIsArchived = await questionTitleAfterArchive.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Не дождались архивации вопроса',
        });

        expect(questionIsArchived).to.equal(true, 'Вопрос не архивирован');
    });
});

/**
 * Проверяем, что в опросе вопрос с формой ввода успешно создаётся и редактируется.
 * Архивируем после проверки.
 */
describe('ocrm-824: Создание и изменение вопроса с формой ввода', () => {
    beforeEach(function() {
        return login(PAGE_URL_OCRM_824, this);
    });

    it('должно работать корректно', async function() {
        await waitForReactRootLoaded(this.browser);
        const date = new Date().toLocaleString('ru');

        const pageHeader = await this.browser.react$('PageHeader');

        await pageHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            interval: CHECK_INTERVAL,
            timeoutMsg: 'Не дождались появления заголовка',
        });

        const titleWithDate = `${TITLE_INPUT_FORM} ${date}`;

        await createQuestion(this, 'Вопрос с формой ввода', titleWithDate);

        const questionTitle = await this.browser.$(`//div[contains(., "${titleWithDate}")]`);

        const questionTitleIsDisplayed = await questionTitle.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления вопроса',
        });

        expect(questionTitleIsDisplayed).to.equal(true, 'Вопрос не создан');

        await editQuestion(this, titleWithDate, EDIT_TITLE_TEXT);

        const titleAfterEdit = `${titleWithDate}${EDIT_TITLE_TEXT}`;

        const questionTitleAfterEdit = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        const questionTitleAfterEditIsDisplayed = await questionTitleAfterEdit.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления отредактированного вопроса',
        });

        expect(questionTitleAfterEditIsDisplayed).to.equal(true, 'Вопрос не отредактирован');

        await archiveQuestion(this, titleAfterEdit);

        const questionTitleAfterArchive = await this.browser.$(`//div[contains(., "${titleAfterEdit}")]`);

        await questionTitleAfterArchive.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Не дождались архивации вопроса',
        });
    });
});
