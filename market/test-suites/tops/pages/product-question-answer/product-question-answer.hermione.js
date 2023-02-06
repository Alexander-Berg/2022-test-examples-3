import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {helpers} from '@yandex-market/kadavr';
import dayjs from 'dayjs';

// suites
import TwoLevelCommentariesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/twoLevelCommentaries';
import SubpageHeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/SubpageHeader';
import OthersAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductQuestionAnswer/othersAnswer';
import ShopAnswerShopExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/shopExists';
import ShopAnswerShopNotExistsSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/shopNotExists';
import ShopAnswerCreationDateSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/ShopAnswer/creationDate';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import SubpageHeader from '@self/platform/spec/page-objects/SubpageHeader';
import ContentManageControls from '@self/platform/components/ContentManageControls/__pageObject';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Section from '@self/platform/spec/page-objects/Section';
import ShopAuthor from '@self/platform/components/ContentAuthor/__pageObject__/ShopAuthor';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';
// helpers
import {hideSmartBannerPopup} from '@self/platform/spec/hermione/helpers/smartBannerPopup';

import {
    questionId,
    questionSlug,
    product,
    productId,
    productSlug,
    answerId,
    otherUserUid,
    getAnswers,
    getQuestions,
    getUsers,
} from './mocks/data.mock';

const ProductQuestionsState = helpers.features.ProductQuestions.ProductQuestionsState;
const {DEFAULT_SHOP_LOGO} = helpers.entities.shop;


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница ответа на вопрос.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(TwoLevelCommentariesSuite, {
            params: {
                pageTemplate: 'touch:product-question-answer',
                pageParams: {
                    answerId,
                    'no-tests': 1,
                },
                entityId: answerId,
                defaultLimit: 5,
            },
            hooks: {
                async beforeEach() {
                    this.params = {
                        ...this.params,
                        schema: {
                            modelAnswers: getAnswers(),
                            users: getUsers(),
                            modelQuestions: getQuestions(),
                        },
                    };
                    await hideSmartBannerPopup(this);
                    await this.browser.setState('report', product);
                },
            },
        }),
        prepareSuite(SubpageHeaderSuite, {
            meta: {
                feature: 'Точка входа',
                id: 'm-touch-2265',
                issue: 'MOBMARKET-9085',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        modelAnswers: getAnswers(),
                        users: getUsers(),
                        modelQuestions: getQuestions(),
                    });
                    await this.browser.setState('report', product);
                    await this.browser.yaOpenPage('touch:product-question-answer', {answerId});
                    await this.browser.yaBuildURL('touch:product-question', {
                        questionId,
                        questionSlug,
                        productId,
                        productSlug,
                    }).then(expectedUrl => {
                        this.params = Object.assign(this.params, {backUrl: expectedUrl});
                    });
                },
            },
            pageObjects: {
                subpageHeader() {
                    return this.createPageObject(SubpageHeader);
                },
            },
        }),

        prepareSuite(OthersAnswerSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        modelAnswers: getAnswers({authorUid: otherUserUid}),
                        users: getUsers(),
                        modelQuestions: getQuestions(),
                    });
                    await this.browser.setState('report', product);
                    await this.browser.yaProfile('ugctest3', 'touch:product-question-answer', {answerId});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                contentManageControls() {
                    return this.createPageObject(ContentManageControls);
                },
                controls() {
                    return this.createPageObject(Controls);
                },
                complaintForm() {
                    return this.createPageObject(ComplaintForm);
                },
                complaintFormHeader() {
                    return this.createPageObject(ComplaintFormHeader);
                },
                complaintFormSubmitButton() {
                    return this.createPageObject(ComplaintFormSubmitButton);
                },
                notification() {
                    return this.createPageObject(Notification);
                },
            },
        }),
        makeSuite('Ответ от магазина.', {
            story: mergeSuites(
                prepareSuite(ShopAnswerShopExistsSuite, {
                    pageObjects: {
                        shopAuthor() {
                            return this.createPageObject(ShopAuthor, {
                                root: Section.root,
                            });
                        },
                    },
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

                            const testAnswerId = 1;
                            await state.createQuestionAnswer(
                                {id: testAnswerId, text: 'i am'},
                                {question, shopInfo}
                            );

                            await state.setState(this.browser);

                            this.params.expectedAuthorName = shopName;
                            this.params.expectedAuthorNameLinkUrl = await this.browser.yaBuildURL('touch:shop', {
                                slug: shopSlug,
                                shopId,
                            });
                            this.params.expectedAuthorLogoUrl = DEFAULT_SHOP_LOGO;

                            await this.browser.yaOpenPage('touch:product-question-answer', {
                                answerId: testAnswerId,
                            });
                        },
                    },
                }),
                prepareSuite(ShopAnswerShopNotExistsSuite, {
                    pageObjects: {
                        shopAuthor() {
                            return this.createPageObject(ShopAuthor, {
                                root: Section.root,
                            });
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const state = new ProductQuestionsState();

                            const user = await state.createUser({id: '1'});
                            const testProduct = await state.createProduct({id: '100'});
                            const question = await state.createProductQuestion(
                                {text: 'who?'},
                                {user, product: testProduct}
                            );

                            const testAnswerId = 1;

                            await state.createQuestionAnswer({
                                id: testAnswerId,
                                text: 'i am',
                                author: {
                                    entity: 'shop',
                                    id: 123,
                                },
                            }, {question});

                            await state.setState(this.browser);

                            this.params.expectedAuthorLogoUrl = DEFAULT_SHOP_LOGO;

                            await this.browser.yaOpenPage('touch:product-question-answer', {
                                answerId: testAnswerId,
                            });
                        },
                    },
                }),
                prepareSuite(ShopAnswerCreationDateSuite, {
                    pageObjects: {
                        shopAuthor() {
                            return this.createPageObject(ShopAuthor, {
                                root: Section.root,
                            });
                        },
                    },
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

                            const testAnswerId = 1;
                            await state.createQuestionAnswer({
                                id: testAnswerId,
                                text: 'i am',
                                created: dayjs('2018-06-01 13:00:00').valueOf(),
                            }, {question, shopInfo});

                            await state.setState(this.browser);

                            this.params.expectedCreatedAtText = /^1 июня 2018/;

                            await this.browser.yaOpenPage('touch:product-question-answer', {
                                answerId: testAnswerId,
                            });
                        },
                    },
                })
            ),
        })
    ),
});
