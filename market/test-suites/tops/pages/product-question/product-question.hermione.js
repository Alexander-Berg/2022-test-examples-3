import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {helpers} from '@yandex-market/kadavr';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-question-page';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import ProductAnswersListTenMoreAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductAnswersList/tenMoreAnswers';
import VideoCarouselSuite from '@self/platform/spec/hermione/test-suites/blocks/VideoCarousel';
import ProductAnswersListTenLessAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductAnswersList/tenLessAnswers';
import QuestionCardRemovableQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/removableQuestion';
import QuestionCardNewQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/newQuestion';
import QuestionCardVotesSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/Votes';
import QuestionCardSubscribeSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/subscribe';
import QuestionCardUnsubscribeSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/unsubscribe';
import AnswerSnippetNewAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/newAnswer';
import QuestionCardProductSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/ProductSnippet';
import QuestionFormSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm';
import QuestionFormActionButtonAuthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/ActionButton/authorizedUser';
import QuestionFormActionButtonUnauthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/ActionButton/unauthorizedUser';
import AnswerSnippetVotesUnauthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/unauthorizedUser';
import AnswerSnippetVotesAuthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/authorizedUser';
import ShopAnswerShopExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/shopExists';
import ShopAnswerShopNotExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/shopNotExists';
import ShopAnswerCreationDateSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/creationDate';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import AnswerSnippet from '@self/platform/components/Question/AnswerSnippet/__pageObject__';
import Base from '@self/platform/spec/page-objects/n-base';
import QuestionForm from '@self/platform/spec/page-objects/QuestionForm';
import QuestionCard from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/QuestionCard';
import ProductQuestionPage from '@self/platform/spec/page-objects/ProductQuestionPage';
import ProductAnswersList from '@self/platform/spec/page-objects/ProductAnswersList';
import QuestionList from '@self/platform/spec/page-objects/QuestionList';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import ShopAuthor from '@self/platform/components/ContentAuthor/__pageObject__/ShopAuthor';
import AskMoreFormWrapper from '@self/platform/spec/page-objects/components/Questions/AskMoreFormWrapper';
// helpers
import {hideSmartBannerPopup} from '@self/platform/spec/hermione/helpers/smartBannerPopup';

import videoUrlsFixture from '../fixtures/videoUrls';

const ProductQuestionsState = helpers.features.ProductQuestions.ProductQuestionsState;
const {DEFAULT_SHOP_LOGO} = helpers.entities.shop;

const questionId = 666;
const questionSlug = 'lol';
const defaultUserUid = 666;
const defaultPublicId = 'lolpop112233';
const productSlug = 'lolProduct';
const productId = 1722193751;
const questionLikeCount = 12345;
const categoryId = 91491;


const defaultQuestion = ({userUid = defaultUserUid, canDelete = false} = {}) => ({
    id: questionId,
    text: 'Some default question?',
    user: {
        entity: 'user',
        uid: Number(userUid),
    },
    author: {
        entity: 'user',
        id: Number(userUid),
    },
    product: {
        entity: 'product',
        id: productId,
    },
    votes: {
        likeCount: questionLikeCount,
        userVote: 0,
    },
    slug: questionSlug,
    answersCount: 0,
    canDelete,
});

const defaultUser = ({uid = defaultUserUid, publicId = defaultPublicId} = {}) => ({
    id: uid,
    public_id: publicId,
    uid: {
        value: uid,
    },
    login: 'lol',
    dbfields: {
        'userinfo.firstname.uid': 'lol',
        'userinfo.lastname.uid': 'pop',
    },
});

const defaultAnswer = ({text = 'lol', id, userUid = defaultUserUid}) => ({
    ...(id ? {id} : {}),
    text,
    author: {
        entity: 'user',
        id: Number(userUid),
    },
    question: {
        id: questionId,
    },
    votes: {
        likeCount: 0,
        dislikeCount: 0,
        userVote: 0,
    },
});

const product = createProduct({
    slug: productSlug,
    categories: [{
        entity: 'category',
        id: categoryId,
    }],
}, productId);
const picture = createEntityPicture({
    original: {
        url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
    },
}, 'product', productId,
'//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/1hq');
const productWithPicture = mergeReportState([
    product,
    picture,
]);

const defaultAnswers = ({count}) => new Array(count)
    .fill(null)
    .map((next, key) =>
        defaultAnswer({text: `lol - ${key.toString(10)}`}));

