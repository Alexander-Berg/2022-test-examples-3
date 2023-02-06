/* eslint-disable no-await-in-loop */

import {expect} from 'chai';
import {alertIsPresent} from 'wdio-wait-for';

import {login, waitForReactRootLoaded} from '../../helpers';
import {LONG_TIMEOUT_MS, TIMEOUT_MS} from '../../constants';

const PAGE_URL = '/metadata/testhttp2';
const date = new Date().toLocaleString('ru').replace(' ', '_');
const METACLASS_NAME = 'ocrm-1637';
const ATTRIBUTE_CODE = `test${date}`;
const ATTRIBUTE_NAME = 'Тестовый атрибут';
const ATTRIBUTE_NAME_EDIT = ' после редактирования';
const ATTRIBUTE_NEW_NAME = `${ATTRIBUTE_NAME}${ATTRIBUTE_NAME_EDIT}`;

/**
 * Проверяет, что в http-метаклассах атрибуты успешно создаются,
 * редактируются и удаляются.
 * TODO: если баг из тикета OCRM-8900 исправят, нужно убрать логику с постоянными обновлениями страницы
 */
describe('ocrm-1637: Создание атрибута', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Добавляет новый атрибут в метаклассе', async function() {
        hermione.skip.in(['yandex-browser', 'chrome'], 'После выполнения OCRM-8900 убрать skip');
        await waitForReactRootLoaded(this.browser);

        const addAttribute = await this.browser.$('span=Добавить атрибут');

        await addAttribute.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });
        await addAttribute.click();

        const modal = await this.browser.$('[data-ow-test-modal-body]');
        const modalIsDisplayed = await modal.isDisplayed();

        expect(modalIsDisplayed).to.equal(true, 'Модальное окно добавления атрибута не открылось');

        const code = await this.browser.$('span=Код атрибута');

        await code.isDisplayed();
        const codeParent = await (await code.parentElement()).parentElement();
        const codeInput = await codeParent.$('input');

        await codeInput.isDisplayed();
        await codeInput.click();
        await codeInput.addValue(ATTRIBUTE_CODE);

        const name = await this.browser.$('span=Название');

        await name.isDisplayed();
        const nameParent = await name.parentElement();
        const nameInput = await nameParent.$('input');

        await nameInput.isDisplayed();
        await nameInput.click();
        await nameInput.addValue(ATTRIBUTE_NAME);

        const saveButton = await this.browser.$('span=Сохранить');

        await saveButton.isDisplayed();
        await saveButton.click();
        const modal2 = await this.browser.$('[data-ow-test-modal-body]');

        await modal2.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Модальное окно добавления атрибута не закрылось',
        });

        const attributeName = await this.browser.$(`div=${ATTRIBUTE_NAME}`);

        const attributeIsDisplayed = await attributeName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });

        if (!attributeIsDisplayed) this.browser.refresh();

        await attributeName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Атрибут не добавлен',
        });
    });
});

