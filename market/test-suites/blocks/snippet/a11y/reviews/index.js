import {makeCase, makeSuite} from 'ginny';

// helpers
import {pluralize} from '@self/root/src/utils/string';

// PageObjects
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import VisuallyHidden from '@self/root/src/uikit/components/VisuallyHidden/__pageObject';

const buildExpectedText = reviewsCount => pluralize(reviewsCount, 'отзыв', 'отзыва', 'отзывов');

export default makeSuite('Количество отзывов', {
    params: {
        reviewsCount: 'Number, количество отзывов на товар',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                reviewsInfo: () => this.createPageObject(Snippet, {
                    parent: this.scrollBox.getItemByIndex(1),
                }),
                visuallyHidden: () => this.createPageObject(VisuallyHidden, {
                    parent: this.reviewsInfo.rating,
                }),
            });
        },
        'имеет текст для незрячих': makeCase({
            async test() {
                const expectedText = buildExpectedText(this.params.reviewsCount);

                await this.visuallyHidden.getText().should.eventually.to.be.equal(
                    expectedText,
                    `Текст для незрячих должен содержать "${expectedText}"`
                );
            },
        }),
    },
});
