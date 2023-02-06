import {assign} from 'ambar';
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    DEFAULT_PRODUCT_SLUG,
    DEFAULT_PRODUCT_ID,
    DEFAULT_QUESTION_ID,
    createUser,
    createProduct as createQuestionProduct,
    createQuestion,
    createReportProductStateWithPicture,
    createAnswers,
    createAnswer,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';
// configs
import seoConfig from '@self/platform/spec/hermione/configs/seo/product-question-page';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {cpaOffer} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
// suites
import AnswersListTenMoreAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswersList/tenMoreAnswers';
import AnswersListTenLessAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswersList/tenLessAnswers';
import QuestionRemoveShownSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionRemove/shown';
import QuestionRemoveHiddenSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionRemove/hidden';
import AnswerRemoveShownSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerRemove/shown';
import AnswerRemoveHiddenSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerRemove/hidden';
import QuestionSnippetVotesAuthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/votesAuthorized';
import ProductQuestionProductSnippetSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ProductQuestion/productSnippet';
import FormTextFieldBackupSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/textFieldBackup';
import FormCharactersCountSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/charactersCount';
import FormMaxCharactersCountSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/maxCharactersCount';
import FormMaxCharactersCountErrorMessageSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/maxCharactersCountErrorMessage';
import FormClearTextFieldSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/clearTextField';
import FormAuthorizeButtonNotExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonNotExists';
import AnswerCommentsNoCommentsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ProductQuestion/AnswerComments/noComments';
import AnswerCommentsThreeCommentsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ProductQuestion/AnswerComments/threeComments';
import AnswerCommentsThreeMoreCommentsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ProductQuestion/AnswerComments/threeMoreComments';
import QuestionSnippetUserExpertiseSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userExpertise';
import QuestionSnippetVotesUnauthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/votesUnauthorized';
import FormAuthorizeButtonExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonExists';
import FormAuthorizeButtonLink from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonLink';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import AlternateUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/alternate-url';
import QuestionSnippetSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/questionSnippet';
import QuestionSnippetRemoveQuestionSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/removeQuestion';
import QuestionSnippetRemoveAnswerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/removeAnswer';
import QuestionAddAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/addAnswer';
import QuestionAddAnswerCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/addAnswerCounter';
import AnswerVotesVotesUnauthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerVotes/votesUnauthorized';
import AnswerVotesVotesAuthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerVotes/votesAuthorized';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import BelarusBoundPhoneDialogSuite from '@self/platform/spec/hermione/test-suites/blocks/components/BelarusBoundPhoneDialog';
// page-objects
import AnswersList from '@self/platform/spec/page-objects/components/Question/AnswersList';
import QuestionSnippet from '@self/platform/spec/page-objects/components/Question/QuestionSnippet';
import QuestionsAnswersRemove from '@self/platform/spec/page-objects/components/QuestionsAnswers/Remove';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import AlternateUrl from '@self/platform/spec/page-objects/alternate-url';
import Answer from '@self/platform/spec/page-objects/components/Question/Answer';
import CommentsList from '@self/platform/spec/page-objects/components/Comment/List';
import ProductSnippet from '@self/platform/spec/page-objects/components/ProductQuestion/ProductSnippet';
import Dialog from '@self/platform/spec/page-objects/components/Dialog';
import RemoveAnswerDialog from '@self/platform/spec/page-objects/components/Question/RemoveAnswerDialog';
import Form from '@self/platform/spec/page-objects/components/QuestionsAnswers/Form';
import InlineNotification from '@self/platform/spec/page-objects/components/InlineNotification';
import Paginator from '@self/platform/components/Paginator/__pageObject';
import BelarusBoundPhoneDialog from '@self/platform/spec/page-objects/components/BelarusBoundPhoneDialog';
import CommentForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import NoQuestions from '@self/platform/spec/page-objects/components/ProductQuestions/NoQuestions';
import AuthorExpertise from '@self/root/src/components/AuthorExpertise/__pageObject';