describe('ocrm-1638: Редактирование атрибута', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Редактирует атрибут метакласса', async function() {
        hermione.skip.in(['yandex-browser', 'chrome'], 'После выполнения OCRM-8900 убрать skip');

        await this.browser.refresh();
        await this.browser.refresh();
        await waitForReactRootLoaded(this.browser);

        const editButton = await this.browser.react$('EditAttribute', {props: {code: ATTRIBUTE_CODE}});

        const attributeIsDisplayed = await editButton.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });

        if (!attributeIsDisplayed) {
            for (let i = 0; i < 5; i++) {
                await this.browser.refresh();
                await waitForReactRootLoaded(this.browser);
                await this.browser.$('div=ocrm-1637').waitForDisplayed();
                const editButtonAfterRefresh = await this.browser.react$('EditAttribute', {
                    props: {code: ATTRIBUTE_CODE},
                });

                const attributeIsDisplayedAfterRefresh = await editButtonAfterRefresh.waitForDisplayed({
                    timeout: TIMEOUT_MS,
                });

                if (attributeIsDisplayedAfterRefresh) break;
            }
        }

        const editButtons = await this.browser.react$$('EditAttribute', {props: {code: ATTRIBUTE_CODE}});

        const attributeIsDisplayedAfterLoop = editButtons.length > 0;

        expect(attributeIsDisplayedAfterLoop).to.equal(true, 'Нет необходимого для редактирования атрибута');

        await editButtons[0].click();

        const modal = await this.browser.$('[data-ow-test-modal-body]');
        const modalIsDisplayed = await modal.isDisplayed();

        expect(modalIsDisplayed).to.equal(true, 'Модальное окно добавления атрибута не открылось');

        const name = await this.browser.$('span=Название');

        await name.isDisplayed();
        const nameParent = await name.parentElement();
        const nameInput = await nameParent.$('input');

        await nameInput.isDisplayed();
        await nameInput.click();
        await nameInput.addValue(ATTRIBUTE_NAME_EDIT);

        const saveButton = await this.browser.$('span=Сохранить');

        await saveButton.isDisplayed();
        await saveButton.click();

        const isSaved = await modal.waitForDisplayed({
            timeout: TIMEOUT_MS,
            reverse: true,
        });

        if (!isSaved) {
            await saveButton.click();
        }

        await modal.waitForDisplayed({
            timeout: TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Модальное окно редактирования атрибута не закрылось',
        });

        const attributeNewName = await this.browser.$(`div=${ATTRIBUTE_NEW_NAME}`);

        const attributeNewNameIsDisplayed = await attributeNewName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });

        if (!attributeNewNameIsDisplayed) await this.browser.refresh();

        await attributeNewName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Название атрибута не изменилось',
        });
    });
});

describe('ocrm-1639: Удаление атрибута', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Удаляет атрибут метакласса', async function() {
        hermione.skip.in(['yandex-browser', 'chrome'], 'После выполнения OCRM-8900 убрать skip');

        await this.browser.refresh();

        await waitForReactRootLoaded(this.browser);

        const deleteButton = await this.browser.react$('DeleteAttribute', {props: {code: ATTRIBUTE_CODE}});

        const attributeIsDisplayed = await deleteButton.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
        });

        if (!attributeIsDisplayed) {
            for (let i = 0; i < 5; i++) {
                await this.browser.refresh();
                await waitForReactRootLoaded(this.browser);
                await this.browser.$(`div=${METACLASS_NAME}`).waitForDisplayed();
                const deleteButtonAfterRefresh = await this.browser.react$('DeleteAttribute', {
                    props: {code: ATTRIBUTE_CODE},
                });

                const attributeIsDisplayedAfterRefresh = await deleteButtonAfterRefresh.waitForDisplayed({
                    timeout: TIMEOUT_MS,
                });

                if (attributeIsDisplayedAfterRefresh) break;
            }
        }

        const deleteButtonAfterLoopIsDisplayed = await this.browser
            .react$('DeleteAttribute', {
                props: {code: ATTRIBUTE_CODE},
            })
            .waitForDisplayed();

        const deleteButtons = await this.browser.react$$('DeleteAttribute', {props: {code: ATTRIBUTE_CODE}});

        const attributeIsDisplayedAfterLoop = deleteButtonAfterLoopIsDisplayed && deleteButtons.length > 0;

        expect(attributeIsDisplayedAfterLoop).to.equal(true, 'Нет необходимого для удаления атрибута');

        await deleteButtons[0].click();

        await this.browser.waitUntil(alertIsPresent(), {
            timeoutMsg: 'Не дождались окна подтверждения удаления атрибута',
        });
        await this.browser.acceptAlert();

        const attributeNewName = await this.browser.$(`div=${ATTRIBUTE_NEW_NAME}`);

        const attributeNewNameIsNotDisplayed = await attributeNewName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
        });

        if (!attributeNewNameIsNotDisplayed) await this.browser.refresh();

        await attributeNewName.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            reverse: true,
            timeoutMsg: 'Атрибут не удалён',
        });
    });
});
