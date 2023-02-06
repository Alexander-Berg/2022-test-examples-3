import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Review} review
 */
export default makeSuite('Блок «Ваш отзыв». Раскрытие отзыва', {
    feature: 'Блок отзыва',
    story: {
        'Кнопка «Читать полностью».': {
            'При клике отзыв раскрывается': makeCase({
                id: 'm-touch-1843',
                issue: 'MOBMARKET-9588',
                async test() {
                    const collapsedHeight = await this.review.getContentHeight();
                    await this.review.clickExpander();
                    const expandedHeight = await this.review.getContentHeight();
                    await this.expect(expandedHeight).to.be.above(collapsedHeight);
                },
            }),
        },
        'Кнопка «Скрыть».': {
            'При клике отзыв сворачивается': makeCase({
                id: 'm-touch-1844',
                issue: 'MOBMARKET-9589',
                async test() {
                    await this.review.clickExpander();
                    const expandedHeight = await this.review.getContentHeight();
                    await this.review.clickExpander();
                    const collapsedHeight = await this.review.getContentHeight();
                    await this.expect(collapsedHeight).to.be.below(expandedHeight);
                },
            }),
        },
    },
});
