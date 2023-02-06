import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createQuestion, createAnswer} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import NoQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/noQuestions';
import TenMoreQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/tenMoreQuestions';
import TenLessQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/tenLessQuestions';
import QuestionSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/questionSnippet';
import ProductQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/productQuestion';
import CategoryQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/categoryQuestion';
import RemovableQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/PersonalCabinet/UserQuestions/removableQuestion';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import UserQuestions from '@self/platform/spec/page-objects/widgets/content/UserQuestions';
import QuestionSnippet from '@self/platform/spec/page-objects/widgets/content/UserQuestions/components/CabinetQuestionSnippet';
import QuestionHeader from '@self/platform/spec/page-objects/widgets/content/UserQuestions/components/QuestionHeader';
import QuestionFooter from '@self/platform/spec/page-objects/widgets/content/UserQuestions/components/QuestionFooter';
import VoteButton from '@self/platform/spec/page-objects/components/VoteButton';

import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

const productId = routes.product.phone.productId;
const productSlug = routes.product.phone.slug;

const {uid: userUid} = profiles.ugctest3;
const user = {
    id: userUid,
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

const defaultCategoryId = 198119;
const defaultCategorySlug = 'elektronika';

const questionSlug = 'lol';
const defaultQuestion = {
    id: 111,
    user: {
        uid: userUid,
        entity: 'user',
    },
    product: {
        id: productId,
        entity: 'product',
    },
    slug: questionSlug,
    category: {
        id: defaultCategoryId,
        slug: defaultCategorySlug,
    },
};

async function prepareQuestionsPage(browser, params) {
    const {
        questionsCount,
        answersCount = 0,
        canDeleteQuestion = false,
        isCategoryQuestion = false,
    } = params;

    await browser.setState('schema', {
        users: [user],
        modelQuestions: new Array(questionsCount)
            .fill(null)
            .map((el, index) => createQuestion({
                ...defaultQuestion,
                id: defaultQuestion.id + index,
                canDelete: canDeleteQuestion,
                product: isCategoryQuestion ? null : defaultQuestion.product,
                answersCount,
            })),
        modelAnswers: new Array(answersCount)
            .fill(null)
            .map((el, index) => createAnswer({
                id: index,
                question: {id: defaultQuestion.id},
                text: 'Test text',
            })),
    });

    await browser.setState('report', productWithPicture);
    return browser.yaProfile('ugctest3', 'market:my-questions');
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Вкладка с вопросами.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(NoQuestionsSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 0});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                userQuestions() {
                    return this.createPageObject(UserQuestions);
                },
            },
        }),
        prepareSuite(TenMoreQuestionsSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 30});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                userQuestions() {
                    return this.createPageObject(UserQuestions);
                },
            },
        }),
        prepareSuite(TenLessQuestionsSuite, {
            params: {
                questionCount: 7,
            },
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 7, answersCount: 1});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                userQuestions() {
                    return this.createPageObject(UserQuestions);
                },
            },
        }),
        prepareSuite(QuestionSnippetSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 1, answersCount: 5});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet);
                },
                voteButton() {
                    return this.createPageObject(VoteButton, {
                        parent: this.questionSnippet,
                    });
                },
                questionHeader() {
                    return this.createPageObject(QuestionHeader, {
                        parent: this.questionSnippet,
                    });
                },
            },
        }),
        prepareSuite(ProductQuestionSuite, {
            params: {
                productId,
                productSlug,
                questionSlug,
                questionId: defaultQuestion.id,
            },
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 1});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet);
                },
                questionFooter() {
                    return this.createPageObject(QuestionFooter, {
                        parent: this.questionSnippet,
                    });
                },
                questionHeader() {
                    return this.createPageObject(QuestionHeader, {
                        parent: this.questionSnippet,
                    });
                },
            },
        }),
        prepareSuite(CategoryQuestionSuite, {
            params: {
                categoryId: defaultCategoryId,
                categorySlug: defaultCategorySlug,
                questionSlug,
                questionId: defaultQuestion.id,
            },
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 1, isCategoryQuestion: true});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet);
                },
                questionFooter() {
                    return this.createPageObject(QuestionFooter, {
                        parent: this.questionSnippet,
                    });
                },
                questionHeader() {
                    return this.createPageObject(QuestionHeader, {
                        parent: this.questionSnippet,
                    });
                },
            },
        }),
        prepareSuite(RemovableQuestionSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 1, canDeleteQuestion: true});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                userQuestions() {
                    return this.createPageObject(UserQuestions);
                },
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet);
                },
                questionHeader() {
                    return this.createPageObject(QuestionHeader, {
                        parent: this.questionSnippet,
                    });
                },
            },
        })
    ),
});
