import 'hermione';
import {expect} from 'chai';

import {login, waitForReactRootLoaded} from '../../helpers';
import {TIMEOUT_MS, CHECK_INTERVAL, LONG_TIMEOUT_MS} from '../../constants';
import {createAnswerOption, editAnswerOption, archiveAnswerOption} from './helpers';

const PAGE_URL_OCRM_826 = 'survey/admin/survey@214358892';
const PAGE_URL_OCRM_886 = 'survey/admin/survey@214358891';
const TITLE_SEVERAL_ANSWERS = 'Тестовый вопрос с выбором нескольких вариантов ответа';
const TITLE_ONE_ANSWER = 'Тестовый вопрос с выбором одного варианта ответа';
const OPTION_TITLE = 'Тот самый вариант ответа';
const EDIT_OPTION_TEXT = ' после редактирования';

const createEditAndArchiveAnswerOption = async (context, questionTitle) => {
    await waitForReactRootLoaded(context.browser);
    const date = new Date().toLocaleString('ru');

    const pageHeader = await context.browser.react$('PageHeader');

    await pageHeader.waitForDisplayed({
        timeout: TIMEOUT_MS,
        interval: CHECK_INTERVAL,
        timeoutMsg: 'Не дождались появления заголовка',
    });

    const questionTitleItem = await context.browser.$(`div=${questionTitle}`);

    const questionTitleIsDisplayed = await questionTitleItem.waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        timeoutMsg: 'Не дождались появления вопроса',
    });

    expect(questionTitleIsDisplayed).to.equal(true, 'Нет вопроса, в который можно добавить варианты ответа');

    const optionTitleWithDate = `${OPTION_TITLE} ${date}`;

    await createAnswerOption(context, questionTitle, optionTitleWithDate);
    const optionTitle = await context.browser.$(`span=${optionTitleWithDate}`);

    const optionTitleItemIsDisplayed = await optionTitle.waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        timeoutMsg: 'Не дождались появления варианта ответа на вопрос',
    });

    expect(optionTitleItemIsDisplayed).to.equal(true, 'Вариант ответа не добавлен');

    const optionTitleAfterEdit = `${optionTitleWithDate}${EDIT_OPTION_TEXT}`;

    await editAnswerOption(context, questionTitle, optionTitleWithDate, EDIT_OPTION_TEXT);

    const questionTitleAfterEditItem = await context.browser.$(`span=${optionTitleAfterEdit}`);

    const questionTitleAfterEditIsDisplayed = await questionTitleAfterEditItem.waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        timeoutMsg: 'Не дождались появления отредактированного варианта ответа',
    });

    expect(questionTitleAfterEditIsDisplayed).to.equal(true, 'Вариант ответа не отредактирован');

    await archiveAnswerOption(context, questionTitle, optionTitleAfterEdit);

    const optionTitleAfterArchive = await context.browser.$(`span=${optionTitleAfterEdit}`);

    const optionIsArchived = await optionTitleAfterArchive.waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        reverse: true,
        timeoutMsg: 'Не дождались архивации варианта ответа',
    });

    expect(optionIsArchived).to.equal(true, 'Вариант ответа не архивирован');
};

/**
 * Проверяем, что в вопросе с выбором одного варианта ответа
 * вариант ответа успешно создаётся, редактируется и архивируется.
 */
describe('ocrm-826: Создание, изменение и архивация варианта ответа в вопросах с одним вариантом', () => {
    beforeEach(function() {
        return login(PAGE_URL_OCRM_826, this);
    });

    it('должно работать корректно', async function() {
        await createEditAndArchiveAnswerOption(this, TITLE_ONE_ANSWER);
    });
});

/**
 * Проверяем, что в вопросе с выбором нескольких вариантов
 * ответа вариант ответа успешно создаётся, редактируется и архивируется.
 */
describe('ocrm-886: Создание, изменение и архивация варианта ответа в вопросах с несколькими вариантами', () => {
    beforeEach(function() {
        return login(PAGE_URL_OCRM_886, this);
    });

    it('должно работать корректно', async function() {
        await createEditAndArchiveAnswerOption(this, TITLE_SEVERAL_ANSWERS);
    });
});
