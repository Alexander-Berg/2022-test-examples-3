import {packFunction} from '@yandex-market/testament/mirror';
import {makeMirror} from '@self/platform/helpers/testament';

import {screen} from '@testing-library/dom';
import {
    dataPromiseMock,
    reviewFiltersMock,
    reviewsOpinionsMock,
} from './__mock__';


// путь к виджету который тестируем
const WIDGET_PATH = '@self/platform/widgets/parts/ProductReviews';

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

// Хелпер для инициализации контекста мандреля. Вызывать либо в самом тесте, либо в beforeEach.
async function makeContext() {
    return mandrelLayer.initContext({
        request: {
            params: {
                page: 0,
                productId: '12259971',
            },
            cookie: {kadavr_session_id: await kadavrLayer.getSessionId()},
        },
    });
}

beforeAll(async () => {
    process.env = {NODE_ENV: 'development'};
    mirror = await makeMirror({
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

    await kadavrLayer.setState('storage.modelOpinions', reviewsOpinionsMock);
});

afterAll(() => {
    // одной командой убиваем все моки
    mirror.destroy();
});

describe('Тач. Страница "Отзывы" карточки модели. Блок отзыва.', () => {
    beforeAll(async () => {
        await jestLayer.backend.runCode((
            expertiseResolvers,
            filterResolvers,
            reviewFiltersMock,
            userExpertiseResolvers,
            fallbackResolver,
            pageResolvers
        ) => {
            jest.doMock(expertiseResolvers, () => ({
                __esModule: true,
                resolveExpertiseDictionary: () => Promise.resolve({result: [], collections: {espertise: {}}}),
            }));
            jest.doMock(filterResolvers, () => ({
                __esModule: true,
                resolveCurrentReviewFiltersSync: () => reviewFiltersMock,
            }));
            jest.doMock(userExpertiseResolvers, () => ({
                __esModule: true,
                resolveUsersExpertiseByHidBulk: () => Promise.resolve({}),
            }));
            jest.doMock(fallbackResolver, () => ({
                default: () => null,
            }));
            jest.doMock(pageResolvers, () => ({
                __esModule: true,
                resolvePageIdSync: () => 'touch:product-reviews',
            }));
        }, [
            require.resolve('@self/root/src/resolvers/expertise'),
            require.resolve('@self/platform/resolvers/filter'),
            reviewFiltersMock,
            require.resolve('@self/root/src/resolvers/userExpertise'),
            require.resolve('@self/root/src/resolvers/logFallBackSucceed'),
            require.resolve('@yandex-market/mandrel/resolvers/page'),
        ]);
    });

    beforeEach(async () => {
        await makeContext({});
    });

    const mountOptions = packFunction(options => ({
        dataPromise: Promise.resolve(options),
    }), [dataPromiseMock]);
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    it.skip('Блок с отзывом на продукт. Кнопка "Ответить". По умолчанию должна содержать ссылку на страницу с этим отзывом', async () => {
        await apiaryLayer.mountWidget(WIDGET_PATH, mountOptions);

        const buttonHref = screen.getByText(/Ответить/i).parentElement.href;
        expect(buttonHref).toEqual('touch:product-review_{\"productId\":12259971,\"reviewId\":\"58206981\",\"slug\":\"telefon-samsung-galaxy-s6-edge-32gb-isscu\"}');
    });

    it('Блок с раскрывающимся контентом отзыва. По умолчанию должен содержать кнопку "Читать полностью"', async () => {
        await apiaryLayer.mountWidget(WIDGET_PATH, mountOptions);

        expect(screen.getByText('Читать полностью')).toBeInTheDocument();
    });

    it('Блок с отзывом от стороннего поставщика. Текст с информацией про источник отзыва. По умолчанию должен отображаться', async () => {
        await apiaryLayer.mountWidget(WIDGET_PATH, mountOptions);

        expect(screen.getByText('Отзыв предоставлен производителем testProvider')).toBeInTheDocument();
    });
});
