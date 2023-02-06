import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок «Ваш отзыв». Голосование', {
    feature: 'Блок отзыва',
    story: {
        'Кнопка дизлайка.': {
            'При клике кнопка окрашивается в чёрный цвет, каунтер увеличивается': makeCase({
                id: 'm-touch-2363',
                issue: 'MOBMARKET-9585',
                async test() {
                    await this.expect(this.votes.isDislikeActive())
                        .to.be.equal(false, 'Кнопка дизлайка не активна');
                    const dislikeCount = await this.votes.getDislikeCount();
                    await this.expect(dislikeCount)
                        .to.be.equal(200);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                    const newDislikeCount = await this.votes.getDislikeCount();
                    await this.expect(newDislikeCount)
                        .to.be.equal(201);
                    await this.expect(this.votes.isDislikeActive())
                        .to.be.equal(true, 'Кнопка дизлайка активна');
                },
            }),
        },
        'Кнопка лайка.': {
            'При клике кнопка окрашивается в чёрный цвет, каунтер увеличивается': makeCase({
                id: 'm-touch-2363',
                issue: 'MOBMARKET-9585',
                async test() {
                    await this.expect(this.votes.isLikeActive())
                        .to.be.equal(false, 'Кнопка лайка не активна');
                    const dislikeCount = await this.votes.getLikeCount();
                    await this.expect(dislikeCount)
                        .to.be.equal(300);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const newDislikeCount = await this.votes.getLikeCount();
                    await this.expect(newDislikeCount)
                        .to.be.equal(301);
                    await this.expect(this.votes.isLikeActive())
                        .to.be.equal(true, 'Кнопка лайка активна');
                },
            }),
        },
    },
});
