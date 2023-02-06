import {screen} from '@testing-library/dom';

import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {makeMirror} from '@self/platform/helpers/testament';
import {absentPhone as productMock} from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/product';

import {alsoViewedResultMock} from '@self/platform/widgets/content/NotOnSale/__spec__/mocks';

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

const WIDGET_PATH = require.resolve('@self/platform/widgets/content/NotOnSale');

const product = createProduct(productMock, productMock.id);
/**
 * @expFlag dsk_out-of-stock-km_similar
 * @ticket MARKETFRONT-81950
 * @start
 */
const productFarma = createProduct({
    ...productMock,
    specs: {
        internal: [{
            value: 'vidal',
            usedParams: [{name: 'N02BB02'}],
        }],
    },
}, productMock.id);
/**
 * @expFlag dsk_out-of-stock-km_similar
 * @ticket MARKETFRONT-81950
 * @end
 */

async function makeContext({exps = {}} = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
        },
        route: {
            name: 'market:product',
            data: {},
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
            params: {
                productId: productMock.id,
            },
        },
    });
}
// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: NotOnSale', () => {
    const setReportState = async product => {
        await kadavrLayer.setState('report', product);
    };

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Основные тесты', () => {
        test('Должен быть отрендерен', async () => {
            await makeContext();
            await setReportState(product);
            await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(screen.getByRole('not-on-sale')).toBeInTheDocument();
        });
    });

    /**
     * @expFlag dsk_out-of-stock-km_similar
     * @ticket MARKETFRONT-81950
     * @start
     */
    describe('Эксперимент с каруселью похожих товаров: похожих нет', () => {
        test('Если ручка похожих ничего не отдает, показывает кнопку "Искать"', async () => {
            await makeContext({
                exps: {
                    'dsk_out-of-stock-km_similar': true,
                },
            });
            await setReportState(product);

            await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(screen.getByRole('similar-search-link')).toBeInTheDocument();
        });

        /**
         * @expFlag dsk_out-of-stock-km_similar-links
         * @ticket MARKETFRONT-82979
         * @start
         */
        test('Если ручка похожих ничего не отдает, показывает блок ссылок поиска', async () => {
            await makeContext({
                exps: {
                    'dsk_out-of-stock-km_similar': true,
                    'dsk_out-of-stock-km_similar-links': true,
                },
            });
            await setReportState(product);

            await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(screen.getByRole('similar-search-links')).toBeInTheDocument();
        });
        /**
         * @expFlag dsk_out-of-stock-km_similar-links
         * @ticket MARKETFRONT-82979
         * @end
         */
    });

    describe('Эксперимент с каруселью похожих товаров: похожие есть', () => {
        beforeEach(async () => {
            await jestLayer.backend.runCode(alsoViewedResultMock => {
                jest.spyOn(require('@self/root/src/resolvers/alsoViewed/resolveAlsoViewed'), 'resolveAlsoViewed')
                    .mockReturnValue(alsoViewedResultMock);
            }, [alsoViewedResultMock]);
        });

        test('Показывает карусель похожих товаров', async () => {
            await makeContext({
                exps: {
                    'dsk_out-of-stock-km_similar': true,
                },
            });
            await setReportState(product);

            await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(screen.getByRole('similar-items')).toBeInTheDocument();
        });

        test('Не показывает карусель похожих товаров для лекарств', async () => {
            await makeContext({
                exps: {
                    'dsk_out-of-stock-km_similar': true,
                },
            });
            await setReportState(productFarma);

            await apiaryLayer.mountWidget(WIDGET_PATH);
            expect(screen.queryByRole('similar-items')).not.toBeInTheDocument();
        });
    });
    /**
     * @expFlag dsk_out-of-stock-km_similar
     * @ticket MARKETFRONT-81950
     * @end
     */
});
