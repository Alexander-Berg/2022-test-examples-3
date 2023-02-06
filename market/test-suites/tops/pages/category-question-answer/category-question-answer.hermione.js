import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    createQuestion as createQuestionHelper,
    createAnswer,
    createUser,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';

import ContentManageControls from '@self/platform/components/ContentManageControls/__pageObject';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';
import Notification from '@self/root/src/components/Notification/__pageObject';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';

import TwoLevelCommentariesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/twoLevelCommentaries';
import ComplainAnswerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductQuestionAnswer/othersAnswer';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_HID = 198119;
const DEFAULT_CATEGORY_SLUG = 'elektronika';
const DEFAULT_QUESTION_SLUG = 'moi-vopros';
const DEFAULT_QUESTION_ID = 1234;

const DEFAULT_ANSWER_ID = 1111;

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

async function prepareCategoryAnswerSchema(params = {}) {
    const {
        canDelete = false,
        answerText = 'My awesome answer',
        canDeleteAnswer = true,
    } = params;

    return {
        users: [DEFAULT_USER],
        modelQuestions: [createQuestion({
            answersCount: 1,
            canDelete,
        })],
        modelAnswers: [createAnswer({
            id: DEFAULT_ANSWER_ID,
            question: {
                id: DEFAULT_QUESTION_ID,
            },
            text: answerText,
            canDelete: canDeleteAnswer,
        })],
    };
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница ответа на категорийный вопрос.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-5122',
    story: mergeSuites(
        prepareSuite(TwoLevelCommentariesSuite, {
            params: {
                pageTemplate: 'touch:category-question-answer',
                pageParams: {
                    'answerId': DEFAULT_ANSWER_ID,
                    'no-tests': 1,
                },
                entityId: DEFAULT_ANSWER_ID,
                defaultLimit: 5,
            },
            hooks: {
                async beforeEach() {
                    const schema = await prepareCategoryAnswerSchema();
                    this.params = {
                        ...this.params,
                        schema,
                    };
                },
            },
        }),
        prepareSuite(ComplainAnswerSuite, {
            hooks: {
                async beforeEach() {
                    const schema = await prepareCategoryAnswerSchema({canDeleteAnswer: false});
                    await this.browser.setState('schema', schema);
                    await this.browser.yaProfile('pan-topinambur', 'touch:category-question-answer',
                        {answerId: DEFAULT_ANSWER_ID});
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
        })
    ),
});
