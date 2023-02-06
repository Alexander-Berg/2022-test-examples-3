import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    checkPopupSteps,
    checkPopupNotShow,
} from './testCases';
import {
    initialMock,
    TEST_CASES,
} from './mockData';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
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

describe('YandexPlusOnboarding', () => {
    describe('Попап с информацией о Яндекс Плюсе', () => {
        beforeAll(async () => {
            await initialMock(jestLayer);
        });

        describe('не отображается', () => {
            TEST_CASES.not_show_cases.map(testCase =>
                it(testCase.caseName, async () => {
                    await checkPopupNotShow(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        testCase.mockData
                    );
                })
            );
        });
        describe('отображается', () => {
            TEST_CASES.show_cases.map(testCase =>
                it(testCase.caseName, async () => {
                    await checkPopupSteps(
                        jestLayer,
                        apiaryLayer,
                        mandrelLayer,
                        testCase.mockData,
                        testCase.onboardingType
                    );
                })
            );
        });
    });
});
