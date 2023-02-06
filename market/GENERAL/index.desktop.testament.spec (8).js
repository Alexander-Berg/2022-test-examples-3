import {makeMirror} from '@self/platform/helpers/testament';
import {baseMockFunctionality, bnplErrorMockFunctionality} from '@self/platform/spec/testament/FinancialProduct/mockFunctionality';
import * as mocks from '@self/platform/spec/testament/FinancialProduct/mockData';
import expect from 'expect';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;

const checkControllerResultData = async (pageId, reqParams, expectResult, mockFn) => {
    await mandrelLayer.initContext({
        request: {params: reqParams},
        route: {name: pageId},
    });

    if (mockFn) {
        await jestLayer.backend.runCode(mockFn, [mocks]);
    }

    await expect(jestLayer.backend.runCode(async expectResult => {
        const expect = require('expect');
        const makeMockedContext = require('@yandex-market/mandrel/mockedContext');
        const context = makeMockedContext();
        const {default: widgetController, FinancialProductError} = require('@self/platform/widgets/content/FinancialProduct/controller');

        if (expectResult === 'error') {
            expect(() => widgetController(context, {})).toThrow(FinancialProductError);
        } else {
            const {data} = widgetController(context, {});
            const resultData = await data;
            expect(resultData).toEqual(expectResult);
        }
    }, [expectResult instanceof Error ? 'error' : expectResult])).resolves.not.toThrow();
};

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');

    await jestLayer.backend.runCode(baseMockFunctionality, [mocks]);
});

afterAll(() => {
    mirror.destroy();
});

describe('FinancialProduct', () => {
    describe('Контроллер', () => {
        describe('для корректных данных ', function () {
            test('OfferPage', () => checkControllerResultData(
                mocks.page.id.offer,
                mocks.params,
                {
                    ...mocks.result,
                    ...mocks.restResult,
                    pageId: 'market:offer',
                    isRedesignedSnippetsExp: false,
                    isCreditBrokerExp: false,
                }
            ));

            test('ProductPage', () => checkControllerResultData(
                mocks.page.id.product,
                mocks.params,
                {
                    ...mocks.productResult,
                    ...mocks.restResult,
                    pageId: 'market:product',
                    isRedesignedSnippetsExp: false,
                    isCreditBrokerExp: false,
                }
            ));
        });

        describe('при ошибке bnpl', function () {
            test('OfferPage', () => checkControllerResultData(
                mocks.page.id.offer,
                mocks.params,
                {
                    ...mocks.bnplResult,
                    pageId: 'market:offer',
                    isRedesignedSnippetsExp: false,
                    isCreditBrokerExp: false,
                },
                bnplErrorMockFunctionality
            ));

            test('ProductPage', () => checkControllerResultData(
                mocks.page.id.product,
                mocks.params,
                {
                    ...mocks.productBnplResult,
                    ...mocks.restResult,
                    pageId: 'market:product',
                    isRedesignedSnippetsExp: false,
                    isCreditBrokerExp: false,
                },
                bnplErrorMockFunctionality
            ));
        });

        describe('Другая страница', () => {
            test('ошибка', () => checkControllerResultData(
                mocks.page.id.fake,
                mocks.params,
                new Error()
            ));
        });
    });
});
