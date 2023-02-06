// @flow
// flowlint untyped-import:off
import {screen, waitFor, fireEvent, within} from '@testing-library/dom';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockLocation, mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as voteQuestionActions from '@self/root/src/actions/answer/vote';

import {
    DEFAULT_PRODUCT_ID,
    DEFAULT_QUESTION_ID,
    createUser,
    createProduct as createQuestionProduct,
    createQuestion,
    createReportProductStateWithPicture,
    createAnswer,
} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import type {Config} from '../controller';
import {user as userMock} from './__mocks__/user';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const widgetPath = require.resolve('@self/platform/widgets/parts/QuestionLayout/AnswerList');

beforeAll(async () => {
    mockLocation();
    mockIntersectionObserver();

    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(
        require.resolve('@self/project/src/utils/router'),
        () => ({buildURL: pageId => pageId, buildUrl: pageId => pageId})
    );

    await jestLayer.doMock(
        require.resolve('@self/platform/widgets/parts/Paginator'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.doMock(
        require.resolve('@self/platform/widgets/parts/QuestionLayout/AnswerForm'),
        () => ({create: () => Promise.resolve(null)})
    );
});

async function makeContext(userParam) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};
    return mandrelLayer.initContext({user: userParam || userMock, request: {cookie}});
}

afterAll(() => {
    mirror.destroy();
});

function makeSchema(params = {}) {
    const user = params.user || createUser(userMock);
    const question = createQuestion({
        answersCount: 1,
    });

    return {
        users: [user],
        modelQuestions: [question],
    };
}

