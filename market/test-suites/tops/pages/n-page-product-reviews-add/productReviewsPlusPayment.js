import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import ReviewCashbackDisclaimerVisibleSuite
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewCashbackDisclaimer/visible';
import ReviewCashbackDisclaimerNotVisibleSuite
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewCashbackDisclaimer/notVisible';

// page-objects
import ProductReviewForm
    from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';

// utils
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

const productId = 12345;
const slug = 'random-fake-slug';
const testUser = profiles.ugctest3;

async function mockPlusCashback(browser) {
    await browser.setState('schema', {
        paymentOffer: [{
            amount: 555,
            entityId: String(productId),
            entityType: 'MODEL_GRADE',
            userId: testUser.uid,
            userType: 'UID',
        }],
    });
}

const plusPaymentQueryParamSuite = makeSuite('Если query-параметр plusPayment задан', {
    story: {
        'Если есть баллы Плюса.': prepareSuite(ReviewCashbackDisclaimerNotVisibleSuite, {
            hooks: {
                async beforeEach() {
                    await mockPlusCashback(this.browser);

                    return this.browser.yaOpenPage('market:product-reviews-add', {
                        productId,
                        slug,
                        plusPayment: '1',
                    });
                },
            },
        }),
        'Если нет баллов Плюса.': prepareSuite(ReviewCashbackDisclaimerVisibleSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaOpenPage('market:product-reviews-add', {
                        productId,
                        slug,
                        plusPayment: '1',
                    });
                },
            },
        }),
    },
});

const plusPaymentNoQueryParamSuite = makeSuite('Если query-параметр plusPayment не задан', {
    story: {
        'Если есть баллы Плюса.': prepareSuite(ReviewCashbackDisclaimerNotVisibleSuite, {
            hooks: {
                async beforeEach() {
                    await mockPlusCashback(this.browser);

                    return this.browser.yaOpenPage('market:product-reviews-add', {
                        productId,
                        slug,
                    });
                },
            },
        }),
        'Если нет баллов Плюса.': prepareSuite(ReviewCashbackDisclaimerNotVisibleSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaOpenPage('market:product-reviews-add', {
                        productId,
                        slug,
                    });
                },
            },
        }),
    },
});

export default makeSuite('Баллы плюса', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    productReviewForm: () => this.createPageObject(
                        ProductReviewForm
                    ),
                });

                const productMock = createProduct({
                    type: 'model',
                    categories: [{
                        id: 123,
                    }],
                    slug,
                    deletedId: null,
                }, productId);

                await this.browser.setState('report', productMock);

                await this.browser.yaLogin(testUser.login, testUser.password);
            },
        },
        makeSuite('Если пользователь не выставлял оценку товару.', {
            story: mergeSuites(
                plusPaymentQueryParamSuite,
                plusPaymentNoQueryParamSuite
            ),
        })
    ),
});
