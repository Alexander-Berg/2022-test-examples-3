import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {
    createQuestion as createQuestionHelper,
    createAnswer,
    createAnswerComment,
    createUser,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';
// suites
import QuestionCategorySnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/CategorySnippet';
import ProductAnswersListTenMoreAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductAnswersList/tenMoreAnswers';
import ProductAnswersListTenLessAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductAnswersList/tenLessAnswers';
import QuestionFormSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm';
import QuestionCardUnsubscribeSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/unsubscribe';
import QuestionCardRemovableQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/removableQuestion';
import AnswerSnippetVotesAuthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/authorizedUser';
import AnswerSnippetCommentLinkWithCommentsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/questionFormForQuestion';
import QuestionFormActionButtonAuthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/ActionButton/authorizedUser';
import QuestionCardSubscribeSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/subscribe';
import AnswerSnippetNewAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/newAnswer';
import QuestionCardNewQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/newQuestion';
import QuestionCardVotesSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionCard/Votes';
import QuestionFormActionButtonUnauthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/ActionButton/unauthorizedUser';
import AnswerSnippetVotesUnauthorizedUserSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/unauthorizedUser';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import CategorySnippet from '@self/platform/spec/page-objects/components/Question/CategorySnippet';
import ProductAnswersList from '@self/platform/spec/page-objects/ProductAnswersList';
import AnswerSnippet from '@self/platform/components/Question/AnswerSnippet/__pageObject__';
import QuestionForm from '@self/platform/spec/page-objects/QuestionForm';
import QuestionList from '@self/platform/spec/page-objects/QuestionList';
import QuestionCard from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/QuestionCard';
import AskMoreFormWrapper from '@self/platform/spec/page-objects/components/Questions/AskMoreFormWrapper';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import AnswerCommentsModal from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/AnswerCommentsModal';
import CategoryQuestionPage from '@self/platform/spec/page-objects/CategoryQuestionPage';

const LOCAL_STORAGE_KEY = 'category-question';
const USER_PROFILE_CONFIG = profiles.ugctest3;

const DEFAULT_HID = 198119;
const DEFAULT_CATEGORY_SLUG = 'elektronika';
const DEFAULT_QUESTION_SLUG = 'moi-vopros';
const DEFAULT_QUESTION_ID = 1234;

const DEFAULT_ANSWER_ID = 1111;
const DEFAULT_ROOT_COMMENT_ID = DEFAULT_ANSWER_ID;

const DEFAULT_CATEGORY = {
    id: DEFAULT_HID,
    slug: DEFAULT_CATEGORY_SLUG,
};
const DEFAULT_USER = createUser({
    id: Number(USER_PROFILE_CONFIG.uid),
    uid: {
        value: Number(USER_PROFILE_CONFIG.uid),
    },
    public_id: USER_PROFILE_CONFIG.publicId,
    login: USER_PROFILE_CONFIG.login,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
});

function createQuestion(params) {
    const {
        id = DEFAULT_QUESTION_ID,
        userUid = DEFAULT_USER.id,
        answersCount = 0,
        questionSlug = DEFAULT_QUESTION_SLUG,
        canDelete = false,
    } = params;

    return createQuestionHelper({
        id,
        category: DEFAULT_CATEGORY,
        product: null,
        user: {
            uid: Number(userUid),
        },
        canDelete,
        answersCount,
        slug: questionSlug,
    });
}

async function prepareCategoryQuestionPage(ctx, params = {}) {
    const {
        answersCount = 0,
        commentsCount = 0,
        isAuth = false,
        canDelete = false,
        answerText = 'My awesome answer',
    } = params;

    let answers = [];
    if (answersCount > 0) {
        answers = [...Array(answersCount)].map((el, index) => createAnswer({
            id: DEFAULT_ANSWER_ID + index,
            question: {
                id: DEFAULT_QUESTION_ID,
            },
            text: answerText,
        }));
    }

    let commentary = [];
    if (commentsCount > 0) {
        commentary = [...Array(commentsCount)].map(() => createAnswerComment({
            author: {
                id: DEFAULT_USER.id,
                entity: 'user',
            },
            parent: undefined,
            entityId: DEFAULT_ROOT_COMMENT_ID,
            state: 'NEW',
        }));
    }

    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        modelQuestions: [createQuestion({
            answersCount,
            canDelete,
        })],
        modelAnswers: answers,
        commentary,
    });
    await ctx.browser.setState('report', productWithPicture);

    const pageParams = {
        categorySlug: DEFAULT_CATEGORY_SLUG,
        questionSlug: DEFAULT_QUESTION_SLUG,
        hid: DEFAULT_HID,
        questionId: DEFAULT_QUESTION_ID,
    };
    if (isAuth) {
        await ctx.browser.yaProfile('ugctest3', 'touch:category-question', pageParams);
    } else {
        await ctx.browser.yaOpenPage('touch:category-question', pageParams);
    }

    return ctx.browser.yaClosePopup(ctx.createPageObject(RegionPopup));
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница категорийного вопроса.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-5122',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    questionPage: () => this.createPageObject(CategoryQuestionPage),
                    productAnswersList: () => this.createPageObject(ProductAnswersList),
                    categorySnippet: () => this.createPageObject(CategorySnippet),
                    answerSnippet: () => this.createPageObject(AnswerSnippet),
                    askMoreFormWrapper: () => this.createPageObject(AskMoreFormWrapper),
                    questionCard: () => this.createPageObject(QuestionCard),
                    questionList: () => this.createPageObject(QuestionList),
                    answerCommentsModal: () => this.createPageObject(AnswerCommentsModal),
                    removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                    questionForm: () => this.createPageObject(QuestionForm, {
                        parent: this.askMoreFormWrapper,
                    }),
                });
            },
        },
        prepareSuite(QuestionCategorySnippetSuite, {
            meta: {
                id: 'm-touch-3031',
            },
            hooks: {
                async beforeEach() {
                    const expectedLink = await this.browser.yaBuildURL('touch:category-questions', {
                        hid: DEFAULT_HID,
                        slug: DEFAULT_CATEGORY_SLUG,
                    });
                    this.params = {
                        ...this.params,
                        expectedLink,
                    };

                    await prepareCategoryQuestionPage(this);
                },
            },
        }),
        prepareSuite(ProductAnswersListTenMoreAnswersSuite, {
            meta: {
                id: 'm-touch-2238',
            },
            hooks: {
                async beforeEach() {
                    const answersCount = 15;

                    await prepareCategoryQuestionPage(this, {
                        answersCount,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
        }),
        prepareSuite(ProductAnswersListTenLessAnswersSuite, {
            meta: {
                id: 'm-touch-2239',
            },
            hooks: {
                async beforeEach() {
                    const answersCount = 5;
                    this.params = {
                        ...this.params,
                        answersCount,
                    };

                    await prepareCategoryQuestionPage(this, {
                        answersCount,
                    });
                },
            },
        }),
        prepareSuite(QuestionFormSuite, {
            hooks: {
                async beforeEach() {
                    this.params = {
                        ...this.params,
                        localStorageKey: LOCAL_STORAGE_KEY,
                    };
                    await prepareCategoryQuestionPage(this);
                },
            },
        }),
        prepareSuite(QuestionCardUnsubscribeSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('marketUtils.data', {
                        subscriptions: [{
                            id: 6610662,
                            subscriptionType: 'QA_NEW_ANSWERS',
                            subscriptionStatus: 'CONFIRMED',
                            parameters: {
                                questionId: DEFAULT_QUESTION_ID,
                            },
                        }],
                    });

                    await prepareCategoryQuestionPage(this, {
                        isAuth: true,
                    });
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(QuestionCardRemovableQuestionSuite, {
            hooks: {
                async beforeEach() {
                    await prepareCategoryQuestionPage(this, {
                        isAuth: true,
                        canDelete: true,
                    });
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(AnswerSnippetVotesAuthorizedUserSuite, {
            hooks: {
                async beforeEach() {
                    await prepareCategoryQuestionPage(this, {
                        isAuth: true,
                        answersCount: 1,
                    });
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(AnswerSnippetCommentLinkWithCommentsSuite, {
            meta: {
                id: 'm-touch-3035',
            },
            hooks: {
                async beforeEach() {
                    const commentsCount = 2;
                    this.params = {
                        ...this.params,
                        expectedText: `${commentsCount} комментария`,
                        answerId: DEFAULT_ANSWER_ID,
                        template: 'touch:category-question-answer',
                    };
                    await prepareCategoryQuestionPage(this, {
                        answersCount: 1,
                        commentsCount,
                    });
                },
            },
        }),
        {
            'Авторизованный пользователь.': mergeSuites(
                mergeSuites(
                    {
                        async beforeEach() {
                            await prepareCategoryQuestionPage(this, {
                                isAuth: true,
                            });
                        },
                        async afterEach() {
                            await this.browser.yaLogout();
                        },
                    },
                    prepareSuite(QuestionFormActionButtonAuthorizedUserSuite),
                    prepareSuite(QuestionCardSubscribeSuite),
                    prepareSuite(AnswerSnippetNewAnswerSuite),
                    prepareSuite(QuestionCardNewQuestionSuite),
                    prepareSuite(QuestionCardVotesSuite)
                ),
                prepareSuite(AnswerSnippetCommentLinkWithCommentsSuite, {
                    meta: {
                        id: 'm-touch-3033',
                    },
                    hooks: {
                        async beforeEach() {
                            const commentsCount = 2;
                            this.params = {
                                ...this.params,
                                answerId: DEFAULT_ANSWER_ID,
                                template: 'touch:category-question-answer',
                                expectedText: `${commentsCount} комментария`,
                            };
                            await prepareCategoryQuestionPage(this, {
                                isAuth: true,
                                answersCount: 1,
                                commentsCount,
                            });
                        },
                        async afterEach() {
                            await this.browser.yaLogout();
                        },
                    },
                })
            ),
        },
        {
            'Неавторизованный пользователь.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareCategoryQuestionPage(this, {
                            answersCount: 1,
                        });
                    },
                },
                prepareSuite(QuestionFormActionButtonUnauthorizedUserSuite),
                prepareSuite(AnswerSnippetVotesUnauthorizedUserSuite)
            ),
        }
    ),
});
