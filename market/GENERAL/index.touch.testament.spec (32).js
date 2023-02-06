import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import * as actions from '../actions';

import {checkButtonClick, checkButton, checkTitle} from './testCases';
import {TEST_SUITES} from './suites';
import {prepareData} from './mockFunctionality';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
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
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('Глобальный пульт управления чекаутом', () => {
    describe.each(TEST_SUITES)('%s', (postfix, lastState, testData) => {
        // eslint-disable-next-line jest/valid-describe
        describe('Компонент инициализируется', () => {
            let changeGlobalDelivery;
            afterAll(() => {
                changeGlobalDelivery.mockClear();
            });

            beforeAll(async () => {
                await makeContext({});

                await prepareData(jestLayer, lastState);

                changeGlobalDelivery = jest.spyOn(actions, 'changeGlobalDelivery');

                await apiaryLayer.mountWidget('@self/root/src/widgets/content/checkout/common/CheckoutControlPanel', {});
            });

            test('кнопка выбора доступна', async () => {
                checkButton(testData.button);
            });

            if (lastState.presetGlobal?.addressId || lastState.presetGlobal?.outletId) {
                test('отображается верный заголовок', async () => {
                    checkTitle(lastState, testData);
                });
            }

            test('клик по кнопке инициализирует редактирование', async () => {
                checkButtonClick(changeGlobalDelivery);
            });
        });
    });
});
