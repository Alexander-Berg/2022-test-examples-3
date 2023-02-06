import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createEntityPicture} from '@yandex-market/kadavr/mocks/Report/helpers/picture';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {randomString} from '@self/root/src/helpers/string';

import {MODEL_QUESTION_ANSWER} from '@self/root/src/entities/agitation/constants';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';
// configs
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-questions-page';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import QuestionListTenMoreQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionList/tenMoreQuestions';
import QuestionListTenLessQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionList/tenLessQuestions';
import QuestionListNoQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionList/noQuestions';
import QuestionSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionSnippet';
import ProductQuestionsPageSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductQuestionsPage';
import QuestionFormSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm';
import QuestionFormForQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/questionFormForQuestion';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import ProductOffersStaticListAllOffersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersStaticList/allOffers';
import ProductOffersSnippetListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippetList';
import ProductOffersSnippetClickOutLinkSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/clickOutLink';
import ProductOffersSnippetPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/price';
import ProductOffersSnippetShopRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/shopRating';
import DefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer';
import GainedExpertisePopupSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/GainedExpertisePopup';
import HighratedSimilarProductsSuite from '@self/platform/spec/hermione/test-suites/blocks/HighratedSimilarProducts';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import QuestionSnippet from '@self/platform/spec/page-objects/QuestionSnippet';
import QuestionList from '@self/platform/spec/page-objects/QuestionList';
import PromptDialog from '@self/platform/spec/page-objects/components/PromptDialog';
import ProductQuestionsPage from '@self/platform/spec/page-objects/ProductQuestionsPage';
import QuestionForm from '@self/platform/spec/page-objects/QuestionForm';
import QuestionCard from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/QuestionCard';
import Base from '@self/platform/spec/page-objects/n-base';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import ProductOffersSnippetList from '@self/platform/containers/ProductOffersSnippetList/__pageObject';
import ProductOfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import Form from '@self/platform/spec/page-objects/AnswerForm.js';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';
import HighratedSimilarProducts from '@self/platform/spec/page-objects/widgets/content/HighratedSimilarProducts';
// helpers
import {hideSmartBannerPopup} from '@self/platform/spec/hermione/helpers/smartBannerPopup';

