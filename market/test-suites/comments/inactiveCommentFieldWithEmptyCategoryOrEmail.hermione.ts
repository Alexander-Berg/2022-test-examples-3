import 'hermione';

import {expect} from 'chai';

import {login, execScript} from '../../helpers';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import AttributePopup from '../../page-objects/attributePopup';
import Button from '../../page-objects/button';

const PAGE_URL = '/entity/ticket@161574441/edit';
const DELETE_CATEGORY_AND_ADD_EMAIL_SCRIPT = `
    api.bcp.edit('ticket@161574441',
    ['categories': [],
    'clientEmail': 'test'])
    `;
const DELETE_EMAIL_AND_ADD_CATEGORY_SCRIPT = `
    def categories = api.db.of('ticketCategory').withFilters{
    eq('brand', 'b2bBeru')
    eq('archived', false)
    }.limit(1).get()
    api.bcp.edit('ticket@161574441',
    ['categories': [categories],
    'clientEmail': ''])
    `;
const DISABLED_COMMENT_FIELD_SELECTOR = '[data-ow-test-disabled-comment-field]';
const DISABLED_PUBLIC_COMMENT_FIELD_SELECTOR = '[data-ow-test-disabled-public-comment-field]';
const CATEGORIES_SELECTOR = '[data-ow-test-attribute-container="categories"]';
const POPUP_SELECTOR = '[data-ow-test-popup]';
const CHECKBOX_SELECTOR = '[data-ow-test-checkbox]';
const CLIENT_EMAIL_SELECTOR = '[data-ow-test-attribute-container="clientEmail"]';
const SAVE_BUTTON_SELECTOR = '[data-ow-test-jmf-card-toolbar-action="save-сохранить"]';

/**
 * План теста:
 * 1. Очистить категорию и проставить email у существующего обращения b2b
 * 2. Перейти на страницу редактирования этого обращения
 * 3. Проверить, что поле для ввода комментария неактивно
 * 4. Добавить в обращение любую категорию
 * 5. Проверить, что поле для ввода комментария активно
 */

describe('ocrm-1164: Блокировка поля ввода у обращений b2b без категории', () => {
    beforeEach(function() {
        return login('/', this);
    });

    it('проверяет, что при отсутствии категории поле для ввода комментария неактивно', async function() {
        const disabledCommentField = new ContentWithLabel(this.browser, 'body', DISABLED_COMMENT_FIELD_SELECTOR);
        const categories = new ContentWithLabel(this.browser, 'body', CATEGORIES_SELECTOR);
        const popupCategories = new AttributePopup(this.browser, 'body', POPUP_SELECTOR);
        const firstElement = new ContentWithLabel(this.browser, POPUP_SELECTOR, CHECKBOX_SELECTOR);
        const saveButton = new Button(this.browser, 'body', SAVE_BUTTON_SELECTOR);

        await execScript(this.browser, DELETE_CATEGORY_AND_ADD_EMAIL_SCRIPT);

        await this.browser.url(PAGE_URL);

        await disabledCommentField.isDisplayed('Поле для ввода комментария не заблокировано при отсутствии категории');

        await categories.isDisplayed();
        await categories.setValue('');
        await popupCategories.isDisplayed();
        await firstElement.click();

        await disabledCommentField.isNotDisplayed('Поле ввода заблокировано после выбора категории');

        await saveButton.isEnabled();
        await saveButton.clickButton();

        const isSuccessfully = await saveButton.waitForInvisible();

        expect(isSuccessfully).to.equal(true, 'Произошла ошибка при сохранении обращения');
    });
});

/**
 * План теста:
 * 1. Очистить email и проставить категорию у существующего обращения b2b
 * 2. Перейти на страницу редактирования этого обращения
 * 3. Проверить, что поле для ввода комментария неактивно
 * 4. Добавить в обращение контактный email
 * 5. Проверить, что поле для ввода комментария активно
 */

describe('ocrm-1402: Поле для ввода внешнего письма блокируется, если не указан контактный e-mail', () => {
    beforeEach(async function() {
        return login('/', this);
    });

    it('проверяет, что при отсутствии email и наличиии категории поле для ввода внешнего комментария неактивно', async function() {
        const disabledPublicCommentField = new ContentWithLabel(
            this.browser,
            'body',
            DISABLED_PUBLIC_COMMENT_FIELD_SELECTOR
        );
        const clientEmail = new ContentWithLabel(this.browser, 'body', CLIENT_EMAIL_SELECTOR);
        const saveButton = new Button(this.browser, 'body', SAVE_BUTTON_SELECTOR);

        await execScript(this.browser, DELETE_EMAIL_AND_ADD_CATEGORY_SCRIPT);

        await this.browser.url(PAGE_URL);

        await disabledPublicCommentField.isDisplayed(
            'Поле для ввода комментария не заблокировано при отсутствии email'
        );

        await clientEmail.isDisplayed();
        await clientEmail.setValue('test');

        await disabledPublicCommentField.isNotDisplayed('Поле ввода заблокировано после указания email');

        await saveButton.isEnabled();
        await saveButton.clickButton();

        const isSuccessfully = await saveButton.waitForInvisible();

        expect(isSuccessfully).to.equal(true, 'Произошла ошибка при сохранении обращения');
    });
});
