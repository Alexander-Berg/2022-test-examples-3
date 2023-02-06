import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {createShopInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {SHOP_GRADE, MODEL_GRADE, MODEL_VIDEO, MODEL_GRADE_TEXT} from '@self/root/src/entities/agitation/constants';

// suites
import UgcPollShopSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/shop';
import UgcPollModelSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/model';
import UgcPollPaidModelSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/paidModel';
import UgcPollModelVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/modelVideo';
import UgcPollModelTextSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcPoll/modelText';
import entryPointToReferralProgram from '@self/root/src/spec/hermione/test-suites/blocks/entryPointToReferralProgram';
// page-objects
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
import ReviewPollPopup from '@self/platform/widgets/parts/ReviewPollPopup/__pageObject';
import VideoAgitationScreen from '@self/platform/widgets/parts/ReviewPollPopup/components/VideoAgitationScreen/__pageObject';
import AgitationPollCard from '@self/project/src/components/AgitationPollCard/__pageObject';
import ExpertiseMotivation from '@self/project/src/components/AgitationPollCard/ExpertiseMotivation/__pageObject';
import YaPlusReviewMotivation from '@self/root/src/components/YaPlusReviewMotivation/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Button from '@self/platform/spec/page-objects/levitan-gui/Button';
import ProductMainFields from '@self/platform/components/ReviewForm/ProductMainFields/__pageObject';
import ShopMainFields from '@self/platform/components/ReviewForm/ShopMainFields/__pageObject';
import ProductReviewNew from '@self/platform/spec/page-objects/widgets/parts/ProductReviewNew';
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
const PRODUCT_SLUG = 'my-product-name';

const PRODUCT_STATE = createProduct({
    slug: PRODUCT_SLUG,
    titles: {
        raw: 'myProductName',
    },
    categories: [
        {
            entity: 'category',
            id: 91491,
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
const CATEGORY_ID = 91491;

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

    await this.reviewPollPopup.waitForOpened();
    await this.ratingInput.setRating(4);
    await this.productReviewNew.setCommentTextField('test comment text');
    await this.productReviewNew.clickNextStepButton();
    await this.browser.allure.runStep('Ждем появления кнопки', () =>
        this.browser.waitForVisible(ProductReviewNew.nextStepButton, 5000)
    );
    await this.productReviewNew.clickSubmitButton();
    await this.setPageObjects({
        gainedExpertise: () => this.createPageObject(GainedExpertise),
    });
    await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
        this.browser.waitForVisible(GainedExpertise.root, 5000)
    );
    await this.browser.allure.runStep('Ждем появления кнопки "Отлично"', () =>
        this.browser.waitForVisible(GainedExpertise.closeButton, 5000)
    );
}

function getPageObjectsForEntryPointToReferralProgram() {
    return {
        productReviewNew() {
            return this.createPageObject(ProductReviewNew);
        },
        ratingInput() {
            return this.createPageObject(RatingInput);
        },
    };
}

export default makeSuite('UGC-опрос.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-39494',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    reviewPollPopup: () => this.createPageObject(ReviewPollPopup),
                    notification: () => this.createPageObject(Notification),
                });
                await this.browser.deleteCookie('ugcp');
            },
        },
        prepareSuite(UgcPollShopSuite, {
            pageObjects: {
                agitationPollCard() {
                    return this.createPageObject(AgitationPollCard);
                },
                shopGradeRatingStars() {
                    return this.createPageObject(RatingInput, {
                        parent: this.agitationPollCard,
                    });
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.agitationPollCard,
                    });
                },
                shopMainFields() {
                    return this.createPageObject(ShopMainFields);
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
                    return this.browser.yaProfile('pan-topinambur', 'touch:index');
                },
            },
        }),
        prepareSuite(UgcPollModelSuite, {
            pageObjects: {
                agitationPollCard() {
                    return this.createPageObject(AgitationPollCard);
                },
                productGradeRatingStars() {
                    return this.createPageObject(RatingInput, {
                        parent: this.agitationPollCard,
                    });
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.agitationPollCard,
                    });
                },
                productMainFields() {
                    return this.createPageObject(ProductMainFields);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie('ugcp');
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
                    return this.browser.yaProfile('pan-topinambur', 'touch:index');
                },
            },
        }),
        prepareSuite(UgcPollPaidModelSuite, {
            pageObjects: {
                agitationPollCard() {
                    return this.createPageObject(AgitationPollCard);
                },
                yaPlusReviewMotivation() {
                    return this.createPageObject(YaPlusReviewMotivation, {
                        parent: this.agitationPollCard,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie('ugcp');
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
                    return this.browser.yaProfile('pan-topinambur', 'touch:index');
                },
            },
            params: {
                paymentOfferAmount: PAYMENT_OFFER_AMOUNT,
            },
        }),
        prepareSuite(UgcPollModelVideoSuite, {
            pageObjects: {
                agitationPollCard() {
                    return this.createPageObject(AgitationPollCard);
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.agitationPollCard,
                    });
                },
                videoAgitationScreen() {
                    return this.createPageObject(VideoAgitationScreen);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie('ugcp');
                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_VIDEO}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_VIDEO,
                            }],
                        },
                    });
                    await this.browser.setState('report', PRODUCT_STATE);
                    return this.browser.yaProfile('pan-topinambur', 'touch:index');
                },
            },
            params: {
                productId: PRODUCT_ID,
                slug: PRODUCT_SLUG,
            },
        }),
        prepareSuite(UgcPollModelTextSuite, {
            pageObjects: {
                agitationPollCard() {
                    return this.createPageObject(AgitationPollCard);
                },
                expertiseMotivation() {
                    return this.createPageObject(ExpertiseMotivation, {
                        parent: this.agitationPollCard,
                    });
                },
                addTextButton() {
                    return this.createPageObject(Button, {
                        parent: this.agitationPollCard,
                    });
                },
                productMainFields() {
                    return this.createPageObject(ProductMainFields);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie('ugcp');
                    await this.browser.setState('schema', {
                        users,
                        agitation: {
                            [PROFILE.uid]: [{
                                id: `${MODEL_GRADE_TEXT}-${PRODUCT_ID}`,
                                entityId: '321',
                                type: MODEL_GRADE_TEXT,
                            }],
                        },
                        gradesOpinions: [{
                            type: 1,
                            product: {id: PRODUCT_ID},
                            user: {uid: PROFILE.uid},
                            averageGrade: 3,
                            pro: '',
                            contra: '',
                            comment: '',
                        }],
                    });
                    await this.browser.setState('report', PRODUCT_STATE);
                    return this.browser.yaProfile('pan-topinambur', 'touch:index');
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
        })
    ),
});
