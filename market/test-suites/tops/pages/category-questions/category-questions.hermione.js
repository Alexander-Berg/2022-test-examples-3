import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createUser, createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import QuestionListTenMoreQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionList/tenMoreQuestions';
import QuestionListTenLessQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionList/tenLessQuestions';
import QuestionSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionSnippet';
import QuestionSnippetMixedProductQuestionSuite from
    '@self/platform/spec/hermione/test-suites/blocks/QuestionSnippet/mixedProductQuestion';
import QuestionFormSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm';
import QuestionFormForQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/QuestionForm/questionFormForQuestion';
import QuestionCategorySnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/CategorySnippet';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import QuestionForm from '@self/platform/spec/page-objects/QuestionForm';
import QuestionCard from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/QuestionCard';
import QuestionList from '@self/platform/spec/page-objects/QuestionList';
import QuestionSnippet from '@self/platform/spec/page-objects/QuestionSnippet';
import PromptDialog from '@self/platform/spec/page-objects/components/PromptDialog';
import CategorySnippet from '@self/platform/spec/page-objects/components/Question/CategorySnippet';

// fixtures
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

const phoneParams = routes.product.phone;

const DEFAULT_HID = 198119;
const DEFAULT_CATEGORY_SLUG = 'elektronika';
const DEFAULT_QUESTION_SLUG = 'moi-vopros';
const DEFAULT_QUESTION_ID = 1234;

const category = {
    id: DEFAULT_HID,
    slug: DEFAULT_CATEGORY_SLUG,
};

function getQuestions(amount, params) {
    const {
        userUid,
        answersCount = 0,
        slug = DEFAULT_QUESTION_SLUG,
        questionTimestamp = Date.now(),
    } = params;

    return [...Array(amount)].map((element, index) => createQuestion({
        id: index + 1,
        created: questionTimestamp,
        category,
        product: null,
        user: {
            uid: userUid,
        },
        text: `Вопрос ${index + 1}`,
        canDelete: answersCount === 0 && (Date.now() - questionTimestamp <= 24 * 60 * 60 * 1000),
        answersCount,
        slug,
    }));
}

function getUsers(userUid) {
    return [createUser({
        id: userUid,
        uid: {
            value: userUid,
        },
        public_id: profiles.ugctest3.publicId,
        login: 'ugctest3',
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
    })];
}

async function prepareQuestionsPage(browser, params, isAuth = false) {
    const {
        questionsCount,
        answersCount,
        questionSlug,
        questionTimestamp,
    } = params;

    const {uid: userUid} = profiles.ugctest3;

    if (questionsCount !== 0) {
        await browser.setState('schema', {
            users: getUsers(userUid),
            modelQuestions: getQuestions(questionsCount, {
                userUid,
                answersCount,
                slug: questionSlug,
                questionTimestamp,
            }),
        });
    }

    await browser.setState('report', productWithPicture);
    const pageParams = {hid: DEFAULT_HID, slug: DEFAULT_CATEGORY_SLUG};
    return isAuth
        ? browser.yaProfile('ugctest3', 'touch:category-questions', pageParams)
        : browser.yaOpenPage('touch:category-questions', pageParams);
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопросов на категорию.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(QuestionListTenMoreQuestionsSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 15});
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
                    await prepareQuestionsPage(this.browser, {questionsCount: 7});
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
                    await this.browser.yaBuildURL('touch:category-question', {
                        categorySlug: DEFAULT_CATEGORY_SLUG,
                        questionSlug: DEFAULT_QUESTION_SLUG,
                        hid: DEFAULT_HID,
                        questionId: DEFAULT_QUESTION_ID,
                    }).then(expectedContentLink => {
                        this.params = Object.assign(this.params, {
                            dataQuestionId: DEFAULT_QUESTION_ID,
                            expectedContentLink,
                            questionType: 'category',
                            slug: DEFAULT_QUESTION_SLUG,
                        });
                    });
                    await prepareQuestionsPage(this.browser, {questionsCount: 0}, true);
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
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
                        root: `[data-question-id="${DEFAULT_QUESTION_ID}"]`,
                    });
                },
            },
        }),
        prepareSuite(QuestionSnippetMixedProductQuestionSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaBuildURL('touch:product', {
                        productId: phoneParams.productId,
                        slug: phoneParams.slug,
                    }).then(expectedProductLink => {
                        this.params = Object.assign(this.params, {
                            dataQuestionId: DEFAULT_QUESTION_ID,
                            expectedProductLink,
                        });
                    });
                    await prepareQuestionsPage(this.browser, {questionsCount: 0}, true);
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                questionList() {
                    return this.createPageObject(QuestionList);
                },
                questionSnippet() {
                    return this.createPageObject(QuestionSnippet, {
                        root: `[data-question-id="${DEFAULT_QUESTION_ID}"]`,
                    });
                },
            },
        }),
        prepareSuite(QuestionFormSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 0});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                questionForm() {
                    return this.createPageObject(QuestionForm);
                },
            },
            params: {
                localStorageKey: 'category-question',
            },
        }),
        prepareSuite(QuestionFormForQuestionSuite, {
            hooks: {
                async beforeEach() {
                    await prepareQuestionsPage(this.browser, {questionsCount: 0}, true);
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
        prepareSuite(QuestionCategorySnippetSuite, {
            hooks: {
                async beforeEach() {
                    const expectedLink = await this.browser.yaBuildURL('touch:list', {
                        slug: 'mobilnye-telefony',
                        nid: '54726',
                    });
                    this.params = {
                        ...this.params,
                        expectedLink,
                    };

                    await prepareQuestionsPage(this.browser, {questionsCount: 3});
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
            pageObjects: {
                categorySnippet() {
                    return this.createPageObject(CategorySnippet);
                },
            },
        })
    ),
});
