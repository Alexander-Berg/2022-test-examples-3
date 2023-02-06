import {makeCase, makeSuite, mergeSuites} from 'ginny';

import Votes from '@self/platform/spec/page-objects/components/Votes';

/**
 * @param {PageObject.components.UserReview.ProductReviewFooter.VotesAndReplies} VotesAndReplies
 */

const EXPEXT_TIMEOUT = 3000;
const EXPEXT_INTERVAL = 500;

export default makeSuite('Лайки и дизлайки.', {
    params: {
        totalLikesCount: 'Общее количество лайков отзыва',
        totalDislikesCount: 'Общее количество дизлайков отзыва',
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                votes: () => this.createPageObject(Votes),
            });
        },
        'По умолчанию': {
            'отображаются': makeCase({
                id: 'marketfront-3893',
                async test() {
                    return this.votes.isVisible()
                        .should.eventually.be.equal(true, 'Лайки и дизлайки отображаются');
                },
            }),
        },
        'Кнопка лайка.': {
            'При клике': {
                'количество лайков увеличивается': makeCase({
                    id: 'marketfront-3894',
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
                'количество лайков увеличивается,': {
                    'а потом при клике "дизлайк" дизлайки увеличиваются, а лайки уменьшаются': makeCase({
                        id: 'marketfront-3933',
                        async test() {
                            const expectedTotalLikesCount = this.params.totalLikesCount;
                            const expectedTotalDislikesCount = this.params.totalDislikesCount;
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
                            await this.browser.allure.runStep('Кликаем на "дизлайк"',
                                () => this.votes.clickDislike()
                            );
                            const newDislikesCount = this.votes.getDislikeCount();
                            newLikesCount = this.votes.getLikeCount();
                            await this.expect(newLikesCount)
                                .to.be.equal(expectedTotalLikesCount, 'Число лайков уменьшилось');
                            return this.expect(newDislikesCount)
                                .to.be.equal(expectedTotalDislikesCount + 1, 'Число дизлайков увеличилось');
                        },
                    }),
                    'а потом при клике "лайк" лайки возвращаются': makeCase({
                        id: 'marketfront-3895',
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
                            await this.browser.allure.runStep('Кликаем на кнопку лайка второй раз',
                                () => this.votes.clickLike()
                            );

                            // Ожидаем когда число дизлайков изменится
                            return this.browser.waitUntil(
                                async () => {
                                    newLikesCount = await this.votes.getLikeCount();
                                    return this.expect(newLikesCount).to.be.equal(expectedTotalLikesCount);
                                },
                                EXPEXT_TIMEOUT,
                                'Число лайков не уменьшилось обратно',
                                EXPEXT_INTERVAL
                            );
                        },
                    }),
                },
            },
        },
        'Кнопка дизлайка.': {
            'При клике': {
                'количество дизлайков увеличивается': makeCase({
                    id: 'marketfront-3896',
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
                'количество дизлайков увеличивается,': {
                    'потом при клике "лайк" лайки увеличиваются, а дизлайки уменьшаются': makeCase({
                        id: 'marketfront-3934',
                        async test() {
                            const expectedTotalLikesCount = this.params.totalLikesCount;
                            const expectedTotalDislikesCount = this.params.totalDislikesCount;
                            const dislikesCount = await this.votes.getDislikeCount();
                            await this.expect(dislikesCount)
                                .to.be.equal(expectedTotalDislikesCount, 'Отображается верное количество дизлайков');
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickDislike(),
                                valueGetter: () => this.votes.getDislikeCount(),
                            });
                            let newDisikesCount = await this.votes.getDislikeCount();
                            await this.expect(newDisikesCount)
                                .to.be.equal(expectedTotalDislikesCount + 1, 'Число дизлайков увеличилось');
                            await this.browser.allure.runStep('Кликаем на кнопку лайка после нажатия дизлайка',
                                () => this.votes.clickLike()
                            );
                            // Ожидаем когда число дизлайков изменится
                            await this.browser.waitUntil(
                                async () => {
                                    newDisikesCount = await this.votes.getDislikeCount();
                                    return this.expect(newDisikesCount).to.be.equal(expectedTotalDislikesCount);
                                },
                                EXPEXT_TIMEOUT,
                                'Число дизлайков не уменьшилось',
                                EXPEXT_INTERVAL
                            );

                            const newLikesCount = this.votes.getLikeCount();
                            return this.expect(newLikesCount)
                                .to.be.equal(expectedTotalLikesCount + 1, 'Число лайков увеличилось');
                        },
                    }),
                    'потом при клике "дизлайк" количество дизлайков возвращается': makeCase({
                        id: 'marketfront-3897',
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
                            await this.browser.allure.runStep('Кликаем на кнопку дизлайка второй раз',
                                () => this.votes.clickDislike()
                            );
                            newDisikesCount = this.votes.getDislikeCount();
                            return this.expect(newDisikesCount)
                                .to.be.equal(expectedTotalDislikesCount, 'Число дизлайков вернулось обратно');
                        },
                    }),
                },
            },
        },
    }),
});
