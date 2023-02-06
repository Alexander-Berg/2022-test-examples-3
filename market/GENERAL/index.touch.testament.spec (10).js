import {screen, queryByText} from '@testing-library/dom';
import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror.js';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

// mocks
import {
    baseMock,
    gridWithVendor,
    gridWithoutVendor,
    gridWithVendorByCategory,
    gridWithoutVendorByCategory,
    listWithVendor,
    listWithoutVendor,
    listWithReview,
    HID,
    SEARCH_TEXT,
} from './mocks';
import SearchResults from '../__pageObject';

const WIDGET_PATH = '@self/platform/widgets/content/SearchResults';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

const makeContext = async params => mandrelLayer.initContext({
    request: {
        params: {
            text: params.text,
            'show-reviews': params['show-reviews'],
            hid: HID,
        },
    },
});

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    jestLayer = mirror.getLayer('jest');
    apiaryLayer = mirror.getLayer('apiary');
});

// выполняется после всех тестов
afterAll(() => {
    mirror.destroy();
});

// сам тест
describe('Widget: SearchResults.', () => {
    describe('Поисковая выдача. Косметические товары.', () => {
        beforeEach(async () => makeContext({text: SEARCH_TEXT}));

        describe('Grid. Производитель', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(baseMock, []);
            });

            test('указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(gridWithVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).not.toBeNull();
                expect(queryByText(container, 'Power 10 Formula one Shot PO Cream Крем для лица освежающий')).not.toBeNull();
            });

            test('не указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(gridWithoutVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).toBeNull();
                expect(queryByText(container, /Power 10 Formula one Shot PO/i)).not.toBeNull();
            });

            test('указывается. Скриншот должен совпадать', async () => {
                await jestLayer.backend.runCode(gridWithVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const root = container.querySelector(SearchResults.root);
                expect(root).toMatchSnapshot();
            });
        });

        describe('List. Производитель', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(baseMock, []);
            });

            test('указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(listWithVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).not.toBeNull();
                expect(queryByText(container, 'Power 10 Formula one Shot PO Cream Крем для лица освежающий')).not.toBeNull();
            });

            test('не указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(listWithoutVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).toBeNull();
                expect(queryByText(container, /Power 10 Formula one Shot PO/i)).not.toBeNull();
            });

            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            test.skip('указывается. Скриншот должен совпадать', async () => {
                await jestLayer.backend.runCode(listWithVendor, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const root = container.querySelector(SearchResults.root);
                expect(root).toMatchSnapshot();
            });
        });
    });

    describe('Категорийная выдача. Косметические товары.', () => {
        beforeEach(async () => makeContext({text: ''}));

        describe('Grid. Производитель', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(baseMock, []);
            });

            test('указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(gridWithVendorByCategory, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).not.toBeNull();
                expect(queryByText(container, 'Power 10 Formula one Shot PO Cream Крем для лица освежающий')).not.toBeNull();
            });

            test('не указывается отдельным полем над названием продукта.', async () => {
                await jestLayer.backend.runCode(gridWithoutVendorByCategory, []);
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {});
                expect(queryByText(container, 'TEST VENDOR')).toBeNull();
                expect(queryByText(container, /Power 10 Formula one Shot PO/i)).not.toBeNull();
            });
        });
    });

    describe('Поисковая выдача. Товары с отзывами.', () => {
        beforeEach(async () => makeContext({text: 'Телефоны Samsung', 'show-reviews': 1}));

        describe('Сниппет с отзывом', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(baseMock, []);
            });

            test('Кнопка "Ответить" должна содержать ссылку на страницу с этим отзывом.', async () => {
                await jestLayer.backend.runCode(listWithReview, []);
                await apiaryLayer.mountWidget(WIDGET_PATH, {});

                const urlParams = JSON.stringify({
                    productId: 1,
                    reviewId: 3,
                    slug: 'it-s-skin-power-10-formula-one-shot-po-cream-krem-dlia-litsa-osvezhaiushchii',
                });
                expect(screen.getByRole('link', {name: 'Ответить'})).toHaveAttribute('href', `touch:product-review_${urlParams}`);
            });

            test('Кнопка "Читать все отзывы" должна содержать ссылку на страницу всех отзывов.', async () => {
                await jestLayer.backend.runCode(listWithReview, []);
                await apiaryLayer.mountWidget(WIDGET_PATH, {});

                const urlParams = JSON.stringify({
                    productId: '1',
                    slug: 'it-s-skin-power-10-formula-one-shot-po-cream-krem-dlia-litsa-osvezhaiushchii',
                });
                expect(screen.getByRole('link', {name: 'Читать все отзывы'})).toHaveAttribute('href', `touch:product-reviews_${urlParams}`);
            });
        });
    });
});
