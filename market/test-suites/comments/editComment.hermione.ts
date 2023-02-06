/* eslint-disable no-await-in-loop */

import 'hermione';
import {expect, assert} from 'chai';

import {login, PageObject, turnOffExperiment} from '../../helpers';
import CommentsContainer, {
    COMMENT_BODY_WRAPPER_ATTRIBUTE,
    COMMENT_WRAPPER_ATTRIBUTE,
    CHAT_TABS_SELECTOR,
} from '../../page-objects/commentsContainer';
import {CHECK_INTERVAL, CLEAR_ALL_SEQUENCE, TIMEOUT_MS} from '../../constants';
import Button from '../../page-objects/button';
import PopperNew from '../../page-objects/popperNew';

const MIN_COMMENTS_COUNT = 7;

const PAGE_URL = '/entity/ticket@185649392';

const getCommentGids = async (containerElement): Promise<string[]> => {
    const comments = await containerElement.comments;

    return Promise.all(comments.map(comment => comment.getAttribute(COMMENT_WRAPPER_ATTRIBUTE)));
};

/**
 * Добавляет в переписку комментарии
 * @param browser
 * @param initialCommentGids
 */
const fillTicketWithComments = async (browser, initialCommentGids) => {
    const chatsTab = new Button(browser, 'body', CHAT_TABS_SELECTOR);
    const commentsEditor = new PageObject(browser, 'body', '[data-ow-test-comment-editor]');
    const addCommentButton = new Button(browser, 'body', '[data-ow-test-add-comment]');
    const commentActions = new PopperNew(browser);
    const editor = new PageObject(browser, '[data-ow-test-comment-editor]', '[contenteditable="true"]');

    await chatsTab.clickButton();

    await commentsEditor.isExisting();

    const editorElement = await editor.root();

    await editorElement.click();

    for (let i = initialCommentGids.length; i < MIN_COMMENTS_COUNT; i++) {
        await editorElement.addValue(`Комментарий №${i}`);
        await addCommentButton.clickButton();
        await commentActions.isDisplayed();
        await commentActions.clickFirstElement();

        const commentSent = await addCommentButton.waitForEnable();

        expect(commentSent).to.equal(true, 'При отправке комментария произошла ошибка');
    }
};

/**
 * План теста:
 * 0. зайти на страницу обращения
 * 1. заполнить переписку комментариями, если их нет
 * 2. сохранить массив комментариев с gid'ами и текстами
 * 3. кликнуть на первый редактируемый комментарий
 * 4. изменить текст в модалке, сохранить
 * 5. убедиться, что комментарий в переписке изменился
 * 6. снова выбрать массив всех комментариев на странице
 *    и сравнить с сохранённым — измениться должен
 *    только тот, который редактировали
 */
describe(`ocrm-1629: Редактирование комментариев`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('должно работать корректно', async function() {
        await turnOffExperiment(this.browser);

        const commentsContainer = new CommentsContainer(this.browser);

        await commentsContainer.isExisting('Контейнер комментариев не найден на странице.');
        await commentsContainer.waitForCommentsLoaded();

        const initialCommentGids: string[] = await getCommentGids(commentsContainer);

        if (initialCommentGids.length < MIN_COMMENTS_COUNT) {
            await fillTicketWithComments(this.browser, initialCommentGids);
        }

        const commentGids: string[] = await getCommentGids(commentsContainer);

        const allCommentsOnPage = await commentsContainer.getAllCommentsOnPage();

        const {editButton, editButtonGid} = await commentsContainer.findEditableComment(commentGids);

        await editButton?.clickButton();

        const editorInModal = new PageObject(this.browser, '[data-ow-test-modal-body]', '[contenteditable="true"]');
        const editorElementInModal = await editorInModal.root();
        const saveButtonInModal = new Button(
            this.browser,
            '[data-ow-test-modal-controls]',
            '[data-ow-test-modal-controls="save"]'
        );

        await editorInModal.isDisplayed();

        const newTextForComment = `Комментарий был изменён ${Date.now()}`;

        await editorElementInModal.click();
        await editorElementInModal.addValue(CLEAR_ALL_SEQUENCE);
        await editorElementInModal.addValue(newTextForComment);

        await saveButtonInModal.clickButton();
        await saveButtonInModal.waitForInvisible();

        const editedComment = new PageObject(
            this.browser,
            CommentsContainer.root,
            `[${COMMENT_BODY_WRAPPER_ATTRIBUTE}="${editButtonGid}"]`
        );

        const isCommentChanged = await this.browser.waitUntil(
            async () => {
                const editedCommentText = await (await editedComment.root()).getText();

                return editedCommentText === newTextForComment;
            },
            {timeout: TIMEOUT_MS, interval: CHECK_INTERVAL}
        );

        expect(isCommentChanged).to.equal(true, 'Комментарий не был изменён');

        const expectedCommentsOnPage = (allCommentsOnPage as Record<string, string>[]).map(comment => {
            if (comment.gid === editButtonGid) {
                return {...comment, ...{text: newTextForComment}};
            }

            return comment;
        });

        const allNewCommentsOnPage = await commentsContainer.getAllCommentsOnPage();

        assert.sameDeepOrderedMembers(expectedCommentsOnPage, allNewCommentsOnPage, 'Задеты другие комментарии');
    });
});
