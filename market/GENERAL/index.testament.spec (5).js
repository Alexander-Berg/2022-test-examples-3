/* eslint-disable market/no-array-constructor-with-spread */
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as locationActions from '@self/project/src/actions/location';
import {screen, waitFor, fireEvent} from '@testing-library/dom';

import {
    UID,
    yandexuid,
    users,
    createQuestion,
    answers,
    commentary,
    DEFAULT_QUESTION_ID,
    prepareAnswers,
} from './__mocks__';
import {createSnippetWithShopState} from './__mocks__/snippetWithShop';

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

async function makeContext(isAuth = false) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId(true)};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            isAuth,
        },
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
        () => ({create: () => Promise.resolve(null)}));

    await jestLayer.doMock(
        require.resolve('@self/project/src/resolvers/request/resolveUserTimezoneOffset'),
        () => () => Promise.resolve(0));

    await jestLayer.doMock(
        require.resolve('@self/root/src/resources/persAuthor/expertise/fetchExpertiseDictionary'),
        () => ({fetchExpertiseDictionary: () => Promise.resolve({result: [], collections: {}})}));

    await jestLayer.runCode(() => {
        const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
        mockRouterFabric()({
            'touch:category-question-answer': ({answerId}) => `/category-question-answer/${answerId}`,
        });
    }, []);

    await jestLayer.runCode(() => {
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter({
            'touch:category-question-answer': ({answerId}) => `/category-question-answer/${answerId}`,
            'external:clickdaemon': ({url, tld}) => `/external-clickdaemon${url}/${tld}`,
        });
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

const testWidget = (params, isAuth) => {
    beforeEach(async () => {
        await makeContext(isAuth);
    });
    test('По умолчанию ведёт на страницу конкретного ответа и содержит корректный текст.', async () => {
        await kadavrLayer.setState('schema', {
            users,
            modelQuestions: [createQuestion({
                answersCount: 2,
                canDelete: true,
            })],
            modelAnswers: answers,
        });
        await kadavrLayer.setState('storage', {
            commentary,
        });
        await apiaryLayer.mountWidget(widgetPath, {
            entity: 'answerComment',
            questionId: DEFAULT_QUESTION_ID,
        });
        const link = screen.getAllByTestId('answer-footer-link')[params.answerIndex];
        expect(link).toHaveAttribute('href', expect.stringMatching(`/category-question-answer/${params.answerId}`));
        expect(link).toHaveTextContent(params.textContent);
    });
};
describe('Блок ответа.', () => {
    describe('Пользователь авторизован.', () => {
        describe('С комментариями.', () => {
            testWidget({answerId: 1, answerIndex: 0, textContent: '2 комментария'}, true);
        });
        describe('Без комментариев.', () => {
            testWidget({answerId: 2, answerIndex: 1, textContent: 'Комментировать'}, true);
        });
    });
    describe('Пользователь не авторизован.', () => {
        describe('С комментариями.', () => {
            testWidget({answerId: 1, answerIndex: 0, textContent: '2 комментария'}, false);
        });
        describe('Без комментариев.', () => {
            testWidget({answerId: 2, answerIndex: 1, textContent: 'Комментировать'}, false);
        });
        test('Ссылка "Читать дальше" при клике по ссылке происходит переход на страницу ответа', async () => {
            await makeContext();
            await kadavrLayer.setState('schema', {
                users,
                modelQuestions: [createQuestion({
                    answersCount: 1,
                    canDelete: true,
                })],
                modelAnswers: prepareAnswers(1, 'А кнопочка работает. '.repeat(50)),
            });
            await kadavrLayer.setState('storage', {
                commentary,
            });
            await apiaryLayer.mountWidget(widgetPath, {
                entity: 'answerComment',
                questionId: DEFAULT_QUESTION_ID,
            });
            const locationChangeSpy = jest.spyOn(locationActions, 'changeLocationByPageId');
            fireEvent.click(screen.getByRole('button', {name: /читать дальше/i}));
            await waitFor(() => {
                expect(locationChangeSpy).toHaveBeenCalledWith('touch:product-question-answer', {answerId: 1});
            });
        });
    });
});
describe('Блок снипета ответа', () => {
    beforeEach(async () => {
        await makeContext();
    });
    describe('c ответом от магазина.', () => {
        describe('Блок CPC оффера.', () => {
            beforeEach(async () => {
                const {reportState, schemaState, data} = await createSnippetWithShopState(true);
                await kadavrLayer.setState('report', reportState);
                await kadavrLayer.setState('schema', schemaState);
                await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
                await apiaryLayer.mountWidget(widgetPath, {
                    questionId: data.question.id,
                    productId: data.product.id,
                });
            });
            test('По умолчанию оффер отображается', async () => {
                await waitFor(async () => {
                    expect(screen.queryByTestId('offer-snippet')).toBeVisible();
                });
            });
            test('По умолчанию ссылка в магазин корректная', async () => {
                const link = screen.getByRole('link', {name: /в магазин/i});
                expect(link).toHaveAttribute('href', expect.stringMatching('/external-clickdaemon/offer-link/ru'));
            });
            test('Цена должна отображаться', async () => {
                const price = screen.getByTestId('price');
                expect(price.textContent).toContain('1 000 ₽');
            });
        });
    });
    describe('c ответом от магазина без оффера.', () => {
        describe('Блок CPC оффера.', () => {
            beforeEach(async () => {
                const {reportState, schemaState, data} = await createSnippetWithShopState(false);
                await kadavrLayer.setState('report', reportState);
                await kadavrLayer.setState('schema', schemaState);
                await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
                await apiaryLayer.mountWidget(widgetPath, {
                    questionId: data.question.id,
                    productId: data.product.id,
                });
            });
            test('По умолчанию не содержит оффера', async () => {
                await waitFor(async () => {
                    expect(screen.queryByTestId('offer-snippet')).toBeNull();
                });
            });
        });
    });
});
