'use strict';

const PO = require('../companies/companies.page-object/index@desktop');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Доступность оставление отзыва в просмотрщике отзывов организации',
}, function() {
    beforeEach(async function() {
        const { browser } = this;

        await browser.authOnRecord('plain-user');

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await browser.yaMockXHR({
            urlDataMap: {
                'ugcpub/feedback/': {},
            },
        });

        await browser.click(PO.oneOrg.tabsMenu.secondItem());
        await browser.yaWaitForVisible(PO.reviewViewerModal());
        await browser.yaShouldBeVisible(PO.reviewViewerModal.myReview(), 'Нет врезки для оставления/редактирования отзыва');
    });

    it('Оставление отзыва', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        const value = await browser.getAttribute(myReview.reviewEditable.ratingDynamic(), 'role');
        assert.equal(value, 'radiogroup', 'Сломан атрибут radiogroup в компоненте оставления отзыва');
        const value2 = await browser.getAttribute(myReview.reviewEditable.ratingDynamic(), 'aria-label');
        assert.equal(value2, 'Выставление оценки', 'Сломан атрибут aria-label у компонента оставления оценки');
        const value3 = await browser.getAttribute(myReview.reviewEditable.ratingDynamic.thirdStar(), 'aria-label');
        assert.equal(value3, 3, 'Сломан атрибут aria-label у звёздочки');
        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());
        const value4 = await browser.getAttribute(myReview.reviewEditable.buttonSuccess(), 'disabled');
        assert.equal(value4, null, 'Сломан атрибут disabled у кнопки Отправить');

        await browser.yaWaitForVisible(myReview.reviewEditable.reviewForm.control());
        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        const value5 = await browser.getAttribute(myReview.reviewEditable.buttonSuccess(), 'disabled');
        assert.equal(value5, null, 'Если написан отзыв, кнопка должна быть активна');
        await browser.click(myReview.reviewEditable.buttonSuccess());
    });

    it('Редактирование отзыва', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());

        await browser.yaWaitForVisible(myReview.reviewEditable.reviewForm.control());
        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        const value = await browser.getAttribute(myReview.review.header.kebab.icon(), 'aria-label');
        assert.equal(value, 'Меню редактирования отзыва', 'Сломан атрибут aria-label в компоненте редактирования отзыва');
        await browser.click(myReview.review.header.kebab.icon());
        const tag = await browser.getTagName(myReview.review.header.kebab.tooltip.buttonChange());
        await assert.equal(tag, 'button', 'Сломана семантика у кнопок редактирования отзыва');
        await browser.click(myReview.review.header.kebab.tooltip.buttonChange());

        await browser.yaWaitForVisible(myReview.reviewEditable.reviewForm.control());
        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        const value2 = await browser.getAttribute(myReview.reviewEditable.buttonSuccess(), 'disabled');
        assert.equal(value2, null, 'Если написан отзыв, кнопка должна быть активна');
        await browser.click(myReview.reviewEditable.buttonSuccess());
    });

    it('Удаление отзыва', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());

        await browser.yaWaitForVisible(myReview.reviewEditable.reviewForm.control());
        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        await browser.click(myReview.review.header.kebab.icon());
        const tag = await browser.getTagName(myReview.review.header.kebab.tooltip.buttonDelete());
        await assert.equal(tag, 'button', 'Сломана семантика у кнопок редактирования отзыва');
        await browser.click(myReview.review.header.kebab.tooltip.buttonDelete());
        await browser.yaWaitForVisible(myReview.dialog());
        await browser.click(myReview.dialog.no());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        await browser.click(myReview.review.header.kebab.icon());
        await browser.click(myReview.review.header.kebab.tooltip.buttonDelete());
        await browser.yaWaitForVisible(myReview.dialog());
        const tag2 = await browser.getTagName(myReview.dialog.yes());
        await assert.equal(tag2, 'button', 'Сломана семантика у кнопок в диалоге удаления отзыва');
        await browser.click(myReview.dialog.yes());
        await browser.yaShouldBeVisible(myReview.reviewEditable.ratingDynamic());
    });
});
