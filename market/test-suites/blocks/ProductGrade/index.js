import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import RatingChubbyStarsSuite from '@self/platform/spec/hermione/test-suites/blocks/RatingChubbyStars';
import RatingChubbyStars from '@self/platform/spec/page-objects/RatingChubbyStars';

/**
 * Тест на компонент с оценкой о продукте.
 * @param {PageObject.ProductGrade} productGrade
 */
export default makeSuite('Блок с оценкой на продукт.', {
    story: mergeSuites(
        prepareSuite(RatingChubbyStarsSuite, {
            pageObjects: {
                stars() {
                    return this.createPageObject(RatingChubbyStars, {
                        parent: this.productGrade,
                    });
                },
            },
        })
    ),
});
