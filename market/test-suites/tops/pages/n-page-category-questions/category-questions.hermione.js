import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser, createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import BelarusBoundPhoneDialogSuite from '@self/platform/spec/hermione/test-suites/blocks/components/BelarusBoundPhoneDialog';
import QuestionsOnePageSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Questions/List/questionsOnePage';
import QuestionsMultiplePagesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/List/questionsMultiplePages';
import QuestionSnippetUserNameSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userName';
import QuestionSnippetUserAvatarSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userAvatar';
import QuestionSnippetPublicationDateSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/publicationDate';
import QuestionSnippetNoAnswers from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/noAnswers';
import QuestionSnippetHasAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/hasAnswers';
import QuestionSnippetAnswerSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/answer';
import QuestionSnippetQuestionContentSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/questionContent';
import FormAuthorizeButtonExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonExists';
import FormAuthorizeButtonLinkSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonLink';
import QuestionVotesVotesUnauthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionVotes/votesUnauthorized';
import CategorySnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/components/CategoryQuestion/categorySnippet';
import QuestionsAnswersRemoveSuite from '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Remove';
import QuestionsListNotificationSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Remove/questionsListNotification';
import QuestionsCaptionSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Questions/Caption';
import QuestionSnippetRemoveHasAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeHasAnswers';
import QuestionSnippetRemoveOldQuestionSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeOldQuestion';
import QuestionSnippetRemoveDialogSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeDialog';
import FormAddQuestionCounterSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/addQuestionCounter';
import FormAuthorizeButtonNotExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonNotExists';
import FormAddQuestionSuite from '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/addQuestion';
import FormClearTextFieldSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/clearTextField';
import FormMaxCharactersCountSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/maxCharactersCount';
import FormCharactersCountSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/charactersCount';
import FormTextFieldBackupSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/textFieldBackup';
import QuestionVotesVotesAuthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionVotes/votesAuthorized';
// page-objects
import QuestionSnippet from '@self/platform/spec/page-objects/components/Questions/QuestionSnippet';
import BigQuestionSnippet from '@self/platform/spec/page-objects/components/Question/QuestionSnippet';
import Caption from '@self/platform/spec/page-objects/widgets/parts/QuestionsLayout/QuestionList/Caption';
import List from '@self/platform/spec/page-objects/widgets/parts/QuestionsLayout/QuestionList/List';
import Paginator from '@self/platform/components/Paginator/__pageObject';
import Remove from '@self/platform/spec/page-objects/components/QuestionsAnswers/Remove';
import InlineNotification from '@self/platform/spec/page-objects/components/InlineNotification';
import Form from '@self/platform/spec/page-objects/components/QuestionsAnswers/Form';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Dialog from '@self/platform/spec/page-objects/components/Dialog';
import BelarusBoundPhoneDialog from '@self/platform/spec/page-objects/components/BelarusBoundPhoneDialog';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import CategorySnippet from '@self/platform/spec/page-objects/components/CategoryQuestion/CategorySnippet';

import defaultOffer from './defaultOffer';

const MOSCOW_REGION = routes.region.ru.lr;
const BELARUS_REGION = routes.region.by.lr;

const DEFAULT_NID = 54440;
const DEFAULT_HID = 198119;
const DEFAULT_CATEGORY_SLUG = 'elektronika';
const DEFAULT_QUESTION_SLUG = 'moi-vopros';

const category = {
    id: DEFAULT_HID,
    slug: DEFAULT_CATEGORY_SLUG,
};
const productWithPictureState = createProduct({
    category,
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
});

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
        author: {
            entity: 'user',
            id: userUid,
        },
        text: `Вопрос ${index + 1}`,
        canDelete: answersCount === 0 && (Date.now() - questionTimestamp <= 24 * 60 * 60 * 1000),
        answersCount,
        slug,
    }));
}

function getUsers(userUid, publicId) {
    return [createUser({
        id: userUid,
        uid: {
            value: userUid,
        },
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
        public_id: publicId,
    })];
}

