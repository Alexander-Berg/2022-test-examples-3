import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    DEFAULT_QUESTION_ID,
    createUser,
    createQuestion,
    createAnswers,
    createAnswer,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import QuestionRemoveShownSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionRemove/shown';
import QuestionRemoveHiddenSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionRemove/hidden';
import AnswerRemoveShownSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerRemove/shown';
import AnswerRemoveHiddenSuite from '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswerRemove/hidden';
import QuestionSnippetVotesAuthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/votesAuthorized';
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
import FormAuthorizeButtonNotExists from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonNotExists';
import AnswersListTenMoreAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswersList/tenMoreAnswers';
import AnswersListTenLessAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/AnswersList/tenLessAnswers';
import QuestionSnippetUserExpertiseSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Questions/QuestionSnippet/userExpertise';
import QuestionSnippetVotesUnauthorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/Question/QuestionSnippet/votesUnauthorized';
import CategorySnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/components/CategoryQuestion/categorySnippet';
import FormAuthorizeButtonExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonExists';
import FormAuthorizeButtonLink from
    '@self/platform/spec/hermione/test-suites/blocks/components/QuestionsAnswers/Form/authorizeButtonLink';
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
import BelarusBoundPhoneDialogSuite from '@self/platform/spec/hermione/test-suites/blocks/components/BelarusBoundPhoneDialog';
// page-objects
import AnswersList from '@self/platform/spec/page-objects/components/Question/AnswersList';
import QuestionSnippet from '@self/platform/spec/page-objects/components/Question/QuestionSnippet';
import QuestionsAnswersRemove from '@self/platform/spec/page-objects/components/QuestionsAnswers/Remove';
import Answer from '@self/platform/spec/page-objects/components/Question/Answer';
import CategorySnippet from '@self/platform/spec/page-objects/components/CategoryQuestion/CategorySnippet';
import Dialog from '@self/platform/spec/page-objects/components/Dialog';
import RemoveAnswerDialog from '@self/platform/spec/page-objects/components/Question/RemoveAnswerDialog';
import Form from '@self/platform/spec/page-objects/components/QuestionsAnswers/Form';
import InlineNotification from '@self/platform/spec/page-objects/components/InlineNotification';
import Paginator from '@self/platform/components/Paginator/__pageObject';
import BelarusBoundPhoneDialog from '@self/platform/spec/page-objects/components/BelarusBoundPhoneDialog';
import CommentForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import NoQuestions from '@self/platform/spec/page-objects/components/CategoryQuestions/NoQuestions';
import AuthorExpertise from '@self/root/src/components/AuthorExpertise/__pageObject';

const MOSCOW_REGION = routes.region.ru.lr;
const BELARUS_REGION = routes.region.by.lr;
const DEFAULT_HID = 198119;
const DEFAULT_NID = 54440;
const DEFAULT_CATEGORY_SLUG = 'elektronika';

