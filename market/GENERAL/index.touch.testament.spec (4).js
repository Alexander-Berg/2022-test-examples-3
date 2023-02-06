import {makeMirror} from '@self/platform/helpers/testament';
import expect from 'expect';
import {backendMock, bnplInfoOnePlan} from './mock';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;

const checkControllerResultData = async controllerParams => {
    const data = await jestLayer.backend.runCode(async controllerParams => {
        const {default: controller} = require('../controller');
        // eslint-disable-next-line no-undef
        const context = getBackend('mandrel')?.getContext();
        const {data} = controller(context, controllerParams);
        return data;
    }, [controllerParams]);
    await expect(data).toMatchSnapshot();
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
    await mandrelLayer.initContext();
});

afterAll(() => {
    mirror.destroy();
});

const params = {
    offerId: '123',
    wareId: '324',
    price: {value: 3000},
    regionId: '213',
    creditInfo: null,
    installmentInfo: null,
};

describe('FinancialProduct', () => {
    describe('Контроллер', () => {
        it('Нет фин предложений', async () => {
            await jestLayer.backend.runCode(backendMock, []);
            await checkControllerResultData({
                ...params,
                yandexBnplInfo: null,
            });
        });
        describe('BNPL', () => {
            it('Резолвер вернул планы', async () => {
                await jestLayer.backend.runCode(backendMock, [bnplInfoOnePlan, {BNPL: true}]);
                await checkControllerResultData({
                    ...params,
                    yandexBnplInfo: {enabled: true},
                });
            });
            it('Резолвер ничего не вернул', async () => {
                await jestLayer.backend.runCode(backendMock, [null, {BNPL: true}]);
                await checkControllerResultData({
                    ...params,
                    yandexBnplInfo: {enabled: true},
                });
            });
            it('Резолвер вернул ошибку', async () => {
                await jestLayer.backend.runCode(backendMock, ['error', {BNPL: true}]);
                await checkControllerResultData({
                    ...params,
                    yandexBnplInfo: {enabled: true},
                });
            });
        });
    });
});