async function authorize() {
    await this.browser.setState('schema', {
        users: [{
            id: profiles.ugctest3.uid,
            public_id: profiles.ugctest3.publicId,
            uid: {
                value: profiles.ugctest3.uid,
            },
            login: 'ugctest3',
            dbfields: {
                'userinfo.firstname.uid': 'firstName',
                'userinfo.lastname.uid': 'lastName',
            },
        }],
    });
    await this.browser.yaProfile('ugctest3');
    return profiles.ugctest3.uid;
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопроса на товар.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    productAnswersList: () => this.createPageObject(ProductAnswersList),
                    questionCard: () => this.createPageObject(QuestionCard),
                    questionPage: () => this.createPageObject(ProductQuestionPage),
                });
                await hideSmartBannerPopup(this);
            },
        },
        prepareSuite(ProductAnswersListTenMoreAnswersSuite, {
            hooks: {
                async beforeEach() {
                    const answersCount = 15;
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: answersCount}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                },
            },
        }),

        prepareSuite(VideoCarouselSuite, {
            hooks: {
                async beforeEach() {
                    const answersCount = 3;
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: answersCount}),
                    };

                    this.params.expectedUrl = '/product--lolProduct/1722193751/videos';

                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.setState('Tarantino.data.result', videoUrlsFixture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                },
            },
            pageObjects: {
                scrollBox() {
                    return this.createPageObject(ScrollBox);
                },
            },
        }),
        prepareSuite(ProductAnswersListTenLessAnswersSuite, {
            hooks: {
                async beforeEach() {
                    const answersCount = 7;
                    this.params = {
                        answersCount,
                    };
                    await this.browser.setState('schema', {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: answersCount}),
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
        }),
        prepareSuite(QuestionCardRemovableQuestionSuite, {
            hooks: {
                async beforeEach() {
                    const userUid = await authorize.call(this);
                    await this.browser.setState('schema', {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid, canDelete: true})],
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
            params: {
                questionId,
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
                removePromptDialog() {
                    return this.createPageObject(RemovePromptDialog);
                },
            },
        }),
        prepareSuite(QuestionCardNewQuestionSuite, {
            hooks: {
                async beforeEach() {
                    const userUid = await authorize.call(this);
                    const schema = {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionCardVotesSuite, {
            hooks: {
                async beforeEach() {
                    const userUid = await authorize.call(this);
                    const schema = {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionCardSubscribeSuite, {
            hooks: {
                async beforeEach() {
                    const schema = {
                        users: [defaultUser({uid: profiles.ugctest3.uid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid: defaultUserUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await Promise.all([
                        await this.browser.setState('schema', schema),
                        await this.browser.setState('report', productWithPicture),
                    ]);
                    await this.browser.yaProfile('ugctest3', 'touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionCardUnsubscribeSuite, {
            hooks: {
                async beforeEach() {
                    const schema = {
                        users: [defaultUser({uid: profiles.ugctest3.uid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid: defaultUserUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await Promise.all([
                        await this.browser.setState('schema', schema),
                        await this.browser.setState('report', productWithPicture),
                        await this.browser.setState('marketUtils.data', {
                            subscriptions: [{
                                id: 6610662,
                                subscriptionType: 'QA_NEW_ANSWERS',
                                subscriptionStatus: 'CONFIRMED',
                                parameters: {
                                    questionId,
                                },
                            }],
                        }),
                    ]);
                    await this.browser.yaProfile('ugctest3', 'touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(AnswerSnippetNewAnswerSuite, {
            hooks: {
                async beforeEach() {
                    const userUid = await authorize.call(this);
                    const schema = {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionCardProductSnippetSuite, {
            hooks: {
                async beforeEach() {
                    const userUid = await authorize.call(this);
                    const schema = {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid})],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    this.params = await Object.assign(this.params, {
                        productId,
                        slug: productSlug,
                    });
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionFormSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        askMoreFormWrapper: () => this.createPageObject(AskMoreFormWrapper),
                        questionForm: () => this.createPageObject(QuestionForm, {
                            parent: this.askMoreFormWrapper,
                        }),
                    });
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            params: {
                localStorageKey: 'product-question',
            },
        }),
        prepareSuite(QuestionFormActionButtonAuthorizedUserSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        askMoreFormWrapper: () => this.createPageObject(AskMoreFormWrapper),
                        questionForm: () => this.createPageObject(QuestionForm, {
                            parent: this.askMoreFormWrapper,
                        }),
                    });
                    await authorize.call(this);
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionFormActionButtonUnauthorizedUserSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        askMoreFormWrapper: () => this.createPageObject(AskMoreFormWrapper),
                        questionForm: () => this.createPageObject(QuestionForm, {
                            parent: this.askMoreFormWrapper,
                        }),
                    });
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: defaultAnswers({count: 0}),
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
        }),
        prepareSuite(AnswerSnippetVotesUnauthorizedUserSuite, {
            hooks: {
                async beforeEach() {
                    const answerId = 666;
                    this.setPageObjects({
                        answerSnippet: () => this.createPageObject(AnswerSnippet, {
                            root: `[data-answer-id="${answerId.toString(10)}"]`,
                        }),
                    });
                    const schema = {
                        users: [defaultUser()],
                        modelQuestions: [defaultQuestion()],
                        modelAnswers: [defaultAnswer({id: answerId})],
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
        }),
        prepareSuite(AnswerSnippetVotesAuthorizedUserSuite, {
            hooks: {
                async beforeEach() {
                    const answerId = 666;
                    this.setPageObjects({
                        answerSnippet: () => this.createPageObject(AnswerSnippet, {
                            root: `[data-answer-id="${answerId.toString(10)}"]`,
                        }),
                    });
                    const userUid = await authorize.call(this);
                    const schema = {
                        users: [defaultUser({uid: userUid, publicId: profiles.ugctest3.publicId})],
                        modelQuestions: [defaultQuestion({userUid})],
                        modelAnswers: [defaultAnswer({id: answerId, userUid})],
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-question', {
                        productSlug,
                        productId,
                        questionSlug,
                        questionId,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),

        makeSuite('Ответы от магазина.', {
            story: mergeSuites(
                prepareSuite(ShopAnswerShopExistsSuite, {
                    hooks: {
                        async beforeEach() {
                            const state = new ProductQuestionsState();

                            const user = await state.createUser({id: '1'});
                            const testProduct = await state.createProduct({id: '100'});
                            const question = await state.createProductQuestion(
                                {text: 'who?'},
                                {user, product: testProduct}
                            );

                            const shopId = '10';
                            const shopSlug = 'super-market';
                            const shopName = 'super market shop';
                            const shopInfo = await state.createShopInfo({id: shopId, slug: shopSlug, shopName});

                            const answerId = 1;
                            await state.createQuestionAnswer({id: answerId, text: 'i am'}, {question, shopInfo});

                            await state.setState(this.browser);

                            this.setPageObjects({
                                shopAuthor: () => this.createPageObject(ShopAuthor, {
                                    root: `[data-answer-id="${answerId.toString(10)}"]`,
                                }),
                            });

                            this.params.expectedAuthorName = shopName;
                            this.params.expectedAuthorNameLinkUrl = await this.browser.yaBuildURL('touch:shop', {
                                slug: shopSlug,
                                shopId,
                            });
                            this.params.expectedAuthorLogoUrl = DEFAULT_SHOP_LOGO;

                            await this.browser.yaOpenPage('touch:product-question', {
                                productSlug: testProduct.slug,
                                productId: testProduct.id,
                                questionSlug: question.slug,
                                questionId: question.id,
                            });
                        },
                    },
                }),
                prepareSuite(ShopAnswerShopNotExistsSuite, {
                    hooks: {
                        async beforeEach() {
                            const state = new ProductQuestionsState();

                            const user = await state.createUser({id: '1'});
                            const testProduct = await state.createProduct({id: '100'});
                            const question = await state.createProductQuestion(
                                {text: 'who?'},
                                {user, product: testProduct}
                            );

                            const answerId = 1;

                            await state.createQuestionAnswer({
                                id: answerId,
                                text: 'i am',
                                author: {
                                    entity: 'shop',
                                    id: 123,
                                },
                            }, {question});

                            await state.setState(this.browser);

                            this.setPageObjects({
                                shopAuthor: () => this.createPageObject(ShopAuthor, {
                                    root: `[data-answer-id="${answerId.toString(10)}"]`,
                                }),
                            });

                            this.params.expectedAuthorLogoUrl = DEFAULT_SHOP_LOGO;

                            await this.browser.yaOpenPage('touch:product-question', {
                                productSlug: testProduct.slug,
                                productId: testProduct.id,
                                questionSlug: question.slug,
                                questionId: question.id,
                            });
                        },
                    },
                }),
                prepareSuite(ShopAnswerCreationDateSuite, {
                    hooks: {
                        async beforeEach() {
                            const state = new ProductQuestionsState();

                            const user = await state.createUser({id: '1'});
                            const testProduct = await state.createProduct({id: '100'});
                            const question = await state.createProductQuestion(
                                {text: 'who?'},
                                {user, product: testProduct}
                            );

                            const answerId = 1;

                            await state.createQuestionAnswer({
                                id: answerId,
                                text: 'i am',
                                author: {
                                    entity: 'shop',
                                    id: 123,
                                },
                                created: Date.now(),
                            }, {question});

                            await state.setState(this.browser);

                            this.setPageObjects({
                                shopAuthor: () => this.createPageObject(ShopAuthor, {
                                    root: `[data-answer-id="${answerId.toString(10)}"]`,
                                }),
                            });

                            this.params.expectedCreatedAtText = /^Сегодня/;

                            await this.browser.yaOpenPage('touch:product-question', {
                                productSlug: testProduct.slug,
                                productId: testProduct.id,
                                questionSlug: question.slug,
                                questionId: question.id,
                            });
                        },
                    },
                })
            ),
        }),
        makeSuite('SEO-разметка страницы.', {
            environment: 'testing',
            story: createStories(
                seoTestConfigs.pageCanonical,
                ({routeConfig, testParams, meta}) => prepareSuite(BaseLinkCanonicalSuite, {
                    meta,
                    hooks: {
                        beforeEach() {
                            return this.browser
                                .yaSimulateBot()
                                .yaOpenPage('touch:product-question', routeConfig);
                        },
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                    params: testParams,
                })
            ),
        })
    ),
});
