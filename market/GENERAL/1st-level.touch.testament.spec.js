import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import commentaryFormat from '@yandex-market/microformats/commentary/commentary.json';
import {fireEvent, getByText, screen, waitFor} from '@testing-library/dom';
import {pluralize} from '@self/root/src/helpers/string';

import {singleCommentaryMock, usersMock} from './__mocks__';
import {newComments} from './__mocks__/helpers';

const State = require('@yandex-market/kadavr/dist/state').default;

const widgetPath = '../';

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

const DEFAULT_LIMIT = 5;
const DEFAULT_REVIEW_ID = 1;

async function makeContext(user = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user,
        request: {
            cookie,
            params: {
                // Параметр который ждет контроллер
                productId: 1,
            },
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mockLocation();
    mirror = await makeMirrorTouch({
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
});

beforeAll(async () => {
    await jestLayer.doMock(
        require.resolve('@self/platform/widgets/content/BelarusBoundPhoneDialog'),
        () => ({create: () => Promise.resolve(null)})
    );
});

afterAll(() => {
    mirror.destroy();
});

describe('Комментарии с двумя уровнями.', () => {
    beforeEach(async () => {
        await makeContext();
    });
    describe('1ый уровень', () => {
        describe('Сниппет комментария. Когда у юзера задан displayName".', () => {
            describe('По умолчанию', () => {
                test('у автора отображается publicDisplayName', async () => {
                    await kadavrLayer.setState('schema', {commentary: singleCommentaryMock, users: usersMock});
                    await apiaryLayer.mountWidget(widgetPath, {
                        entity: 'shopReviewComment',
                        entityId: 1,
                    });
                    const userName = screen.getByTestId('public-user-info').textContent;
                    return expect(userName).toEqual('Vasya P.', 'Имя пользователя отображается корректно');
                });
            });
        });

        describe('Форма оставления комментария. Для авторизованного пользователя', () => {
            test('При вводе 1001-го символа', async () => {
                await kadavrLayer.setState('schema', {commentary: singleCommentaryMock, users: usersMock});
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });

                const bigForm = screen.getByTestId('comment-big-form-authorized');

                await step('Поле ввода комментария должно отображаться', async () => {
                    expect(bigForm).not.toBeNull();
                });

                const textField = bigForm.querySelector('div:first-child');
                const textArea = textField.querySelector('textarea');
                fireEvent.change(textArea, {target: {value: 'a'.repeat(1000)}});
                const counter = textField.children[1];
                fireEvent.change(textArea, {target: {value: 'a'.repeat(1001)}});

                return expect(counter.textContent).toEqual('1001 / 2000', 'счетчик символов увеличивается на 1');
            });
            test('При удалении 1002-го символа', async () => {
                await kadavrLayer.setState('schema', {commentary: singleCommentaryMock, users: usersMock});
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });

                const bigForm = screen.getByTestId('comment-big-form-authorized');

                await step('Поле ввода комментария должно отображаться', async () => {
                    expect(bigForm).not.toBeNull();
                });

                const textField = bigForm.querySelector('div:first-child');
                const textArea = textField.querySelector('textarea');
                fireEvent.change(textArea, {target: {value: 'a'.repeat(1002)}});
                const counter = textField.children[1];
                fireEvent.change(textArea, {target: {value: 'a'.repeat(1001)}});

                return expect(counter.textContent).toEqual('1001 / 2000', 'счетчик символов уменьшается на 1');
            });
        });
        describe('Для неавторизованного пользователя', () => {
            beforeEach(async () => {
                await jestLayer.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/user'), 'resolveCurrentUserDenormalizedSync')
                        .mockReturnValue({});
                }, []);
            });
            test('По умолчанию приглашение авторизоваться отображается', async () => {
                await kadavrLayer.setState('schema', {commentary: singleCommentaryMock});
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });

                return expect(screen.queryByTestId('comment-big-form-authorize-button')).toBeTruthy();
            });
        });
        describe('Список сниппетов комментариев. Когда комментариев больше дефолтного значения', () => {
            beforeEach(async () => {
                const commentaryMock = [...newComments({
                    count: DEFAULT_LIMIT,
                    text: 'a'.repeat(50),
                    entityId: DEFAULT_REVIEW_ID,
                }), {
                    id: DEFAULT_LIMIT + 1,
                    text: 'b'.repeat(50),
                    state: 'NEW',
                    entityId: DEFAULT_REVIEW_ID,
                }];
                const commentaries = commentaryMock.map(item => State.fake({schema: commentaryFormat}, item));
                await kadavrLayer.setState('schema', {users: usersMock});
                await kadavrLayer.setState('storage', {commentary: commentaries});
            });
            test('Количество комментариев по умолчанию отображается верное', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                const count = DEFAULT_LIMIT + 1;

                screen.debug(undefined, Number.MAX_SAFE_INTEGER);
                const title = screen.getByTestId('commentaries-title');
                const expectedText = `${count}\xa0${pluralize(count, 'комментарий', 'комментария', 'комментариев')}`;
                return expect(title.textContent).toEqual(expectedText, 'Верное количество комментариев');
            });
            test('Кнопка "показать еще" по умолчанию отображается', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                const caption = `Ещё ${1} ${pluralize(1, 'комментарий', 'комментария', 'комментариев')}`;
                return expect(screen.queryByText(caption)).toBeTruthy();
            });
            test('Первый комментарий по умолчанию отображается верный', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                const firstCommentSnippet = screen.getAllByTestId('comment-snippet')[0];
                return expect(firstCommentSnippet.getAttribute('data-comment-id')).toEqual('1', 'Первый комментарий отображается верный');
            });
            test('При клике "Показать ещё" отображаются оставшиеся комментарии', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });

                await step('Кликаем на кнопку "Показать ещё"', async () => {
                    const caption = `Ещё ${1} ${pluralize(1, 'комментарий', 'комментария', 'комментариев')}`;
                    getByText(container, caption).click();
                });

                await waitFor(async () => {
                    await expect(screen.findAllByTestId('comment-snippet')).resolves.toHaveLength(6, 'Кнопка "показать еще" загрузила оставшиеся комментарии');
                });
            });
        });
        describe('Список сниппетов комментариев. Когда комментариев меньше или равно дефолтному значению', () => {
            beforeEach(async () => {
                const commentaryMock = [...newComments({
                    count: DEFAULT_LIMIT,
                    text: 'a'.repeat(50),
                    entityId: DEFAULT_REVIEW_ID,
                })];
                const commentaries = commentaryMock.map(item => State.fake({schema: commentaryFormat}, item));
                await kadavrLayer.setState('schema', {users: usersMock});
                await kadavrLayer.setState('storage', {commentary: commentaries});
            });
            test('Кнопка "показать еще" по умолчанию не отображается', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                const showMoreButton = screen.queryByTestId('commentaries-show-more-button');
                return expect(showMoreButton).toBeFalsy();
            });
        });
    });
});
