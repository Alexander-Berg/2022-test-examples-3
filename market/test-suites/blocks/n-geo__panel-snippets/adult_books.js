import {makeSuite, makeCase} from 'ginny';
import {castArray} from 'ambar';

const ADULT_COOKIE_NAME = 'adult';

/**
 * @param {PageObject.GeoSnippet} geoSnippet
 * @param {PageObject.ProductWarinings} productWarnings
 */
export default makeSuite('Панель со сниппетами.', {
    feature: 'Adult книги',
    environment: 'kadavr',
    story: {
        'Без установленной adult куки': {
            async beforeEach() {
                await this.browser.deleteCookie(ADULT_COOKIE_NAME)
                    .then(() => this.browser.refresh());

                return this.geoSnippet.waitForVisible();
            },
            'есть как минимум одна плашка 18+ на выдаче.': makeCase({
                id: 'marketfront-1099',
                issue: 'MARKETVERSTKA-25307',
                test() {
                    return this.productWarnings.getWarningsByAge(18)
                        .then(result => result.value.length)
                        .should.eventually.to.be.at.least(1, 'Должна выводится хотя бы одна плашка.');
                },
            }),
            'есть как минимум одно предупреждение о возрастном ограничении.': makeCase({
                id: 'marketfront-1099',
                issue: 'MARKETVERSTKA-25307',
                test() {
                    return this.productWarnings.getWarningsByAge(18).getText()
                        .then(warningText => castArray(warningText))
                        .should.eventually.to.be.an('array').that.does.include(
                            'Возрастное ограничение',
                            'Должно выводиться хотя бы одно предупреждение.'
                        );
                },
            }),
        },
    },
});