import {productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import {
    buildProductOffersResultsState,
    URL_TO_SHOP,
    PRICE,
} from '@self/platform/spec/hermione/fixtures/product/productOffers';

const productId = 1722193751;
const productTitle = 'Смартфон Samsung Galaxy S8';
const productSlug = 'smartfon-samsung-galaxy-s8';

const category = {
    entity: 'category',
    id: 91491,
    name: 'Мобильные телефоны',
    fullName: 'Мобильные телефоны',
    type: 'guru',
    isLeaf: true,
};

const product = createProduct({
    titles: {
        raw: productTitle,
    },
    categories: [category],
    slug: productSlug,
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
const userUid = profiles.ugctest3.uid;
const user = {
    id: userUid,
    public_id: profiles.ugctest3.publicId,
    uid: {
        value: userUid,
    },
    login: 'lol',
    display_name: {
        name: 'lol pop',
        public_name: 'Lol P.',
        display_name_empty: false,
    },
    dbfields: {
        'userinfo.firstname.uid': 'lol',
        'userinfo.lastname.uid': 'pop',
    },
};
const questionSlug = 'lol';
const dataQuestionId = 666;
const defaultQuestion = {
    user: {
        uid: userUid,
        entity: 'user',
    },
    product: {
        id: productId,
        entity: 'product',
    },
    slug: questionSlug,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопросов на товар.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(QuestionListTenMoreQuestionsSuite, {
            hooks: {
                async beforeEach() {
                    await hideSmartBannerPopup(this);
                    await this.browser.setState('schema', {
                        users: [user],
                        modelQuestions: new Array(15)
                            .fill(null)
                            .map(() => defaultQuestion),
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage(
                        'touch:product-questions',
                        {
                            productId,
                            slug: productSlug,
                        }
                    );
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
            },
        }),
        prepareSuite(QuestionListTenLessQuestionsSuite, {
            params: {
                questionCount: 7,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users: [user],
                        modelQuestions: new Array(7)
                            .fill(null)
                            .map(() => ({...defaultQuestion})),
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-questions', {productId, slug: productSlug});
                },
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
            },
        }),
        prepareSuite(QuestionListNoQuestionsSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users: [user],
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage(
                        'touch:product-questions',
                        {
                            productId,
                            slug: productSlug,
                        }
                    );
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
            },
        }),
        prepareSuite(QuestionSnippetSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaBuildURL('touch:product-question', {
                        questionSlug,
                        productSlug,
                        questionId: dataQuestionId,
                        productId,
                    }).then(expectedContentLink => {
                        this.params = Object.assign(this.params, {
                            dataQuestionId,
                            expectedContentLink,
                            questionType: 'product',
                            slug: questionSlug,
                        });
                    });
                    await this.browser.setState('schema', {
                        users: [user],
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaProfile('ugctest3', 'touch:product-questions', {
                        productId,
                        slug: productSlug,
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
                promptDialog() {
                    return this.createPageObject(PromptDialog);
                },
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet, {
                        root: `[data-question-id="${dataQuestionId}"]`,
                    });
                },
            },
        }),
        prepareSuite(ProductQuestionsPageSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users: [user],
                        modelQuestions: new Array(5)
                            .fill(null)
                            .map(() => ({...defaultQuestion})),
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-questions', {
                        productId,
                        slug: productSlug,
                    });
                },
            },
            params: {
                productId,
                slug: productSlug,
            },
            pageObjects: {
                productQuestionPage() {
                    return this.createPageObject(ProductQuestionsPage);
                },
            },
        }),
        prepareSuite(QuestionFormSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaOpenPage('touch:product-questions', {productId, slug: productSlug});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                questionForm() {
                    return this.createPageObject(QuestionForm);
                },
            },
            params: {
                localStorageKey: 'product-question',
            },
        }),
        prepareSuite(QuestionFormForQuestionSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        users: [user],
                    });
                    await this.browser.setState('report', productWithPicture);
                    await this.browser.yaProfile(
                        'ugctest3', 'touch:product-questions', {productId, slug: productSlug}
                    );
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionForm() {
                    return this.createPageObject(QuestionForm);
                },
                questionCard() {
                    return this.createPageObject(QuestionCard);
                },
            },
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
                                .yaOpenPage('touch:product-questions', routeConfig);
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
        }),
        makeSuite('Статичный виджет ДО и Топ6', {
            environment: 'kadavr',
            story: mergeSuites({
                async beforeEach() {
                    const state = buildProductOffersResultsState({
                        offersCount: 100,
                    });
                    await this.browser.setState('report', state);

                    await this.browser.yaOpenPage('touch:product-questions', phoneProductRoute);

                    // Виджет с ДО загружается лениво.
                    await this.browser.yaSlowlyScroll();
                },

                'Содержимое.': mergeSuites(
                    prepareSuite(ProductOffersStaticListAllOffersSuite, {
                        params: {
                            productId: phoneProductRoute.productId,
                            slug: phoneProductRoute.slug,
                        },
                        pageObjects: {
                            productOffersStaticList() {
                                return this.createPageObject(ProductOffersStaticList);
                            },
                        },

                        hooks: {
                            async beforeEach() {
                                await this.productOffersStaticList.waitForVisible();
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetListSuite, {
                        pageObjects: {
                            productOffersSnippetList() {
                                return this.createPageObject(ProductOffersSnippetList);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.productOffersSnippetList.waitForVisible();
                            },
                        },
                    })
                ),
                'Сниппет Топ 6.': mergeSuites(
                    prepareSuite(ProductOffersSnippetClickOutLinkSuite, {
                        meta: {
                            id: 'm-touch-2886',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedHref: URL_TO_SHOP,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetPriceSuite, {
                        meta: {
                            id: 'm-touch-2888',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedPriceValue: PRICE.value,
                            expectedPriceCurrency: PRICE.currency,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetShopRatingSuite, {
                        meta: {
                            id: 'm-touch-2796',
                            issue: 'MOBMARKET-12244',
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    })
                ),
            }),
        }),
        makeSuite('Дефолтный оффер', {
            environment: 'kadavr',
            story: prepareSuite(DefaultOfferSuite, {
                pageObjects: {
                    defaultOffer() {
                        return this.createPageObject(DefaultOffer);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const state = mergeState([
                            productWithDefaultOffer,
                            buildProductOffersResultsState({
                                offersCount: 6,
                            }),
                        ]);

                        await this.browser.setState('report', state);

                        await this.browser.yaOpenPage('touch:product-questions', phoneProductRoute);

                        // Виджет с ДО загружается лениво.
                        await this.browser.yaSlowlyScroll();

                        await this.defaultOffer.waitForVisible();
                    },
                },
            }),
        }),
        {
            'Пользователь авторизован.': mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaProfile(profiles.dzot61.login, 'touch:index');
                    },
                },
                prepareSuite(GainedExpertisePopupSuite, {
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                        gainedExpertise() {
                            return this.createPageObject(GainedExpertise);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const questionId = 1;
                            await this.browser.setState('report', productWithPicture);
                            await this.browser.setState('schema', {
                                users: [user],
                                modelQuestions: [
                                    {
                                        id: questionId,
                                        ...defaultQuestion,
                                    },
                                ],
                            }
                            );
                            await this.browser.yaOpenPage('touch:product-question', {
                                productId,
                                productSlug,
                                questionId,
                                questionSlug,
                            });
                            const gainExpertise = createGainExpertise(MODEL_QUESTION_ANSWER, 10, profiles.dzot61.uid);
                            await this.browser.setState('storage', {gainExpertise});
                        },
                    },
                    params: {
                        questionSlug,
                        productSlug,
                        expectedBadgeText: '',
                    },
                })
            ),
        },
        makeSuite('Вопросы о товаре с низким рейтингом.', {
            environment: 'kadavr',
            story: prepareSuite(HighratedSimilarProductsSuite, {
                pageObjects: {
                    highratedSimilarProducts() {
                        return this.createPageObject(HighratedSimilarProducts);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const productsCount = 3;
                        const lowRatedProduct = {...productWithPicture};
                        lowRatedProduct.collections.product[productId].preciseRating = 3;

                        const otherProducts = [];

                        for (let i = 0; i < productsCount; i++) {
                            otherProducts.push(createProduct({
                                showUid: `${randomString()}_${i}`,
                                slug: 'test-product',
                                categories: [category],
                                preciseRating: 5,
                            }));
                        }

                        const reportState = mergeReportState([
                            lowRatedProduct,
                            ...otherProducts,
                        ]);

                        await this.browser.setState('report', reportState);
                        await this.browser.setState('schema', {
                            modelQuestions: new Array(10)
                                .fill(null)
                                .map(() => ({...defaultQuestion})),
                        });
                        await this.browser.yaOpenPage('touch:product-questions', {
                            productId,
                            productSlug,
                        });
                    },
                },
                params: {
                    snippetsCount: 3,
                },
            }),
        })
    ),
});
