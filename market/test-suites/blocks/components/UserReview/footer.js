import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ReviewFooter from '@self/platform/spec/page-objects/components/UserReview/ReviewFooter';
import Votes from '@self/platform/spec/page-objects/components/Votes';

/**
 * @param {PageObject.components.UserReview.ReviewFooter} reviewFooter
 */

export default makeSuite('Футер c лайками и комментариями.', {
    params: {
        expectedReviewPageLink: 'Ожидаемая ссылка на отзыв',
        totalLikesCount: 'Общее количество лайков отзыва',
        totalDislikesCount: 'Общее количество дизлайков отзыва',
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                reviewFooter: () => this.createPageObject(ReviewFooter),
                votes: () => this.createPageObject(Votes),
            });
        },
        'Лайки и дизлайки.': {
            'По умолчанию': {
                'отображаются': makeCase({
                    id: 'm-touch-3214',
                    async test() {
                        return this.votes.isVisible()
                            .should.eventually.be.equal(true, 'Лайки и дизлайки отображаются');
                    },
                }),
            },
            'Кнопка лайка.': {
                'При клике': {
                    'количество лайков увеличивается': makeCase({
                        id: 'm-touch-3215',
                        async test() {
                            const expectedTotalLikesCount = this.params.totalLikesCount;
                            const likesCount = await this.votes.getLikeCount();

                            await this.expect(likesCount)
                                .to.be.equal(expectedTotalLikesCount, 'Отображается верное количество лайков');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });

                            const newLikesCount = this.votes.getLikeCount();

                            return this.expect(newLikesCount)
                                .to.be.equal(expectedTotalLikesCount + 1, 'Число лайков увеличилось');
                        },
                    }),
                },
                'При двойном клике': {
                    'количество лайков сначала увеличивается, а потом возвращается': makeCase({
                        id: 'm-touch-3216',
                        async test() {
                            const expectedTotalLikesCount = this.params.totalLikesCount;
                            const likesCount = await this.votes.getLikeCount();

                            await this.expect(likesCount)
                                .to.be.equal(expectedTotalLikesCount, 'Отображается верное количество лайков');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });

                            let newLikesCount = this.votes.getLikeCount();

                            await this.expect(newLikesCount)
                                .to.be.equal(expectedTotalLikesCount + 1, 'Число лайков увеличилось');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });

                            newLikesCount = this.votes.getLikeCount();

                            return this.expect(newLikesCount)
                                .to.be.equal(expectedTotalLikesCount, 'Число лайков уменьшилось');
                        },
                    }),
                },
            },
            'Кнопка дизлайка.': {
                'При клике': {
                    'количество дизлайков увеличивается': makeCase({
                        id: 'm-touch-3217',
                        async test() {
                            const expectedTotalDislikesCount = this.params.totalDislikesCount;
                            const dislikesCount = await this.votes.getDislikeCount();

                            await this.expect(dislikesCount)
                                .to.be.equal(expectedTotalDislikesCount, 'Отображается верное количество дизлайков');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickDislike(),
                                valueGetter: () => this.votes.getDislikeCount(),
                            });

                            const newDislikesCount = this.votes.getDislikeCount();

                            return this.expect(newDislikesCount)
                                .to.be.equal(expectedTotalDislikesCount + 1, 'Число дизлайков увеличилось');
                        },
                    }),
                },
                'При двойном клике': {
                    'количество дизлайков сначала увеличивается, а потом возвращается': makeCase({
                        id: 'm-touch-3218',
                        async test() {
                            const expectedTotalDislikesCount = this.params.totalDislikesCount;
                            const dislikesCount = await this.votes.getDislikeCount();

                            await this.expect(dislikesCount)
                                .to.be.equal(expectedTotalDislikesCount, 'Отображается верное количество дизлайков');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickDislike(),
                                valueGetter: () => this.votes.getDislikeCount(),
                            });

                            let newDisikesCount = this.votes.getDislikeCount();

                            await this.expect(newDisikesCount)
                                .to.be.equal(expectedTotalDislikesCount + 1, 'Число дизлайков увеличилось');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickDislike(),
                                valueGetter: () => this.votes.getDislikeCount(),
                            });

                            newDisikesCount = this.votes.getDislikeCount();

                            return this.expect(newDisikesCount)
                                .to.be.equal(expectedTotalDislikesCount, 'Число дизлайков уменьшилось');
                        },
                    }),
                },
            },
        },
    }),
});
