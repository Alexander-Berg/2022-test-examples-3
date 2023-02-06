import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {JestLayer} */
let jestLayer;

beforeAll(async () => {
    mirror = await makeMirrorTouch({
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

    await Promise.all([
        jestLayer.doMock(
            require.resolve('@self/platform/resolvers/amp'),
            () => ({
                resolveAmpSku: () => Promise.resolve({result: '123'}),
                resolveAmpSkuFilters: () => Promise.resolve([]),
                resolveAmpSpecsInfo: () => Promise.resolve({
                    values: null,
                    showSpecs: true,
                    isHealthCategory: false,
                    description: 'description',
                    title: 'title',
                    linkText: 'linkText',
                    isShowSpecsDescription: true,
                }),
                resolveAmpMetaInfo: () => Promise.resolve({title: 'title', description: 'description', canonical: 'canonical', images: null}),
            })),
        jestLayer.doMock(
            require.resolve('@self/root/src/resolvers/amp/resolveProductInfoParams'),
            () => ({
                resolveProductInfoParams: () => Promise.resolve({productIds: [345], specificationSet: [{}]}),
            })),
        jestLayer.doMock(
            require.resolve('@self/root/src/resolvers/report'),
            () => ({
                resolveProductInfo: () => Promise.resolve({
                    result: [456],
                    collections: {product: {456: {id: 456, entity: 'product'}}, sku: {123: {id: 123, entity: 'sku'}}},
                }),
            }))]);

    await mandrelLayer.initContext({
        request: {
            params: {
                productId: '456',
                skuId: '123',
            },
        },
    });
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: AmpProductPage', () => {
    it('должен генерировать верные данные', async () => {
        await expect(jestLayer.backend.runCode(async () => {
            const expect = require('expect');
            const makeMockedContext = require('@yandex-market/mandrel/mockedContext');
            const context = makeMockedContext();
            const {default: widgetController} = require('@self/platform/widgets/pages.amp/AmpProductPage/controller');


            const {data} = widgetController(context, {});
            const resultData = await data;
            expect(resultData).toEqual({
                skuId: '123',
                productId: 456,
                currentEntity: 'product',
                filters: [],
                specs: {
                    values: null,
                    showSpecs: true,
                    isHealthCategory: false,
                    description: 'description',
                    title: 'title',
                    linkText: 'linkText',
                    isShowSpecsDescription: true,
                },
                questionsCount: 0,
            });
        }, [])).resolves.not.toThrow();
    });
});
