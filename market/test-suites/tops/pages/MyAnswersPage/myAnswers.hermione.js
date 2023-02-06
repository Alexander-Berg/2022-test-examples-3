import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser, createQuestion, createAnswer} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

// suites
import ZeroStateSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinetZeroState';
import TenLessAnswersSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserAnswers/tenLessAnswers';
import TenMoreAnswersSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserAnswers/tenMoreAnswers';
import ContentUserInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ContentUserInfo';
import QAFooterSuite from '@self/platform/spec/hermione/test-suites/blocks/components/PersonalCabinetQAFooter';
import RemovableAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/components/PersonalCabinetCard/removableCard';
import PersonalCabinetHeadlineSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinetProductHeadline';
import CabinetAnswerSnippet from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserAnswers/cabinetAnswerSnippet';


// page-objects
import UserAnswers from '@self/platform/widgets/content/UserAnswers/__pageObject';
import SnippetHeadline from '@self/platform/widgets/content/UserQuestions/components/SnippetHeadline/__pageObject';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER_INFO = {
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
    public_id: USER_PROFILE_CONFIG.publicId,
};
const DEFAULT_USER = createUser(DEFAULT_USER_INFO);

const DEFAULT_CATEGORY = {
    id: 198119,
    slug: 'elektronika',
};
const DEFAULT_QUESTION = {
    id: 213,
    slug: 'default-question-slug',
    likeCount: 1,
    text: 'Awesome question',
};
const DEFAULT_ANSWER = {
    id: 1221,
    text: 'Awesome answer to an awesome question',
    likeCount: 5,
};
const DEFAULT_PRODUCT_CONFIG = {
    id: 14206682,
    slug: 'smartfon-apple-iphone-7-128gb',
    entity: 'product',
};
const productWithPictureState = createProduct({
    ...DEFAULT_PRODUCT_CONFIG,
    pictures: [{
        original: {
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
        },
        thumbnails: [{
            containerWidth: 500,
            containerHeight: 500,
            url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/9hq',
            width: 500,
            height: 500,
        }],
    }],
}, DEFAULT_PRODUCT_CONFIG.id);

const TEN_LESS_ANSWERS_COUNT = 5;
const TEN_MORE_ANSWERS_COUNT = 15;

const prepareUserAnswersPage = async (ctx, params = {}) => {
    const {
        answersCount = 1,
        category = DEFAULT_CATEGORY,
        product = DEFAULT_PRODUCT_CONFIG,
        canDelete = true,
    } = params;

    const answers = [];
    const questions = [];

    if (answersCount > 0) {
        for (let i = 0; i < answersCount; i++) {
            const answer = createAnswer({
                canDelete,
                text: DEFAULT_ANSWER.text,
                id: DEFAULT_ANSWER.id + i,
                question: {
                    id: DEFAULT_QUESTION.id + i,
                },
                votes: {
                    likeCount: DEFAULT_ANSWER.likeCount,
                    userVote: 0,
                },
                user: {
                    uid: DEFAULT_USER_INFO.id,
                },
                author: {
                    id: DEFAULT_USER_INFO.id,
                },
            });
            answers.push(answer);

            const question = createQuestion({
                id: DEFAULT_QUESTION.id + i,
                slug: DEFAULT_QUESTION.slug,
                text: DEFAULT_QUESTION.text,
                product,
                category,
            });
            questions.push(question);
        }
    }

    await ctx.browser.setState('report', productWithPictureState);
    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        modelQuestions: questions,
        modelAnswers: answers,
    });

    await ctx.browser.yaProfile('ugctest3', 'market:my-answers', {});
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Личный кабинет. Страница с ответами пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-7561',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    userAnswers: () => this.createPageObject(UserAnswers),
                    snippetHeadline: () => this.createPageObject(SnippetHeadline),
                });
            },
        },
        prepareSuite(ZeroStateSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersPage(this, {
                        answersCount: 0,
                    });
                },
            },
        }),
        prepareSuite(TenLessAnswersSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserAnswersPage(this, {
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
                    await prepareUserAnswersPage(this, {
                        answersCount: TEN_MORE_ANSWERS_COUNT,
                    });
                },
            },
            params: {
                answersCount: TEN_MORE_ANSWERS_COUNT,
            },
        }),
        {
            'Сниппет ответа на товар.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareUserAnswersPage(this);

                        const questionLink = await this.browser.yaBuildURL('market:product-question', {
                            productId: DEFAULT_PRODUCT_CONFIG.id,
                            productSlug: DEFAULT_PRODUCT_CONFIG.slug,
                            questionSlug: DEFAULT_QUESTION.slug,
                            questionId: DEFAULT_QUESTION.id,
                        });
                        const productLink = await this.browser.yaBuildURL('market:product', {
                            productId: DEFAULT_PRODUCT_CONFIG.id,
                            slug: DEFAULT_PRODUCT_CONFIG.slug,
                        });

                        this.params = {
                            ...this.params,
                            likesCount: DEFAULT_ANSWER.likeCount,
                            linkText: 'Нет комментариев',
                            userName: DEFAULT_USER_INFO.display_name.public_name,
                            footerLink: questionLink,
                            headerLink: productLink,
                            questionText: DEFAULT_QUESTION.text,
                            questionContentLink: questionLink,
                            answerText: DEFAULT_ANSWER.text,
                            answerContentLink: questionLink,
                        };
                    },
                },
                prepareSuite(ContentUserInfoSuite),
                prepareSuite(PersonalCabinetHeadlineSuite),
                prepareSuite(QAFooterSuite),
                prepareSuite(RemovableAnswerSuite),
                prepareSuite(CabinetAnswerSnippet)
            ),
        },
        {
            'Сниппет ответа на категорию.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareUserAnswersPage(this, {
                            product: null,
                        });

                        const questionLink = await this.browser.yaBuildURL('market:category-question', {
                            hid: DEFAULT_CATEGORY.id,
                            questionId: DEFAULT_QUESTION.id,
                            categorySlug: DEFAULT_CATEGORY.slug,
                            questionSlug: DEFAULT_QUESTION.slug,
                        });
                        const categoryLink = await this.browser.yaBuildURL('market:category-questions', {
                            hid: DEFAULT_CATEGORY.id,
                            slug: DEFAULT_CATEGORY.slug,
                        });

                        this.params = {
                            ...this.params,
                            likesCount: DEFAULT_ANSWER.likeCount,
                            linkText: 'Нет комментариев',
                            userName: DEFAULT_USER_INFO.display_name.public_name,
                            footerLink: questionLink,
                            headerLink: categoryLink,
                            questionText: DEFAULT_QUESTION.text,
                            questionContentLink: questionLink,
                            answerText: DEFAULT_ANSWER.text,
                            answerContentLink: questionLink,
                        };
                    },
                },
                prepareSuite(ContentUserInfoSuite),
                prepareSuite(PersonalCabinetHeadlineSuite),
                prepareSuite(QAFooterSuite),
                prepareSuite(RemovableAnswerSuite),
                prepareSuite(CabinetAnswerSnippet)
            ),
        }
    ),
});
