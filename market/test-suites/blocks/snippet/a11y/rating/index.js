import {makeCase, makeSuite} from 'ginny';

// PageObjects
import Rating from '@self/root/src/uikit/components/Rating/__pageObject';

const prepareRatingExpectedText = value => `Рейтинг: ${value} из 5`;

export default makeSuite('Рейтинг', {
    params: {
        starsCount: 'Number, рейтинг, количество звездочек у товара',
    },
    defaultParams: {
        starsCount: 0,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                rating: () => this.createPageObject(Rating, {
                    parent: this.scrollBox.getItemByIndex(1),
                }),
            });
        },
        'имеет корректные теги': makeCase({
            async test() {
                const expectedText = prepareRatingExpectedText(this.params.starsCount);

                await this.rating.getAttribute().should.eventually.to.be.equal(
                    expectedText,
                    `Текст озвучки должен быть "${expectedText}"`
                );
            },
        }),
    },
});
