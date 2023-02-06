import userEvent from '@testing-library/user-event';
import {packFunction} from '@yandex-market/testament/mirror';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {data} from './__mocks__/data';

const {mockIntersectionObserver} = require('@self/root/src/helpers/testament/mock');


const widgetPath = '@self/platform/widgets/content/ProductFiltersEmbedded';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
let kadavrLayer;

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {cookie},
        page: {
            pageId: 'touch:product',
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    jestLayer = mirror.getLayer('jest');
    kadavrLayer = mirror.getLayer('kadavr');
});

afterAll(() => {
    mirror.destroy();
});

describe('Карточка модели.', () => {
    beforeEach(async () => {
        await makeContext();

        await jestLayer.backend.runCode(() => {
            jest.spyOn(require('@self/platform/resolvers/beauty'), 'resolveIsBeautyCategories')
                .mockResolvedValue(false);
        }, []);

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/ProductTableSize'),
            () => ({create: () => Promise.resolve(null)})
        );
    });

    describe('Все предложения. ', () => {
        beforeEach(() => {
            mockIntersectionObserver();
        });

        describe('Фильтры. ', () => {
            test('Отрображаются и переключаются корректно', async () => {
                await apiaryLayer.mountWidget(
                    widgetPath,
                    packFunction(
                        dataMock => ({
                            productId: 1414986413,
                            dataPromise: Promise.resolve(dataMock),
                        }),
                        [data]
                    )
                );
                const user = userEvent.setup();

                const filters = document.querySelectorAll('[data-autotest-id="VisualFilter-enum"]');
                expect(filters).toHaveLength(2);

                const [colorFilter, memoryFilter] = filters;

                expect(colorFilter).toHaveTextContent('Цвет товара:(PRODUCT)RED');
                expect(memoryFilter).toHaveTextContent('Конфигурация памяти:128 ГБ');

                const colorOptions = colorFilter.querySelectorAll('[data-autotest-id="visual-filter-control"] input');
                expect(colorOptions).toHaveLength(6);

                await user.click(colorOptions[1]);
                expect(colorFilter).toHaveTextContent('Цвет товара:Альпийский зеленый');

                const memoryOptions = memoryFilter.querySelectorAll('[data-autotest-id="visual-filter-control"] input');
                expect(memoryOptions).toHaveLength(3);

                await user.click(memoryOptions[1]);
                expect(memoryFilter).toHaveTextContent('Конфигурация памяти:256 ГБ');
            });
        });
    });
});

