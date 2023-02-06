import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import YaPlusReviewMotivationSuite from '@self/platform/spec/hermione/test-suites/blocks/components/YaPlusReviewMotivation';

/**
 * @property {PageObjects.ReviewPollCard} reviewPollCard
 * @property {PageObjects.YaPlusReviewMotivation} yaPlusReviewMotivation
 */
export default makeSuite('Платная агитация написать отзыв на товар.', {
    params: {
        productId: 'Айди товара',
        agitationId: 'Айди агитации',
    },
    story: mergeSuites(
        prepareSuite(YaPlusReviewMotivationSuite),
        {
            'При клике на кнопку "Написать отзыв"': {
                'должна открыться форма создания отзыва с параметром agitationId.': makeCase({
                    async test() {
                        const actualUrl = await this.reviewPollCard.getLinkHref();
                        return this.expect(actualUrl, 'Ссылка корректная')
                            .to.be.link({
                                pathname: `product--.*/${this.params.productId}/reviews/add`,
                                query: {
                                    agitationId: this.params.agitationId,
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        }
    ),
});
