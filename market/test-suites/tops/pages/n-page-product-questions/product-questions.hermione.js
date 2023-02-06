import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createReportProductStateWithPicture} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';
import {MODEL_QUESTION_ANSWER} from '@self/root/src/entities/agitation/constants';

// suites
import ListQuestionsOnePageSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Questions/List/questionsOnePage';
import ListQuestionsMultiplePagesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/List/questionsMultiplePages';
import QuestionSnippetUserNameSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userName';
import QuestionSnippetUserAvatarSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userAvatar';
import QuestionSnippetPublicationDateSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/publicationDate';
import QuestionSnippetNoAnswersSuite from
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
import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import QuestionsAnswersRemoveSuite from '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Remove';
import QuestionsListNotificationSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Remove/questionsListNotification';
import QuestionsCaptionSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Questions/Caption';
import FormAuthorizeButtonNotExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonNotExists';
import QuestionSnippetRemoveHasAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeHasAnswers';
import QuestionSnippetRemoveOldQuestionSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeOldQuestion';
import QuestionSnippetRemoveDialogSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/removeDialog';
import FormAddQuestionCounterSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/addQuestionCounter';
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
import BelarusBoundPhoneDialogSuite from '@self/platform/spec/hermione/test-suites/blocks/components/BelarusBoundPhoneDialog';
import GainedExpertisePopupSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/GainedExpertisePopup';

// page-objects
import QuestionSnippet from '@self/platform/spec/page-objects/components/Questions/QuestionSnippet';
import ProductQuestionQuestionSnippet from '@self/platform/spec/page-objects/components/Question/QuestionSnippet';
import Caption from '@self/platform/widgets/parts/ProductQuestionsLayout/QuestionList/__pageObject/Caption';
import List from '@self/platform/widgets/parts/ProductQuestionsLayout/QuestionList/__pageObject';
import Paginator from '@self/platform/components/Paginator/__pageObject';
import Remove from '@self/platform/spec/page-objects/components/QuestionsAnswers/Remove';
import InlineNotification from '@self/platform/spec/page-objects/components/InlineNotification';
import Form from '@self/platform/spec/page-objects/components/QuestionsAnswers/Form';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Dialog from '@self/platform/spec/page-objects/components/Dialog';
import BelarusBoundPhoneDialog from '@self/platform/spec/page-objects/components/BelarusBoundPhoneDialog';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

import seo from './seo';

const MOSCOW_REGION = routes.region.ru.lr;
const BELARUS_REGION = routes.region.by.lr;

function getQuestions(amount, params) {
    const {
        productId,
        userUid,
        answersCount = 0,
        slug = '',
        questionTimestamp = Date.now(),
    } = params;

    const canDelete = (answersCount === 0) && (Date.now() - questionTimestamp <= 24 * 60 * 60 * 1000);
    const created = questionTimestamp;

    return [...Array(amount)].map((element, index) => ({
        id: index + 1,
        created,
        product: {
            id: productId,
        },
        user: {
            uid: userUid,
        },
        author: {
            entity: 'user',
            id: userUid,
        },
        text: `Вопрос ${index + 1}`,
        canDelete,
        answersCount,
        slug,
        votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
    }));
}

function getUsers(userUid, publicId) {
    return [{
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
    }];
}

