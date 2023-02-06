import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createProduct, createSku, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createShopInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';

import {MODEL_GRADE, SHOP_GRADE, MODEL_VIDEO, MODEL_GRADE_TEXT} from '@self/root/src/entities/agitation/constants';

// configs
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';
import {profiles} from '@self/platform/spec/hermione/configs/profiles.js';

// suites
import UserTasksZerroStateSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserTasksSuite/zerroState';
import QuestionAgitationCardSuite from '@self/platform/spec/hermione/test-suites/blocks/components/QuestionAgitationCard';
import OnePageQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/QuestionAgitationsSuite/onePageQuestions';
import TwoPageQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/QuestionAgitationsSuite/twoPagesQuestions';
import ThreePageQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/QuestionAgitationsSuite/threePagesQuestions';
import OnePagePollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPollsSuite/onePagePolls';
import OnePageSkuPollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPollsSuite/onePageSkuPolls';
import TwoPagePollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPollsSuite/twoPagePolls';
import ThreePagePollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPollsSuite/threePagePolls';
import NoPollsSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ReviewPollsSuite/noPolls';
import ReviewPollCardModelSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ReviewPollCard/model';
import ReviewPollCardPaidModelSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ReviewPollCard/paidModel';
import ReviewPollCardPaidModelTextSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ReviewPollCard/paidModelText';
import ReviewPollCardShopSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ReviewPollCard/shop';
import NextAchievementSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/NextAchievement';
import GainedExpertiseSuite from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/GainedExpertise';

// page-objects
import QuestionAgitationCard from '@self/platform/components/QuestionAgitationCard/__pageObject';
import QuestionAgitations from '@self/platform/widgets/content/QuestionAgitations/__pageObject';
import UserTasks from '@self/platform/widgets/parts/UserTasks/__pageObject';
import ReviewPolls from '@self/platform/widgets/content/ReviewPolls/__pageObject';
import NextAchievement from '@self/platform/widgets/content/NextAchievement/__pageObject';
import ReviewPollCard from '@self/platform/components/ReviewPollCard/__pageObject';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import AgitationCard from '@self/platform/components/AgitationCard/__pageObject';
import AgitationCardMenu from '@self/platform/components/AgitationCardMenu/__pageObject';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';
import YaPlusReviewMotivation from '@self/root/src/components/YaPlusReviewMotivation/__pageObject';

const ONE_PAGE_QUESTIONS = 3;
const TWO_PAGES_QUESTIONS = 4;
const THREE_PAGES_QUESTIONS = 7;

const ONE_PAGE_POLLS = 3;
const TWO_PAGES_POLLS = 4;
const THREE_PAGES_POLLS = 7;

const userUid = profiles.ugctest3.uid;
const currentUser = createUser({
    id: userUid,
    uid: {
        value: userUid,
    },
    login: profiles.ugctest3.login,
    public_id: profiles.ugctest3.publicId,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
});

const DEFAULT_PRODUCT = {
    id: 14206682, // id продукта совпадает с id productWithPicture,
    slug: 'smartfon-apple-iphone-7-128gb', // slug продукта совпадает с slug productWithPicture
    name: 'Тестовый телефон',
};

const DEFAULT_SHOP_INFO = {
    entity: 'shop',
    id: 321,
    shopName: 'Магазин Зин-зин',
    slug: 'magazinzinzin',
};

const prepareAgitationQuestions = async (ctx, params = {}) => {
    const users = [...Array(params.questionsCount || 1)].map((_, index) => createUser({
        id: String(index + 1),
        uid: {
            value: String(index + 1),
        },
        login: String(index + 1),
        public_id: `p${(index + 1)}`,
        display_name: {
            public_name: params.questionAuthor || `Question Author ${index + 1}`,
        },
    }));

    users.push(currentUser);

    const modelQuestions = [...Array(params.questionsCount || 1)].map((_, index) => {
        const settings = {
            id: index + 1,
            slug: `question${index + 1}`,
            user: {entity: 'user', uid: String(index + 1)},
            author: {entity: 'user', id: String(index + 1)},
            product: {entity: 'product', id: String(DEFAULT_PRODUCT.id)},
        };
        params.question && (settings.text = params.question);
        return settings;
    });

    const userAgitations = [...Array(params.questionsCount || 1)].map((_, index) => ({
        id: `5-${index + 1}`,
        entityId: `${index + 1}`,
        type: 5,
    }));

    await ctx.browser
        .setState('report', productWithPicture)
        .setState('schema', {
            users,
            modelQuestions,
            agitation: {
                [userUid]: userAgitations,
            },
        });

    await ctx.browser.yaProfile('ugctest3', 'market:my-tasks');

    return ctx.browser.yaWaitForPageReady();
};

