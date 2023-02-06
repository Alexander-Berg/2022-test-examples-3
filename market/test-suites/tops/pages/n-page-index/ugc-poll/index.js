import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {createShopInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';

import {SHOP_GRADE, MODEL_GRADE} from '@self/root/src/entities/agitation/constants';

// suites
import ProductReviewPopupFormSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductReviewPopupForm';
import ProductReviewPopupFormWithExtraTaskSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductReviewPopupForm/withExtraTasks';
import UgcPollShopSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/shop';
import UgcPollModelSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/model';
import UgcPollPaidModelSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/paidModel';
import entryPointToReferralProgram from '@self/root/src/spec/hermione/test-suites/blocks/entryPointToReferralProgram';
// page-objects
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ReviewPollScreen from '@self/platform/spec/page-objects/components/ReviewPollPopup/Screen';
import ReviewPollScreenManager from '@self/platform/spec/page-objects/components/ReviewPollPopup/Popup';
import ReviewPollShopGrade from '@self/platform/spec/page-objects/components/ReviewPollPopup/ShopGrade';
import ReviewPollProductGrade from '@self/platform/spec/page-objects/components/ReviewPollPopup/ProductGrade';
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import UserReview from '@self/platform/components/UserReview/__pageObject';
import ExpertiseMotivation from '@self/project/src/components/AgitationPollCard/ExpertiseMotivation/__pageObject';
import YaPlusReviewMotivation from '@self/root/src/components/YaPlusReviewMotivation/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

const PROFILE = profiles['pan-topinambur'];
const users = [createUser({
    ...PROFILE,
    uid: {
        value: PROFILE.uid,
    },
    id: PROFILE.uid,
})];

const DEFAULT_SHOP_INFO = {
    entity: 'shop',
    id: 123,
    name: 'myShopName',
    shopName: 'myShopName',
    slug: 'magazinzinzin',
};

const PRODUCT_ID = 321;
const CATEGORY_ID = 91491;

const PRODUCT_STATE = createProduct({
    slug: 'my-product-name',
    titles: {
        raw: 'myProductName',
    },
    categories: [
        {
            entity: 'category',
            id: CATEGORY_ID,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
            slug: 'mobilnye-telefony',
        },
    ],
    type: 'model',
    pictures: [],
    deletedId: null,
}, PRODUCT_ID);

const PAYMENT_OFFER_AMOUNT = 555;

async function prepareStateForCheckEntryPointToReferralProgramFromAgitationPopup() {
    await this.browser.setState('schema', {
        users,
        agitation: {
            [PROFILE.uid]: [{
                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                entityId: '321',
                type: MODEL_GRADE,
            }],
        },
        productFactors: {[CATEGORY_ID]: []},
    });

    await prepareStateForCheckEntryPointToReferralProgram.call(this);
}

async function prepareStateForCheckEntryPointToReferralProgramFromPaiedAgitationPopup() {
    await this.browser.setState('schema', {
        users,
        agitation: {
            [PROFILE.uid]: [{
                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                entityId: '321',
                type: MODEL_GRADE,
                data: {
                    persPayAvailable: '1',
                },
            }],
        },
        paymentOffer: [{
            amount: PAYMENT_OFFER_AMOUNT,
            entityId: String(PRODUCT_ID),
            entityType: 'MODEL_GRADE',
            userId: PROFILE.uid,
            userType: 'UID',
        }],
    });

    await prepareStateForCheckEntryPointToReferralProgram.call(this);
}

async function prepareStateForCheckEntryPointToReferralProgram() {
    const gainExpertise = createGainExpertise(0, 20, PROFILE.uid);
    await this.browser.setState('storage', {gainExpertise});
    await this.browser.setState('report', PRODUCT_STATE);

    await this.browser.yaProfile('pan-topinambur', 'market:index');

    await this.reviewPollScreen.waitForOpened();
    await this.productGradeRatingStars.setRating(4);
    await this.reviewForm.waitForVisible();

    const {reviewForm, browser} = this;

    await reviewForm
        .isVisible()
        .should.eventually.equal(true, 'Форма отзыва видна')
        .then(() => browser.yaScenario(this, 'productReviews.fillForm.firstStep'))
        .then(() => reviewForm.submitFirstStep())
        .then(() => reviewForm.waitForSecondStep())
        .then(() => reviewForm.submitSecondStep())
        .then(() => reviewForm.waitForInvisible());

    await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
        this.browser.waitForVisible(GainedExpertise.root, 5000)
    );
}

