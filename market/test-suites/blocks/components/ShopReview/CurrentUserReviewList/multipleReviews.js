import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CurrentUserReviewList} currentUserReviewList`
 */
export default makeSuite('Список блоков «Ваш отзыв». Несколько отзывов', {
    feature: 'Рейтинг магазина',
    story: {
        'По умолчанию': {
            'отображаются все отзывы и каждый из них содержит заголовок «Ваш отзыв»':
                makeCase({
                    id: 'm-touch-2367',
                    issue: 'MOBMARKET-9590',
                    async test() {
                        await this.expect(this.currentUserReviewList.getReviewsCount())
                            .to.be.equal(2, 'Отображается 2 блока «Ваш отзыв»');
                        const texts = await this.currentUserReviewList.getReviewTitleTexts();
                        await Promise.all(texts.map(text =>
                            this.expect(text)
                                .to.be.equal('Ваш отзыв', 'В заголовке отображается «Ваш отзыв»')
                        ));
                    },
                }),
        },
    },
});
