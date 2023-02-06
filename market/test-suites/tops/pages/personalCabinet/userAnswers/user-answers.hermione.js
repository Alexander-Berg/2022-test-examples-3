import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {
    createUser,
    createAnswerComment,
    createQuestion,
    createAnswer as createAnswerHelper,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

// suites
import ZeroStateSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/zeroState';
import TenLessAnswersSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/tenLessAnswers';
import TenMoreAnswersSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/tenMoreAnswers';
import RemovableAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/removableAnswer';
import ProductAnswerSnippetSuite
    from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/productAnswerSnippet';
import CategoryAnswerSnippetSuite
    from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserAnswers/categoryAnswerSnippet';

// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import PersonalCabinetZeroState from '@self/platform/spec/page-objects/components/PersonalCabinetZeroState';
import CabinetAnswerSnippet from '@self/platform/spec/page-objects/widgets/content/UserAnswers/components/CabinetAnswerSnippet';
import QuestionHeader from '@self/platform/spec/page-objects/widgets/content/UserQuestions/components/QuestionHeader';
import QuestionFooter from '@self/platform/spec/page-objects/widgets/content/UserQuestions/components/QuestionFooter';
import AnswerContentBody from '@self/platform/spec/page-objects/widgets/content/UserAnswers/components/AnswerContentBody';
import UserAnswers from '@self/platform/spec/page-objects/widgets/content/UserAnswers';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Notification from '@self/root/src/components/Notification/__pageObject';
import VoteButton from '@self/platform/spec/page-objects/components/VoteButton';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER = createUser({
    id: USER_PROFILE_CONFIG.uid,
    uid: {
        value: USER_PROFILE_CONFIG.uid,
    },
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

const DEFAULT_ANSWER_ID = 123;

const DEFAULT_QUESTION_ID = 231;
const DEFAULT_QUESTION_SLUG = 'default-question-slug';

const DEFAULT_CATEGORY = {
    id: 198119,
    slug: 'elektronika',
};
const DEFAULT_PRODUCT = {
    id: 14206682, // id продукта совпадает с id productWithPicture,
    slug: 'smartfon-apple-iphone-7-128gb', // slug продукта совпадает с slug productWithPicture,
    entity: 'product',
};

const TEN_LESS_ANSWERS_COUNT = 5;
const TEN_MORE_ANSWERS_COUNT = 15;
const DEFAULT_LIKES_COUNT = 5;
const COMMENTS_COUNT = 5;

const createAnswer = (params = {}) => {
    const {
        id = DEFAULT_ANSWER_ID,
        questionId = DEFAULT_QUESTION_ID,
        canDelete = false,
        withUserVote = false,
        likeCount = DEFAULT_LIKES_COUNT,
    } = params;

    return createAnswerHelper({
        id,
        question: {
            id: questionId,
        },
        votes: {
            likeCount,
            userVote: withUserVote ? 1 : 0,
        },
        user: {
            uid: DEFAULT_USER.id,
        },
        author: {
            id: DEFAULT_USER.id,
        },
        canDelete,
    });
};

const createAnswerComments = ({answerId, count}) => [...Array(count)].map(() => createAnswerComment({
    state: 'NEW',
    entityId: answerId,
}));

const prepareUserAnswersTabPage = async (ctx, params = {}) => {
    const {
        answersCount = 1,
        commentsCount = 0,
        canDelete = false,
        withUserVote = false,
        category = DEFAULT_CATEGORY,
        product = DEFAULT_PRODUCT,
        likeCount = DEFAULT_LIKES_COUNT,
    } = params;

    const answers = [];
    const questions = [];
    let comments = [];

    if (answersCount > 0) {
        for (let i = 0; i < answersCount; i++) {
            const answer = createAnswer({
                id: DEFAULT_ANSWER_ID + i,
                questionId: DEFAULT_QUESTION_ID + i,
                canDelete,
                withUserVote,
                commentsCount,
                likeCount,
            });
            answers.push(answer);

            if (commentsCount > 0) {
                comments = comments.concat(createAnswerComments({
                    answerId: DEFAULT_ANSWER_ID + i,
                    count: commentsCount,
                }));
            }

            const question = createQuestion({
                id: DEFAULT_QUESTION_ID + i,
                slug: DEFAULT_QUESTION_SLUG,
                product,
                category,
            });
            questions.push(question);
        }
    }

    await ctx.browser.setState('report', productWithPicture);
    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        modelQuestions: questions,
        modelAnswers: answers,
        commentary: comments,
    });

    await ctx.browser.yaProfile('ugctest3', 'market:my-answers');
    return ctx.browser.yaClosePopup(ctx.createPageObject(RegionPopup));
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница личного кабинета. Вкладка с ответами пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-6453',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    zeroState: () => this.createPageObject(PersonalCabinetZeroState),
                    userAnswers: () => this.createPageObject(UserAnswers),
                    cabinetAnswerSnippet: () => this.createPageObject(CabinetAnswerSnippet),
                    removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                    notification: () => this.createPageObject(Notification),
                    questionHeader: () => this.createPageObject(QuestionHeader),
                    answerContentBody: () => this.createPageObject(AnswerContentBody),
                    questionFooter: () => this.createPageObject(QuestionFooter),
                    voteButton: () => this.createPageObject(VoteButton),
                });
            },
        },
        prepareSuite(ZeroStateSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        answersCount: 0,
                    });
                },
            },
        }),
        prepareSuite(TenLessAnswersSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        answersCount: TEN_LESS_ANSWERS_COUNT,
                    });
                },
            },
            params: {
                answersCount: TEN_LESS_ANSWERS_COUNT,
            },
        }),
        prepareSuite(TenMoreAnswersSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        answersCount: TEN_MORE_ANSWERS_COUNT,
                    });
                },
            },
            params: {
                answersCount: TEN_MORE_ANSWERS_COUNT,
            },
        }),
        prepareSuite(RemovableAnswerSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        canDelete: true,
                    });
                },
            },
        }),
        prepareSuite(ProductAnswerSnippetSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        commentsCount: COMMENTS_COUNT,
                    });
                },
            },
            params: {
                productId: DEFAULT_PRODUCT.id,
                productSlug: DEFAULT_PRODUCT.slug,
                questionSlug: DEFAULT_QUESTION_SLUG,
                questionId: DEFAULT_QUESTION_ID,
                answerId: DEFAULT_ANSWER_ID,
                commentsCount: COMMENTS_COUNT,
                likesCount: DEFAULT_LIKES_COUNT,
            },
        }),
        prepareSuite(CategoryAnswerSnippetSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersTabPage(this, {
                        commentsCount: COMMENTS_COUNT,
                        product: null,
                    });
                },
            },
            params: {
                categoryId: DEFAULT_CATEGORY.id,
                categorySlug: DEFAULT_CATEGORY.slug,
                questionSlug: DEFAULT_QUESTION_SLUG,
                questionId: DEFAULT_QUESTION_ID,
                answerId: DEFAULT_ANSWER_ID,
                commentsCount: COMMENTS_COUNT,
                likesCount: DEFAULT_LIKES_COUNT,
            },
        })
    ),
});
