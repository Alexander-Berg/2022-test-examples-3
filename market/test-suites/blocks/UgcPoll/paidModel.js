import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import YaPlusReviewMotivationSuite from '@self/platform/spec/hermione/test-suites/blocks/components/YaPlusReviewMotivation';

/**
 * Тесты на попап-агитацию оставления отзыва на товар с наличием платы за отзыв
 * @property {PageObjects.ReviewPollScreen} reviewPollScreen
 * @property {PageObjects.YaPlusReviewMotivation} yaPlusReviewMotivation
 */
export default makeSuite('Платный опрос на товар.', {
    params: {
        paymentOfferAmount: 'Сколько баллов готовы дать за отзыв',
    },
    story: {
        beforeEach() {
            return this.reviewPollScreen.waitForOpened();
        },
        'Экран "Общая оценка".': mergeSuites(
            prepareSuite(YaPlusReviewMotivationSuite)
        ),
    },
});
