import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import {ExpandableComment} from '../../page-objects/expandableComment';
import CommentsContainer from '../../page-objects/commentsContainer';

/**
 * Проверить, что:
 * в обращении, где есть кнопка цитирования, нет кнопки разворачивания комментария.
 */
describe(`ocrm-1214: Сворачивание комментариев в обращениях (с цитированием)`, () => {
    beforeEach(function() {
        return login('/entity/ticket@76669004', this);
    });

    it(`В обращениях, где большой текст спрятан под кнопкой цитирования, нет кнопки "Развернуть комментарий"`, async function() {
        const commentsContainer = new CommentsContainer(this.browser);

        await commentsContainer.isExisting('Контейнер комментариев не найден на странице.');
        await commentsContainer.waitForCommentsLoaded();

        const expandableComment = new ExpandableComment(this.browser);
        const isExpandCommentsButtonPresent = await (await expandableComment.button).isExisting();

        expect(isExpandCommentsButtonPresent).to.equal(
            false,
            'На странице отображается кнопка "Развернуть комментарий"'
        );
    });
});

/**
 * Проверить, что:
 * в обращении логистической поддержки в больших сообщениях от партнера работает
 * кнопка "Развернуть комментарий"/"Свернуть комментарий".
 */
describe(`ocrm-1213: Сворачивание комментариев в обращениях (логистика)`, () => {
    beforeEach(function() {
        return login('/entity/ticket@154794469', this);
    });

    it(`В обращениях, где большой текст спрятан под кнопкой цитирования, нет кнопки "Развернуть комментарий"`, async function() {
        const commentsContainer = new CommentsContainer(this.browser);

        await commentsContainer.isExisting('Контейнер комментариев не найден на странице.');
        await commentsContainer.waitForCommentsLoaded();

        const expandableComment = new ExpandableComment(this.browser);

        const commentHighBeforeExpanding = await expandableComment.getHeightOfDisplayedText();
        const commentGradientBeforeExpanding = await expandableComment.isTextBlurred();
        const buttonTextBeforeExpanding = await expandableComment.getButtonText();

        await expandableComment.clickButton();

        const commentExpanded = await expandableComment.waitForHeightChanged(commentHighBeforeExpanding);
        const commentHeightAfterExpanding = await expandableComment.getHeightOfDisplayedText();

        const commentGradientAfterExpanding = await expandableComment.isTextBlurred();
        const buttonTextAfterExpanding = await expandableComment.getButtonText();

        await expandableComment.clickButton();

        const commentCollapsed = await expandableComment.waitForHeightChanged(commentHeightAfterExpanding);
        const commentHeightAfterCollapsing = await expandableComment.getHeightOfDisplayedText();

        const commentGradientAfterCollapsing = await expandableComment.isTextBlurred();
        const buttonTextAfterCollapsing = await expandableComment.getButtonText();

        expect(commentHighBeforeExpanding).to.equal('200px', 'Высота текста до нажатия на кнопку не верная');
        expect(commentGradientBeforeExpanding).to.equal(true, 'На тексте свернутого комментария отсутствует градиент');
        expect(buttonTextBeforeExpanding).to.equal(
            'Развернуть комментарий',
            'До разворачивания на кнопке отображается неверный текст'
        );
        expect(commentExpanded).to.equal(true, 'Высота текста не изменилась после нажатия на кнопку');
        expect(commentGradientAfterExpanding).to.equal(
            false,
            'После разворачивания комментария у текста не пропал градиент'
        );
        expect(buttonTextAfterExpanding).to.equal(
            'Свернуть комментарий',
            'После разворачивания комментария на кнопке отображается неверный текст'
        );
        expect(commentCollapsed).to.equal(true, 'После нажатия на "Свернуть комментарий" комментарий не свернулся');
        expect(commentHeightAfterCollapsing).to.equal(
            '200px',
            'Высота текста после нажатия на "Свернуть комментарий" не вернулась на заданое значение'
        );
        expect(commentGradientAfterCollapsing).to.equal(
            true,
            'После нажатия на "Свернуть комментарий" на комментарии отсутствует градиент'
        );
        expect(buttonTextAfterCollapsing).to.equal(
            'Развернуть комментарий',
            'После нажатия на "Свернуть комментарий" текст кнопки не изменился на "Развернуть комментарий"'
        );
    });
});