const MOSCOW_REGION = routes.region.ru.lr;
const BELARUS_REGION = routes.region.by.lr;
const SHOP_ID = 774;
const SHOP_NAME = 'someShopName';
const SHOP_SLUG = 'someShopSlug';
const SHOP_LOGO = '//avatars.mds.yandex.net/get-market-shop-logo/1539910/2a0000016a44aa3e1d7da56fc50014c7b893/orig';
const expertise = {
    expertiseId: 9,
    value: 33,
    levelValue: 13,
    level: 2,
};
const shop = {
    id: SHOP_ID,
    slug: SHOP_SLUG,
    name: SHOP_NAME,
    shopName: SHOP_NAME,
    logo: SHOP_LOGO,
};

async function authorize(passportHost) {
    await this.browser.setState('schema', {
        users: [createUser()],
    });
    await this.browser.yaProfile('ugctest3', 'market:index', null, passportHost);
    return profiles.ugctest3.uid;
}

const createRouteParams = (product, question) => ({
    productSlug: product.slug,
    productId: product.id,
    questionSlug: question.slug,
    questionId: question.id,
});

// временные ф-ии, будет аналог в кадавре
const createAnswerComment = props => ({
    id: 1,
    state: 'NEW',
    entityId: '1',
    entity: 'answerComment',
    changed: false,
    votes: {
        dislikeCount: 0,
        likeCount: 0,
        userVote: 0,
    },
    childCount: 0,
    updateTime: Math.floor(Date.now() / 1000),
    author: {id: profiles.ugctest3.uid, entity: 'user'},
    ...props,
});
const createAnswerComments = count => {
    let id = 1;
    return [...Array(count)].map(() => createAnswerComment({id: id++}));
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопроса на товар.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Авторизованный пользователь.': mergeSuites(
                {
                    async beforeEach() {
                        await authorize.call(this);
                        const answersList = this.createPageObject(AnswersList);
                        const questionSnippet = this.createPageObject(QuestionSnippet);
                        const questionRemoveLink = this.createPageObject(QuestionsAnswersRemove, {
                            parent: questionSnippet,
                        });

                        const secondAnswerRemoveLink = this.createPageObject(QuestionsAnswersRemove, {
                            parent: AnswersList.getNthAnswerSelector(2),
                        });

                        const questionVotes = this.createPageObject(Votes, {
                            parent: questionSnippet,
                        });

                        this.setPageObjects({
                            answersList: () => answersList,
                            questionSnippet: () => questionSnippet,
                            questionRemoveLink: () => questionRemoveLink,
                            answerRemoveLink: () => secondAnswerRemoveLink,
                            questionVotes: () => questionVotes,
                        });
                    },

                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(AnswersListTenMoreAnswersSuite, {
                    pageObjects: {
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 15;

                            const user = createUser({
                                ...profiles.ugctest3,
                                uid: {
                                    value: profiles.ugctest3.uid,
                                },
                                id: profiles.ugctest3.uid,
                                public_id: profiles.ugctest3.publicId,
                            });
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswersListTenLessAnswersSuite, {
                    pageObjects: {
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 5;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionRemoveShownSuite, {
                    meta: {
                        id: 'marketfront-2884',
                        issue: 'MARKETVERSTKA-31263',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({canDelete: true});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionRemoveHiddenSuite, {
                    meta: {
                        id: 'marketfront-2882',
                        issue: 'MARKETVERSTKA-31261',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswerRemoveShownSuite, {
                    meta: {
                        id: 'marketfront-2885',
                        issue: 'MARKETVERSTKA-31265',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 5;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount, canDelete: true});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount).map((answer, i) => ({
                                    ...answer,
                                    canDelete: (i === 1),
                                })),
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswerRemoveHiddenSuite, {
                    meta: {
                        id: 'marketfront-2883',
                        issue: 'MARKETVERSTKA-31262',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 3;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount, canDelete: true});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount).map(answer => ({
                                    ...answer,
                                    canDelete: false,
                                })),
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetVotesAuthorizedSuite, {
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({
                                votes: {
                                    likeCount: 1,
                                    dislikeCount: 0,
                                    userVote: 0,
                                },
                                canDelete: false,
                            });

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetUserExpertiseSuite, {
                    meta: {
                        id: 'marketfront-4169',
                        issue: 'MARKETFRONT-16499',
                        feature: 'Структура страницы',
                    },
                    pageObjects: {
                        authorExpertise() {
                            return this.createPageObject(AuthorExpertise);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 1;

                            const users = [createUser({
                                ...profiles.ugctest3,
                                uid: {
                                    value: profiles.ugctest3.uid,
                                },
                                id: profiles.ugctest3.uid,
                                public_id: profiles.ugctest3.publicId,
                            }), createUser({
                                ...profiles.testachi,
                                uid: {
                                    value: profiles.testachi.uid,
                                },
                                id: profiles.testachi.uid,
                                public_id: profiles.testachi.publicId,
                            })];
                            const product = createQuestionProduct({
                                id: DEFAULT_PRODUCT_ID,
                                slug: DEFAULT_PRODUCT_SLUG,
                                type: 'model',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: 91461,
                                        name: 'Телефоны',
                                        fullName: 'Телефоны и аксессуары к ним',
                                        type: 'gurulight',
                                        isLeaf: false,
                                    },
                                ],
                                navnodes: [
                                    {
                                        entity: 'navnode',
                                        id: 54437,
                                        hasPromo: true,
                                        generalName: 'dolor',
                                    },
                                ],
                            });
                            const question = createQuestion({
                                answersCount,
                                canDelete: true,
                                product: {
                                    id: DEFAULT_PRODUCT_ID,
                                },
                                author: {
                                    entity: 'user',
                                    id: profiles.ugctest3.uid,
                                },
                            });
                            const answers = createAnswers(answersCount).map(answer => ({
                                ...answer,
                                user: {
                                    uid: profiles.testachi.uid,
                                },
                                author: {
                                    entity: 'user',
                                    id: profiles.testachi.uid,
                                },
                                canDelete: false,
                            }));
                            const userExpertise = answers.map(answer => ({
                                userId: answer.author.id,
                                ...expertise,
                            }));

                            const schema = {
                                users,
                                modelQuestions: [question],
                                modelAnswers: answers,
                            };

                            await this.browser.setState('storage', {userExpertise});
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(ProductQuestionProductSnippetSuite, {
                    pageObjects: {
                        productSnippet() {
                            return this.createPageObject(ProductSnippet);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct({
                                id: DEFAULT_PRODUCT_ID,
                                slug: DEFAULT_PRODUCT_SLUG,
                            });
                            const question = createQuestion({
                                product: {
                                    id: DEFAULT_PRODUCT_ID,
                                },
                                votes: {
                                    likeCount: 1,
                                    dislikeCount: 0,
                                    userVote: 0,
                                },
                                canDelete: false,
                            });

                            this.params.expectedProductSlug = DEFAULT_PRODUCT_SLUG;
                            this.params.expectedProductId = DEFAULT_PRODUCT_ID;

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                makeSuite('Форма добавления ответа', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const user = createUser();
                                const product = createQuestionProduct({
                                    id: DEFAULT_PRODUCT_ID,
                                    slug: DEFAULT_PRODUCT_SLUG,
                                });
                                const question = createQuestion({
                                    product: {
                                        id: DEFAULT_PRODUCT_ID,
                                    },
                                });

                                const schema = {
                                    users: [user],
                                    modelQuestions: [question],
                                };

                                await this.browser.setState('schema', schema);
                                await this.browser.setState('report', createReportProductStateWithPicture(product));
                                await this.browser.yaOpenPage(
                                    'market:product-question',
                                    createRouteParams(product, question)
                                );
                            },
                        },
                        prepareSuite(FormTextFieldBackupSuite, {
                            meta: {
                                id: 'marketfront-2887',
                                issue: 'MARKETVERSTKA-31292',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                            params: {
                                localStorageKey: 'product-question-answer-form',
                            },
                        }),
                        prepareSuite(FormCharactersCountSuite, {
                            meta: {
                                id: 'marketfront-2888',
                                issue: 'MARKETVERSTKA-31293',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                            params: {
                                charactersCount: 2500,
                                maxCharactersCount: 5000,
                            },
                        }),
                        prepareSuite(FormMaxCharactersCountSuite, {
                            meta: {
                                id: 'marketfront-2889',
                                issue: 'MARKETVERSTKA-31294',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                            params: {
                                charactersCount: 5000,
                                maxCharactersCount: 5000,
                            },
                        }),
                        prepareSuite(FormMaxCharactersCountErrorMessageSuite, {
                            meta: {
                                id: 'marketfront-2890',
                                issue: 'MARKETVERSTKA-31295',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                            params: {
                                charactersCount: 5000,
                                maxCharactersCount: 5000,
                            },
                        }),
                        prepareSuite(FormClearTextFieldSuite, {
                            meta: {
                                id: 'marketfront-2892',
                                issue: 'MARKETVERSTKA-31297',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                            params: {
                                questionsCount: 0,
                            },
                        }),
                        prepareSuite(FormAuthorizeButtonNotExistsSuite, {
                            meta: {
                                id: 'marketfront-2896',
                                issue: 'MARKETVERSTKA-31270',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                        })
                    ),
                }),
                prepareSuite(AnswerCommentsNoCommentsSuite, {
                    pageObjects: {
                        answer() {
                            return this.createPageObject(Answer);
                        },
                        commentsList() {
                            return this.createPageObject(CommentsList);
                        },
                        commentForm() {
                            return this.createPageObject(CommentForm);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 1;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});
                            const answer = createAnswer();

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: [answer],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswerCommentsThreeCommentsSuite, {
                    pageObjects: {
                        answer() {
                            return this.createPageObject(Answer);
                        },
                        commentsList() {
                            return this.createPageObject(CommentsList);
                        },
                        commentForm() {
                            return this.createPageObject(CommentForm);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 1;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});
                            const answer = createAnswer();
                            const commentary = createAnswerComments(3);

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: [answer],
                                commentary,
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswerCommentsThreeMoreCommentsSuite, {
                    pageObjects: {
                        answer() {
                            return this.createPageObject(Answer);
                        },
                        commentsList() {
                            return this.createPageObject(CommentsList);
                        },
                        commentForm() {
                            return this.createPageObject(CommentForm);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 1;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});
                            const answer = createAnswer();
                            const commentary = createAnswerComments(4);

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: [answer],
                                commentary,
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                })
            ),
            'Неавторизованный пользователь.': mergeSuites(
                {
                    async beforeEach() {
                        const answersList = this.createPageObject(AnswersList);
                        const questionSnippet = this.createPageObject(QuestionSnippet);
                        const questionRemoveLink = this.createPageObject(QuestionsAnswersRemove, {
                            parent: questionSnippet,
                        });

                        const questionVotes = this.createPageObject(Votes, {
                            parent: questionSnippet,
                        });

                        this.setPageObjects({
                            answersList: () => answersList,
                            questionSnippet: () => questionSnippet,
                            questionRemoveLink: () => questionRemoveLink,
                            questionVotes: () => questionVotes,
                        });
                    },
                },
                prepareSuite(AnswersListTenMoreAnswersSuite, {
                    pageObjects: {
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 15;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(AnswersListTenLessAnswersSuite, {
                    pageObjects: {
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const answersCount = 5;

                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionRemoveHiddenSuite, {
                    meta: {
                        id: 'marketfront-2882',
                        issue: 'MARKETVERSTKA-31261',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetVotesUnauthorizedSuite, {
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct();
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                prepareSuite(ProductQuestionProductSnippetSuite, {
                    pageObjects: {
                        productSnippet() {
                            return this.createPageObject(ProductSnippet);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const product = createQuestionProduct({
                                id: DEFAULT_PRODUCT_ID,
                                slug: DEFAULT_PRODUCT_SLUG,
                            });
                            const question = createQuestion({
                                product: {
                                    id: DEFAULT_PRODUCT_ID,
                                },
                                votes: {
                                    likeCount: 1,
                                    dislikeCount: 0,
                                    userVote: 0,
                                },
                                canDelete: false,
                            });

                            this.params.expectedProductSlug = DEFAULT_PRODUCT_SLUG;
                            this.params.expectedProductId = DEFAULT_PRODUCT_ID;

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage(
                                'market:product-question',
                                createRouteParams(product, question)
                            );
                        },
                    },
                }),
                makeSuite('Кнопка Войти', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const user = createUser();
                                const product = createQuestionProduct({
                                    id: DEFAULT_PRODUCT_ID,
                                    slug: DEFAULT_PRODUCT_SLUG,
                                });
                                const question = createQuestion({
                                    product: {
                                        id: DEFAULT_PRODUCT_ID,
                                    },
                                });

                                const schema = {
                                    users: [user],
                                    modelQuestions: [question],
                                };

                                await this.browser.setState('schema', schema);
                                await this.browser.setState('report', createReportProductStateWithPicture(product));
                                await this.browser.yaOpenPage(
                                    'market:product-question',
                                    createRouteParams(product, question)
                                );
                            },
                        },
                        prepareSuite(FormAuthorizeButtonExistsSuite, {
                            meta: {
                                id: 'marketfront-2895',
                                issue: 'MARKETVERSTKA-31272',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                        }),
                        prepareSuite(FormAuthorizeButtonLink, {
                            meta: {
                                id: 'marketfront-2894',
                                issue: 'MARKETVERSTKA-31271',
                            },
                            pageObjects: {
                                form() {
                                    return this.createPageObject(Form);
                                },
                            },
                        })
                    ),
                })
            ),
        },
        makeSuite('Мета-тэги', {
            environment: 'kadavr',
            story: mergeSuites(
                prepareSuite(CanonicalUrlSuite, {
                    meta: {
                        id: 'marketfront-2404',
                        issue: 'MARKETVERSTKA-31330',
                    },
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const {routeParams, testParams} = seoConfig.canonicalUrl;

                            const {productId, productSlug, questionId, questionSlug} = routeParams;
                            this.params = assign(this.params, testParams);

                            const user = createUser();
                            const product = createQuestionProduct({
                                slug: productSlug,
                            });
                            const question = createQuestion({slug: questionSlug});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage('market:product-question', {
                                productSlug,
                                productId,
                                questionSlug,
                                questionId,
                            });
                        },
                    },
                }),
                prepareSuite(AlternateUrlSuite, {
                    meta: {
                        id: 'marketfront-2331',
                        issue: 'MARKETVERSTKA-31330',
                    },
                    pageObjects: {
                        alternateUrl() {
                            return this.createPageObject(AlternateUrl);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const {routeParams, testParams} = seoConfig.alternateUrl;

                            const {productId, productSlug, questionId, questionSlug} = routeParams;

                            this.params = assign(this.params, testParams);

                            const user = createUser();
                            const product = createQuestionProduct({
                                slug: productSlug,
                            });
                            const question = createQuestion({slug: questionSlug});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', createReportProductStateWithPicture(product));
                            await this.browser.yaOpenPage('market:product-question', {
                                productSlug,
                                productId,
                                questionSlug,
                                questionId,
                            });
                        },
                    },
                })
            ),
        }),
        prepareSuite(QuestionSnippetSuite, {
            meta: {
                environment: 'kadavr',
                feature: 'Контент страницы',
            },
            pageObjects: {
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet);
                },
            },
            hooks: {
                async beforeEach() {
                    const userName = 'ugctest3';
                    const userPublicName = 'Ugc T.';
                    const date = new Date();
                    date.setFullYear(date.getFullYear() - 1);
                    date.setMonth(0);
                    date.setDate(1);

                    const user = createUser({
                        display_name: {
                            name: userName,
                            public_name: userPublicName,
                            display_name_empty: false,
                            avatar: {
                                default: '36777/515095637-1546054309',
                                empty: false,
                            },
                        },
                    });
                    const product = createQuestionProduct();
                    const question = createQuestion({
                        created: date.getTime(),
                        canDelete: false,
                    });

                    this.params.expectedUserName = userPublicName;
                    this.params.expectedDate = 'Год назад';

                    const schema = {
                        users: [user],
                        modelQuestions: [question],
                    };

                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', createReportProductStateWithPicture(product));
                    await this.browser.yaOpenPage(
                        'market:product-question',
                        createRouteParams(product, question)
                    );
                },
            },
        }),
        makeSuite('Удаление вопроса', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                            type: 'model',
                            categories: [
                                {
                                    entity: 'category',
                                    id: 91461,
                                    name: 'Телефоны',
                                    fullName: 'Телефоны и аксессуары к ним',
                                    type: 'gurulight',
                                    isLeaf: false,
                                },
                            ],
                            navnodes: [
                                {
                                    entity: 'navnode',
                                    id: 54437,
                                    hasPromo: true,
                                    generalName: 'dolor',
                                },
                            ],
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            canDelete: true,
                        });

                        this.params.expectedTargetUrl = await this.browser.yaBuildURL('market:product-questions', {
                            productId: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.yaProfile(
                            'ugctest3',
                            'market:product-question',
                            createRouteParams(product, question)
                        );
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionSnippetRemoveQuestionSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                        dialog() {
                            return this.createPageObject(Dialog, {
                                root: '.removeQuestionDialogPortal',
                            });
                        },
                        noQuestions() {
                            return this.createPageObject(NoQuestions);
                        },
                    },
                })
            ),
        }),
        makeSuite('Удаление ответа', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await authorize.call(this);
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            answersCount: 2,
                            canDelete: false,
                        });

                        const answer = createAnswer({
                            question: {
                                id: DEFAULT_QUESTION_ID,
                            },
                            canDelete: true,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [answer, {...answer, id: 2}],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.setState('Cataloger.tree', {});
                        await this.browser.yaOpenPage(
                            'market:product-question',
                            createRouteParams(product, question)
                        );
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionSnippetRemoveAnswerSuite, {
                    pageObjects: {
                        answerSnippet() {
                            return this.createPageObject(Answer);
                        },
                        dialog() {
                            return this.createPageObject(RemoveAnswerDialog);
                        },
                        answersList() {
                            return this.createPageObject(AnswersList);
                        },
                    },
                    params: {
                        answersCount: 2,
                    },
                })
            ),
        }),
        makeSuite('Добавление ответа', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            form: () => this.createPageObject(Form),
                            inlineNotification: () => this.createPageObject(InlineNotification),
                            answersList: () => this.createPageObject(AnswersList),
                            answerSnippet: () => this.createPageObject(Answer),
                        });

                        await authorize.call(this);
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            answersCount: 1,
                        });

                        const answer = createAnswer({
                            question: {
                                id: DEFAULT_QUESTION_ID,
                            },
                            canDelete: true,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [answer],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.setState('Cataloger.tree', {});
                        await this.browser.yaOpenPage('market:product-question', createRouteParams(product, question));
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionAddAnswerSuite, {
                    meta: {
                        id: 'marketfront-2891',
                        issue: 'MARKETVERSTKA-31296',
                    },
                }),
                prepareSuite(QuestionAddAnswerCounterSuite)
            ),
        }),
        makeSuite('Добавление первого ответа к вопросу', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            form: () => this.createPageObject(Form),
                            inlineNotification: () => this.createPageObject(InlineNotification),
                            answersList: () => this.createPageObject(AnswersList),
                            answerSnippet: () => this.createPageObject(Answer),
                        });

                        await authorize.call(this);
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            answersCount: 0,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.setState('Cataloger.tree', {});
                        await this.browser.yaOpenPage('market:product-question', createRouteParams(product, question));
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionAddAnswerSuite, {
                    meta: {
                        id: 'marketfront-2891',
                        issue: 'MARKETVERSTKA-31296',
                    },
                })
            ),
        }),
        prepareSuite(AnswerVotesVotesUnauthorizedSuite, {
            hooks: {
                async beforeEach() {
                    const answerSnippet = this.createPageObject(Answer);
                    const answerVotes = this.createPageObject(Votes, {
                        parent: answerSnippet,
                    });
                    this.setPageObjects({
                        answerSnippet: () => answerSnippet,
                        answerVotes: () => answerVotes,
                    });

                    const user = createUser();
                    const product = createQuestionProduct();
                    const question = createQuestion({
                        answersCount: 1,
                    });
                    const answer = createAnswer();

                    const schema = {
                        users: [user],
                        modelQuestions: [question],
                        modelAnswers: [answer],
                    };

                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', createReportProductStateWithPicture(product));
                    await this.browser.yaOpenPage(
                        'market:product-question',
                        createRouteParams(product, question)
                    );
                },
            },
        }),
        prepareSuite(AnswerVotesVotesAuthorizedSuite, {
            hooks: {
                async beforeEach() {
                    await authorize.call(this);
                    const answerSnippet = this.createPageObject(Answer);
                    const answerVotes = this.createPageObject(Votes, {
                        parent: answerSnippet,
                    });

                    this.setPageObjects({
                        answerSnippet: () => answerSnippet,
                        answerVotes: () => answerVotes,
                    });

                    const user = createUser();
                    const product = createQuestionProduct();
                    const question = createQuestion({
                        answersCount: 1,
                    });
                    const answer = createAnswer();

                    const schema = {
                        users: [user],
                        modelQuestions: [question],
                        modelAnswers: [answer],
                    };

                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', createReportProductStateWithPicture(product));
                    await this.browser.yaOpenPage(
                        'market:product-question',
                        createRouteParams(product, question)
                    );
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
        {
            'Ответ от магазина с CPA оффером': mergeSuites(
                {
                    async beforeEach() {
                        const date = new Date();
                        const user = createUser({
                            ...profiles.ugctest3,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            id: profiles.ugctest3.uid,
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct();
                        const question = createQuestion({
                            answersCount: 1,
                        });
                        const answer = createAnswer({
                            created: date.getTime(),
                            author: {
                                entity: 'shop',
                                id: 431782,
                            },
                        });
                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [answer],
                        };

                        await this.browser.setState('Carter.items', []);
                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', mergeState([
                            createReportProductStateWithPicture(product),
                            createShopInfo({...shop, id: 431782}, 431782),
                            cpaOffer,
                        ]));

                        await this.browser.yaOpenPage(
                            'market:product-question',
                            createRouteParams(product, question)
                        );
                    },
                },
                prepareSuite(CartButtonSuite)
            ),
        },
        {
            'Пользователь авторизован на домене .by  и создает новый ответ': mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaOpenPage('market:index', {
                            pcr: MOSCOW_REGION,
                            lr: BELARUS_REGION,
                        });

                        // перенос сессионной куки кадавра на домен .by
                        // и смена региона на Минск
                        await this.browser.setKadavrSesssionCookies();

                        await authorize.call(this, 'passport.yandex.by');

                        // state
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            answersCount: 0,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.setState('Cataloger.tree', {});

                        await this.browser.yaOpenPage(
                            'market:product-question',
                            createRouteParams(product, question)
                        );
                    },
                    async afterEach() {
                        // меняем регион обратно на Москву
                        await this.browser.yaOpenPage('market:index', {
                            pcr: BELARUS_REGION,
                            lr: MOSCOW_REGION,
                        });
                    },
                },
                prepareSuite(BelarusBoundPhoneDialogSuite, {
                    meta: {
                        feature: 'Диалог привязки телефона',
                        id: 'marketfront-3102',
                        issue: 'MARKETVERSTKA-32375',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                        boundPhoneDialog() {
                            return this.createPageObject(BelarusBoundPhoneDialog);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.form.clickTextarea();
                            await this.form.setText('Мой новый ответ');
                            await this.form.clickSubmitButton();
                        },
                    },
                })
            ),
        },
        {
            'Пользователь авторизован на домене .by и создает новый комментарий': mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaOpenPage('market:index', {
                            pcr: MOSCOW_REGION,
                            lr: BELARUS_REGION,
                        });

                        // перенос сессионной куки кадавра на домен .by
                        // и смена региона на Минск
                        await this.browser.setKadavrSesssionCookies();

                        await authorize.call(this, 'passport.yandex.by');

                        // state
                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const product = createQuestionProduct({
                            id: DEFAULT_PRODUCT_ID,
                            slug: DEFAULT_PRODUCT_SLUG,
                        });
                        const question = createQuestion({
                            product: {
                                id: DEFAULT_PRODUCT_ID,
                            },
                            answersCount: 1,
                        });
                        const answer = createAnswer();

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [answer],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', createReportProductStateWithPicture(product));
                        await this.browser.setState('Cataloger.tree', {});

                        await this.browser.yaOpenPage(
                            'market:product-question',
                            createRouteParams(product, question)
                        );
                    },
                    async afterEach() {
                        // меняем регион обратно на Москву
                        await this.browser.yaOpenPage('market:index', {
                            pcr: BELARUS_REGION,
                            lr: MOSCOW_REGION,
                        });
                    },
                },
                prepareSuite(BelarusBoundPhoneDialogSuite, {
                    meta: {
                        feature: 'Диалог привязки телефона',
                        id: 'marketfront-3103',
                        issue: 'MARKETVERSTKA-32375',
                    },
                    pageObjects: {
                        answerSnippet() {
                            return this.createPageObject(Answer);
                        },
                        commentForm() {
                            return this.createPageObject(CommentForm, Answer.root);
                        },
                        boundPhoneDialog() {
                            return this.createPageObject(BelarusBoundPhoneDialog);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.answerSnippet.clickCommentButton();
                            await this.commentForm.clickTextarea();
                            await this.commentForm.setText('Мой новый комментарий');
                            await this.commentForm.clickSendButton();
                        },
                    },
                })
            ),
        }
    ),
});
