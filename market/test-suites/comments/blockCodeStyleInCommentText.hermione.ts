import 'hermione';
import {expect} from 'chai';

import {login, waitForReactRootLoaded} from '../../helpers';
import Button from '../../page-objects/button';
import PopperNew from '../../page-objects/popperNew';
import {CHAT_TABS_SELECTOR} from '../../page-objects/commentsContainer';
import {Keys} from '../../constants';

const LONG_COMMENT_TEXT =
    'Письмо от 18 октября: в пункте доставки произошел сбой, мне пришло извещение, что покупка ждет меня в пункте выдачи, но по сегодняшний день она не поступила в пункт выдачи.';
const FULL_COMMENT_TEXT = 'test\ntest\n test';

const PAGE_URL = '/entity/ticket@209926089';

/**
 * Проверяем, что:
 * для длинного комментария по нажатию на кнопку "Блок кода" применяется нужный стиль,
 * текст комментария не выходит за рамки редактора,
 * после отправки комментария стиль сохраняется.
 */
describe(`ocrm-1700: Длинный текст со стилем "Блок кода" не выходит за границы текстового редактора`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Текст со стилем "Блок кода" меньше ширины редактора', async function() {
        await waitForReactRootLoaded(this.browser);
        const chatsTab = new Button(this.browser, 'body', CHAT_TABS_SELECTOR);
        const editorBody = await this.browser.$(`[data-ow-test-comment-editor]`);
        const editor = await this.browser.$(`[data-ow-test-comment-editor] [contenteditable="true"]`);
        const addCommentButton = new Button(this.browser, 'body', '[data-ow-test-add-comment]');
        const commentActions = new PopperNew(this.browser);

        await chatsTab.clickButton();
        await editor.isExisting();
        const addCodeBlockStyleButton = await this.browser.$('button[title="Блок кода"]');

        await editor.click();
        await editor.addValue(LONG_COMMENT_TEXT);
        const commentText = await editor.$('span');

        await commentText.isExisting();
        await commentText.doubleClick();

        await addCodeBlockStyleButton.isEnabled();
        await addCodeBlockStyleButton.click();
        const isStyleApplied = await editor.$('pre').isExisting();

        expect(isStyleApplied).to.equal(true, 'Стиль в редакторе не применился');

        const commentTextWidth = await commentText.getSize('width');
        const editorParent = await editorBody.parentElement();
        const editorWidth = await editorParent.getSize('width');

        expect(commentTextWidth).to.be.below(editorWidth, 'Текст выходит за границы редактора');

        await addCommentButton.clickButton();
        await commentActions.clickFirstElement();
        await addCommentButton.waitForEnabled();

        const lastComment = await this.browser.$('[data-ow-test-comment]:last-of-type');
        const isStyleAppliedToComment = await lastComment.$('pre').isExisting();

        expect(isStyleAppliedToComment).to.equal(true, 'Стиль в комментарии не применился');
    });
});

/**
 * Проверяем, что:
 * для текста по нажатию на кнопку "Блок кода" применяется нужный стиль;
 * после нажатия на Enter и вставке нового текста стиль применяется к новому абзацу;
 * после повторного нажатия на Enter и вставке нового текста не происходит перевод курсора на начало текста,
 * стиль применяется к новому абзацу;
 * после отправки комментария стиль сохраняется.
 */
describe(`ocrm-1701: При использовании стиля "Блок кода" последующий текст без стиля корректно пишется, вставляется и переносится на другую строку`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Текст со стилем "Блок кода" не ломает другой текст в редакторе', async function() {
        await waitForReactRootLoaded(this.browser);
        const chatsTab = new Button(this.browser, 'body', CHAT_TABS_SELECTOR);
        const editor = await this.browser.$(`[data-ow-test-comment-editor] [contenteditable="true"]`);
        const addCommentButton = new Button(this.browser, 'body', '[data-ow-test-add-comment]');
        const commentActions = new PopperNew(this.browser);

        await chatsTab.clickButton();
        await editor.isExisting();
        const addCodeBlockStyleButton = await this.browser.$('button[title="Блок кода"]');

        await editor.click();
        await editor.addValue('test');
        const commentText = await editor.$('span');

        await commentText.isExisting();
        await commentText.doubleClick();

        await addCodeBlockStyleButton.isEnabled();
        await addCodeBlockStyleButton.click();
        const isStyleApplied = await editor.$('pre').isExisting();

        expect(isStyleApplied).to.equal(true, 'Стиль в редакторе не применился');

        await editor.click();
        await editor.addValue(Keys.ENTER);
        await editor.addValue('test');

        const isStyleAppliedTwice = await editor.$('pre:nth-of-type(2)').isExisting();

        expect(isStyleAppliedTwice).to.equal(true, 'Стиль в редакторе для второго абзаца не применился');

        await editor.addValue(Keys.ENTER);
        await editor.addValue(' test');

        const newCommentText = await editor.getText();

        expect(newCommentText).to.equal(FULL_COMMENT_TEXT, 'Текст вставился некорректно');

        const isStyleAppliedThreeTimes = await editor.$('pre:nth-of-type(3)').isExisting();

        expect(isStyleAppliedThreeTimes).to.equal(true, 'Стиль в редакторе для третьего абзаца не применился');

        await addCommentButton.clickButton();
        await commentActions.clickFirstElement();
        await addCommentButton.waitForEnabled();

        const lastComment = await this.browser.$('[data-ow-test-comment]:last-of-type');
        const isStyleAppliedToComment = await lastComment.$('pre').isExisting();

        expect(isStyleAppliedToComment).to.equal(true, 'Стиль в комментарии не применился');

        const lastCommentBody = await lastComment.$('[data-ow-test-comment-body]');
        const textFromComment = await lastCommentBody.getText();

        expect(textFromComment).to.equal(FULL_COMMENT_TEXT, 'Текст в комментарии некорректный');
    });
});