async function prepareQuestionsPage(browser, params) {
    const {
        questionsCount,
        answersCount,
        questionSlug,
        questionTimestamp,
        productSlug = 'model',
    } = params;
    const productId = 1;

    const {uid: userUid, publicId} = params.author || profiles.ugctest3;

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
    const product = createReportProductStateWithPicture({
        id: productId,
        deletedId: null,
        type: 'model',
        slug: productSlug,
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
    await browser.setState('report', product);
    await browser.yaOpenPage('market:product-questions', {productId, slug: productSlug});
}

async function prepareAuthorizedQuestionsPage(browser, params, passportHost) {
    await browser.yaProfile('ugctest3', 'market:index', null, passportHost);
    await prepareQuestionsPage(browser, params);
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вопросов на товар.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Пользователь не авторизован.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareQuestionsPage(this.browser, this.params);
                    },
                },
                prepareSuite(ListQuestionsOnePageSuite, {
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
                prepareSuite(ListQuestionsMultiplePagesSuite, {
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
                prepareSuite(QuestionSnippetNoAnswersSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 0,
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                prepareSuite(HeadBannerProductAbsenceSuite, {
                    meta: {
                        id: 'marketfront-3390',
                        issue: 'MARKETVERSTKA-33961',
                    },
                    params: {
                        pageId: 'market:product-questions',
                    },
                })
            ),
        },
        {
            'Пользователь авторизован.': mergeSuites(
                {
                    async beforeEach() {
                        await prepareAuthorizedQuestionsPage(this.browser, this.params);
                        const users = [createUser({
                            ...profiles.ugctest3,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            id: profiles.ugctest3.uid,
                        })];
                        await this.browser.setState('schema', {
                            users,
                        });
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
                            return this.createPageObject(QuestionSnippet).remove;
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
                prepareSuite(ListQuestionsOnePageSuite, {
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
                prepareSuite(ListQuestionsMultiplePagesSuite, {
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
                prepareSuite(QuestionSnippetNoAnswersSuite, {
                    pageObjects: {
                        questionSnippet() {
                            return this.createPageObject(QuestionSnippet);
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 0,
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                    },
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
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
                            await this.browser.yaOpenPage('market:product-question', {
                                productId: 1,
                                productSlug: 'tovar',
                                questionId: 1,
                                questionSlug: 'moi-vopros',
                            });
                            const gainExpertise = createGainExpertise(MODEL_QUESTION_ANSWER, 30, profiles.ugctest3.uid);
                            await this.browser.setState('storage', {gainExpertise});
                        },
                    },
                    params: {
                        questionsCount: 1,
                        answersCount: 0,
                        author: profiles.ugctest3,
                        questionSlug: 'moi-vopros',
                        productSlug: 'tovar',
                        expectedBadgeText: 'Вы достигли 2 уровня',
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
                        pageId: 'market:product-questions',
                        pageParams: {productId: 1, productSlug: 'tovar'},
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
                        id: 'marketfront-2827',
                        issue: 'MARKETVERSTKA-31058',
                    },
                    pageObjects: {
                        form() {
                            return this.createPageObject(Form);
                        },
                        questionSnippet() {
                            return this.createPageObject(ProductQuestionQuestionSnippet);
                        },
                        inlineNotification() {
                            return this.createPageObject(InlineNotification);
                        },
                    },
                    params: {
                        productSlug: 'tovar',
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
                }),
                seo
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

                        await prepareAuthorizedQuestionsPage(this.browser, this.params, 'passport.yandex.by');
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
                        id: 'marketfront-3101',
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
                            await this.form.setText('Мой новый вопрос');
                            await this.form.clickSubmitButton();
                        },
                    },
                })
            ),
        },

        makeSuite('Диалог подтверждения возраста. Adult контент.', {
            environment: 'kadavr',
            feature: 'Диалог подтверждения возраста',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            adultConfirmationPopup() {
                                return this.createPageObject(AdultConfirmationPopup);
                            },
                        });

                        const productId = 12345;
                        const productSlug = 'foobar';

                        const state = mergeState([
                            createReportProductStateWithPicture({
                                id: productId,
                                deletedId: null,
                                type: 'model',
                                slug: productSlug,
                            }),
                            {
                                data: {
                                    search: {adult: true},
                                },
                            },
                        ]);
                        await this.browser.setState('report', state);
                        await this.browser.yaOpenPage('market:product-questions', {productId, slug: productSlug});
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4036',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4041',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4046',
                    },
                })
            ),
        })
    ),
});
