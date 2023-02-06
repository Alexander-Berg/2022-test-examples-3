'use strict';

const path = require('path');
const veryBigCatImage = path.resolve(__dirname, 'images', 'veryBigCatImage.jpg');
const PO = require('../companies/companies.page-object/index@desktop');
const disableElemAnimation = require('./disableElemAnimation');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Оставление отзыва в просмотрщике отзывов организации',
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

    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.assertView('plain', PO.reviewViewerModal.myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [PO.reviewViewerModal.myReview.reviewEditable.header.author.image()],
        });
    });

    it('Оставить отзыв', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());
        const value = await browser.getAttribute(myReview.reviewEditable.buttonSuccess(), 'disabled');
        assert.equal(value, null, 'Кнопка должна быть активна, если есть оценка');
        await browser.assertView('selected-rating', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });

        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        const value2 = await browser.getAttribute(myReview.reviewEditable.buttonSuccess(), 'disabled');
        assert.equal(value2, null, 'Кнопка должна быть активна, если есть оценка и отзыв');
        await browser.assertView('written-review', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.reviewThanksMessage());
        await browser.execute(disableElemAnimation, myReview.reviewThanksMessage.image());

        await browser.assertView('sent-review', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.review.date(), myReview.reviewEditable.header.author.image()],
        });

        await this.browser.yaCheckLink2({
            selector: myReview.reviewThanksMessage.button(),
            url: {
                href: '/ugcpub/cabinet',
                ignore: ['protocol', 'hostname'],
                queryValidator: query => {
                    assert(query.utm_medium, 'serp');
                    assert(query.utm_content, 'from_review');

                    return true;
                },
            },
            baobab: { path: '/$page/$parallel/$result/composite/reviews-viewer/tabs-panes-modal/reviews-page/my-review/reviewThanksMessage/button' },
        });
    });

    it('Отмена отправки', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());

        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        await browser.click(myReview.reviewEditable.buttonCancel());
        await browser.yaWaitForVisible(myReview.reviewEditable.header.reviewButton());
        await browser.assertView('cancel-review', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());
        await browser.yaWaitForHidden(myReview.reviewEditable.header.reviewButton());
        await browser.assertView('cancel-rating', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
    });

    it('Отправка и редактирование отзыва', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());

        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        await browser.click(myReview.review.header.kebab.icon());
        await browser.click(myReview.review.header.kebab.tooltip.buttonChange());
        await browser.assertView('edit', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
    });

    it('Удаление отзыва', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());

        await browser.setValue(
            myReview.reviewEditable.reviewForm.control(),
            'Отличное заведение!',
        );

        await browser.click(myReview.reviewEditable.buttonSuccess());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        await browser.click(myReview.review.header.kebab.icon());
        await browser.click(myReview.review.header.kebab.tooltip.buttonDelete());
        await browser.yaWaitForVisible(myReview.dialog());
        await browser.assertView('delete-dialog', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
        await browser.click(myReview.dialog.no());
        await browser.yaWaitForVisible(myReview.review.header.kebab());
        await browser.click(myReview.review.header.kebab.icon());
        await browser.click(myReview.review.header.kebab.tooltip.buttonDelete());
        await browser.yaWaitForVisible(myReview.dialog());
        await browser.click(myReview.dialog.yes());
        await browser.assertView('deleted', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
    });

    it('Дисклеймер о допустимом размере фотографии', async function() {
        const { browser } = this;
        const myReview = PO.reviewViewerModal.myReview;

        await browser.click(myReview.reviewEditable.ratingDynamic.thirdStar());
        await browser.chooseFile(myReview.reviewEditable.photoInput(), veryBigCatImage);
        await browser.assertView('photos-error', myReview(), {
            invisibleElements: `body > *:not(${PO.reviewViewerModal()})`,
            hideElements: [myReview.reviewEditable.header.author.image()],
        });
    });
});
