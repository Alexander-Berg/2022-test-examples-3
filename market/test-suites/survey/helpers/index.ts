/* eslint-disable no-await-in-loop */

import ContentWithLabel from '../../../page-objects/contentWithLabel';
import {CHECK_INTERVAL, LONG_TIMEOUT_MS, TIMEOUT_MS} from '../../../constants';

/** Создаёт в опросе новый вопрос типа type и с названием title */
export const createQuestion = async (context, type, title): Promise<void> => {
    const titleInput = new ContentWithLabel(
        context.browser,
        'body',
        '[data-ow-test-properties-list-attribute="title"]'
    );
    const addQuestion = await context.browser.$('button=Добавить вопрос');

    await addQuestion.isExisting();
    await addQuestion.click();

    const questionTypeMenuItem = await context.browser.$(`//span[text()="${type}"]`);

    await questionTypeMenuItem.waitForDisplayed({
        timeout: TIMEOUT_MS,
        interval: CHECK_INTERVAL,
        timeoutMsg: `Не дождались появления кнопки создания вопроса типа ${type}`,
    });
    await questionTypeMenuItem.click();
    await context.browser.react$('ModalControls').waitForDisplayed();

    await titleInput.isDisplayed();
    await titleInput.setValue(title);
    const confirmButton = await context.browser.react$('ModalControls').$('button=Создать');

    await confirmButton.click();
};

/** Создаёт в вопросе с названием questionTitle новый вариант ответа c названием optionTitle */
export const createAnswerOption = async (context, questionTitle, optionTitle): Promise<void> => {
    const titleInput = new ContentWithLabel(
        context.browser,
        'body',
        '[data-ow-test-properties-list-attribute="title"]'
    );

    const questionTitleItem = await context.browser.$(`//div[contains(., "${questionTitle}")]`);
    const questionParent = await questionTitleItem.parentElement();

    const addQuestionAnswerOption = await questionParent.$('button=Добавить вариант ответа');

    await addQuestionAnswerOption.isExisting();
    await addQuestionAnswerOption.click();

    await context.browser.react$('ModalControls').waitForDisplayed();

    await titleInput.isDisplayed();
    await titleInput.setValue(optionTitle);
    const confirmButton = await context.browser.react$('ModalControls').$('button=Создать');

    await confirmButton.click();
};

/** Редактирует вопрос с названием title в опросе – добавляет к названию editText */
export const editQuestion = async (context, title, editText): Promise<void> => {
    const questionTitle = await context.browser.$(`//div[contains(., "${title}")]`);
    const questionLiParent = await (await questionTitle.parentElement()).parentElement();
    const titleInput = new ContentWithLabel(
        context.browser,
        'body',
        '[data-ow-test-properties-list-attribute="title"]'
    );
    const questionEditButton = await questionLiParent.$('[title="Редактировать"]');

    await questionEditButton.isEnabled();
    await questionEditButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
    });

    await titleInput.isDisplayed();
    await titleInput.setValue(editText);

    const saveButton = await context.browser.react$('ModalControls').$('button=Сохранить');

    await saveButton.click();
};

/** Редактирует вариант ответа с названием optionTitle в вопросе с названием questionTitle – добавляет к названию editText */
export const editAnswerOption = async (context, questionTitle, optionTitle, editText): Promise<void> => {
    const titleInput = new ContentWithLabel(
        context.browser,
        'body',
        '[data-ow-test-properties-list-attribute="title"]'
    );
    const questionTitleItem = await context.browser.$(`//div[contains(., "${questionTitle}")]`);
    const questionParent = await questionTitleItem.parentElement();
    const optionTitleItem = await questionParent.$(`//span[contains(., "${optionTitle}")]`);
    const optionParent = await optionTitleItem.parentElement();
    const answerOptionEditButton = await optionParent.$('[title="Редактировать"]');

    await answerOptionEditButton.isEnabled();
    await answerOptionEditButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
    });

    await titleInput.isDisplayed();
    await titleInput.setValue(editText);

    const saveButton = await context.browser.react$('ModalControls').$('button=Сохранить');

    await saveButton.click();
};

/** Архивирует вопрос с названием title в опросе */
export const archiveQuestion = async (context, title) => {
    const questionTitle = await context.browser.$(`//div[contains(., "${title}")]`);
    const questionLiParent = await (await questionTitle.parentElement()).parentElement();

    const questionArchiveButton = await questionLiParent.$('[title="Архивировать"]');

    await questionArchiveButton.isEnabled();
    await questionArchiveButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed();

    const sureButton = context.browser.react$('ModalControls').$('button=Да');

    await sureButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        reverse: true,
        timeoutMsg: 'Модалка подтверждения архивации не закрылась',
    });
};

/** Архивирует вариант ответа с названием optionTitle вопроса с названием questionTitle */
export const archiveAnswerOption = async (context, questionTitle, optionTitle): Promise<void> => {
    const questionTitleItem = await context.browser.$(`//div[contains(., "${questionTitle}")]`);
    const questionParent = await questionTitleItem.parentElement();
    const optionTitleItem = await questionParent.$(`//span[contains(., "${optionTitle}")]`);
    const optionParent = await optionTitleItem.parentElement();
    const answerOptionArchiveButton = await optionParent.$('[title="Архивировать"]');

    await answerOptionArchiveButton.isEnabled();
    await answerOptionArchiveButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
    });

    const sureButton = context.browser.react$('ModalControls').$('button=Да');

    await sureButton.click();

    await context.browser.react$('ModalControls').waitForDisplayed({
        timeout: LONG_TIMEOUT_MS,
        reverse: true,
        timeoutMsg: 'Модалка подтверждения архивации не закрылась',
    });
};
