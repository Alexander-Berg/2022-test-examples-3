import {makeSuite, makeCase} from 'ginny';

const TITLE = 'Отзывов с текстом ещё нет — ваш может стать первым. ' +
    'Ниже все отзывы без текста, которые оставили другие пользователи';

/**
 * Тесты на заголовок списка отзывов без текста
 *
 * @param {PageObject.ReviewsToolbar} reviewsToolbar
 */
export default makeSuite('Заголовок списка отзывов без текста', {
    id: 'marketfront-3466',
    feature: 'Отзыв без текста',
    issue: 'MARKETVERSTKA-34024',
    story: {
        [`должен быть равен "${TITLE}"`]: makeCase({
            test() {
                const titleText = this.reviewsToolbar.getTitleText();
                return titleText.should.eventually.to.be.equal(TITLE);
            },
        }),
    },
});
