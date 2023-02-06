// @flow
// flowlint untyped-import:off
import {screen, waitFor, fireEvent} from '@testing-library/dom';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import ContentUserInfo from '@self/root/src/components/ContentUserInfo/__pageObject';
import * as voteQuestionActions from '@self/platform/actions/questionsAnswers/question';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import QuestionSnippet from '@self/platform/spec/page-objects/components/Questions/QuestionSnippet';
import NoQuestions from '@self/platform/spec/page-objects/components/CategoryQuestions/NoQuestions';

import type {Config} from '../controller';
import {user} from './__mocks__/user';
import {categoryId, questionText, createCategoryQuestions} from './__mocks__/questions';
import {products} from './__mocks__/products';


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

const widgetPath = require.resolve('@self/platform/widgets/parts/QuestionsLayout/QuestionList');

beforeAll(async () => {
    mockLocation();

    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {asLibrary: true},
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(
        require.resolve('@self/project/src/utils/router'),
        () => ({buildURL: pageId => pageId, buildUrl: pageId => pageId})
    );
});

async function makeContext(userParam) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};
    return mandrelLayer.initContext({user: userParam, request: {cookie}});
}

afterAll(() => {
    mirror.destroy();
});

describe('QuestionList', () => {
    const widgetProps: Config = {hid: categoryId};

    describe.each([
        ['аввторизован', user, 'меняется', '8', 'не происходит', 0],
        ['не авторизован', {isAuth: false}, 'не меняется', '7', 'происходит', 1],
    ])('пользователь %s', (
        _,
        userParam,
        likesExpectations,
        likesExpectedCount,
        redirectExpectations,
        redirectExpectedCount
    ) => {
        describe('вопросы отсутствуют', () => {
            let container: HTMLElement;
            beforeEach(async () => {
                await makeContext(userParam);
                await kadavrLayer.setState('report', products);
                await kadavrLayer.setState('schema', {users: [user]});
                await kadavrLayer.setState('storage.modelQuestions', createCategoryQuestions(0));
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                container = _container;
            });

            it('отображаться заголовок', async () => {
                expect(container.querySelector(NoQuestions.root)).toBeInTheDocument();
            });

            it('нет ни одного снипета вопроса', async () => {
                expect(container.querySelectorAll(QuestionSnippet.root)).toHaveLength(0);
            });
        });

        describe('меньше 10 вопросов', () => {
            let container: HTMLElement;
            const questionsCount = 9;
            beforeEach(async () => {
                await makeContext(userParam);
                await kadavrLayer.setState('report', products);
                await kadavrLayer.setState('schema', {users: [user]});
                await kadavrLayer.setState('storage.modelQuestions', createCategoryQuestions(questionsCount));
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                container = _container;
            });

            it('отображается верное кол-во вопросов', async () => {
                expect(container.querySelectorAll(QuestionSnippet.root)).toHaveLength(questionsCount);
            });
        });

        describe('больше 10 вопросов', () => {
            let container: HTMLElement;
            const questionsCount = 15;
            beforeEach(async () => {
                await makeContext(userParam);
                await kadavrLayer.setState('report', products);
                await kadavrLayer.setState('schema', {users: [user]});
                await kadavrLayer.setState('storage.modelQuestions', createCategoryQuestions(questionsCount));
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                container = _container;
            });

            it('отображается 10 вопросов', () => {
                expect(container.querySelectorAll(QuestionSnippet.root)).toHaveLength(10);
            });
        });

        describe('сниппет вопроса на товар', () => {
            const questionsCount = 9;
            const questionsState = createCategoryQuestions(questionsCount);
            const links = {headerLink: 'market:category-questions', questionLink: 'market:category-question'};

            let container: HTMLElement;
            const addVoteQuestionSpy = jest.spyOn(voteQuestionActions, 'addVoteQuestion');

            beforeEach(async () => {
                await makeContext(userParam);
                await kadavrLayer.setState('report', products);
                await kadavrLayer.setState('schema', {users: [user]});
                await kadavrLayer.setState('storage.modelQuestions', questionsState);
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                container = _container;
            });

            describe('информация о пользователе', () => {
                it('содержит аватарку пользователя', async () => {
                    expect(container.querySelector(ContentUserInfo.root)).toBeTruthy();
                });

                it('содержит имя пользователя', async () => {
                    expect(screen.queryAllByText(user.display_name.public_name)).toHaveLength(questionsCount);
                });

                it('содержит дату создания', async () => {
                    expect(screen.queryAllByText('Год назад')).toHaveLength(questionsCount);
                });
            });

            describe('содержимое сниппета вопроса', () => {
                it('содержит текст вопроса', async () => {
                    expect(container).toHaveTextContent(questionText);
                });

                it('содержит ссылку на ответы', async () => {
                    const textLink = screen.getAllByText(questionText);

                    expect(textLink[0]).toHaveAttribute('href', links.questionLink);
                });
            });

            describe('футер', () => {
                it('содержит информацию о кол-ве ответов', async () => {
                    expect(container).toHaveTextContent('Ещё 7 ответов');
                });

                it('содержит ссылку на ответы', async () => {
                    expect(container.querySelector(QuestionSnippet.answerLink)).toHaveAttribute('href', links.questionLink);
                });

                it('содержит кол-во лайков', async () => {
                    expect(container.querySelector(Votes.UpVote)).toHaveTextContent('7');
                });

                describe('при клике', () => {
                    it(`${likesExpectations} кол-во лайков`, async () => {
                        fireEvent.click(container.querySelector(`${Votes.UpVote} button`));

                        await waitFor(() => {
                            expect(addVoteQuestionSpy).toHaveBeenCalledWith({questionId: 0});
                        });

                        await waitFor(() => {
                            expect(container.querySelector(`${Votes.UpVote} button`)).toHaveTextContent(likesExpectedCount);
                        });

                        fireEvent.click(container.querySelector(`${Votes.UpVote} button`));

                        await waitFor(() => {
                            expect(container.querySelector(`${Votes.UpVote} button`)).toHaveTextContent('7');
                        });
                    });

                    it(`${redirectExpectations} редирект`, async () => {
                        window.location.assign.mockClear();

                        fireEvent.click(container.querySelector(`${Votes.UpVote} button`));

                        await waitFor(() => {
                            expect(window.location.assign).toHaveBeenCalledTimes(redirectExpectedCount);
                        });
                    });
                });
            });
        });
    });
});