const category = {
    id: DEFAULT_NID,
    hid: DEFAULT_HID,
    slug: DEFAULT_CATEGORY_SLUG,
    nid: DEFAULT_NID,
};
const navnode = {
    id: DEFAULT_NID,
    category,
    fullName: 'Тестовая категория',
    slug: DEFAULT_CATEGORY_SLUG,
};
const expertise = {
    expertiseId: 9,
    value: 33,
    levelValue: 13,
    level: 2,
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

// Этот способ авторизации нужен для проверки Беларуси
async function authorize(passportHost) {
    await this.browser.setState('schema', {
        users: [createUser()],
    });
    await this.browser.yaProfile('ugctest3', 'market:index', null, passportHost);
    return profiles.ugctest3.uid;
}

const createRouteParams = question => ({
    categorySlug: category.slug,
    hid: category.hid,
    questionSlug: question.slug,
    questionId: question.id,
});

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница категорийного вопроса.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Авторизованный пользователь.': mergeSuites(
                {
                    async beforeEach() {
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
                prepareSuite(QuestionRemoveShownSuite, {
                    meta: {
                        id: 'marketfront-2884',
                        issue: 'MARKETVERSTKA-31263',
                        feature: 'Структура страницы',
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const question = createQuestion({canDelete: true});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
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
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
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
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
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
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetUserExpertiseSuite, {
                    meta: {
                        id: 'marketfront-4170',
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
                            const question = createQuestion({
                                answersCount,
                                canDelete: true,
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
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetVotesAuthorizedSuite, {
                    hooks: {
                        async beforeEach() {
                            const user = createUser({
                                ...profiles.ugctest3,
                                uid: {
                                    value: profiles.ugctest3.uid,
                                },
                                id: profiles.ugctest3.uid,
                                public_id: profiles.ugctest3.publicId,
                            });
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
                            await this.browser.setState('report', productWithPictureState);
                            return this.browser.yaProfile(
                                'ugctest3',
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                makeSuite('Форма добавления ответа', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const user = createUser({
                                    ...profiles.ugctest3,
                                    uid: {
                                        value: profiles.ugctest3.uid,
                                    },
                                    id: profiles.ugctest3.uid,
                                    public_id: profiles.ugctest3.publicId,
                                });
                                const question = createQuestion({
                                    category: {
                                        id: DEFAULT_HID,
                                    },
                                });

                                const schema = {
                                    users: [user],
                                    modelQuestions: [question],
                                };

                                await this.browser.setState('schema', schema);
                                await this.browser.setState('report', productWithPictureState);
                                return this.browser.yaProfile(
                                    'ugctest3',
                                    'market:category-question',
                                    createRouteParams(question)
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
                        prepareSuite(FormAuthorizeButtonNotExists, {
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
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            await this.browser.yaOpenPage(
                                'market:category-question',
                                createRouteParams(question)
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
                            const question = createQuestion({answersCount});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                                modelAnswers: createAnswers(answersCount),
                            };
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            await this.browser.yaOpenPage(
                                'market:category-question',
                                createRouteParams(question)
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
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            await this.browser.yaOpenPage(
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                prepareSuite(QuestionSnippetVotesUnauthorizedSuite, {
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const question = createQuestion({canDelete: false});

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            await this.browser.yaOpenPage(
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                prepareSuite(CategorySnippetSuite, {
                    meta: {
                        id: 'marketfront-3597',
                        issue: 'MARKETVERSTKA-34994',
                    },
                    pageObjects: {
                        categorySnippet() {
                            return this.createPageObject(CategorySnippet);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const user = createUser();
                            const question = createQuestion({
                                category: {
                                    id: DEFAULT_HID,
                                },
                                votes: {
                                    likeCount: 1,
                                    dislikeCount: 0,
                                    userVote: 0,
                                },
                                canDelete: false,
                            });

                            this.params.expectedUrl = await this.browser.yaBuildURL(
                                'market:category-questions',
                                {
                                    slug: DEFAULT_CATEGORY_SLUG,
                                    hid: DEFAULT_HID,
                                }
                            );

                            const schema = {
                                users: [user],
                                modelQuestions: [question],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', productWithPictureState);
                            await this.browser.yaOpenPage(
                                'market:category-question',
                                createRouteParams(question)
                            );
                        },
                    },
                }),
                makeSuite('Кнопка Войти', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const user = createUser();
                                const question = createQuestion({
                                    category: {
                                        id: DEFAULT_HID,
                                    },
                                });

                                const schema = {
                                    users: [user],
                                    modelQuestions: [question],
                                };

                                await this.browser.setState('schema', schema);
                                await this.browser.setState('report', productWithPictureState);
                                await this.browser.yaOpenPage(
                                    'market:category-question',
                                    createRouteParams(question)
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
        prepareSuite(QuestionSnippetSuite, {
            meta: {
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
                    await this.browser.setState('report', productWithPictureState);
                    await this.browser.yaOpenPage(
                        'market:category-question',
                        createRouteParams(question)
                    );
                },
            },
        }),
        makeSuite('Удаление вопроса', {
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
                        const question = createQuestion({
                            category,
                            canDelete: true,
                            product: null,
                        });

                        this.params.expectedTargetUrl = await this.browser.yaBuildURL('market:category-questions', {
                            hid: DEFAULT_HID,
                            slug: DEFAULT_CATEGORY_SLUG,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.setState('Cataloger.tree', navnode);
                        return this.browser.yaProfile(
                            'ugctest3',
                            'market:category-question',
                            createRouteParams(question)
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
                        const question = createQuestion({
                            answersCount: 2,
                            canDelete: false,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: new Array(2).fill().map((_, index) => createAnswer({
                                id: index,
                                question: {
                                    id: DEFAULT_QUESTION_ID,
                                },
                                canDelete: true,
                            })),
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', productWithPictureState);
                        return this.browser.yaProfile(
                            'ugctest3',
                            'market:category-question',
                            createRouteParams(question)
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
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            form: () => this.createPageObject(Form),
                            inlineNotification: () => this.createPageObject(InlineNotification),
                            answersList: () => this.createPageObject(AnswersList),
                            answerSnippet: () => this.createPageObject(Answer),
                        });

                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const question = createQuestion({
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
                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.setState('Cataloger.tree', navnode);
                        return this.browser.yaProfile(
                            'ugctest3',
                            'market:category-question',
                            createRouteParams(question)
                        );
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionAddAnswerSuite, {
                    meta: {
                        id: 'marketfront-3600',
                        issue: 'MARKETVERSTKA-35000',
                    },
                }),
                prepareSuite(QuestionAddAnswerCounterSuite)
            ),
        }),
        makeSuite('Добавление первого ответа к вопросу', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            form: () => this.createPageObject(Form),
                            inlineNotification: () => this.createPageObject(InlineNotification),
                            answersList: () => this.createPageObject(AnswersList),
                            answerSnippet: () => this.createPageObject(Answer),
                        });

                        const user = createUser({
                            id: profiles.ugctest3.uid,
                            uid: {
                                value: profiles.ugctest3.uid,
                            },
                            public_id: profiles.ugctest3.publicId,
                        });
                        const question = createQuestion({
                            category: {
                                id: DEFAULT_HID,
                            },
                            answersCount: 0,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                            modelAnswers: [],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.setState('Cataloger.tree', navnode);
                        return this.browser.yaProfile(
                            'ugctest3',
                            'market:category-question',
                            createRouteParams(question)
                        );
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                prepareSuite(QuestionAddAnswerSuite, {
                    meta: {
                        id: 'marketfront-3600',
                        issue: 'MARKETVERSTKA-35000',
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
                    await this.browser.setState('report', productWithPictureState);
                    return this.browser.yaOpenPage(
                        'market:category-question',
                        createRouteParams(question)
                    );
                },
            },
        }),
        prepareSuite(AnswerVotesVotesAuthorizedSuite, {
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
                    await this.browser.setState('report', productWithPictureState);
                    return this.browser.yaProfile(
                        'ugctest3',
                        'market:category-question',
                        createRouteParams(question)
                    );
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
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
                        const question = createQuestion({
                            category: {
                                id: DEFAULT_HID,
                            },
                            answersCount: 0,
                        });

                        const schema = {
                            users: [user],
                            modelQuestions: [question],
                        };

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.setState('Cataloger.tree', navnode);

                        await this.browser.yaOpenPage(
                            'market:category-question',
                            createRouteParams(question)
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
                        answerSnippet() {
                            return this.createPageObject(Answer);
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
                        const question = createQuestion({
                            category: {
                                id: DEFAULT_HID,
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
                        await this.browser.setState('report', productWithPictureState);
                        await this.browser.setState('Cataloger.tree', navnode);

                        await this.browser.yaOpenPage(
                            'market:category-question',
                            createRouteParams(question)
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