async function prepareQuestionsPage(browser, params, isAuth = false) {
    const {
        questionsCount,
        answersCount,
        questionSlug,
        questionTimestamp,
    } = params;
    const productId = 1;

    const {uid: userUid, publicId} = profiles.ugctest3;

    await browser.setState('schema', {
        users: getUsers(userUid, publicId),
        modelQuestions: getQuestions(questionsCount, {
            productId,
            userUid,
            answersCount,
            slug: questionSlug,
            questionTimestamp,
        }),
    });
    await browser.setState('report', productWithPictureState);
    const pageParams = {hid: DEFAULT_HID, slug: DEFAULT_CATEGORY_SLUG};
    return isAuth
        ? browser.yaProfile('ugctest3', 'market:category-questions', pageParams)
        : browser.yaOpenPage('market:category-questions', pageParams);
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопросов на категорию.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Пользователь не авторизован.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareQuestionsPage(this.browser, this.params);
                    },
                },
                prepareSuite(QuestionsOnePageSuite, {
                    pageObjects: {
                        list() {
                            return this.createPageObject(List);
                        },
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    params: {
                        questionsCount: 7,
                    },
                }),
                prepareSuite(QuestionsMultiplePagesSuite, {
                    pageObjects: {
                        list() {
                            return this.createPageObject(List);
                        },
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    params: {
                        questionsCount: 12,
                    },
                }),
                prepareSuite(QuestionSnippetUserNameSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetUserAvatarSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetPublicationDateSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetNoAnswers, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 0,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetHasAnswersSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 5,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetAnswerSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetQuestionContentSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(FormAuthorizeButtonExistsSuite, {
                    meta: {
                        id: 'marketfront-2814',
                        issue: 'MARKETVERSTKA-31048',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                }),
                prepareSuite(FormAuthorizeButtonLinkSuite, {
                    meta: {
                        id: 'marketfront-2816',
                        issue: 'MARKETVERSTKA-31047',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                }),
                prepareSuite(QuestionVotesVotesUnauthorizedSuite, {
                    pageObjects: {
                        questionVotes() {
                            const questionSnippet = this.createPageObject(QuestionSnippet);
                            return this.createPageObject(Votes, {
                                parent: questionSnippet,
                            });
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(CategorySnippetSuite, {
                    meta: {
                        id: 'marketfront-3588',
                        issue: 'MARKETVERSTKA-34986',
                    },
                    pageObjects: {
                        categorySnippet() {
                            return this.createPageObject(CategorySnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 5,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:list', {
                                nid: DEFAULT_NID,
                                slug: DEFAULT_CATEGORY_SLUG,
                            });
                        },
                    },
                })
            ),
        },
        {
            'Пользователь авторизован.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareQuestionsPage(this.browser, this.params, true);
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionsAnswersRemoveSuite, {
                    pageObjects: {
                        remove() {
                            return this.createPageObject(Remove);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionsListNotificationSuite, {
                    pageObjects: {
                        remove() {
                            return this.createPageObject(Remove);
                        },
                        dialog() {
                            return this.createPageObject(Dialog, {
                                root: '.removeQuestionDialogPortal',
                            });
                        },
                        notification() {
                            return this.createPageObject(Notification);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionsCaptionSuite, {
                    pageObjects: {
                        caption() {
                            return this.createPageObject(Caption);
                        },
                        remove() {
                            return this.createPageObject(Remove);
                        },
                        dialog() {
                            return this.createPageObject(Dialog, {
                                root: '.removeQuestionDialogPortal',
                            });
                        },
                    },
                    params: {
                        questionsCount: 2,
                    },
                }),
                prepareSuite(QuestionsOnePageSuite, {
                    pageObjects: {
                        list() {
                            return this.createPageObject(List);
                        },
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    params: {
                        questionsCount: 7,
                    },
                }),
                prepareSuite(QuestionsMultiplePagesSuite, {
                    pageObjects: {
                        list() {
                            return this.createPageObject(List);
                        },
                        paginator() {
                            return this.createPageObject(Paginator);
                        },
                    },
                    params: {
                        questionsCount: 12,
                    },
                }),
                prepareSuite(QuestionSnippetUserNameSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetUserAvatarSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetPublicationDateSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(QuestionSnippetNoAnswers, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 0,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetHasAnswersSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 5,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetAnswerSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetQuestionContentSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:category-question', {
                                hid: DEFAULT_HID,
                                questionId: 1,
                                categorySlug: DEFAULT_CATEGORY_SLUG,
                                questionSlug: DEFAULT_QUESTION_SLUG,
                            });
                        },
                    },
                }),
                prepareSuite(QuestionSnippetRemoveHasAnswersSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 2,
                    },
                }),
                prepareSuite(QuestionSnippetRemoveOldQuestionSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        questionTimestamp: Date.now() - (25 * 60 * 60 * 1000),
                    },
                }),
                prepareSuite(QuestionSnippetRemoveDialogSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                        dialog() {
                            return this.createPageObject(Dialog, {
                                root: '.removeQuestionDialogPortal',
                            });
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                }),
                prepareSuite(FormAddQuestionCounterSuite, {
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                        caption() {
                            return this.createPageObject(Caption);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        pageId: 'market:category-questions',
                        pageParams: {hid: DEFAULT_HID, slug: DEFAULT_CATEGORY_SLUG},
                    },
                }),
                prepareSuite(FormAuthorizeButtonNotExistsSuite, {
                    meta: {
                        id: 'marketfront-2815',
                        issue: 'MARKETVERSTKA-31049',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                }),
                prepareSuite(FormAddQuestionSuite, {
                    meta: {
                        id: 'marketfront-3594',
                        issue: 'MARKETVERSTKA-34998',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                        questionSnippet() {
                            return this.createPageObject(BigQuestionSnippet);
                        },
                        inlineNotification() {
                            return this.createPageObject(InlineNotification);
                        },
                    },
                }),
                prepareSuite(FormClearTextFieldSuite, {
                    meta: {
                        id: 'marketfront-2831',
                        issue: 'MARKETVERSTKA-31059',
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
                prepareSuite(FormMaxCharactersCountSuite, {
                    meta: {
                        id: 'marketfront-2841',
                        issue: 'MARKETVERSTKA-31056',
                        feature: 'Добавление вопроса',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                    params: {
                        questionsCount: 0,
                        charactersCount: 2000,
                        maxCharactersCount: 2000,
                    },
                }),
                prepareSuite(FormCharactersCountSuite, {
                    meta: {
                        id: 'marketfront-2834',
                        issue: 'MARKETVERSTKA-31055',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                    params: {
                        questionsCount: 0,
                        charactersCount: 1000,
                        maxCharactersCount: 2000,
                    },
                }),
                prepareSuite(FormTextFieldBackupSuite, {
                    meta: {
                        id: 'marketfront-2832',
                        issue: 'MARKETVERSTKA-31054',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                    },
                    params: {
                        localStorageKey: 'product-questions',
                    },
                }),
                prepareSuite(QuestionVotesVotesAuthorizedSuite, {
                    pageObjects: {
                        questionVotes() {
                            const questionSnippet = this.createPageObject(QuestionSnippet);
                            return this.createPageObject(Votes, {
                                parent: questionSnippet,
                            });
                        },
                    },
                    params: {
                        questionsCount: 1,
                    },
                })
            ),
        },
        {
            'Пользователь авторизован на домене .by и создает новый вопрос': mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.yaOpenPage('market:index', {
                            pcr: MOSCOW_REGION,
                            lr: BELARUS_REGION,
                        });

                        // перенос сессионной куки кадавра на домен .by
                        // и смена региона на Минск
                        await this.browser.setKadavrSesssionCookies();

                        await this.browser.yaProfile('ugctest3', 'market:index', null, 'passport.yandex.by');
                        await prepareQuestionsPage(this.browser, this.params);
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
                        id: 'marketfront-3595',
                        issue: 'MARKETVERSTKA-34989',
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
                            await this.form.setText('Мой новый вопрос');
                            await this.form.clickSubmitButton();
                        },
                    },
                })
            ),
        },
        defaultOffer
    ),
});