const prepareUserPoll = async (ctx, params = {}) => {
    const productReviewAgitation = {
        id: `${params.type}-${DEFAULT_PRODUCT.id}`,
        entityId: String(DEFAULT_PRODUCT.id),
        type: params.type,
        data: {
            ...(params.paymentOfferAmount ? {persPayAvailable: '1'} : {}),
        },
    };

    const shopReviewAgitation = {
        id: `${SHOP_GRADE}-123`,
        entityId: String(DEFAULT_SHOP_INFO.id),
        type: SHOP_GRADE,
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
    const shopGradeOpinion = {
        type: 0,
        shop: {id: DEFAULT_SHOP_INFO.id},
        user: {uid: currentUser.id},
        averageGrade: 3,
    };

    let gradesOpinions = [];
    if (params.withGrade) {
        if (params.type === MODEL_GRADE || params.type === MODEL_GRADE_TEXT) {
            gradesOpinions = [productGradeOpinion];
        }
        if (params.type === SHOP_GRADE) {
            gradesOpinions = [shopGradeOpinion];
        }
    }

    await ctx.browser
        .setState('report', productWithPicture)
        .setState('ShopInfo.collections', createShopInfo(DEFAULT_SHOP_INFO, DEFAULT_SHOP_INFO.id))
        .setState('schema', {
            gradesOpinions,
            users: [currentUser],
            agitation: {
                [userUid]: [(params.type === MODEL_GRADE || params.type === MODEL_GRADE_TEXT ? productReviewAgitation : shopReviewAgitation)],
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

const prepareUserPolls = async (ctx, params = {}) => {
    const products = [...Array(params.polls || 1)].map(
        (_, index) => createProduct({slug: 'product'}, String(index + 1))
    );
    await ctx.browser
        .setState('report', mergeState(products))
        .setState('schema', {
            users: [currentUser],
            agitation: {
                [userUid]: [...Array(params.polls || 1)].map((_, index) => ({
                    id: `${MODEL_GRADE}-123${index}`,
                    entityId: String(index + 1),
                    type: MODEL_GRADE,
                })),
            },
        });

    await ctx.browser.yaProfile('ugctest3', 'market:my-tasks');

    return ctx.browser.yaWaitForPageReady();
};

const SKU_ID = '1234567890';
const SKU_TITLE = 'SKU TITLE';

const prepareUserPollsWithSku = async ctx => {
    const products = [createProduct({slug: 'product'}, '1')];

    const skuTitles = {raw: SKU_TITLE};

    const skus = [
        createSku({titles: skuTitles}, SKU_ID),
    ];

    await ctx.browser
        .setState('report', mergeState([...products, ...skus]))
        .setState('schema', {
            users: [currentUser],
            agitation: {
                [userUid]: [
                    {
                        id: `${MODEL_GRADE}-1231`,
                        entityId: '1',
                        type: MODEL_GRADE,
                        data: {
                            sku: SKU_ID,
                        },
                    },
                ],
            },
        });

    await ctx.browser.yaProfile('ugctest3', 'market:my-tasks');

    return ctx.browser.yaWaitForPageReady();
};

async function prepareExpertise(ctx, gradeType, gainedValue) {
    const gainExpertise = createGainExpertise(gradeType, gainedValue, profiles.ugctest3.uid);
    await ctx.browser.setState('storage', {gainExpertise});
    await ctx.browser.yaProfile(profiles.ugctest3.login, 'market:index');
    await ctx.browser.yaOpenPage('market:my-tasks');
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Личный кабинет. Вкладка с заданиями пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-8139',
    story: {
        'Пользователь написал 10 отзыв c фотографиями.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        nextAchievement: () => this.createPageObject(NextAchievement),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(NextAchievementSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser
                            .setState('schema', {
                                users: [currentUser],
                                userAchievements: {
                                    [userUid]: [{
                                        totalEventsCount: 10,
                                        achievementId: 1,
                                        pendingEventsCount: 0,
                                        confirmedEventsCount: 10,
                                    },
                                    {
                                        totalEventsCount: 10,
                                        achievementId: 0,
                                        pendingEventsCount: 0,
                                        confirmedEventsCount: 10,
                                    }],
                                },
                            });

                        await this.browser.yaProfile('ugctest3', 'market:my-tasks');

                        return this.browser.yaWaitForPageReady();
                    },
                },
                params: {
                    nextAchievementName: 'Гуру фотографии',
                    nextAchievementDescription: 'Выдаётся за 50 отзывов на товары с фотографиями',
                    nextAchievementProgress: '10/50',
                },
            })
        ),
        'Есть задания - только агитация оценок.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        reviewPolls: () => this.createPageObject(ReviewPolls),
                        reviewPollCard: () => this.createPageObject(
                            ReviewPollCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                        ratingStars: () => this.createPageObject(
                            RatingStars,
                            {
                                parent: this.reviewPollCard,
                            }
                        ),
                        agitationCard: () => this.createPageObject(
                            AgitationCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                        agitationCardMenu: () => this.createPageObject(
                            AgitationCardMenu,
                            {
                                parent: this.agitationCard,
                            }
                        ),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            {
                'Одно задание, но у товара уже есть оценка пользователя.': prepareSuite(NoPollsSuite, {
                    hooks: {
                        beforeEach() {
                            return prepareUserPoll(this, {type: MODEL_GRADE, withGrade: true});
                        },
                    },
                }),
                'Одно задание, но у магазина уже есть оценка пользователя.': prepareSuite(NoPollsSuite, {
                    hooks: {
                        beforeEach() {
                            return prepareUserPoll(this, {type: SHOP_GRADE, withGrade: true});
                        },
                    },
                }),
            },
            prepareSuite(OnePageSkuPollsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareUserPollsWithSku(this);
                    },
                },
                params: {
                    polls: 1,
                    title: SKU_TITLE,
                },
            }),
            prepareSuite(OnePagePollsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareUserPolls(this, {polls: ONE_PAGE_POLLS});
                    },
                },
                params: {
                    polls: ONE_PAGE_POLLS,
                },
            }),
            prepareSuite(TwoPagePollsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareUserPolls(this, {polls: TWO_PAGES_POLLS});
                    },
                },
                params: {
                    polls: TWO_PAGES_POLLS,
                },
            }),
            prepareSuite(ThreePagePollsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareUserPolls(this, {polls: THREE_PAGES_POLLS});
                    },
                },
                params: {
                    polls: THREE_PAGES_POLLS,
                },
            }),
            prepareSuite(ReviewPollCardShopSuite, {
                hooks: {
                    async beforeEach() {
                        this.params = {
                            ...this.params,
                            reviewUrl: await this.browser.yaBuildURL(
                                'market:shop-reviews-add', {
                                    shopId: DEFAULT_SHOP_INFO.id,
                                    slug: DEFAULT_SHOP_INFO.slug,
                                }
                            ),
                            entityUrl: await this.browser.yaBuildURL(
                                'market:shop', {
                                    shopId: DEFAULT_SHOP_INFO.id,
                                    slug: DEFAULT_SHOP_INFO.slug,
                                }
                            ),
                        };

                        return prepareUserPoll(this, {type: SHOP_GRADE});
                    },
                },
                params: {
                    ratingHeading: 'Оцените магазин',
                    expectedAverageGrade: 3,
                },
            }),
            prepareSuite(ReviewPollCardModelSuite, {
                hooks: {
                    async beforeEach() {
                        this.params = {
                            ...this.params,
                            reviewUrl: await this.browser.yaBuildURL(
                                'market:product-reviews-add', {
                                    productId: DEFAULT_PRODUCT.id,
                                    slug: DEFAULT_PRODUCT.slug,
                                }
                            ),
                            entityUrl: await this.browser.yaBuildURL(
                                'market:product', {
                                    productId: DEFAULT_PRODUCT.id,
                                    slug: DEFAULT_PRODUCT.slug,
                                }
                            ),
                        };
                        return prepareUserPoll(this, {type: MODEL_GRADE});
                    },
                },
                params: {
                    ratingHeading: 'Оцените товар',
                    expectedAverageGrade: 3,
                },
            }),
            prepareSuite(ReviewPollCardPaidModelSuite, {
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
        'Есть задания - только агитация написать отзыв на товар.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        reviewPolls: () => this.createPageObject(ReviewPolls),
                        reviewPollCard: () => this.createPageObject(
                            ReviewPollCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                        ratingStars: () => this.createPageObject(
                            RatingStars,
                            {
                                parent: this.reviewPollCard,
                            }
                        ),
                        agitationCard: () => this.createPageObject(
                            AgitationCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                        agitationCardMenu: () => this.createPageObject(
                            AgitationCardMenu,
                            {
                                parent: this.agitationCard,
                            }
                        ),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(ReviewPollCardPaidModelTextSuite, {
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
        'Нет заданий (ни оценок, ни вопросов).': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        userTasks: () => this.createPageObject(UserTasks),
                        questionAgitations: () => this.createPageObject(QuestionAgitations),
                        reviewPolls: () => this.createPageObject(ReviewPolls),
                        nextAchievement: () => this.createPageObject(NextAchievement),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(UserTasksZerroStateSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [currentUser],
                        });

                        await this.browser.yaProfile('ugctest3', 'market:my-tasks');

                        return this.browser.yaWaitForPageReady();
                    },
                },
                params: {
                    title: 'Пока у вас нет заданий',
                    subtitle: 'Начните публиковать на Маркете отзывы и отвечайте на' +
                        ' вопросы пользователей, тогда мы предложим вам новые задания.',
                },
            })
        ),
        'Есть задания - только вопросы других пользователей.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        questionAgitations: () => this.createPageObject(QuestionAgitations),
                        questionAgitationCard: () => this.createPageObject(
                            QuestionAgitationCard,
                            {
                                parent: this.questionAgitations,
                            }
                        ),
                        agitationCard: () => this.createPageObject(
                            AgitationCard,
                            {
                                parent: this.reviewPolls,
                            }
                        ),
                        agitationCardMenu: () => this.createPageObject(
                            AgitationCardMenu,
                            {
                                parent: this.agitationCard,
                            }
                        ),
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            prepareSuite(QuestionAgitationCardSuite, {
                hooks: {
                    async beforeEach() {
                        const questionParams = {
                            question: 'todo',
                            questionAuthor: 'WTF',
                        };
                        this.params = {
                            ...this.params,
                            ...questionParams,
                            productUrl: await this.browser.yaBuildURL('market:product', {
                                slug: DEFAULT_PRODUCT.slug,
                                productId: DEFAULT_PRODUCT.id,
                            }),
                            productName: DEFAULT_PRODUCT.name,
                            questionUrl: await this.browser.yaBuildURL('market:product-question', {
                                productId: DEFAULT_PRODUCT.id,
                                productSlug: DEFAULT_PRODUCT.slug,
                                questionId: 1,
                                questionSlug: 'question1',
                            }),
                        };
                        return prepareAgitationQuestions(this, questionParams);
                    },
                },
            }),
            prepareSuite(OnePageQuestionsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareAgitationQuestions(this, {questionsCount: ONE_PAGE_QUESTIONS});
                    },
                },
                params: {
                    questionsCount: ONE_PAGE_QUESTIONS,
                },
            }),
            prepareSuite(TwoPageQuestionsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareAgitationQuestions(this, {questionsCount: TWO_PAGES_QUESTIONS});
                    },
                },
                params: {
                    questionsCount: TWO_PAGES_QUESTIONS,
                },
            }),
            prepareSuite(ThreePageQuestionsSuite, {
                hooks: {
                    beforeEach() {
                        return prepareAgitationQuestions(this, {questionsCount: THREE_PAGES_QUESTIONS});
                    },
                },
                params: {
                    questionsCount: THREE_PAGES_QUESTIONS,
                },
            })
        ),
        'Пользователю выдана новая экспертность за оценку модели.': prepareSuite(GainedExpertiseSuite, {
            pageObjects: {
                gainedExpertise() {
                    return this.createPageObject(GainedExpertise);
                },
            },
            hooks: {
                async beforeEach() {
                    await prepareExpertise(this, MODEL_GRADE, 3);
                },
            },
            params: {
                expectedBadgeText: '',
            },
        }),
        'Пользователю выдана новая экспертность за оценку магазина.': prepareSuite(GainedExpertiseSuite, {
            pageObjects: {
                gainedExpertise() {
                    return this.createPageObject(GainedExpertise);
                },
            },
            hooks: {
                async beforeEach() {
                    await prepareExpertise(this, SHOP_GRADE, 1);
                },
            },
            params: {
                expectedBadgeText: '',
            },
        }),
        'Пользователю выдана новая экспертность за добавленное видео.': prepareSuite(GainedExpertiseSuite, {
            pageObjects: {
                gainedExpertise() {
                    return this.createPageObject(GainedExpertise);
                },
            },
            hooks: {
                async beforeEach() {
                    await prepareExpertise(this, MODEL_VIDEO, 50);
                },
            },
            params: {
                expectedBadgeText: 'Вы достигли 2 уровня',
            },
        }),
    },
});
