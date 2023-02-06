import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createProduct, createSku, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {MODEL_GRADE, MODEL_GRADE_TEXT} from '@self/root/src/entities/agitation/constants';

import OnePageSkuPollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPolls/onePageSkuPolls';
import ReviewPollsPaidModelSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPolls/paidModel';
import ReviewPollsPaidModelTextSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPolls/paidModelText';

import ReviewPolls from '@self/platform/widgets/content/ReviewPolls/__pageObject';
import AgitationCard from '@self/platform/components/AgitationCard/__pageObject';
import YaPlusReviewMotivation from '@self/root/src/components/YaPlusReviewMotivation/__pageObject';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
import ReviewPollCard from '@self/platform/widgets/content/ReviewPolls/components/ReviewPollCard/__pageObject';

const userUid = '636368980';
const currentUser = createUser({
    id: userUid,
    uid: {
        value: userUid,
    },
    login: 'ugctest3',
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
});

const ONE_PAGE_POLLS = 1;

const SKU_ID = '1234567890';
const SKU_TITLE = 'SKU TITLE';

const DEFAULT_PRODUCT = {
    id: 14206682,
    slug: 'smartfon-apple-iphone-7-128gb',
    name: 'Тестовый телефон',
};

const prepareUserPoll = async (ctx, params = {}) => {
    const productReviewAgitation = {
        id: `${params.type}-${DEFAULT_PRODUCT.id}`,
        entityId: String(DEFAULT_PRODUCT.id),
        type: params.type,
        data: {
            ...(params.paymentOfferAmount ? {persPayAvailable: '1'} : {}),
            ...(params.skuId ? {sku: params.skuId} : {}),
        },
    };
    const productGradeOpinion = {
        type: 1,
        product: {id: DEFAULT_PRODUCT.id},
        user: {uid: currentUser.id},
        averageGrade: 3,
        pro: '',
        contra: '',
        comment: '',
    };

    let gradesOpinions = [];
    if (params.withGrade) {
        if (params.type === MODEL_GRADE || params.type === MODEL_GRADE_TEXT) {
            gradesOpinions = [productGradeOpinion];
        }
    }

    const products = [
        createProduct({slug: 'product'}, DEFAULT_PRODUCT.id),
    ];
    const skus = params.skuId ? [
        createSku({titles: {raw: SKU_TITLE}}, SKU_ID),
    ] : [];

    await ctx.browser
        .setState('report', mergeState([...products, ...skus]))
        .setState('schema', {
            gradesOpinions,
            users: [currentUser],
            agitation: {
                [userUid]: [(params.type === MODEL_GRADE || params.type === MODEL_GRADE_TEXT ? productReviewAgitation : null)],
            },
            ...(params.paymentOfferAmount && (params.type === MODEL_GRADE || params.type === MODEL_GRADE_TEXT) ? {
                paymentOffer: [{
                    amount: params.paymentOfferAmount,
                    entityId: String(DEFAULT_PRODUCT.id),
                    entityType: 'MODEL_GRADE',
                    userId: userUid,
                    userType: 'UID',
                }],
            } : {}),
        });

    await ctx.browser.yaProfile('ugctest3', 'market:my-tasks');

    return ctx.browser.yaWaitForPageReady();
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Личный кабинет. Вкладка с заданиями пользователя.', {
    'environment': 'kadavr',
    'story': {
        'Есть задания - только агитация оценок.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        reviewPolls: () => this.createPageObject(ReviewPolls),
                        agitationCard: () => this.createPageObject(AgitationCard),
                        reviewPollCard: () => this.createPageObject(
                            ReviewPollCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(OnePageSkuPollsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareUserPoll(this, {
                            skuId: SKU_ID,
                            type: MODEL_GRADE,
                        });
                    },
                },
                params: {
                    polls: ONE_PAGE_POLLS,
                    title: SKU_TITLE,
                },
            }),
            prepareSuite(ReviewPollsPaidModelSuite, {
                pageObjects: {
                    yaPlusReviewMotivation() {
                        return this.createPageObject(YaPlusReviewMotivation, {
                            parent: this.agitationCard,
                        });
                    },
                    ratingStars() {
                        return this.createPageObject(RatingInput, {
                            parent: this.reviewPollCard,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        return prepareUserPoll(this, {
                            type: MODEL_GRADE,
                            paymentOfferAmount: 555,
                        });
                    },
                },
                params: {
                    paymentOfferAmount: 555,
                    agitationId: `${MODEL_GRADE}-${DEFAULT_PRODUCT.id}`,
                    productId: String(DEFAULT_PRODUCT.id),
                },
            })
        ),
        'Есть задания - только агитация оставить тектовый отзыв на товар.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        reviewPolls: () => this.createPageObject(ReviewPolls),
                        agitationCard: () => this.createPageObject(AgitationCard),
                        reviewPollCard: () => this.createPageObject(
                            ReviewPollCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(ReviewPollsPaidModelTextSuite, {
                pageObjects: {
                    yaPlusReviewMotivation() {
                        return this.createPageObject(YaPlusReviewMotivation, {
                            parent: this.agitationCard,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        return prepareUserPoll(this, {
                            type: MODEL_GRADE_TEXT,
                            paymentOfferAmount: 555,
                            withGrade: true,
                        });
                    },
                },
                params: {
                    paymentOfferAmount: 555,
                    agitationId: `${MODEL_GRADE_TEXT}-${DEFAULT_PRODUCT.id}`,
                    productId: String(DEFAULT_PRODUCT.id),
                },
            })
        ),
    },
});