function getPageObjectsForEntryPointToReferralProgram() {
    return {
        reviewPollProductGrade() {
            return this.createPageObject(ReviewPollProductGrade);
        },
        productGradeRatingStars() {
            return this.createPageObject(RatingStars, {
                parent: this.reviewPollProductGrade,
                root: ReviewPollProductGrade.ratingStars,
            });
        },
        reviewForm() {
            return this.createPageObject(ProductReviewForm);
        },
        ratingStars() {
            return this.createPageObject(RatingStars, {parent: ProductReviewForm.rating});
        },
    };
}

export default makeSuite('UGC-опрос.', {
    environment: 'kadavr',
    issue: 'MARKETVERSTKA-33874',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    reviewPollScreen: () => this.createPageObject(ReviewPollScreen),
                    reviewPollScreenManager: () => this.createPageObject(ReviewPollScreenManager),
                    notification: () => this.createPageObject(Notification),
                });
                await this.browser.deleteCookie('ugcp');
            },
        },
        prepareSuite(UgcPollShopSuite, {
            pageObjects: {
                reviewPollShopGrade() {
                    return this.createPageObject(ReviewPollShopGrade);
                },
                shopGradeRatingStars() {
                    return this.createPageObject(RatingStars, {
                        parent: this.reviewPollShopGrade,
                        root: ReviewPollShopGrade.ratingStars,
                    });
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.reviewPollShopGrade,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie('ugcp');
                    const shopInfo = createShopInfo(DEFAULT_SHOP_INFO, DEFAULT_SHOP_INFO.id);

                    await this.browser
                        .setState('report', shopInfo)
                        .setState('ShopInfo.collections', shopInfo)
                        .setState('schema', {
                            users,
                            agitation: {
                                [PROFILE.uid]: [{
                                    id: `${SHOP_GRADE}-${DEFAULT_SHOP_INFO.id}`,
                                    entityId: String(DEFAULT_SHOP_INFO.id),
                                    type: SHOP_GRADE,
                                }],
                            },
                        });
                    return this.browser.yaProfile('pan-topinambur', 'market:index');
                },
            },
        }),
        prepareSuite(UgcPollModelSuite, {
            pageObjects: {
                reviewPollProductGrade() {
                    return this.createPageObject(ReviewPollProductGrade);
                },
                productGradeRatingStars() {
                    return this.createPageObject(RatingStars, {
                        parent: this.reviewPollProductGrade,
                        root: ReviewPollProductGrade.ratingStars,
                    });
                },
                reviewForm() {
                    return this.createPageObject(ProductReviewForm);
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.reviewPollProductGrade,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_GRADE,
                            }],
                        },
                    });
                    await this.browser.setState('report', PRODUCT_STATE);
                    return this.browser.yaProfile('pan-topinambur', 'market:index');
                },
            },
        }),
        prepareSuite(UgcPollPaidModelSuite, {
            pageObjects: {
                reviewPollProductGrade() {
                    return this.createPageObject(ReviewPollProductGrade);
                },
                yaPlusReviewMotivation() {
                    return this.createPageObject(YaPlusReviewMotivation, {
                        parent: this.reviewPollProductGrade,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_GRADE,
                                data: {
                                    persPayAvailable: '1',
                                },
                            }],
                        },
                        paymentOffer: [{
                            amount: PAYMENT_OFFER_AMOUNT,
                            entityId: String(PRODUCT_ID),
                            entityType: 'MODEL_GRADE',
                            userId: PROFILE.uid,
                            userType: 'UID',
                        }],
                    });
                    await this.browser.setState('report', PRODUCT_STATE);
                    return this.browser.yaProfile('pan-topinambur', 'market:index');
                },
            },
            params: {
                paymentOfferAmount: PAYMENT_OFFER_AMOUNT,
            },
        }),
        prepareSuite(ProductReviewPopupFormSuite, {
            pageObjects: {
                reviewPollProductGrade() {
                    return this.createPageObject(ReviewPollProductGrade);
                },
                productGradeRatingStars() {
                    return this.createPageObject(RatingStars, {
                        parent: this.reviewPollProductGrade,
                        root: ReviewPollProductGrade.ratingStars,
                    });
                },
                reviewForm() {
                    return this.createPageObject(ProductReviewForm);
                },
                reviewItem() {
                    return this.createPageObject(UserReview);
                },
            },
            hooks: {
                async beforeEach() {
                    const users = [createUser({
                        ...PROFILE,
                        uid: {
                            value: PROFILE.uid,
                        },
                        id: PROFILE.uid,
                    })];
                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_GRADE,
                            }],
                        },
                        productFactors: {
                            [CATEGORY_ID]: [
                                {id: 742, title: 'Экран'},
                                {id: 743, title: 'Камера'},
                                {id: 744, title: 'Время автономной работы'},
                                {id: 745, title: 'Объем памяти'},
                                {id: 746, title: 'Производительность'},
                            ],
                        },
                    });
                    await this.browser.setState('report', PRODUCT_STATE);
                    await this.browser.yaProfile('pan-topinambur', 'market:index');

                    await this.reviewPollScreen.waitForOpened();
                    await this.productGradeRatingStars.setRating(4);
                    return this.reviewForm.waitForVisible();
                },
            },
        }),
        prepareSuite(ProductReviewPopupFormWithExtraTaskSuite, {
            pageObjects: {
                reviewPollProductGrade() {
                    return this.createPageObject(ReviewPollProductGrade);
                },
                productGradeRatingStars() {
                    return this.createPageObject(RatingStars, {
                        parent: this.reviewPollProductGrade,
                        root: ReviewPollProductGrade.ratingStars,
                    });
                },
                reviewForm() {
                    return this.createPageObject(ProductReviewForm);
                },
                reviewItem() {
                    return this.createPageObject(UserReview);
                },
            },
            hooks: {
                async beforeEach() {
                    const gainExpertise = createGainExpertise(0, 20, PROFILE.uid);

                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_GRADE}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_GRADE,
                            }],
                        },
                        productFactors: {[CATEGORY_ID]: []},
                    });
                    await this.browser.setState('storage', {gainExpertise});
                    await this.browser.setState('report', PRODUCT_STATE);
                    await this.browser.yaProfile('pan-topinambur', 'market:index');

                    await this.reviewPollScreen.waitForOpened();
                    await this.productGradeRatingStars.setRating(4);
                    return this.reviewForm.waitForVisible();
                },
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Попап агитации. ' +
                'Точка входа в рефералку. ' +
                'Пользователь не достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4818',
            },
            pageObjects: getPageObjectsForEntryPointToReferralProgram.call(this),
            params: {
                linkLabel: 'Получить 300 баллов за друга',
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgramFromAgitationPopup,
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Попап агитации. ' +
                'Точка входа в рефералку. ' +
                'Пользователь достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4819',
            },
            pageObjects: getPageObjectsForEntryPointToReferralProgram.call(this),
            params: {
                linkLabel: 'Рекомендовать Маркет друзьям',
                isGotFullReward: true,
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgramFromAgitationPopup,
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Попап агитации за баллы. ' +
                'Точка входа в рефералку. ' +
                'Пользователь плюсовик не достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4838',
            },
            pageObjects: getPageObjectsForEntryPointToReferralProgram.call(this),
            params: {
                isYaPlus: true,
                linkLabel: 'Получить 300 баллов за друга',
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgramFromPaiedAgitationPopup,
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Попап агитации за баллы. ' +
                'Точка входа в рефералку. ' +
                'Пользователь плюсовик достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4839',
            },
            pageObjects: getPageObjectsForEntryPointToReferralProgram.call(this),
            params: {
                isYaPlus: true,
                linkLabel: 'Рекомендовать Маркет друзьям',
                isGotFullReward: true,
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgramFromPaiedAgitationPopup,
            },
        }),
        prepareSuite(entryPointToReferralProgram({shouldShowLink: false}), {
            suiteName:
                'Попап агитации за баллы. ' +
                'Точка входа в рефералку. ' +
                'Пользователь неплюсовик.',
            pageObjects: getPageObjectsForEntryPointToReferralProgram.call(this),
            params: {
                isYaPlus: false,
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgramFromPaiedAgitationPopup,
            },
        })
    ),
});