describe('AnswerList', () => {
    const widgetProps: Config = {
        questionId: DEFAULT_QUESTION_ID,
        productId: DEFAULT_PRODUCT_ID,
        categoryId: 666,
        isQuestionAuthor: false,
        routeParams: {
            hid: 666,
            questionId: 0,
            categorySlug: 'category',
            questionSlug: 'question',
        },
        pageRoute: 'market:category-question',
    };

    describe('сниппет ответа на вопрос', () => {
        describe('тулбар', () => {
            const addLikeVoteAnswerSpy = jest.spyOn(voteQuestionActions, 'addLikeVoteAnswer');
            const removeLikeVoteAnswerSpy = jest.spyOn(voteQuestionActions, 'removeLikeVoteAnswer');
            const addDislikeVoteAnswerSpy = jest.spyOn(voteQuestionActions, 'addDislikeVoteAnswer');
            const removeDislikeVoteAnswerSpy = jest.spyOn(voteQuestionActions, 'removeDislikeVoteAnswer');

            describe('при клике на лайк', () => {
                describe('если пользователь авторизован', () => {
                    it('увеличивается кол-во лайков', async () => {
                        await makeContext();

                        const likeCount = 6;
                        const dislikeCount = 6;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: 0}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const upVote = screen.getByRole('up-vote');

                        fireEvent.click(within(upVote).getByRole('button'));

                        await waitFor(() => {
                            expect(addLikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(upVote).getByRole('button')).toHaveTextContent(String(likeCount + 1));
                        });
                    });

                    it('если лайк уже был поставлен, уменьшается кол-во лайков', async () => {
                        await makeContext();

                        const likeCount = 6;
                        const dislikeCount = 6;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: 1}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const upVote = screen.getByRole('up-vote');
                        fireEvent.click(within(upVote).getByRole('button'));

                        await waitFor(() => {
                            expect(removeLikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(upVote).getByRole('button')).toHaveTextContent(String(likeCount - 1));
                        });
                    });

                    it('если дизлайк был поставлен, увеличивается количество лайков и уменьшается кол-во дизлайков', async () => {
                        await makeContext();

                        const dislikeCount = 9;
                        const likeCount = 9;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: -1}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const upVote = screen.getByRole('up-vote');
                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(upVote).getByRole('button'));

                        await waitFor(() => {
                            expect(addLikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(downVote).getByRole('button')).toHaveTextContent(String(dislikeCount - 1));
                            expect(within(upVote).getByRole('button')).toHaveTextContent(String(likeCount + 1));
                        });
                    });

                    it('не происходит редирект', async () => {
                        await makeContext();

                        const dislikeCount = 6;
                        const likeCount = 6;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: 0}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        window.location.assign.mockClear();

                        const upVote = screen.getByRole('up-vote');

                        fireEvent.click(within(upVote).getByRole('button'));

                        await waitFor(() => {
                            expect(window.location.assign).toHaveBeenCalledTimes(0);
                        });
                    });
                });

                describe('если пользователь не авторизован', () => {
                    it('происходит редирект', async () => {
                        await makeContext({isAuth: false});

                        const schema = makeSchema({user: {isAuth: false}});

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer()]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        window.location.assign.mockClear();

                        const upVote = screen.getByRole('up-vote');

                        fireEvent.click(within(upVote).getByRole('button'));

                        await waitFor(() => {
                            expect(window.location.assign).toHaveBeenCalledTimes(1);
                        });
                    });
                });
            });

            describe('при клике на дизлайк', () => {
                describe('если пользователь авторизован', () => {
                    it('увеличивается кол-во дизлайков', async () => {
                        await makeContext();

                        const likeCount = 6;
                        const dislikeCount = 6;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: 0}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(downVote).getByRole('button'));

                        await waitFor(() => {
                            expect(addDislikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(downVote).getByRole('button')).toHaveTextContent(String(likeCount + 1));
                        });
                    });

                    it('если дизлайк уже был поставлен, уменьшается кол-во дизлайков', async () => {
                        await makeContext();

                        const likeCount = 6;
                        const dislikeCount = 6;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: -1}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(downVote).getByRole('button'));

                        await waitFor(() => {
                            expect(removeDislikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(downVote).getByRole('button')).toHaveTextContent(String(dislikeCount - 1));
                        });
                    });

                    it('если лайк был поставлен, увеличивается количество дизлайков и уменьшается кол-во лайков', async () => {
                        await makeContext();

                        const dislikeCount = 9;
                        const likeCount = 9;

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer({votes: {likeCount, dislikeCount, userVote: 1}})]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        const upVote = screen.getByRole('up-vote');
                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(downVote).getByRole('button'));

                        await waitFor(() => {
                            expect(addDislikeVoteAnswerSpy).toHaveBeenCalledWith({answerId: 1});
                        });

                        await waitFor(() => {
                            expect(within(downVote).getByRole('button')).toHaveTextContent(String(dislikeCount + 1));
                            expect(within(upVote).getByRole('button')).toHaveTextContent(String(likeCount - 1));
                        });
                    });

                    it('не происходит редирект', async () => {
                        await makeContext();

                        const schema = makeSchema();

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer()]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        window.location.assign.mockClear();

                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(downVote).getByRole('button'));

                        await waitFor(() => {
                            expect(window.location.assign).toHaveBeenCalledTimes(0);
                        });
                    });
                });

                describe('если пользователь не авторизован', () => {
                    it('происходит редирект', async () => {
                        await makeContext({isAuth: false});

                        const schema = makeSchema({user: {isAuth: false}});

                        await kadavrLayer.setState('schema', schema);
                        await kadavrLayer.setState('storage.modelAnswers', [createAnswer()]);
                        await kadavrLayer.setState('report', createReportProductStateWithPicture(createQuestionProduct()));
                        await apiaryLayer.mountWidget(widgetPath, widgetProps);

                        window.location.assign.mockClear();

                        const downVote = screen.getByRole('down-vote');

                        fireEvent.click(within(downVote).getByRole('button'));

                        await waitFor(() => {
                            expect(window.location.assign).toHaveBeenCalledTimes(1);
                        });
                    });
                });
            });
        });
    });
});
