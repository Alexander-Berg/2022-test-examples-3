import 'hermione';
import {expect} from 'chai';
import url from 'url';

import {login, pause, waitForReactRootLoaded} from '../../helpers';
import Button from '../../page-objects/button';
import {SELECT_ALL_SEQUENCE} from '../../constants';
import PopperNew from '../../page-objects/popperNew';
import {CHAT_TABS_SELECTOR} from '../../page-objects/commentsContainer';

const PAGE_URL = '/entity/ticket@209632820';

const addLinkToCommentText = async (browser, editor): Promise<void> => {
    const addLinkButton = await browser.$('button[title="Добавить ссылку"]');
    const urlInput = await browser.$('input[placeholder="https://example.com/"]');
    const confirmLinkButton = await browser.$('button[title="Подтвердить добавление ссылки"]');
    const text = await editor.$('span');

    await editor.click();
    await editor.addValue('test');
    await text.isExisting();
    await text.doubleClick();
    await editor.addValue(SELECT_ALL_SEQUENCE);
    await addLinkButton.waitForEnabled();
    await addLinkButton.click();
    await urlInput.waitForDisplayed();
    await urlInput.click();
    await urlInput.addValue(PAGE_URL);
    await confirmLinkButton.isClickable();
    await confirmLinkButton.click();
};

/**
 * Проверяем, что:
 * после отправки одного комментария можно добавить гиперссылку в текст следующего,
 * после отправки второго ссылка в теле последнего комментария корректна.
 */
describe(`ocrm-1698: Добавление гиперссылки в тестовом редакторе работает после добавления комментария`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('После отправки двух комментариев ссылка в последнем соответствует заданной', async function() {
        await waitForReactRootLoaded(this.browser);
        const chatsTab = new Button(this.browser, 'body', CHAT_TABS_SELECTOR);
        const editor = await this.browser.$(`[data-ow-test-comment-editor] [contenteditable="true"]`);
        const addCommentButton = new Button(this.browser, 'body', '[data-ow-test-add-comment]');
        const commentActions = new PopperNew(this.browser);
        const lastComment = await this.browser.$('[data-ow-test-comment]:last-of-type');

        await chatsTab.clickButton();
        await editor.isExisting();
        await editor.click();
        await editor.addValue('test');
        await addCommentButton.clickButton();
        await commentActions.clickFirstElement();
        await addCommentButton.waitForEnabled();
        await addLinkToCommentText(this.browser, editor);
        await addCommentButton.clickButton();
        await commentActions.clickFirstElement();
        await addCommentButton.waitForEnabled();
        await pause(500);
        const lastCommentLink = lastComment.$('a');
        const href = await lastCommentLink.getProperty('href');
        const {baseUrl = ''} = this.browser.options;
        const resultUrl = String(new URL(url.resolve(baseUrl, PAGE_URL)));

        expect(href).to.equal(resultUrl, 'Ссылки в последнем комментарии нет или она не соответствует заданной');
    });
});

/**
 * Проверяем, что:
 * Можно добавить гиперссылку в текст комментария,
 * после отправки ссылка в теле последнего комментария корректна.
 */
describe(`ocrm-1699: Добавление гиперссылки в тестовом редакторе`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('После отправки комментария ссылка в последнем комментарии соответствует заданной', async function() {
        await waitForReactRootLoaded(this.browser);
        const chatsTab = new Button(this.browser, 'body', CHAT_TABS_SELECTOR);
        const editor = await this.browser.$(`[data-ow-test-comment-editor] [contenteditable="true"]`);
        const addCommentButton = new Button(this.browser, 'body', '[data-ow-test-add-comment]');
        const commentActions = new PopperNew(this.browser);
        const lastComment = await this.browser.$('[data-ow-test-comment]:last-of-type');

        await chatsTab.clickButton();
        await editor.isExisting();
        await addLinkToCommentText(this.browser, editor);
        await addCommentButton.clickButton();
        await commentActions.clickFirstElement();
        await addCommentButton.waitForEnabled();
        await pause(500);
        const lastCommentLink = lastComment.$('a');
        const href = await lastCommentLink.getProperty('href');
        const {baseUrl = ''} = this.browser.options;
        const resultUrl = String(new URL(url.resolve(baseUrl, PAGE_URL)));

        expect(href).to.equal(resultUrl, 'Ссылки в последнем комментарии нет или она не соответствует заданной');
    });
});
