import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import YaPlusReviewMotivationSuite from '@self/platform/spec/hermione/test-suites/blocks/components/YaPlusReviewMotivation';

/**
 * @property {PageObjects.ReviewPollCard} reviewPollCard
 * @property {PageObjects.RatingStars} ratingStars
 * @property {PageObjects.YaPlusReviewMotivation} yaPlusReviewMotivation
 */
export default makeSuite('Платная агитация оценить товар.', {
    params: {
        productId: 'Айди товара',
        agitationId: 'Айди агитации',
    },
    story: mergeSuites(
        prepareSuite(YaPlusReviewMotivationSuite),
        {
            'При клике на звезды': {
                'должна открыться форма создания отзыва с параметром agitationId.': makeCase({
                    id: 'm-touch-3569',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(
                            () => this.ratingStars.setRating(4)
                        );
                        return this.browser.allure.runStep('Проверяем URL страницы после перехода', () =>
                            this.browser
                                .getUrl()
                                .should.eventually.be.link(
                                    {
                                        pathname: `product--.*/${this.params.productId}/reviews/add`,
                                        query: {
                                            agitationId: this.params.agitationId,
                                        },
                                    },
                                    {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }
                                ));
                    },
                }),
            },
        }
    ),
});
